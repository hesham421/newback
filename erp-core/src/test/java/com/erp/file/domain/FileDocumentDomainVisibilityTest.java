package com.erp.file.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.entity.FileDocument;
import org.junit.jupiter.api.Test;

/** erp-core step 07 — the publish decisions of {@link FileDocumentDomain} and the entity's visibility helpers. */
class FileDocumentDomainVisibilityTest {

    private static FileDocument document(String status) {
        return FileDocument.builder().fileStatusId(status).build();
    }

    @Test
    void assertCanBePublic_refusesACategoryThatDoesNotAllowPublic_with409() {
        FileDocumentDomain domain = FileDocumentDomain.from(document(FileDocumentDomain.STATUS_ACTIVE));

        assertThatCode(() -> domain.assertCanBePublic(true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> domain.assertCanBePublic(false))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.CONFLICT);
                assertThat(e.getErrorCode()).isEqualTo("FILE_PUBLIC_NOT_ALLOWED");
            });
    }

    @Test
    void assertNotDeleted_treatsASoftDeletedDocumentAsNotFound() {
        assertThatCode(() -> FileDocumentDomain.from(document(FileDocumentDomain.STATUS_ARCHIVED)).assertNotDeleted(1L))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> FileDocumentDomain.from(document(FileDocumentDomain.STATUS_DELETED)).assertNotDeleted(1L))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
                assertThat(e.getErrorCode()).isEqualTo("FILE_DOCUMENT_NOT_FOUND");
            });
    }

    @Test
    void entityHelpers_publishSetsSlugAndVisibility_unpublishClearsBoth_newDocumentsArePrivate() {
        FileDocument document = document(FileDocumentDomain.STATUS_ACTIVE);
        assertThat(document.getVisibility()).isEqualTo(FileDocumentDomain.VISIBILITY_PRIVATE);

        document.publish("slug-1");
        assertThat(document.getVisibility()).isEqualTo("PUBLIC");
        assertThat(document.getPublicSlug()).isEqualTo("slug-1");

        document.unpublish();
        assertThat(document.getVisibility()).isEqualTo("PRIVATE");
        assertThat(document.getPublicSlug()).isNull();
    }
}
