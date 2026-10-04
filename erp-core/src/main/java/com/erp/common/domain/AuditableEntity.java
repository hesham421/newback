package com.erp.common.domain;

import com.erp.common.audit.AuditEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditEntityListener.class)
public abstract class AuditableEntity {

    // length = 100 mirrors the NARROWEST live physical width: SEC/MDL/FIN (16 tables) define
    // CREATED_BY/UPDATED_BY as VARCHAR(100); CU/NOTIF/FILE (6 tables) are intentionally wider at
    // VARCHAR(255). The value itself is the SEC username (JWT subject), itself capped at 100 by
    // SEC_USER.username, so 100 is the true upper bound — do NOT widen this back "for consistency".
    @Column(name = "CREATED_BY", updatable = false, length = 100)
    private String createdBy;

    @Column(name = "CREATED_AT", updatable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Column(name = "UPDATED_AT")
    private Instant updatedAt;
}
