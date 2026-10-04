package com.erp.audit.listener;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.audit.crossmodule.AuditChange;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.audit.crossmodule.Audited;
import com.erp.audit.domain.AuditEventDomain;
import com.erp.audit.service.AuditRecordingService;
import com.erp.common.domain.AuditableEntity;
import java.sql.Blob;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * The opt-in entity listener of the generic audit log (erp-core step 10): for every entity annotated
 * with {@link Audited}, Hibernate's {@code PostInsert}/{@code PostUpdate}/{@code PostDelete} events
 * become one {@code CREATE}/{@code UPDATE}/{@code DELETE} audit row whose {@code CHANGES} list the
 * persistent properties that were set (create), differ (update) or were removed (delete).
 *
 * <p>The row is written through {@link AuditRecordingService} — the code behind {@code AuditApi.record}
 * — on the Hibernate session's own JDBC connection ({@code Session.doWork}), i.e. inside the
 * transaction that is flushing the change: a rollback removes both. An update whose only differences
 * are ignored properties (sensitive, technical, or {@code @Audited(ignore)}) writes nothing.
 *
 * <p>Tenant of the row: the entity's own {@code TENANT_ID} for a tenant-scoped entity, otherwise the
 * current tenant (a global entity such as {@code CORE_TENANT} is audited in the tenant that changed it).
 *
 * <p>Registered with Hibernate by {@code AuditHibernateConfiguration}; reached only from Hibernate,
 * so no {@code @PreAuthorize}.
 */
@Component
@RequiredArgsConstructor
public class AuditedEntityListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    /** Resolved lazily: the listener is handed to Hibernate while the EntityManagerFactory is being built. */
    private final ObjectProvider<AuditRecordingService> recordingService;

    private final Map<Class<?>, Optional<AuditedType>> auditedTypes = new ConcurrentHashMap<>();

    /** The {@link Audited} settings of one entity class. */
    record AuditedType(String entityType, Set<String> ignored) {
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        auditedType(event.getEntity(), event.getPersister()).ifPresent(type -> {
            List<AuditChange> changes = new ArrayList<>();
            forEachRecordable(event.getPersister(), type, (index, name, propertyType) -> {
                Object value = simplify(event.getSession(), propertyType, event.getState()[index]);
                if (value != null) {
                    changes.add(new AuditChange(name, null, value));
                }
            });
            record(event.getSession(), event.getEntity(), type, AuditApi.ACTION_CREATE, event.getId(), changes,
                "إنشاء", "Created");
        });
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        auditedType(event.getEntity(), event.getPersister()).ifPresent(type -> {
            Object[] oldState = event.getOldState();
            Object[] state = event.getState();
            Set<Integer> dirty = dirtyIndexes(event.getDirtyProperties());
            List<AuditChange> changes = new ArrayList<>();
            forEachRecordable(event.getPersister(), type, (index, name, propertyType) -> {
                if (oldState == null && !dirty.contains(index)) {
                    return;   // no loaded snapshot: only the properties Hibernate reports dirty
                }
                Object before = oldState == null ? null : simplify(event.getSession(), propertyType, oldState[index]);
                Object after = simplify(event.getSession(), propertyType, state[index]);
                if (!Objects.equals(before, after)) {
                    changes.add(new AuditChange(name, before, after));
                }
            });
            if (!changes.isEmpty()) {
                record(event.getSession(), event.getEntity(), type, AuditApi.ACTION_UPDATE, event.getId(), changes,
                    "تعديل", "Updated");
            }
        });
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        auditedType(event.getEntity(), event.getPersister()).ifPresent(type -> {
            List<AuditChange> changes = new ArrayList<>();
            Object[] deleted = event.getDeletedState();
            if (deleted != null) {
                forEachRecordable(event.getPersister(), type, (index, name, propertyType) -> {
                    Object value = simplify(event.getSession(), propertyType, deleted[index]);
                    if (value != null) {
                        changes.add(new AuditChange(name, value, null));
                    }
                });
            }
            record(event.getSession(), event.getEntity(), type, AuditApi.ACTION_DELETE, event.getId(), changes,
                "حذف", "Deleted");
        });
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private void record(SharedSessionContractImplementor session, Object entity, AuditedType type, String action, Object id,
                        List<AuditChange> changes, String verbAr, String verbEn) {
        String entityId = id == null ? null : String.valueOf(id);
        AuditEntry entry = AuditEntry.builder()
            .tenantId(entity instanceof AuditableEntity scoped ? scoped.getTenantId() : null)
            .action(action)
            .entityType(type.entityType())
            .entityId(entityId)
            .summaryAr(verbAr + " " + type.entityType() + " رقم " + entityId)
            .summaryEn(verbEn + " " + type.entityType() + " #" + entityId)
            .changes(changes)
            .build();
        AuditRecordingService service = recordingService.getObject();
        session.doWork(connection -> service.record(entry, connection));
    }

    private Optional<AuditedType> auditedType(Object entity, EntityPersister persister) {
        return auditedTypes.computeIfAbsent(persister.getMappedClass(), mapped -> {
            Audited audited = mapped.getAnnotation(Audited.class);
            if (audited == null) {
                return Optional.empty();
            }
            String entityType = audited.entityType().isBlank() ? persister.getEntityName() : audited.entityType();
            return Optional.of(new AuditedType(entityType, Set.of(audited.ignore())));
        });
    }

    @FunctionalInterface
    private interface PropertyVisitor {
        void visit(int index, String name, Type type);
    }

    /** Visits every persistent property that may appear in {@code CHANGES} (no collections). */
    private static void forEachRecordable(EntityPersister persister, AuditedType type, PropertyVisitor visitor) {
        String[] names = persister.getPropertyNames();
        Type[] types = persister.getPropertyTypes();
        for (int i = 0; i < names.length; i++) {
            if (types[i].isCollectionType() || !AuditEventDomain.isRecordable(names[i], type.ignored())) {
                continue;
            }
            visitor.visit(i, names[i], types[i]);
        }
    }

    /**
     * A property value as written to {@code CHANGES}: an association becomes the referenced id,
     * binary content is left out ({@code null}), everything else is kept for the recorder to turn
     * into a JSON scalar.
     */
    private static Object simplify(SharedSessionContractImplementor session, Type type, Object value) {
        if (value == null || value instanceof byte[] || value instanceof Byte[] || value instanceof char[]
            || value instanceof Blob || value instanceof Clob) {
            return null;
        }
        if (type.isEntityType()) {
            return session.getFactory().getPersistenceUnitUtil().getIdentifier(value);
        }
        return value;
    }

    private static Set<Integer> dirtyIndexes(int[] dirtyProperties) {
        return dirtyProperties == null ? Set.of()
            : Set.copyOf(Arrays.stream(dirtyProperties).boxed().toList());
    }
}
