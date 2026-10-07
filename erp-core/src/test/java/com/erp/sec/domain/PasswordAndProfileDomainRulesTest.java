package com.erp.sec.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.exception.SecErrorCodes;
import org.junit.jupiter.api.Test;

/** erp-core 1.3.0 (TM-D) — RULE-SEC-056 (PasswordPolicy) and the UserDomain decisions RULE-SEC-057/058/060/061. */
class PasswordAndProfileDomainRulesTest {

    private final PasswordPolicy defaults = PasswordPolicy.create(8, 200, true, true);

    @Test
    void defaultPolicy_needsEightToTwoHundredCharactersWithALetterAndADigit() {
        assertThat(defaults.accepts("Passw0rd!Tc1")).isTrue();
        assertThat(defaults.accepts("Test1234")).as("the documented bootstrap password").isTrue();
        assertThat(defaults.accepts("كلمةسر12")).as("letters of any script").isTrue();
        assertThat(defaults.accepts("short1")).isFalse();
        assertThat(defaults.accepts("abcdefgh")).as("no digit").isFalse();
        assertThat(defaults.accepts("12345678")).as("no letter").isFalse();
        assertThat(defaults.accepts("a1".repeat(100))).as("exactly 200").isTrue();
        assertThat(defaults.accepts("a1".repeat(100) + "x")).as("201").isFalse();
        assertThat(defaults.accepts(null)).isFalse();
    }

    @Test
    void policyIsConfigurable_andNeverBelowOneCharacter() {
        PasswordPolicy relaxed = PasswordPolicy.create(4, 10, false, false);
        assertThat(relaxed.accepts("abcd")).isTrue();
        assertThat(relaxed.accepts("1234")).isTrue();
        assertThat(relaxed.accepts("abc")).isFalse();
        PasswordPolicy degenerate = PasswordPolicy.create(0, -5, false, false);
        assertThat(degenerate.getMinLength()).isEqualTo(1);
        assertThat(degenerate.getMaxLength()).isEqualTo(1);
    }

    @Test
    void assertAcceptable_raisesThePolicyCode_namingTheField_withTheBounds() {
        assertThatThrownBy(() -> defaults.assertAcceptable("newPassword", "weak"))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                assertThat(e.getErrorCode()).isEqualTo(SecErrorCodes.SEC_400_PASSWORD_POLICY);
                assertThat(e.getArgs()).containsExactly(8, 200);
                assertThat(e.getErrors()).singleElement()
                    .satisfies(detail -> assertThat(detail.field()).isEqualTo("newPassword"));
            });
        assertThatCode(() -> defaults.assertAcceptable("newPassword", "Str0ngEnough")).doesNotThrowAnyException();
    }

    @Test
    void anAdministratorChosenPassword_mustBeChangedUnlessTheRequestSaysFalse() {
        assertThat(UserDomain.passwordChangeRequiredFor(null)).isTrue();
        assertThat(UserDomain.passwordChangeRequiredFor(Boolean.TRUE)).isTrue();
        assertThat(UserDomain.passwordChangeRequiredFor(Boolean.FALSE)).isFalse();
    }

    @Test
    void adminSetOnOneself_isRefusedWith422() {
        assertThatThrownBy(() -> UserDomain.assertNotSelfForAdminPasswordSet(true))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
                assertThat(e.getErrorCode()).isEqualTo(SecErrorCodes.SEC_422_PASSWORD_SELF);
            });
        assertThatCode(() -> UserDomain.assertNotSelfForAdminPasswordSet(false)).doesNotThrowAnyException();
    }

    @Test
    void aWrongCurrentPassword_isRefusedWith403() {
        assertThatThrownBy(() -> UserDomain.assertCurrentPasswordMatches(false))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.FORBIDDEN);
                assertThat(e.getErrorCode()).isEqualTo(SecErrorCodes.SEC_403_PASSWORD_CURRENT_INVALID);
            });
        assertThatCode(() -> UserDomain.assertCurrentPasswordMatches(true)).doesNotThrowAnyException();
    }

    @Test
    void photoLimits_areOneMegabyteOfPngJpegOrWebp_andARejectionNamesTheFilePart() {
        assertThat(UserDomain.PHOTO_MAX_BYTES).isEqualTo(1_048_576L);
        assertThat(UserDomain.PHOTO_TYPES).containsExactlyInAnyOrder("image/png", "image/jpeg", "image/webp");
        assertThatThrownBy(() -> UserDomain.assertPhotoAccepted(false))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                assertThat(e.getErrorCode()).isEqualTo(SecErrorCodes.SEC_400_PHOTO_INVALID);
                assertThat(e.getErrors()).singleElement().satisfies(detail -> assertThat(detail.field()).isEqualTo("file"));
            });
    }
}
