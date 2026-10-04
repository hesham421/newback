package com.erp.file.entity;

import com.erp.common.converter.BooleanNumberConverter;
import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENTITY-FILE-002 — FileCategory (file category with per-category size/type limits). Source:
 * db-script-FILE.md DBS-FILE-001, DATA-DOM.md. categoryCode is the natural key (RULE-FILE-007),
 * normalized upper-case. maxSizeBytes / allowedContentTypes feed the per-category overrides used
 * by RULE-FILE-001/002. Persistence-only; the uniqueness decision lives in FileCategoryDomain.
 */
@Entity
@Table(name = "FILE_CATEGORY",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FILE_CATEGORY_CATEGORY_CODE", columnNames = {"CATEGORY_CODE"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FileCategory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_category_seq")
    @SequenceGenerator(name = "file_category_seq", sequenceName = "SEQ_FILE_CATEGORY", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "CATEGORY_CODE", length = 50, nullable = false)
    private String categoryCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 200, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 100, nullable = false)
    private String nameEn;

    @Column(name = "MAX_SIZE_BYTES")
    private Long maxSizeBytes;

    @Column(name = "ALLOWED_CONTENT_TYPES", columnDefinition = "TEXT")
    private String allowedContentTypes;

    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isActive = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (categoryCode != null) {
            categoryCode = categoryCode.trim().toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (categoryCode != null) {
            categoryCode = categoryCode.trim().toUpperCase();
        }
    }

    public void activate() {
        this.isActive = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActive = Boolean.FALSE;
    }
}
