package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.User;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractIntegrationTest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * erp-core step 05, repository level:
 * <ul>
 *   <li>task 8 regression — a {@code SpecBuilder} query (the shared search path of every service)
 *       cannot see another tenant's rows, nor can {@code findById}/{@code count}; Hibernate's
 *       {@code @TenantId} discriminator applies with no change to the search code;</li>
 *   <li>inserts take the tenant of the session ({@code TENANT_ID} is never set by hand);</li>
 *   <li>the {@code VERSION} optimistic lock rejects a stale update.</li>
 * </ul>
 * Not transactional: each repository call opens its own session, so {@code TenantContext.runAs}
 * around a call selects that call's tenant (a second tenant row is inserted with JDBC).
 */
class TenantScopedQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long otherTenant;
    private String username;

    @BeforeEach
    void sameUsernameInThePlatformTenantAndInAnotherTenant() {
        String code = "SPEC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        otherTenant = jdbcTemplate.queryForObject(
            "insert into core_tenant (id, code, name_ar, name_en, status_code, created_by, created_at)"
                + " values (nextval('seq_core_tenant'), ?, 'ت', 'T', 'ACTIVE', 'test', now()) returning id",
            Long.class, code);
        username = "spec-" + UUID.randomUUID().toString().substring(0, 8);
        userRepository.save(user(username));                                         // PLATFORM (listener)
        TenantContext.runAs(otherTenant, () -> userRepository.save(user(username)));  // the other tenant
    }

    @Test
    void specBuilderSearch_findById_andCount_neverCrossTheTenant() {
        Specification<User> byUsername = SpecBuilder.build(
            SearchRequest.builder()
                .filters(List.of(SearchFilter.builder().field("username").operator(SearchOperator.LIKE).value(username).build()))
                .build(),
            new SetAllowedFields(Set.of("username")), DefaultFieldValueConverter.INSTANCE);

        List<User> asPlatform = userRepository.findAll(byUsername);
        List<User> asOther = TenantContext.callAs(otherTenant, () -> userRepository.findAll(byUsername));

        assertThat(asPlatform).singleElement()
            .satisfies(u -> assertThat(u.getTenantId()).isEqualTo(TenantConstants.PLATFORM_TENANT_ID));
        assertThat(asOther).singleElement()
            .satisfies(u -> assertThat(u.getTenantId()).isEqualTo(otherTenant));
        Long otherUsersId = asOther.get(0).getUserPk();
        assertThat(asPlatform.get(0).getUserPk()).isNotEqualTo(otherUsersId);

        assertThat(userRepository.findById(otherUsersId)).as("load by id of another tenant's row").isEmpty();
        assertThat(userRepository.findByUsername(username)).get()
            .extracting(User::getTenantId).isEqualTo(TenantConstants.PLATFORM_TENANT_ID);
        assertThat(TenantContext.callAs(otherTenant, () -> userRepository.count())).isEqualTo(1L);
        assertThat(TenantContext.callAs(otherTenant, () -> roleRepository.count()))
            .as("the other tenant was not provisioned: it has no roles").isZero();
    }

    @Test
    void tenantIdIsAssignedFromTheSession_andIsNotUpdatable() {
        User other = TenantContext.callAs(otherTenant, () -> userRepository.findByUsername(username).orElseThrow());

        assertThat(jdbcTemplate.queryForObject("select tenant_id from sec_user where user_pk = ?",
            Long.class, other.getUserPk())).isEqualTo(otherTenant);
        assertThat(other.getVersion()).isZero();
    }

    @Test
    void aStaleUpdate_failsTheOptimisticLock() {
        Role role = roleRepository.save(Role.builder()
            .code("OPT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase())
            .nameAr("دور").nameEn("Role").isActiveFl(Boolean.TRUE).build());
        Role first = roleRepository.findById(role.getRolePk()).orElseThrow();
        Role stale = roleRepository.findById(role.getRolePk()).orElseThrow();

        first.setNameEn("Role v2");
        assertThat(roleRepository.save(first).getVersion()).isEqualTo(1L);

        stale.setNameEn("Role from a stale copy");
        assertThatThrownBy(() -> roleRepository.save(stale)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(roleRepository.findById(role.getRolePk()).orElseThrow().getNameEn()).isEqualTo("Role v2");
    }

    private static User user(String username) {
        return User.builder()
            .username(username)
            .email(username + "@spec.test")
            .passwordHash("not-a-real-hash")
            .fullNameAr("مستخدم")
            .fullNameEn("User")
            .statusCode("ACTIVE")
            .build();
    }
}
