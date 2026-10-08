package com.erp.file.crossmodule;

import java.time.Instant;

/** A single-use download token for {@code GET /api/v1/files/download?token=} and the instant it expires. */
public record DownloadGrant(String token, Instant expiresAt) {
}
