package com.erp.file.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.file.crossmodule.FileImageStoreApi;
import com.erp.file.crossmodule.ImageRejection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * erp-core 1.3.0 (TM-D D.4) — RULE-FILE-008 (type from the bytes, size) and RULE-FILE-009 (SVG allow-list),
 * including every bypass of the review-round-1 probe (namespace prefixes, CSS escapes, SMIL, xml:base,
 * DOCTYPE / entities, non-UTF-8).
 */
class ImageValidationDomainServiceTest {

    private static final Set<String> RASTER = Set.of(FileImageStoreApi.TYPE_PNG, FileImageStoreApi.TYPE_JPEG,
        FileImageStoreApi.TYPE_WEBP);
    private static final Set<String> WITH_SVG = Set.of(FileImageStoreApi.TYPE_PNG, FileImageStoreApi.TYPE_JPEG,
        FileImageStoreApi.TYPE_WEBP, FileImageStoreApi.TYPE_SVG);
    private static final String NS = "xmlns=\"http://www.w3.org/2000/svg\"";
    private static final String BS = "\\";

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};

    @Test
    void detectsPngJpegWebpAndSvgFromTheBytes_andNothingElse() {
        assertThat(ImageValidationDomainService.detectType(PNG)).isEqualTo("image/png");
        assertThat(ImageValidationDomainService.detectType(JPEG)).isEqualTo("image/jpeg");
        assertThat(ImageValidationDomainService.detectType(WEBP)).isEqualTo("image/webp");
        assertThat(ImageValidationDomainService.detectType(utf8("<svg " + NS + "/>"))).isEqualTo("image/svg+xml");
        assertThat(ImageValidationDomainService.detectType(utf8("﻿<?xml version=\"1.0\"?>\n<!-- logo -->\n<svg"
            + " viewBox=\"0 0 1 1\"></svg>"))).isEqualTo("image/svg+xml");
        assertThat(ImageValidationDomainService.detectType(EXE)).isNull();
        assertThat(ImageValidationDomainService.detectType(utf8("<html><svg/></html>"))).isNull();
        assertThat(ImageValidationDomainService.detectType("GIF89a....".getBytes(StandardCharsets.US_ASCII))).isNull();
    }

    @Test
    void checksSizeThenTypeThenSvgSafety() {
        assertThat(ImageValidationDomainService.check(new byte[0], 10, RASTER).rejection()).isEqualTo(ImageRejection.EMPTY);
        assertThat(ImageValidationDomainService.check(PNG, PNG.length - 1L, RASTER).rejection())
            .isEqualTo(ImageRejection.TOO_LARGE);
        assertThat(ImageValidationDomainService.check(EXE, 1_000, RASTER).rejection())
            .isEqualTo(ImageRejection.TYPE_NOT_ALLOWED);
        assertThat(ImageValidationDomainService.check(utf8("<svg " + NS + "/>"), 1_000, RASTER).rejection())
            .as("photos never take SVG").isEqualTo(ImageRejection.TYPE_NOT_ALLOWED);
        ImageValidationDomainService.Verdict png = ImageValidationDomainService.check(PNG, PNG.length, RASTER);
        assertThat(png.accepted()).isTrue();
        assertThat(png.contentType()).isEqualTo("image/png");
        assertThat(ImageValidationDomainService.check(utf8("<svg " + NS + "><circle r=\"1\"/></svg>"), 1_000, WITH_SVG)
            .contentType()).isEqualTo("image/svg+xml");
        assertThat(ImageValidationDomainService.check(utf8("<svg><circle r=\"1\"/></svg>"), 1_000, WITH_SVG).rejection())
            .as("no SVG namespace: not an SVG image a browser renders").isEqualTo(ImageRejection.UNSAFE_SVG);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        // earlier cases
        "<svg NS><script>alert(1)</script></svg>",
        "<svg NS><SCRIPT src=\"x.js\"/></svg>",
        "<svg NS onload=\"alert(1)\"/>",
        "<svg/onload=alert(1)>",
        "<svg NS><rect onClick='x()'/></svg>",
        "<svg NS\tonload=\"x\"/>",
        "<svg NS\nonclick=\"x\"/>",
        "<svg NS><image href=\"https://evil.test/x.png\"/></svg>",
        "<svg NS><image href=\"data:image/svg+xml;base64,AAAA\"/></svg>",
        "<svg NS><image src=\"http://evil\"/></svg>",
        "<svg NS xmlns:xlink=\"http://www.w3.org/1999/xlink\"><use xlink:href=\"http://evil.test/s.svg#a\"/></svg>",
        "<svg NS><use href=\"http://evil/x.svg#a\"/></svg>",
        "<svg NS><use href = \"http://evil\"/></svg>",
        "<svg NS><a href=\"javascript:alert(1)\"><text>x</text></a></svg>",
        "<svg NS><a href=\"&#106;avascript:alert(1)\"><text>x</text></a></svg>",
        "<svg NS><a xlink:href=\"javascript:alert(1)\"><text>x</text></a></svg>",
        "<svg NS><rect style=\"fill:url(https://evil.test/p)\"/></svg>",
        "<svg NS><rect style=\"fill:url(&#104;ttp://evil/x)\"/></svg>",
        "<svg NS><rect fill=\"url( 'http://evil/x' )\"/></svg>",
        "<svg NS><style>@import 'http://evil';</style></svg>",
        "<svg NS><foreignObject><div/></foreignObject></svg>",
        "<svg NS><![CDATA[<script>]]></svg>",
        "<!DOCTYPE svg [<!ENTITY x \"y\">]><svg NS>&x;</svg>",
        "<?xml version=\"1.0\"?><!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><svg NS>&x;</svg>",
        "<!DOCTYPE l [<!ENTITY a \"aaaa\"><!ENTITY b \"&a;&a;\">]><svg NS>&b;</svg>",
        "<!DOCTYPE svg [<!ENTITY s \"&#60;script&#62;alert(1)&#60;/script&#62;\">]><svg NS>&s;</svg>",
        "<!DOCTYPE html><svg NS/>",
        "<!-- c --><svg NS><script/></svg>",
        "<?xml version=\"1.0\" x=\">\"?><svg NS/>",
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><svg NS/>",
        "<svg NS><?php echo 1; ?></svg>",
        // review round 1 bypasses
        "<svg NS xmlns:s=\"http://www.w3.org/2000/svg\"><s:script>alert(1)</s:script></svg>",
        "<svg NS><h:script xmlns:h=\"http://www.w3.org/1999/xhtml\">alert(1)</h:script></svg>",
        "<svg NS xmlns:x=\"http://www.w3.org/2000/svg\"><x:foreignObject><h:iframe"
            + " xmlns:h=\"http://www.w3.org/1999/xhtml\" src=\"//evil\"/></x:foreignObject></svg>",
        "<svg NS><a><animate attributeName=\"href\" to=\"https://evil\"/><text>x</text></a></svg>",
        "<svg NS><rect><animate attributeName=\"href\" to=\"https://evil\"/></rect></svg>",
        "<svg NS><set attributeName=\"onmouseover\" to=\"alert(1)\"/></svg>",
        "<svg NS><rect attributeName=\"href\"/></svg>",
        "<svg NS><style>@impBSort 'http://evil';</style></svg>",
        "<svg NS><style>rect{fill:uBSrl(http://evil/x)}</style></svg>",
        "<svg NS><rect style=\"fill:uBSrl(http://evil/x)\"/></svg>",
        "<svg NS xml:base=\"http://evil/\"><use href=\"#x\"/></svg>",
        "<svg NS><g xml:base=\"http://evil/\"/></svg>",
        "<svg NS xmlns:ink=\"http://www.inkscape.org/namespaces/inkscape\" ink:label=\"x\"/>",
    })
    void unsafeOrUnknownSvg_isRejected(String template) {
        String svg = template.replace("NS", NS).replace("BS", BS);
        assertThat(ImageValidationDomainService.isSafeSvg(svg)).as(svg).isFalse();
        assertThat(ImageValidationDomainService.check(utf8(svg), 10_000, WITH_SVG).rejection())
            .as(svg).isIn(ImageRejection.UNSAFE_SVG, ImageRejection.TYPE_NOT_ALLOWED);
    }

    @Test
    void nonUtf8Svg_isRejected() {
        String svg = "<svg " + NS + "><script>alert(1)</script></svg>";
        assertThat(ImageValidationDomainService.check(("﻿" + svg).getBytes(StandardCharsets.UTF_16BE), 10_000,
            WITH_SVG).accepted()).isFalse();
        assertThat(ImageValidationDomainService.check(("<svg " + NS + "/>").getBytes(StandardCharsets.UTF_16LE), 10_000,
            WITH_SVG).accepted()).isFalse();
        byte[] latin1 = ("<svg " + NS + "><title>café</title></svg>").getBytes(StandardCharsets.ISO_8859_1);
        assertThat(ImageValidationDomainService.isSafeSvg(latin1)).as("malformed UTF-8").isFalse();
    }

    @Test
    void svgWithOnlyLocalReferencesAndPresentation_isSafe() {
        String logo = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- logo -->\n<svg " + NS
            + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 10 10\" version=\"1.1\">"
            + "<title>Acme</title><defs><linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#fff\"/></linearGradient>"
            + "<style><![CDATA[.a{fill:url(#g);stroke:#000}]]></style></defs>"
            + "<rect class=\"a\" width=\"10\" height=\"10\" fill=\"url(#g)\" style=\"opacity:.5\"/>"
            + "<use href=\"#g\"/><use xlink:href='#g'/><text x=\"1\" y=\"8\" font-size=\"2\" xml:space=\"preserve\">"
            + "&#60;script&#62; one</text><path d=\"M0 0L10 10\"/></svg>";
        assertThat(ImageValidationDomainService.isSafeSvg(logo)).isTrue();
        assertThat(ImageValidationDomainService.check(utf8(logo), 10_000, WITH_SVG).contentType()).isEqualTo("image/svg+xml");
        assertThat(ImageValidationDomainService.isSafeSvg("<svg " + NS + "><rect width=\"1\" height=\"1\"/></svg>")).isTrue();
    }

    @Test
    void rasterMagicFollowedByMarkup_staysTheRasterType() {
        assertThat(ImageValidationDomainService.check(concat(Arrays.copyOf(PNG, 8), utf8("<html><script>")), 1_000, RASTER)
            .contentType()).isEqualTo("image/png");
        assertThat(ImageValidationDomainService.check(concat(Arrays.copyOf(JPEG, 3), utf8("<script>")), 1_000, RASTER)
            .contentType()).isEqualTo("image/jpeg");
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
