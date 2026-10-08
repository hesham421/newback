package com.erp.file.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.exception.FileErrorCodes;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** tenant-maturity C5 review round 1 — RULE-FILE-012: restricted documents are 404 without the authority; deletion purges. */
class FileDocumentDomainRestrictedTest {

    @Test
    void aRestrictedDocument_isNotFoundWithoutItsAuthority_andAnOrdinaryOneIsAlwaysVisible() {
        assertThatCode(() -> FileDocumentDomain.assertVisibleTo(7L, null, Set.of())).doesNotThrowAnyException();
        assertThatCode(() -> FileDocumentDomain.assertVisibleTo(7L, "PLATFORM_TENANT_MANAGE",
            Set.of("PERM_FILE_BROWSER_VIEW", "PLATFORM_TENANT_MANAGE"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> FileDocumentDomain.assertVisibleTo(7L, "PLATFORM_TENANT_MANAGE",
            Set.of("PERM_FILE_BROWSER_VIEW", "PERM_FILE_BROWSER_DELETE")))
            .isInstanceOf(LocalizedException.class)
            .satisfies(e -> {
                assertThat(((LocalizedException) e).getErrorCode()).isEqualTo(FileErrorCodes.FILE_DOCUMENT_NOT_FOUND);
                assertThat(((LocalizedException) e).getStatus()).isEqualTo(Status.NOT_FOUND);
            });
    }

    @Test
    void onlyDeletingARestrictedDocument_purgesItsContent() {
        assertThat(FileDocumentDomain.purgesContentOn("PLATFORM_TENANT_MANAGE", FileDocumentDomain.STATUS_DELETED)).isTrue();
        assertThat(FileDocumentDomain.purgesContentOn("PLATFORM_TENANT_MANAGE", FileDocumentDomain.STATUS_ARCHIVED)).isFalse();
        assertThat(FileDocumentDomain.purgesContentOn(null, FileDocumentDomain.STATUS_DELETED)).isFalse();
    }
}
