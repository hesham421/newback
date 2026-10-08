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
 * DOCTYPE / entities, non-UTF-8) and of round 2 (processing instructions, split style text, URL-less CSS
 * fetches, nested {@code <use>}, depth).
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

    /** Review round 2: the reviewer's SvgProbe2 attack set plus the CSS-grammar cases (NS / BS placeholders). */
    @ParameterizedTest
    @ValueSource(strings = {
        // processing instructions anywhere (document level included)
        "<?xml version=\"1.0\"?><?xml-stylesheet type=\"text/css\" href=\"http://evil/x.css\"?><svg NS><rect/></svg>",
        "<?xml-stylesheet type=\"text/xsl\" href=\"/x.xsl\"?><svg NS><rect/></svg>",
        "<svg NS><rect/></svg><?xml-stylesheet href=\"http://evil/x.css\"?>",
        "<svg NS><?xml-stylesheet href=\"http://evil/x.css\"?><rect/></svg>",
        // style text split by comments or CDATA
        "<svg NS><style>@imp<!-- x -->ort 'http://evil/x.css';</style></svg>",
        "<svg NS><style>rect{fill:u<!-- x -->rl(http://evil/x)}</style></svg>",
        "<svg NS><style>rect{fill:u<![CDATA[rl(http://evil/x)}]]></style></svg>",
        "<svg NS><style>rect{fill:url(#a)} x{behavior:java<!---->script:alert(1)}</style></svg>",
        "<svg NS><style><!-- rect{fill:red} --></style></svg>",
        "<svg NS><style>rect{fill:red}<title>x</title></style></svg>",
        // CSS functions and rules that fetch without url(
        "<svg NS><style>svg{background-image:image-set('http://evil/x.png' 1x)}</style></svg>",
        "<svg NS><style>svg{background-image:-webkit-image-set('x.png' 1x)}</style></svg>",
        "<svg NS><style>svg{background-image:image-set(\"x.png\" 1x)}</style></svg>",
        "<svg NS><style>svg{background:image('x.png')}</style></svg>",
        "<svg NS><style>svg{background:cross-fade(url(#a), 'x.png')}</style></svg>",
        "<svg NS><style>svg{background:src('x.png')}</style></svg>",
        "<svg NS><style>svg{background:element(#a)}</style></svg>",
        "<svg NS><style>svg{background:paint(x)}</style></svg>",
        "<svg NS><style>@font-face{font-family:x;src:url(http://evil/f.woff)}</style></svg>",
        "<svg NS><style>@font-face{font-family:x;src:local(x)}</style></svg>",
        "<svg NS><style>@namespace svg url(http://www.w3.org/2000/svg);</style></svg>",
        "<svg NS><style>@charset 'utf-8';</style></svg>",
        "<svg NS><style>rect{fill:url( '//evil/x' )}</style></svg>",
        "<svg NS><rect style=\"fill:image-set('http://evil/x.png' 1x)\"/></svg>",
        "<svg NS><rect style=\"cursor:url(x.cur),auto\"/></svg>",
        "<svg NS><rect fill=\"image-set('//evil/x.png' 1x)\"/></svg>",
        "<svg NS><rect style=\"fill:ur/**/l(http://evil/x)\"/></svg>",
        "<svg NS><style>rect{fill:url(data:image/png;base64,AA)}</style></svg>",
        "<svg NS><style>rect{-moz-binding:url(#x)}</style></svg>",
        "<svg NS><rect style=\"fill:url(#x);stroke:url(http://evil)\"/></svg>",
        "<svg NS><rect style=\"fill:u&#x5c;rl(http://evil)\"/></svg>",
        "<svg NS><style>rect{fill:u&#x5c;rl(http://evil)}</style></svg>",
        // namespaces, roots, elements and attributes of SvgProbe2
        "<svg NS><Script>alert(1)</Script></svg>",
        "<SVG NS/>",
        "<svg NS><g xmlns=\"http://www.w3.org/1999/xhtml\"><script>alert(1)</script></g></svg>",
        "<svg xmlns=\"http://www.w3.org/1999/xhtml\"><script>alert(1)</script></svg>",
        "<svg NS xmlns:s=\"http://www.w3.org/2000/svg\" s:onload=\"alert(1)\"/>",
        "<svg NS><use href=\"#x javascript:alert(1)\"/></svg>",
        "<svg NS xmlns:xlink=\"http://www.w3.org/1999/xlink\"><use xlink:href=\"data:image/svg+xml,x#a\"/></svg>",
        "<svg NS><text><![CDATA[x]]></text></svg>",
        "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"><svg NS/>",
        "<svg NS data-x=\"javascript:alert(1)\"/>",
        "<svg NS><g data-src=\"//evil/x\"/></svg>",
        // still refused on purpose: editor metadata (export plain / optimised SVG)
        "<svg NS xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" viewBox=\"0 0 10 10\"><metadata id=\"m\"><rdf:RDF/></metadata><rect width=\"10\" height=\"10\"/></svg>",
        "<svg NS xmlns:sodipodi=\"http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd\" viewBox=\"0 0 10 10\"><sodipodi:namedview id=\"n\"/><rect width=\"10\" height=\"10\"/></svg>",
        "<svg NS xmlns:inkscape=\"http://www.inkscape.org/namespaces/inkscape\"><g inkscape:label=\"L\"/></svg>",
        // nested <use> references (renderer amplification)
        "<svg NS><defs><rect id=\"a\"/><use id=\"b\" href=\"#a\"/></defs><use href=\"#b\"/></svg>",
        "<svg NS><defs><rect id=\"a\"/><g id=\"b\"><use href=\"#a\"/></g></defs><use href=\"#b\"/></svg>",
        "<svg NS><g id=\"self\"><use href=\"#self\"/></g></svg>",
    })
    void roundTwoAttacksAndRefusedExports_areRejected(String template) {
        String svg = template.replace("NS", NS).replace("BS", BS);
        assertThat(ImageValidationDomainService.isSafeSvg(svg)).as(svg).isFalse();
    }

    /** Review round 2: the legitimate exports of SvgProbe2 (Figma, Illustrator incl. data-name, Sketch) still pass. */
    @ParameterizedTest
    @ValueSource(strings = {
        "<svg NS><defs><rect id=\"x\" width=\"1\" height=\"1\"/></defs><use href=\"#x\"/></svg>",
        "﻿<svg NS><rect width=\"1\" height=\"1\"/></svg>",
        "﻿<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg NS/>",
        "<!-- logo --><svg NS><rect/></svg>",
        "<svg NS><!-- <script>alert(1)</script> --><rect/></svg>",
        "<svg width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" NS><path fill-rule=\"evenodd\" clip-rule=\"evenodd\" d=\"M12 2L2 22h20L12 2z\" fill=\"#0A84FF\"/></svg>",
        "<svg width=\"40\" height=\"40\" viewBox=\"0 0 40 40\" fill=\"none\" NS><g clip-path=\"url(#clip0_1_2)\"><rect width=\"40\" height=\"40\" rx=\"8\" fill=\"#111\"/></g><defs><clipPath id=\"clip0_1_2\"><rect width=\"40\" height=\"40\" fill=\"white\"/></clipPath></defs></svg>",
        "<svg NS viewBox=\"0 0 100 100\"><defs><linearGradient id=\"g\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\"><stop offset=\"0\" stop-color=\"#f00\"/><stop offset=\"1\" stop-color=\"#00f\" stop-opacity=\".5\"/></linearGradient></defs><circle cx=\"50\" cy=\"50\" r=\"40\" fill=\"url(#g)\"/><text x=\"50\" y=\"55\" font-family=\"'Segoe UI', Arial\" text-anchor=\"middle\">ACME</text></svg>",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!-- Generator: Adobe Illustrator 24.0.0, SVG Export Plug-In . SVG Version: 6.00 Build 0)  -->\n<svg version=\"1.1\" id=\"Layer_1\" NS xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\" viewBox=\"0 0 100 100\" style=\"enable-background:new 0 0 100 100;\" xml:space=\"preserve\">\n<style type=\"text/css\">\n\t.st0{fill:#FF0000;}\n</style>\n<circle class=\"st0\" cx=\"50\" cy=\"50\" r=\"40\"/>\n</svg>",
        "<svg NS viewBox=\"0 0 10 10\"><g id=\"Layer_2\" data-name=\"Layer 2 (final)\"><rect width=\"10\" height=\"10\"/></g></svg>",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><svg width=\"20px\" height=\"20px\" viewBox=\"0 0 20 20\" version=\"1.1\" NS xmlns:xlink=\"http://www.w3.org/1999/xlink\"><title>logo</title><desc>Created with Sketch.</desc><g id=\"Page-1\" stroke=\"none\" stroke-width=\"1\" fill=\"none\" fill-rule=\"evenodd\"><circle fill=\"#D8D8D8\" cx=\"10\" cy=\"10\" r=\"10\"></circle></g></svg>",
        "<svg NS><style>.a{fill:rgb(1,2,3);stroke:hsl(10 50% 50%)} @media (prefers-color-scheme: dark) and (min-width: 10px){.a{fill:#fff}} /* note */</style><rect class=\"a\" transform=\"translate(1 2) rotate(45 5 5) scale(2)\"/></svg>",
        "<svg NS><style><![CDATA[.b{fill:url(#g);filter:drop-shadow(0 0 1px #000)}]]></style><rect class=\"b\"/></svg>",
    })
    void roundTwoLegitimateExports_areAccepted(String template) {
        String svg = template.replace("NS", NS);
        assertThat(ImageValidationDomainService.isSafeSvg(svg)).as(svg).isTrue();
    }

    @Test
    void useAmplificationAndDepth_areBounded() {
        StringBuilder amplified = new StringBuilder("<svg " + NS + "><defs><rect id=\"a0\" width=\"1\" height=\"1\"/>");
        for (int i = 1; i <= 12; i++) {
            amplified.append("<g id=\"a").append(i).append("\">");
            for (int j = 0; j < 10; j++) {
                amplified.append("<use href=\"#a").append(i - 1).append("\"/>");
            }
            amplified.append("</g>");
        }
        amplified.append("</defs><use href=\"#a12\"/></svg>");
        assertThat(ImageValidationDomainService.isSafeSvg(amplified.toString())).as("10^12 instances").isFalse();

        assertThat(ImageValidationDomainService.isSafeSvg(uses(SvgAllowList.MAX_USE_ELEMENTS))).isTrue();
        assertThat(ImageValidationDomainService.isSafeSvg(uses(SvgAllowList.MAX_USE_ELEMENTS + 1))).isFalse();

        assertThat(ImageValidationDomainService.isSafeSvg(nested(SvgAllowList.MAX_DEPTH))).isTrue();
        assertThat(ImageValidationDomainService.isSafeSvg(nested(SvgAllowList.MAX_DEPTH + 1))).isFalse();
        assertThat(ImageValidationDomainService.isSafeSvg(nested(140_000))).isFalse();

        StringBuilder wide = new StringBuilder("<svg " + NS + ">");
        while (wide.length() < 1_000_000) {
            wide.append("<rect/>");
        }
        assertThat(ImageValidationDomainService.isSafeSvg(wide.append("</svg>").toString())).as("wide, 1 MB").isTrue();
    }

    /**
     * Package D review round 3, fixed in E: browsers resolve a duplicated id to the first element in tree order;
     * flat decoys placed after the real targets must not hide a nested {@code <use>} chain (91 uses, 10^9 instances).
     */
    @Test
    void duplicateIds_areRefused_soNoDecoyHidesANestedUseChain() {
        StringBuilder decoy = new StringBuilder("<svg " + NS + "><defs><rect id=\"g0\" width=\"1\" height=\"1\"/>");
        for (int level = 1; level <= 9; level++) {
            decoy.append("<g id=\"g").append(level).append("\">");
            for (int j = 0; j < 10; j++) {
                decoy.append("<use href=\"#g").append(level - 1).append("\"/>");
            }
            decoy.append("</g>");
        }
        for (int level = 1; level <= 9; level++) {
            decoy.append("<rect id=\"g").append(level).append("\"/>");
        }
        decoy.append("</defs><use href=\"#g9\"/></svg>");
        assertThat(ImageValidationDomainService.isSafeSvg(decoy.toString())).as("decoy chain").isFalse();
        assertThat(ImageValidationDomainService.check(utf8(decoy.toString()), 1_048_576L, WITH_SVG).rejection())
            .isEqualTo(ImageRejection.UNSAFE_SVG);

        assertThat(ImageValidationDomainService.isSafeSvg("<svg " + NS + "><rect id=\"a\"/><circle id=\"a\" r=\"1\"/></svg>"))
            .as("any duplicate id, even without <use>").isFalse();
        assertThat(ImageValidationDomainService.isSafeSvg("<svg " + NS + "><rect id=\"a\"/><circle id=\"b\" r=\"1\"/>"
            + "<use href=\"#a\"/><use href=\"#b\"/></svg>")).as("unique ids").isTrue();
    }

    /** {@code count} flat {@code <use>} references to one rectangle. */
    private static String uses(int count) {
        return "<svg " + NS + "><defs><rect id=\"r\" width=\"1\" height=\"1\"/></defs>"
            + "<use href=\"#r\"/>".repeat(count) + "</svg>";
    }

    /** An {@code <svg>} root (depth 1) holding {@code depth - 1} nested {@code <g>}. */
    private static String nested(int depth) {
        return "<svg " + NS + ">" + "<g>".repeat(depth - 1) + "</g>".repeat(depth - 1) + "</svg>";
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
