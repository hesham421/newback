package com.erp.common.search;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableBuilder {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private PageableBuilder() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static Pageable from(SearchRequest searchRequest, Set<String> allowedSortFields) {
        int page = Math.max(searchRequest.getPage(), 0);
        int size = searchRequest.getSize() <= 0
            ? DEFAULT_PAGE_SIZE
            : Math.min(searchRequest.getSize(), MAX_PAGE_SIZE);

        String sortField = searchRequest.getSortField();
        if (sortField == null || !allowedSortFields.contains(sortField)) {
            return PageRequest.of(page, size);
        }
        Sort.Direction direction = searchRequest.getSortDirection() != null
            ? searchRequest.getSortDirection() : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}
