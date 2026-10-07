package com.erp.file.crossmodule;

/** The outcome of {@link FileImageStoreApi#storePublicImage}: exactly one of {@code image} / {@code rejection}. */
public record ImageStoreResult(StoredImage image, ImageRejection rejection) {

    public static ImageStoreResult stored(StoredImage image) {
        return new ImageStoreResult(image, null);
    }

    public static ImageStoreResult rejected(ImageRejection rejection) {
        return new ImageStoreResult(null, rejection);
    }

    public boolean isStored() {
        return image != null;
    }
}
