package com.erp.common.search;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import java.util.List;
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

    /**
     * erp-core 1.2.0: a page whose rows would start past {@link Integer#MAX_VALUE} (e.g.
     * {@code page = 2147483647}) is rejected with 400 {@code VALIDATION_ERROR} on field {@code page}.
     * JPA's first-result is an {@code int}: such an offset used to overflow into a negative value and
     * surface as a 500.
     */
    public static Pageable from(SearchRequest searchRequest, Set<String> allowedSortFields) {
        int page = Math.max(searchRequest.getPage(), 0);
        int size = searchRequest.getSize() <= 0
            ? DEFAULT_PAGE_SIZE
            : Math.min(searchRequest.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size + size > Integer.MAX_VALUE) {
            throw LocalizedException.withDetails(Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR,
                List.of(ErrorDetail.ofField("page", CommonErrorCodes.VALIDATION_ERROR)));
        }

        String sortField = searchRequest.getSortField();
        if (sortField == null || !allowedSortFields.contains(sortField)) {
            return PageRequest.of(page, size);
        }
        Sort.Direction direction = searchRequest.getSortDirection() != null
            ? searchRequest.getSortDirection() : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
}
