package com.erp.common.search;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public final class ActiveFlagQueryHelper {

    private ActiveFlagQueryHelper() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static Predicate isActive(Root<?> root, CriteriaBuilder cb, String activeFieldName, Boolean active) {
        if (active == null) {
            return cb.conjunction();
        }
        return cb.equal(root.get(activeFieldName), active);
    }
}
