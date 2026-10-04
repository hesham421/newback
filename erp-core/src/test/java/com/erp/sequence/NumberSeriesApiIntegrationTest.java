package com.erp.sequence;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.testsupport.AbstractIntegrationTest;
import com.erp.testsupport.StaffApiClient;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 09 — the admin API {@code /api/v1/sequence/series} over HTTP (whole security chain): a
 * happy path and a failure path per endpoint, authentication, and tenant isolation (a tenant's super role
 * holds {@code PERM_SEQUENCE_SERIES_*} through the catalog and sees only its own series).
 */
class NumberSeriesApiIntegrationTest extends AbstractIntegrationTest {

    private static final String SERIES = "/api/v1/sequence/series";

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private StaffApiClient http;
    private String token;

    @BeforeEach
    void platformToken() {
        http = new StaffApiClient(port);
        token = http.token("PLATFORM", StaffApiClient.platformOperator(jdbcTemplate, passwordEncoder));
    }

    @Test
    void create_201_withDefaults_then_duplicate_409_and_invalidPattern_400() {
        String code = StaffApiClient.unique("API");

        HttpResponse<String> created = http.post(token, SERIES, "{\"code\":\"" + code.toLowerCase() + "\",\"prefix\":\"INV\"}");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        assertThat((String) JsonPath.read(created.body(), "$.data.code")).isEqualTo(code);
        assertThat((String) JsonPath.read(created.body(), "$.data.pattern")).isEqualTo("{PREFIX}-{YYYY}-{SEQ:6}");
        assertThat((String) JsonPath.read(created.body(), "$.data.resetPolicy")).isEqualTo("YEARLY");
        assertThat((String) JsonPath.read(created.body(), "$.data.periodKey"))
            .isEqualTo(String.valueOf(java.time.LocalDate.now().getYear()));
        assertThat(((Number) JsonPath.read(created.body(), "$.data.nextValue")).longValue()).isEqualTo(1L);

        HttpResponse<String> duplicate = http.post(token, SERIES, "{\"code\":\"" + code + "\"}");
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(StaffApiClient.errorCode(duplicate)).isEqualTo("NUMBER_SERIES_CODE_DUPLICATE");

        HttpResponse<String> unknownToken = http.post(token, SERIES,
            "{\"code\":\"" + StaffApiClient.unique("BAD") + "\",\"pattern\":\"{DAY}-{SEQ:3}\"}");
        assertThat(unknownToken.statusCode()).isEqualTo(400);
        assertThat(StaffApiClient.errorCode(unknownToken)).isEqualTo("SEQUENCE_PATTERN_INVALID");

        HttpResponse<String> yearlyWithoutYear = http.post(token, SERIES,
            "{\"code\":\"" + StaffApiClient.unique("BAD") + "\",\"pattern\":\"{PREFIX}-{SEQ:3}\",\"resetPolicy\":\"YEARLY\"}");
        assertThat(yearlyWithoutYear.statusCode()).isEqualTo(400);
        assertThat(StaffApiClient.errorCode(yearlyWithoutYear)).isEqualTo("SEQUENCE_PATTERN_INVALID");

        HttpResponse<String> badCode = http.post(token, SERIES, "{\"code\":\"has space\"}");
        assertThat(badCode.statusCode()).isEqualTo(400);
    }

    @Test
    void getById_200_and_404() {
        long id = create(StaffApiClient.unique("GET"), "{PREFIX}-{SEQ:4}", "NEVER");

        HttpResponse<String> found = http.get(token, SERIES + "/" + id);
        assertThat(found.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(found.body(), "$.data.resetPolicy")).isEqualTo("NEVER");
        assertThat((String) JsonPath.read(found.body(), "$.data.periodKey")).isEmpty();

        HttpResponse<String> missing = http.get(token, SERIES + "/987654321");
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(StaffApiClient.errorCode(missing)).isEqualTo("NUMBER_SERIES_NOT_FOUND");
    }

    @Test
    void search_200_byCode_and_400_onAnUnsupportedFilter() {
        String code = StaffApiClient.unique("SRC");
        create(code, "{PREFIX}-{SEQ:4}", "NEVER");

        HttpResponse<String> found = http.post(token, SERIES + "/search",
            "{\"filters\":[{\"field\":\"code\",\"operator\":\"EQUALS\",\"value\":\"" + code + "\"}]}");
        assertThat(found.statusCode()).as(found.body()).isEqualTo(200);
        List<String> codes = JsonPath.read(found.body(), "$.data.content[*].code");
        assertThat(codes).containsExactly(code);

        HttpResponse<String> unsupported = http.post(token, SERIES + "/search",
            "{\"filters\":[{\"field\":\"tenantId\",\"operator\":\"EQUALS\",\"value\":\"2\"}]}");
        assertThat(unsupported.statusCode()).isEqualTo(400);
    }

