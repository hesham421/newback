package com.erp.common.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * erp-core 1.2.0 — a page whose rows would start past {@code Integer.MAX_VALUE} is a 400
 * {@code VALIDATION_ERROR} on {@code page} instead of an int overflow (500) in JPA's first-result.
 */
class PageableBuilderTest {

    @Test
    void theLargestRepresentablePage_isAccepted() {
        int size = 20;
        int lastPage = (Integer.MAX_VALUE - size) / size;

        Pageable pageable = PageableBuilder.from(request(lastPage, size), Set.of());

        assertThat(pageable.getPageNumber()).isEqualTo(lastPage);
        assertThat(pageable.getOffset() + pageable.getPageSize()).isLessThanOrEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void onePageFurther_isAValidationErrorOnPage() {
        int size = 20;
        int tooFar = (Integer.MAX_VALUE - size) / size + 1;

        assertRejected(request(tooFar, size));
    }

    @Test
    void intMaxPage_isAValidationErrorOnPage() {
        assertRejected(request(Integer.MAX_VALUE, 20));
        assertRejected(request(Integer.MAX_VALUE, 0));    // default size applies
        assertRejected(request(Integer.MAX_VALUE, 1));
    }

    @Test
    void negativePageAndOversizedSize_keepTheirExistingClamping() {
        Pageable pageable = PageableBuilder.from(request(-5, 10_000), Set.of());

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(200);
    }

    private static void assertRejected(SearchRequest request) {
        assertThatThrownBy(() -> PageableBuilder.from(request, Set.of()))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                assertThat(e.getErrorCode()).isEqualTo(CommonErrorCodes.VALIDATION_ERROR);
                assertThat(e.getErrors()).singleElement().satisfies(detail -> {
                    assertThat(detail.field()).isEqualTo("page");
                    assertThat(detail.errorCode()).isEqualTo(CommonErrorCodes.VALIDATION_ERROR);
                });
            });
    }

    private static SearchRequest request(int page, int size) {
        return SearchRequest.builder().page(page).size(size).build();
    }
}
