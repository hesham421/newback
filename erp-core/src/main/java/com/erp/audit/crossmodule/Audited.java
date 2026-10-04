package com.erp.audit.crossmodule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts a JPA entity into the generic audit log (erp-core step 10). After every insert, update and
 * delete of an annotated entity, the audit module's Hibernate listener diffs the persistent
 * properties and records one {@code CREATE} / {@code UPDATE} / {@code DELETE} row in
 * {@code CORE_AUDIT_EVENT} — in the same transaction and on the same JDBC connection as the change, so
 * a rolled-back change leaves no audit row.
 *
 * <p>What is never recorded as a change:
 * <ul>
 *   <li>the properties named in {@link #ignore()};</li>
 *   <li>any property whose name contains a denylisted word ({@code password}, {@code secret},
 *       {@code token}, {@code hash}, {@code credential}, {@code apikey}, {@code privatekey},
 *       {@code salt}) — see {@link AuditApi#SENSITIVE_FIELD_WORDS};</li>
 *   <li>the audit columns, the optimistic-lock version and the tenant id;</li>
 *   <li>collections and binary values.</li>
 * </ul>
 * An update whose only differences are such properties writes no row.
 *
 * <pre>{@code
 * @Audited(entityType = "SEC_USER", ignore = {"passwordHash"})
 * public class User extends AuditableEntity { ... }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Audited {

    /**
     * The {@code ENTITY_TYPE} written on each row (by convention the table name, e.g. {@code SEC_USER});
     * blank → the JPA entity name.
     */
    String entityType() default "";

    /** Property names (Java field names) never written to {@code CHANGES}. */
    String[] ignore() default {};
}
