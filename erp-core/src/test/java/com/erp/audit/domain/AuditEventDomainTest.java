package com.erp.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.audit.crossmodule.AuditChange;
import com.erp.audit.exception.AuditErrorCodes;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests of the audit row rules (erp-core step 10). */
class AuditEventDomainTest {

    @Test
    void validActions_areAccepted() {
        for (String action : List.of("CREATE", "UPDATE", "DELETE", "STATUS_CHANGE", "LOGIN", "PASSWORD_RESET", "ABC",
            "A".repeat(64))) {
            assertThat(AuditEventDomain.create(action).getAction()).isEqualTo(action);
        }
    }

    @Test
    void invalidActions_areRejectedWithAuditActionInvalid() {
        for (String action : new String[] {null, "", "AB", "login", "Login", "LOG-IN", "LOG IN", "LOGIN1",
            "A".repeat(65)}) {
            assertThatThrownBy(() -> AuditEventDomain.create(action)).as(String.valueOf(action))
                .isInstanceOfSatisfying(LocalizedException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(AuditErrorCodes.AUDIT_ACTION_INVALID);
                    assertThat(e.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                });
        }
    }

    @Test
    void sensitiveFieldNames_areDetectedCaseInsensitively() {
        assertThat(List.of("password", "passwordHash", "PASSWORD_HASH", "resetToken", "tokenHash", "clientSecret",
            "contentHash", "apiKey", "credentials", "privateKey", "salt"))
            .allSatisfy(field -> assertThat(AuditEventDomain.isSensitive(field)).as(field).isTrue());
        assertThat(List.of("username", "email", "fullNameEn", "statusCode", "visibility", "configValue"))
            .allSatisfy(field -> assertThat(AuditEventDomain.isSensitive(field)).as(field).isFalse());
    }

    @Test
    void recordable_excludesSensitiveTechnicalAndIgnoredFields() {
        Set<String> ignored = Set.of("lastLoginAt");
        assertThat(AuditEventDomain.isRecordable("fullNameEn", ignored)).isTrue();
        assertThat(AuditEventDomain.isRecordable("lastLoginAt", ignored)).isFalse();
        assertThat(AuditEventDomain.isRecordable("passwordHash", Set.of())).isFalse();
        for (String technical : AuditEventDomain.TECHNICAL_FIELDS) {
            assertThat(AuditEventDomain.isRecordable(technical, Set.of())).as(technical).isFalse();
        }
    }

    @Test
    void recordableChanges_dropsSensitiveAndNullEntries() {
        List<AuditChange> changes = new java.util.ArrayList<>(List.of(
            new AuditChange("title", "a", "b"), new AuditChange("secretKey", "x", "y")));
        changes.add(null);
        assertThat(AuditEventDomain.create("UPDATE").recordableChanges(changes))
            .extracting(AuditChange::field).containsExactly("title");
        assertThat(AuditEventDomain.create("UPDATE").recordableChanges(null)).isEmpty();
    }
}
