package com.erp.sec.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.CustomerVerifyToken;
import com.erp.sec.entity.PasswordResetToken;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** erp-core step 06 — the customer-realm decisions of the SEC domain objects. */
class CustomerDomainRulesTest {

    private static final Instant NOW = Instant.parse("2026-10-04T10:00:00Z");

    @Test
    void createCustomer_refusesATakenEmail_withCustomerEmailTaken() {
        assertThat(UserDomain.createCustomer("a@b.c", false).getStatusCode()).isEqualTo("PENDING_VERIFICATION");
        assertThatThrownBy(() -> UserDomain.createCustomer("a@b.c", true))
            .isInstanceOf(LocalizedException.class)
            .extracting("errorCode").isEqualTo(SecErrorCodes.CUSTOMER_EMAIL_TAKEN);
    }

    @Test
    void anUnverifiedCustomer_mayNotLogIn_butReceivesNotifications() {
        User pending = User.builder().statusCode(User.STATUS_PENDING_VERIFICATION).isActiveFl(true).build();
        UserDomain domain = UserDomain.from(pending);
        assertThat(domain.awaitsVerification()).isTrue();
        assertThat(domain.canReceiveNotifications()).isTrue();
        assertThatThrownBy(domain::assertCustomerVerified)
            .isInstanceOf(LocalizedException.class)
            .extracting("errorCode").isEqualTo(SecErrorCodes.CUSTOMER_NOT_VERIFIED);

        UserDomain active = UserDomain.from(User.builder().statusCode("ACTIVE").isActiveFl(true).build());
        active.assertCustomerVerified();
        assertThat(active.canReceiveNotifications()).isTrue();
        assertThat(UserDomain.from(User.builder().statusCode("DISABLED").build()).canReceiveNotifications()).isFalse();
    }

    @Test
    void verifyToken_isUsableOnlyUnusedAndUnexpired() {
        CustomerVerifyTokenDomain.from(CustomerVerifyToken.builder().expiresAt(NOW.plusSeconds(60)).build()).assertUsable(NOW);

        for (CustomerVerifyToken token : new CustomerVerifyToken[] {
            CustomerVerifyToken.builder().expiresAt(NOW.minusSeconds(1)).build(),
            CustomerVerifyToken.builder().expiresAt(NOW.plusSeconds(60)).usedAt(NOW.minusSeconds(5)).build()}) {
            assertThatThrownBy(() -> CustomerVerifyTokenDomain.from(token).assertUsable(NOW))
                .isInstanceOf(LocalizedException.class)
                .extracting("errorCode").isEqualTo(SecErrorCodes.VERIFY_TOKEN_INVALID);
        }
    }

    @Test
    void resetToken_ofTheOtherRealm_isRefusedLikeAnUnknownOne() {
        PasswordResetTokenDomain token = PasswordResetTokenDomain.from(
            PasswordResetToken.builder().expiresAt(NOW.plusSeconds(60)).build());
        token.assertUsable(NOW, User.REALM_CUSTOMER, User.REALM_CUSTOMER);
        assertThatThrownBy(() -> token.assertUsable(NOW, User.REALM_STAFF, User.REALM_CUSTOMER))
            .isInstanceOf(LocalizedException.class)
            .extracting("errorCode").isEqualTo(SecErrorCodes.SEC_409_RESET_TOKEN_INVALID);
    }
}
