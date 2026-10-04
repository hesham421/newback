package com.erp.notif;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.erp.notif.crossmodule.DispatchCommand;
import com.erp.notif.crossmodule.NotificationDispatchApi;
import com.erp.notif.domain.NotificationLogDomain;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.notif.exception.NotifErrorCodes;
import com.erp.notif.service.NotificationInboxService;
import com.erp.testsupport.AbstractAsyncIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 08 — the IN_APP channel end to end: a dispatch on {@code IN_APP} is delivered
 * asynchronously into the recipient's {@code NOTIF_INBOX}; the recipient lists it and marks it read
 * over HTTP ({@code GET /api/v1/notif/inbox}, {@code PATCH /api/v1/notif/inbox/{id}/read}) with its own
 * token; another user neither sees nor can mark it (404 {@code INBOX_ITEM_NOT_FOUND}); no token → 401.
 */
class NotificationInboxApiIntegrationTest extends AbstractAsyncIntegrationTest {

    private static final long PLATFORM = 1L;

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private NotificationDispatchApi dispatchApi;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NotificationInboxService inboxService;

    private NotifTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new NotifTestFixtures(port, jdbc, passwordEncoder);
    }

    @Test
    void inAppDispatch_landsInTheRecipientsInbox_whoListsAndMarksItRead_andNobodyElseCan() {
        String aliceName = NotifTestFixtures.unique("inbox-alice-");
        String bobName = NotifTestFixtures.unique("inbox-bob-");
        long alice = fixtures.activeUser(PLATFORM, aliceName);
        fixtures.activeUser(PLATFORM, bobName);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("inbox-dispatcher", null, List.of()));
        long logId = dispatchApi.dispatch(new DispatchCommand(alice, "ACCOUNT_ACTIVATION", List.of("IN_APP"),
            "TEST", 4711L, "TEST_REF", Map.of("actionLink", "https://app.example.test/x", "expiresAt", "tomorrow")))
            .get(0);
        SecurityContextHolder.clearContext();

        await().atMost(ASYNC_TIMEOUT).until(() -> NotificationLogDomain.STATUS_SENT.equals(
            jdbc.queryForObject("SELECT NOTIFICATION_STATUS_ID FROM NOTIF_LOG WHERE ID = ?", String.class, logId)));

        String aliceToken = fixtures.token(NotifTestFixtures.PLATFORM, aliceName);
        String bobToken = fixtures.token(NotifTestFixtures.PLATFORM, bobName);

        // alice sees one unread item, rendered (placeholders substituted)
        HttpResponse<String> list = fixtures.get(aliceToken, "/api/v1/notif/inbox");
        assertThat(list.statusCode()).as(list.body()).isEqualTo(200);
        List<Integer> ids = JsonPath.read(list.body(), "$.data.content[*].id");
        assertThat(ids).hasSize(1);
        long itemId = ids.get(0);
        assertThat((Boolean) JsonPath.read(list.body(), "$.data.content[0].read")).isFalse();
        assertThat((String) JsonPath.read(list.body(), "$.data.content[0].bodyEn"))
            .contains("https://app.example.test/x").doesNotContain("{actionLink}");
        assertThat((String) JsonPath.read(list.body(), "$.data.content[0].referenceType")).isEqualTo("TEST_REF");

        // bob sees nothing of alice's and cannot mark it
        HttpResponse<String> bobList = fixtures.get(bobToken, "/api/v1/notif/inbox");
        assertThat(bobList.statusCode()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(bobList.body(), "$.data.content")).isEmpty();
        HttpResponse<String> bobMark = fixtures.patch(bobToken, "/api/v1/notif/inbox/" + itemId + "/read");
        assertThat(bobMark.statusCode()).isEqualTo(404);
        assertThat(NotifTestFixtures.errorCode(bobMark)).isEqualTo(NotifErrorCodes.INBOX_ITEM_NOT_FOUND);

        // alice marks it read (idempotent), then the unread view is empty
        HttpResponse<String> mark = fixtures.patch(aliceToken, "/api/v1/notif/inbox/" + itemId + "/read");
        assertThat(mark.statusCode()).as(mark.body()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(mark.body(), "$.data.read")).isTrue();
        String readAt = JsonPath.read(mark.body(), "$.data.readAt");
        HttpResponse<String> again = fixtures.patch(aliceToken, "/api/v1/notif/inbox/" + itemId + "/read");
        assertThat(again.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(again.body(), "$.data.readAt")).isEqualTo(readAt);

        HttpResponse<String> unread = fixtures.get(aliceToken, "/api/v1/notif/inbox?unreadOnly=true");
        assertThat((List<?>) JsonPath.read(unread.body(), "$.data.content")).isEmpty();
        HttpResponse<String> all = fixtures.get(aliceToken, "/api/v1/notif/inbox");
        assertThat((List<?>) JsonPath.read(all.body(), "$.data.content")).hasSize(1);
    }

    /**
     * The customer realm (erp-core step 06): a customer token resolves to the customer's own
     * {@code SEC_USER} id and reads/marks its in-app items on the customer chain
     * ({@code /api/v1/customers/me/inbox}); a staff user with the same e-mail sees none of them, and each
     * realm's token is refused on the other realm's inbox path (403 {@code REALM_MISMATCH}).
     */
    @Test
    void customerToken_readsAndMarksItsOwnInbox_onTheCustomerChain() {
        String email = NotifTestFixtures.unique("shopper-") + "@notif.test";
        long customer = fixtures.activeCustomer(PLATFORM, email);
        // a staff account with the same e-mail-shaped username in the same tenant: a different SEC_USER row
        long staff = fixtures.activeUser(PLATFORM, email);
        assertThat(staff).isNotEqualTo(customer);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("inbox-dispatcher", null, List.of()));
        long logId = dispatchApi.dispatch(new DispatchCommand(customer, "ACCOUNT_ACTIVATION", List.of("IN_APP"),
            "TEST", null, null, Map.of("actionLink", "https://shop.example.test/a", "expiresAt", "soon"))).get(0);
        SecurityContextHolder.clearContext();
        await().atMost(ASYNC_TIMEOUT).until(() -> NotificationLogDomain.STATUS_SENT.equals(
            jdbc.queryForObject("SELECT NOTIFICATION_STATUS_ID FROM NOTIF_LOG WHERE ID = ?", String.class, logId)));

        String customerToken = fixtures.customerToken(NotifTestFixtures.PLATFORM, email);
        String staffToken = fixtures.token(NotifTestFixtures.PLATFORM, email);

        HttpResponse<String> list = fixtures.get(customerToken, "/api/v1/customers/me/inbox");
        assertThat(list.statusCode()).as(list.body()).isEqualTo(200);
        List<Integer> ids = JsonPath.read(list.body(), "$.data.content[*].id");
        assertThat(ids).hasSize(1);
        assertThat(((Number) JsonPath.read(list.body(), "$.data.content[0].recipientUserId")).longValue())
            .isEqualTo(customer);

        HttpResponse<String> mark = fixtures.patch(customerToken, "/api/v1/customers/me/inbox/" + ids.get(0) + "/read");
        assertThat(mark.statusCode()).as(mark.body()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(mark.body(), "$.data.read")).isTrue();

        // the staff namesake owns nothing of it
        HttpResponse<String> staffList = fixtures.get(staffToken, "/api/v1/notif/inbox");
        assertThat(staffList.statusCode()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(staffList.body(), "$.data.content")).isEmpty();

        // realms stay on their own chain
        HttpResponse<String> customerOnStaffPath = fixtures.get(customerToken, "/api/v1/notif/inbox");
        assertThat(customerOnStaffPath.statusCode()).isEqualTo(403);
        assertThat(NotifTestFixtures.errorCode(customerOnStaffPath)).isEqualTo("REALM_MISMATCH");
        HttpResponse<String> staffOnCustomerPath = fixtures.get(staffToken, "/api/v1/customers/me/inbox");
        assertThat(staffOnCustomerPath.statusCode()).isEqualTo(403);
        assertThat(NotifTestFixtures.errorCode(staffOnCustomerPath)).isEqualTo("REALM_MISMATCH");
    }

    @Test
    void unknownItem_is404_andNoToken_is401() {
        String name = NotifTestFixtures.unique("inbox-carol-");
        fixtures.activeUser(PLATFORM, name);
        String token = fixtures.token(NotifTestFixtures.PLATFORM, name);

        HttpResponse<String> unknown = fixtures.patch(token, "/api/v1/notif/inbox/987654321/read");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(NotifTestFixtures.errorCode(unknown)).isEqualTo(NotifErrorCodes.INBOX_ITEM_NOT_FOUND);

        assertThat(fixtures.get(null, "/api/v1/notif/inbox").statusCode()).isEqualTo(401);
    }

    @Test
    void aPrincipalThatIsNoUserAccount_getsNotifChannelUnavailable() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ghost-principal-without-account", null, List.of()));

        assertThatThrownBy(() -> inboxService.list(false, 0, 20))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.FORBIDDEN);
                assertThat(e.getErrorCode()).isEqualTo(NotifErrorCodes.NOTIF_CHANNEL_UNAVAILABLE);
            });
    }
}
