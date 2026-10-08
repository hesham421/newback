package com.erp.sec.service;

import com.erp.file.crossmodule.FileDocumentLookupApi;
import com.erp.sec.entity.User;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * tenant-maturity D (XM-SEC-006) — turns {@code SEC_USER.PHOTO_FILE_ID} into the public URL clients get
 * ({@code photoUrl}) through FILE's {@link FileDocumentLookupApi}; a page of users costs one FILE query.
 * A discarded or missing document simply yields no URL.
 */
@Component
@RequiredArgsConstructor
public class UserPhotoUrls {

    private final FileDocumentLookupApi fileDocumentLookupApi;

    /** The photo URL of {@code user}, or {@code null}. */
    public String of(User user) {
        return user == null || user.getPhotoFileId() == null ? null
            : fileDocumentLookupApi.publicUrl(user.getPhotoFileId()).orElse(null);
    }

    /** Photo URLs keyed by {@code userPk}; users without a servable photo are absent. */
    public Map<Long, String> of(Collection<User> users) {
        Map<Long, String> urlsByDocument = fileDocumentLookupApi.publicUrls(
            users.stream().map(User::getPhotoFileId).filter(Objects::nonNull).toList());
        Map<Long, String> urlsByUser = new HashMap<>();
        for (User user : users) {
            String url = user.getPhotoFileId() == null ? null : urlsByDocument.get(user.getPhotoFileId());
            if (url != null) {
                urlsByUser.put(user.getUserPk(), url);
            }
        }
        return urlsByUser;
    }
}