    @Test
    void update_200_appliesToTheCode_and_400_onAnInvalidPattern() {
        long id = create(StaffApiClient.unique("UPD"), "{PREFIX}-{YYYY}-{SEQ:4}", "YEARLY");

        HttpResponse<String> updated = http.put(token, SERIES + "/" + id,
            "{\"prefix\":\"NEW\",\"pattern\":\"{PREFIX}/{YY}/{SEQ:5}\"}");
        assertThat(updated.statusCode()).as(updated.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(updated.body(), "$.data.prefix")).isEqualTo("NEW");
        assertThat((String) JsonPath.read(updated.body(), "$.data.pattern")).isEqualTo("{PREFIX}/{YY}/{SEQ:5}");

        HttpResponse<String> invalid = http.put(token, SERIES + "/" + id, "{\"pattern\":\"{PREFIX}-{SEQ:5}\"}");
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(StaffApiClient.errorCode(invalid)).isEqualTo("SEQUENCE_PATTERN_INVALID");

        HttpResponse<String> missing = http.put(token, SERIES + "/987654321", "{\"pattern\":\"{SEQ:3}\"}");
        assertThat(missing.statusCode()).isEqualTo(404);
    }

    @Test
    void deactivate_then_activate_200_and_404() {
        long id = create(StaffApiClient.unique("ACT"), "{PREFIX}-{SEQ:4}", "NEVER");

        HttpResponse<String> deactivated = http.put(token, SERIES + "/" + id + "/deactivate", "");
        assertThat(deactivated.statusCode()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(deactivated.body(), "$.data.isActive")).isFalse();

        HttpResponse<String> activated = http.put(token, SERIES + "/" + id + "/activate", "");
        assertThat(activated.statusCode()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(activated.body(), "$.data.isActive")).isTrue();

        assertThat(http.put(token, SERIES + "/987654321/deactivate", "").statusCode()).isEqualTo(404);
        assertThat(http.put(token, SERIES + "/987654321/activate", "").statusCode()).isEqualTo(404);
    }

    @Test
    void withoutAToken_401() {
        assertThat(http.post(null, SERIES + "/search", "{}").statusCode()).isEqualTo(401);
        assertThat(http.post(null, SERIES, "{\"code\":\"X\"}").statusCode()).isEqualTo(401);
    }

    @Test
    void anotherTenantsAdmin_managesItsOwnSeries_andNeverSeesPlatformRows() {
        String platformCode = StaffApiClient.unique("ISO");
        long platformId = create(platformCode, "{PREFIX}-{SEQ:4}", "NEVER");

        String tenantCode = StaffApiClient.unique("SQT");
        http.provisionTenant(token, tenantCode);          // provisioning copies PLATFORM's series definitions
        String tenantToken = http.token(tenantCode, StaffApiClient.TENANT_ADMIN);

        assertThat(http.get(tenantToken, SERIES + "/" + platformId).statusCode()).isEqualTo(404);
        HttpResponse<String> copy = http.post(tenantToken, SERIES + "/search",
            "{\"filters\":[{\"field\":\"code\",\"operator\":\"EQUALS\",\"value\":\"" + platformCode + "\"}]}");
        assertThat(copy.statusCode()).isEqualTo(200);
        List<Number> copyIds = JsonPath.read(copy.body(), "$.data.content[*].id");
        assertThat(copyIds).singleElement().satisfies(id -> assertThat(id.longValue()).isNotEqualTo(platformId));

        String tenantOnly = StaffApiClient.unique("OWN");
        HttpResponse<String> own = http.post(tenantToken, SERIES,
            "{\"code\":\"" + tenantOnly + "\",\"pattern\":\"{SEQ:3}\",\"resetPolicy\":\"NEVER\"}");
        assertThat(own.statusCode()).as(own.body()).isEqualTo(201);
        long ownId = ((Number) JsonPath.read(own.body(), "$.data.id")).longValue();
        assertThat(http.get(token, SERIES + "/" + ownId).statusCode()).isEqualTo(404);
    }

    private long create(String code, String pattern, String policy) {
        HttpResponse<String> created = http.post(token, SERIES,
            "{\"code\":\"" + code + "\",\"prefix\":\"P\",\"pattern\":\"" + pattern + "\",\"resetPolicy\":\"" + policy + "\"}");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        return ((Number) JsonPath.read(created.body(), "$.data.id")).longValue();
    }
}
