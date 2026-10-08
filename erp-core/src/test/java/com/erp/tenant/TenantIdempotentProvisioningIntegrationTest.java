package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.idempotency.IdempotencyKeyRetentionJob;
import com.erp.common.idempotency.IdempotentResponses;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * tenant-maturity C4 — {@code Idempotency-Key} on {@code POST /api/v1/platform/tenants} over real HTTP (REQ-TENANT-036,
 * RULE-TENANT-025 / -026, ADR-TENANT-003): replay, conflict, invalid keys, failures not stored, the header absent,
 * concurrent first requests, expiry and the retention job, and the key's tenant scope. Every tenant code is fresh.
 */
class TenantIdempotentProvisioningIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANTS = "/api/v1/platform/tenants";
    private static final String KEY = IdempotentResponses.IDEMPOTENCY_KEY_HEADER;
    private static final String REPLAYED = IdempotentResponses.REPLAYED_HEADER;
    private static final String ENDPOINT = "POST /api/v1/platform/tenants";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private IdempotencyKeyRetentionJob retentionJob;

    private TenantHttp http;
    private String operator;
    private String platformToken;

    @BeforeEach
    void platformOperatorLogsIn() {
        http = new TenantHttp(port);
        operator = TenantHttp.platformOperator(jdbcTemplate, passwordEncoder);
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE, operator);
    }

    @Test
    void sameKeyAndBody_replaysTheStoredAnswer_markedReplayed_evenInAnotherFieldOrder_andCreatesNothing() {
        String code = TenantHttp.unique("IDA");
        String key = newKey();
        String body = TenantHttp.createTenantBody(code);

        HttpResponse<String> first = create(key, body);
        HttpResponse<String> second = create(key, body);
        HttpResponse<String> reordered = create(key, reorderedBody(code));

        assertThat(first.statusCode()).as(first.body()).isEqualTo(201);
        assertThat(first.headers().firstValue(REPLAYED)).as("a first answer is not a replay").isEmpty();
        for (HttpResponse<String> replay : List.of(second, reordered)) {
            assertThat(replay.statusCode()).as(replay.body()).isEqualTo(201);
            assertThat(replay.headers().firstValue(REPLAYED)).contains("true");
            assertThat((Map<?, ?>) JsonPath.read(replay.body(), "$.data")).isEqualTo(JsonPath.read(first.body(), "$.data"));
            assertThat((String) JsonPath.read(replay.body(), "$.timestamp"))
                .isEqualTo(JsonPath.read(first.body(), "$.timestamp"));
        }
        long id = ((Number) JsonPath.read(first.body(), "$.data.id")).longValue();
        assertThat(count("SELECT COUNT(*) FROM CORE_TENANT WHERE CODE = ?", code)).as("one tenant").isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM SEC_USER WHERE TENANT_ID = ?", id)).as("one administrator").isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT TENANT_ID, ENDPOINT, REQUEST_HASH, RESPONSE_STATUS,"
            + " RESPONSE_BODY, CREATED_BY FROM CORE_IDEMPOTENCY_KEY WHERE IDEMPOTENCY_KEY = ?", key);
        assertThat(row).containsEntry("tenant_id", TenantConstants.PLATFORM_TENANT_ID).containsEntry("endpoint", ENDPOINT)
            .containsEntry("response_status", 201).containsEntry("created_by", operator);
        assertThat((String) row.get("request_hash")).matches("[0-9a-f]{64}");
        assertThat((String) row.get("response_body")).contains(code).doesNotContain(TenantHttp.PASSWORD);
        assertThat(first.body() + second.body() + reordered.body()).doesNotContain(TenantHttp.PASSWORD);
    }

    @Test
    void sameKeyWithAnotherBody_orByAnotherUser_is409IdempotencyKeyConflict_andCreatesNothing() {
        String code = TenantHttp.unique("IDB");
        String other = TenantHttp.unique("IDC");
        String key = newKey();
        assertThat(create(key, TenantHttp.createTenantBody(code)).statusCode()).isEqualTo(201);

        HttpResponse<String> anotherBody = create(key, TenantHttp.createTenantBody(other));
        assertThat(anotherBody.statusCode()).as(anotherBody.body()).isEqualTo(409);
        assertThat(TenantHttp.errorCode(anotherBody)).isEqualTo("IDEMPOTENCY_KEY_CONFLICT");
        assertThat(anotherBody.headers().firstValue(REPLAYED)).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM CORE_TENANT WHERE CODE = ?", other)).isZero();

        String secondOperator = http.token(TenantConstants.PLATFORM_TENANT_CODE,
            TenantHttp.anotherPlatformOperator(jdbcTemplate, passwordEncoder));
        HttpResponse<String> anotherUser = http.post(secondOperator, TENANTS, TenantHttp.createTenantBody(code), KEY, key);
        assertThat(anotherUser.statusCode()).as("never another user's stored answer").isEqualTo(409);
        assertThat(TenantHttp.errorCode(anotherUser)).isEqualTo("IDEMPOTENCY_KEY_CONFLICT");

        HttpResponse<String> replay = create(key, TenantHttp.createTenantBody(code));
        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(replay.headers().firstValue(REPLAYED)).contains("true");
        assertThat(count("SELECT COUNT(*) FROM CORE_IDEMPOTENCY_KEY WHERE IDEMPOTENCY_KEY = ?", key)).isEqualTo(1);
    }

    @Test
    void invalidKeys_are400_aRefusedCreateStoresNothing_andWithoutTheHeaderNothingChanged() {
        String code = TenantHttp.unique("IDD");
        for (String invalid : List.of("k".repeat(65), "bad key", "a/b", "")) {
            HttpResponse<String> refused = create(invalid, TenantHttp.createTenantBody(code));
            assertThat(refused.statusCode()).as("key '%s': %s", invalid, refused.body()).isEqualTo(400);
            assertThat(TenantHttp.errorCode(refused)).isEqualTo("IDEMPOTENCY_KEY_INVALID");
        }
        assertThat(count("SELECT COUNT(*) FROM CORE_TENANT WHERE CODE = ?", code)).isZero();
        assertThat(create("k".repeat(64), TenantHttp.createTenantBody(TenantHttp.unique("IDE"))).statusCode())
            .as("64 characters are accepted").isEqualTo(201);

        String key = newKey();
        HttpResponse<String> badCode = create(key, TenantHttp.createTenantBody("bad"));
        assertThat(badCode.statusCode()).isEqualTo(400);
        assertThat(TenantHttp.errorCode(badCode)).isEqualTo("TENANT_CODE_INVALID");
        assertThat(count("SELECT COUNT(*) FROM CORE_IDEMPOTENCY_KEY WHERE IDEMPOTENCY_KEY = ?", key)).as("not stored").isZero();
        HttpResponse<String> corrected = create(key, TenantHttp.createTenantBody(code));
        assertThat(corrected.statusCode()).as(corrected.body()).isEqualTo(201);
        assertThat(corrected.headers().firstValue(REPLAYED)).as("the failure was not replayed").isEmpty();

        HttpResponse<String> withoutKey = http.createTenant(platformToken, code);
        assertThat(withoutKey.statusCode()).as("1.2.0 behaviour without the header").isEqualTo(409);
        assertThat(TenantHttp.errorCode(withoutKey)).isEqualTo("TENANT_CODE_DUPLICATE");
    }

    @Test
    void twoSimultaneousFirstRequestsWithOneKey_provisionExactlyOnce_andBothAnswerTheSameTenant() throws Exception {
        String code = TenantHttp.unique("IDF");
        String key = newKey();
        String body = TenantHttp.createTenantBody(code);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return create(key, body);
                }));
            }
            start.countDown();
            List<HttpResponse<String>> answers = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                answers.add(future.get(60, TimeUnit.SECONDS));
            }

            assertThat(answers).allSatisfy(answer -> assertThat(answer.statusCode()).as(answer.body()).isEqualTo(201));
            assertThat(answers.stream().map(answer -> answer.headers().firstValue(REPLAYED).orElse("no")))
                .containsExactlyInAnyOrder("true", "no");
            assertThat(answers.stream().map(answer -> ((Number) JsonPath.read(answer.body(), "$.data.id")).longValue())
                .distinct()).hasSize(1);
            assertThat(count("SELECT COUNT(*) FROM CORE_TENANT WHERE CODE = ?", code)).isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM CORE_IDEMPOTENCY_KEY WHERE IDEMPOTENCY_KEY = ?", key)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void anExpiredKey_countsAsUnused_andTheRetentionJobDeletesEveryOldRow_tenantByTenant() {
        String key = newKey();
        String code = TenantHttp.unique("IDG");
        assertThat(create(key, TenantHttp.createTenantBody(code)).statusCode()).isEqualTo(201);
        age(key, 25);

        String later = TenantHttp.unique("IDH");
        HttpResponse<String> reused = create(key, TenantHttp.createTenantBody(later));
        assertThat(reused.statusCode()).as("an expired key is not a conflict: %s", reused.body()).isEqualTo(201);
        assertThat(reused.headers().firstValue(REPLAYED)).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM CORE_TENANT WHERE CODE = ?", later)).isEqualTo(1);

        long otherTenant = http.provisionTenant(platformToken, TenantHttp.unique("IDI"));
        String otherTenantsKey = newKey();
        insertRow(otherTenant, otherTenantsKey, "other");
        age(otherTenantsKey, 30);
        age(key, 25);
        String fresh = newKey();
        assertThat(create(fresh, TenantHttp.createTenantBody(TenantHttp.unique("IDJ"))).statusCode()).isEqualTo(201);

        assertThat(retentionJob.run()).isGreaterThanOrEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM CORE_IDEMPOTENCY_KEY WHERE IDEMPOTENCY_KEY IN (?, ?)", key, otherTenantsKey))
            .as("both old rows, PLATFORM's and the other tenant's").isZero();
        assertThat(count("SELECT COUNT(*) FROM CORE_IDEMPOTENCY_KEY WHERE IDEMPOTENCY_KEY = ?", fresh)).isEqualTo(1);
    }

    @Test
    void aKeyStoredInAnotherTenant_isNeverSeenByPlatform() {
        long otherTenant = http.provisionTenant(platformToken, TenantHttp.unique("IDK"));
        String key = newKey();
        insertRow(otherTenant, key, operator);

        String code = TenantHttp.unique("IDL");
        HttpResponse<String> created = create(key, TenantHttp.createTenantBody(code));

        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat(created.headers().firstValue(REPLAYED)).as("not the other tenant's row").isEmpty();
        assertThat((String) JsonPath.read(created.body(), "$.data.code")).isEqualTo(code);
        assertThat(jdbcTemplate.queryForList("SELECT TENANT_ID FROM CORE_IDEMPOTENCY_KEY WHERE IDEMPOTENCY_KEY = ?"
            + " ORDER BY TENANT_ID", Long.class, key)).containsExactly(TenantConstants.PLATFORM_TENANT_ID, otherTenant);
    }

    private HttpResponse<String> create(String key, String body) {
        return http.post(platformToken, TENANTS, body, KEY, key);
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }

    /** The same request as {@link TenantHttp#createTenantBody}, its fields in another order and spaced differently. */
    private static String reorderedBody(String code) {
        return "{ \"adminFullNameEn\" : \"Administrator\", \"adminFullNameAr\" : \"مدير\",\n  \"adminPassword\" : \""
            + TenantHttp.PASSWORD + "\", \"adminEmail\" : \"admin@" + code.toLowerCase() + ".test\",\n"
            + "  \"adminUsername\" : \"admin\", \"nameEn\" : \"Tenant " + code + "\", \"nameAr\" : \"مستأجر " + code
            + "\", \"code\" : \"" + code + "\" }";
    }

    /** A stored answer written straight into {@code tenantId}'s rows (the endpoint is PLATFORM-only). */
    private void insertRow(long tenantId, String key, String createdBy) {
        jdbcTemplate.update("INSERT INTO CORE_IDEMPOTENCY_KEY (ID, TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT, REQUEST_HASH,"
                + " RESPONSE_STATUS, RESPONSE_BODY, CREATED_BY) VALUES (nextval('SEQ_CORE_IDEMPOTENCY_KEY'), ?, ?, ?, ?, 201,"
                + " '{\"success\":true,\"data\":{\"id\":-1}}', ?)",
            tenantId, key, ENDPOINT, "0".repeat(64), createdBy);
    }

    private void age(String key, int hours) {
        jdbcTemplate.update("UPDATE CORE_IDEMPOTENCY_KEY SET CREATED_AT = now() - make_interval(hours => ?)"
            + " WHERE IDEMPOTENCY_KEY = ?", hours, key);
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }
}
