package com.erp.file.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.entity.FileCategory;
import com.erp.file.exception.FileErrorCodes;

/**
 * Domain companion for ENTITY-FILE-002 (FileCategory) — enforces RULE-FILE-007 (unique
 * categoryCode). The service resolves the "code already taken" fact (via an existsBy check) and
 * passes it in; this class only decides. No Spring/JPA annotations, no repository access;
 * constructed only via the static factories.
 */
public final class FileCategoryDomain {

    private final String categoryCode;

    private FileCategoryDomain(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    /** RULE-FILE-007 — construction-time uniqueness guard for a new category. */
    public static FileCategoryDomain create(String categoryCode, boolean codeAlreadyTaken) {
        if (codeAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FileErrorCodes.FILE_CATEGORY_CODE_DUPLICATE, categoryCode);
        }
        return new FileCategoryDomain(categoryCode);
    }

    /** Reconstructs a Domain view over a persisted entity. */
    public static FileCategoryDomain from(FileCategory entity) {
        return new FileCategoryDomain(entity.getCategoryCode());
    }

    /** RULE-FILE-007 — decision only: guards a categoryCode change against an existing code. */
    public void assertCodeAvailable(boolean codeAlreadyTaken) {
        if (codeAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FileErrorCodes.FILE_CATEGORY_CODE_DUPLICATE, categoryCode);
        }
    }

    public String getCategoryCode() {
        return categoryCode;
    }
}
