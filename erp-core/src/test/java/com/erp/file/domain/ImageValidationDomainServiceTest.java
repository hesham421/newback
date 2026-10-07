package com.erp.file.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.file.crossmodule.FileImageStoreApi;
import com.erp.file.crossmodule.ImageRejection;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** erp-core 1.3.0 (TM-D D.4) — RULE-FILE-008 (type from the bytes, size) and RULE-FILE-009 (SVG safety). */
class ImageValidationDomainServiceTest {

    private static final Set<String> RASTER = Set.of(FileImageStoreApi.TYPE_PNG, FileImageStoreApi.TYPE_JPEG,
        FileImageStoreApi.TYPE_WEBP);
    private static final Set<String> WITH_SVG = Set.of(FileImageStoreApi.TYPE_PNG, FileImageStoreApi.TYPE_JPEG,
        FileImageStoreApi.TYPE_WEBP, FileImageStoreApi.TYPE_SVG);

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};

    @Test
    void detectsPngJpegWebpAndSvgFromTheBytes_andNothingElse() {
        assertThat(ImageValidationDomainService.detectType(PNG)).isEqualTo("image/png");
        assertThat(ImageValidationDomainService.detectType(JPEG)).isEqualTo("image/jpeg");
        assertThat(ImageValidationDomainService.detectType(WEBP)).isEqualTo("image/webp");
        assertThat(ImageValidationDomainService.detectType(svg("<svg xmlns=\"http://www.w3.org/2000/svg\"/>")))
            .isEqualTo("image/svg+xml");
        assertThat(ImageValidationDomainService.detectType(svg("﻿<?xml version=\"1.0\"?>\n<!-- logo -->\n<svg"
            + " viewBox=\"0 0 1 1\"></svg>"))).isEqualTo("image/svg+xml");
        assertThat(ImageValidationDomainService.detectType(EXE)).isNull();
        assertThat(ImageValidationDomainService.detectType(svg("<html><svg/></html>"))).isNull();
        assertThat(ImageValidationDomainService.detectType("GIF89a....".getBytes(StandardCharsets.US_ASCII))).isNull();
    }

    @Test
    void checksSizeThenTypeThenSvgSafety() {
        assertThat(ImageValidationDomainService.check(new byte[0], 10, RASTER).rejection()).isEqualTo(ImageRejection.EMPTY);
        assertThat(ImageValidationDomainService.check(PNG, PNG.length - 1L, RASTER).rejection())
            .isEqualTo(ImageRejection.TOO_LARGE);
        assertThat(ImageValidationDomainService.check(EXE, 1_000, RASTER).rejection())
            .isEqualTo(ImageRejection.TYPE_NOT_ALLOWED);
        assertThat(ImageValidationDomainService.check(svg("<svg/>"), 1_000, RASTER).rejection())
            .as("photos never take SVG").isEqualTo(ImageRejection.TYPE_NOT_ALLOWED);
        ImageValidationDomainService.Verdict png = ImageValidationDomainService.check(PNG, PNG.length, RASTER);
        assertThat(png.accepted()).isTrue();
        assertThat(png.contentType()).isEqualTo("image/png");
        assertThat(ImageValidationDomainService.check(svg("<svg><circle r=\"1\"/></svg>"), 1_000, WITH_SVG).contentType())
            .isEqualTo("image/svg+xml");
    }

    @Test
    void svgWithActiveOrExternalContent_isUnsafe() {
        assertUnsafe("<svg><script>alert(1)</script></svg>");
        assertUnsafe("<svg><SCRIPT src=\"x.js\"/></svg>");
        assertUnsafe("<svg onload=\"alert(1)\"/>");
        assertUnsafe("<svg><rect onClick='x()'/></svg>");
        assertUnsafe("<svg><image href=\"https://evil.test/x.png\"/></svg>");
        assertUnsafe("<svg><use xlink:href=\"http://evil.test/s.svg#a\"/></svg>");
        assertUnsafe("<svg><a href=\"javascript:alert(1)\"/></svg>");
        assertUnsafe("<svg><rect style=\"fill:url(https://evil.test/p)\"/></svg>");
        assertUnsafe("<svg><foreignObject><div/></foreignObject></svg>");
        assertUnsafe("<!DOCTYPE svg [<!ENTITY x \"y\">]><svg>&x;</svg>");
    }

    @Test
    void svgWithOnlyInternalReferences_isSafe() {
        assertThat(ImageValidationDomainService.isSafeSvg("<svg xmlns=\"http://www.w3.org/2000/svg\""
            + " xmlns:xlink=\"http://www.w3.org/1999/xlink\"><defs><linearGradient id=\"g\"/></defs>"
            + "<rect fill=\"url(#g)\"/><use href=\"#g\"/><use xlink:href='#g'/><text font-size=\"2\">one</text></svg>"))
            .isTrue();
    }

    private static void assertUnsafe(String text) {
        assertThat(ImageValidationDomainService.check(svg(text), 10_000, WITH_SVG).rejection())
            .as(text).isIn(ImageRejection.UNSAFE_SVG, ImageRejection.TYPE_NOT_ALLOWED);
        assertThat(ImageValidationDomainService.isSafeSvg(text)).as(text).isFalse();
    }

    private static byte[] svg(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
