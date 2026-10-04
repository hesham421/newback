package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.util.TokenHasher;
import com.erp.sec.dto.PasswordResetCompleteRequest;
import com.erp.sec.entity.PasswordResetToken;
import com.erp.sec.entity.User;
import com.erp.sec.repository.PasswordResetTokenRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.service.PasswordResetService;
import com.erp.testsupport.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * erp-core step 10 on the SEC side: the {@code @Audited} listener on {@code User} writes its row in the
 * flushing transaction (a rollback leaves none), never records the password hash (an update that only
 * changes it writes nothing), and a completed password reset is recorded as {@code PASSWORD_RESET}
 * for the account in the generic log, next to its {@code SEC_AUDIT_LOG} row.
 */
class SecAuditIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;
    @Autowired
    private PasswordResetService passwordResetService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void auditedChange_inARolledBackTransaction_leavesNoAuditRow() {
        AtomicLong userId = new AtomicLong();
        transactionTemplate.executeWithoutResult(status -> {
            User saved = userRepository.saveAndFlush(newUser("rb"));
            userId.set(saved.getUserPk());
            // the listener wrote the row inside this transaction ...
            assertThat(rows(saved.getUserPk())).hasSize(1);
            status.setRollbackOnly();
        });
        // ... and the rollback removed it together with the user
        assertThat(rows(userId.get())).isEmpty();
        assertThat(userRepository.findById(userId.get())).isEmpty();
    }

    @Test
    void passwordOnlyUpdate_writesNoRow_andAnotherFieldDoes() {
        User user = userRepository.save(newUser("pw"));
        assertThat(rows(user.getUserPk())).extracting(row -> row.get("action")).containsExactly("CREATE");
        assertThat((String) rows(user.getUserPk()).get(0).get("changes")).doesNotContain("BCRYPT-PLACEHOLDER");

        transactionTemplate.executeWithoutResult(status -> {
            User loaded = userRepository.findById(user.getUserPk()).orElseThrow();
            loaded.setPasswordHash("BCRYPT-PLACEHOLDER-2");
            loaded.setLastLoginAt(java.time.Instant.now());
        });
        assertThat(rows(user.getUserPk())).extracting(row -> row.get("action")).containsExactly("CREATE");

        transactionTemplate.executeWithoutResult(status -> {
            User loaded = userRepository.findById(user.getUserPk()).orElseThrow();
            loaded.setPasswordHash("BCRYPT-PLACEHOLDER-3");
            loaded.setFullNameEn("Renamed");
        });
        List<Map<String, Object>> rows = rows(user.getUserPk());
        assertThat(rows).extracting(row -> row.get("action")).containsExactly("CREATE", "UPDATE");
        assertThat((String) rows.get(1).get("changes")).contains("fullNameEn").doesNotContain("passwordHash")
            .doesNotContain("BCRYPT-PLACEHOLDER");
    }

    @Test
    void completedPasswordReset_isRecordedAsPasswordReset_forTheAccount() {
        User user = userRepository.save(newUser("reset"));
        String rawToken = UUID.randomUUID().toString();
        resetTokenRepository.save(PasswordResetToken.builder().user(user).tokenHash(TokenHasher.sha256Hex(rawToken)).build());

        passwordResetService.complete(PasswordResetCompleteRequest.builder()
            .token(rawToken).newPassword("N3w-Passw0rd!").build());

        Map<String, Object> row = jdbc.queryForMap("SELECT actor, actor_realm, actor_user_id, entity_type, tenant_id"
            + " FROM core_audit_event WHERE action = 'PASSWORD_RESET' AND entity_id = ?", String.valueOf(user.getUserPk()));
        assertThat(row).containsEntry("actor", user.getUsername()).containsEntry("actor_realm", "STAFF")
            .containsEntry("actor_user_id", user.getUserPk()).containsEntry("entity_type", "SEC_USER")
            .containsEntry("tenant_id", 1L);
        // the hash change itself is ignored: no UPDATE row
        assertThat(rows(user.getUserPk())).extracting(r -> r.get("action")).doesNotContain("UPDATE");
        // SEC_AUDIT_LOG keeps its own security-specific row
        assertThat(jdbc.queryForObject("SELECT count(*) FROM sec_audit_log WHERE event_type_code ="
            + " 'PASSWORD_RESET_COMPLETED' AND actor_user_id = ?", Integer.class, user.getUserPk())).isEqualTo(1);
    }

    private static User newUser(String prefix) {
        String username = "sec-aud-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        return User.builder()
            .username(username)
            .email(username + "@audit.test")
            .passwordHash("BCRYPT-PLACEHOLDER-1")
            .fullNameAr("مستخدم")
            .fullNameEn("User")
            .build();
    }

    private List<Map<String, Object>> rows(long userId) {
        return jdbc.queryForList("SELECT action, changes::text AS changes FROM core_audit_event"
            + " WHERE entity_type = 'SEC_USER' AND entity_id = ? AND action IN ('CREATE', 'UPDATE', 'DELETE') ORDER BY id",
            String.valueOf(userId));
    }
}
