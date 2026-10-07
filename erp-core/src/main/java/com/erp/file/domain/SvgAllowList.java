package com.erp.file.domain;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * RULE-FILE-009 (tenant-maturity D.4, review rounds 1–2) — an SVG is accepted only when a hardened,
 * namespace-aware parse succeeds, the document holds comments and one {@code <svg>} root only, and the tree
 * passes the allow-lists below (elements, attributes, local references, CSS) and the {@code <use>} limits.
 * The full rule is RULE-FILE-009 in {@code governance/analysis/modules/FILE/P1/srs.md} (1.3.0 addendum).
 */
final class SvgAllowList {

    static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    /** Static drawing elements only: no script, foreignObject, a, image, feImage, animation, switch or metadata. */
    private static final Set<String> ELEMENTS = Set.of(
        "svg", "g", "defs", "symbol", "use", "title", "desc", "style",
        "path", "rect", "circle", "ellipse", "line", "polyline", "polygon",
        "text", "tspan", "textPath",
        "linearGradient", "radialGradient", "stop", "clipPath", "mask", "pattern", "marker",
        "filter", "feBlend", "feColorMatrix", "feComponentTransfer", "feFuncR", "feFuncG", "feFuncB", "feFuncA",
        "feComposite", "feDropShadow", "feFlood", "feGaussianBlur", "feMerge", "feMergeNode", "feMorphology",
        "feOffset", "feTile");

    /** Geometry, presentation and paint attributes; no event handler, no {@code attributeName}, no {@code src}. */
    private static final Set<String> ATTRIBUTES = Set.of(
        "id", "class", "style", "lang", "x", "y", "x1", "y1", "x2", "y2", "cx", "cy", "r", "rx", "ry", "fx", "fy", "fr",
        "width", "height", "d", "points", "transform", "viewBox", "preserveAspectRatio", "version", "baseProfile",
        "href", "fill", "fill-opacity", "fill-rule", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin",
        "stroke-miterlimit", "stroke-dasharray", "stroke-dashoffset", "stroke-opacity", "opacity", "color",
        "display", "visibility", "overflow", "clip-path", "clip-rule", "mask", "filter", "offset", "stop-color",
        "stop-opacity", "gradientUnits", "gradientTransform", "spreadMethod", "patternUnits", "patternContentUnits",
        "patternTransform", "clipPathUnits", "maskUnits", "maskContentUnits", "filterUnits", "primitiveUnits",
        "markerWidth", "markerHeight", "markerUnits", "refX", "refY", "orient", "marker-start", "marker-mid",
        "marker-end", "font-family", "font-size", "font-weight", "font-style", "font-variant", "font-stretch",
        "text-anchor", "dominant-baseline", "alignment-baseline", "baseline-shift", "letter-spacing", "word-spacing",
        "text-decoration", "writing-mode", "dx", "dy", "rotate", "textLength", "lengthAdjust", "startOffset",
        "method", "spacing", "side", "pathLength", "in", "in2", "result", "stdDeviation", "mode", "operator",
        "k1", "k2", "k3", "k4", "values", "type", "tableValues", "slope", "intercept", "amplitude", "exponent",
        "radius", "flood-color", "flood-opacity", "lighting-color", "color-interpolation",
        "color-interpolation-filters", "shape-rendering", "text-rendering", "image-rendering", "vector-effect",
        "mix-blend-mode", "isolation", "paint-order", "media", "title");

    private static final Set<String> XML_ATTRIBUTES = Set.of("space", "lang");

    /** Attributes whose value is free text, never CSS: {@code title} and inert {@code data-*}. */
    private static final Pattern DATA_ATTRIBUTE = Pattern.compile("data-[a-z0-9_.-]+");

    /** Deepest element nesting accepted (logos nest far less; the walk is recursive). */
    static final int MAX_DEPTH = 64;

    /** Most {@code <use>} elements in one document (renderer amplification). */
    static final int MAX_USE_ELEMENTS = 100;

    /**
     * CSS functions that never fetch anything; any other function ({@code image-set}, {@code image},
     * {@code src}, {@code cross-fade}, {@code element}, {@code paint}, vendor prefixes, ...) is refused.
     * {@code url} is allowed only as {@code url(#…)}.
     */
    private static final Set<String> CSS_FUNCTIONS = Set.of(
        "url", "rgb", "rgba", "hsl", "hsla", "hwb", "lab", "lch", "oklab", "oklch", "color", "calc", "min", "max",
        "clamp", "var", "linear-gradient", "radial-gradient", "conic-gradient", "repeating-linear-gradient",
        "repeating-radial-gradient", "matrix", "matrix3d", "translate", "translatex", "translatey", "translate3d",
        "scale", "scalex", "scaley", "scale3d", "rotate", "rotatex", "rotatey", "rotate3d", "skew", "skewx", "skewy",
        "perspective", "cubic-bezier", "steps", "blur", "brightness", "contrast", "drop-shadow", "grayscale",
        "hue-rotate", "invert", "opacity", "saturate", "sepia");

    /** An identifier followed by {@code (}, whitespace tolerated (an at-rule name is not one). */
    private static final Pattern CSS_FUNCTION = Pattern.compile("(?<![@a-z0-9_-])([a-z0-9_-]+)\\s*\\(");

    /** Media-query keywords that may precede a parenthesis without being a function. */
    private static final Set<String> MEDIA_KEYWORDS = Set.of("and", "or", "not", "only");
    private static final Pattern AT_RULE = Pattern.compile("@([a-z-]*)");
    private static final Pattern URL = Pattern.compile("url\\s*\\(\\s*([\"']?)\\s*(.)");
    private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final List<String> CSS_FORBIDDEN = List.of(
        "\\", "<", "//", "javascript:", "vbscript:", "data:", "expression(", "behavior", "-moz-binding");

    private SvgAllowList() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** Whether {@code svg} (already strict-UTF-8 decoded) parses securely and passes the allow-list. */
    static boolean accepts(String svg) {
        Document document = parse(svg);
        if (document == null || !onlyCommentsAndOneRoot(document)) {
            return false;
        }
        Element root = document.getDocumentElement();
        if (root == null || !"svg".equals(root.getLocalName()) || !SVG_NS.equals(root.getNamespaceURI())) {
            return false;
        }
        Walk walk = new Walk();
        return walk.accepts(root, 1) && walk.usesAreFlat();
    }

    /** Document level: comments and the single root element — no processing instruction anywhere. */
    private static boolean onlyCommentsAndOneRoot(Document document) {
        int elements = 0;
        NodeList children = document.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            short type = children.item(i).getNodeType();
            if (type == Node.ELEMENT_NODE) {
                elements++;
            } else if (type != Node.COMMENT_NODE) {
                return false;
            }
        }
        return elements == 1;
    }

    /** One pass over the tree: element and attribute checks, plus the ids and {@code <use>} elements seen. */
    private static final class Walk {

        private final Map<String, Element> byId = new HashMap<>();
        private final List<Element> uses = new ArrayList<>();

        boolean accepts(Element element, int depth) {
            if (depth > MAX_DEPTH || !SVG_NS.equals(element.getNamespaceURI())
                || !ELEMENTS.contains(element.getLocalName())) {
                return false;
            }
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                if (!acceptsAttribute((Attr) attributes.item(i))) {
                    return false;
                }
            }
            if (element.hasAttribute("id")) {
                byId.put(element.getAttribute("id"), element);
            }
            if ("use".equals(element.getLocalName()) && uses.add(element) && uses.size() > MAX_USE_ELEMENTS) {
                return false;
            }
            if ("style".equals(element.getLocalName())) {
                return acceptsStyleSheet(element);
            }
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                switch (child.getNodeType()) {
                    case Node.ELEMENT_NODE -> {
                        if (!accepts((Element) child, depth + 1)) {
                            return false;
                        }
                    }
                    case Node.TEXT_NODE, Node.COMMENT_NODE -> {
                        // inert outside <style>
                    }
                    default -> {
                        return false;   // CDATA outside <style>, processing instructions, entity references
                    }
                }
            }
            return true;
        }

        /** No {@code <use>} may reference a {@code <use>} or a subtree containing one (no nested references). */
        boolean usesAreFlat() {
            for (Element use : uses) {
                String href = use.hasAttribute("href") ? use.getAttribute("href") : use.getAttributeNS(XLINK_NS, "href");
                Element target = href == null || href.isEmpty() ? null : byId.get(href.substring(1));
                if (target != null && ("use".equals(target.getLocalName())
                    || target.getElementsByTagNameNS(SVG_NS, "use").getLength() > 0)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A {@code <style>} holds text and CDATA only (no comment, element or PI, which could split a token), and
     * the concatenated sheet passes {@link #safeCss}.
     */
    private static boolean acceptsStyleSheet(Element style) {
        NodeList children = style.getChildNodes();
        StringBuilder sheet = new StringBuilder();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            short type = child.getNodeType();
            if (type != Node.TEXT_NODE && type != Node.CDATA_SECTION_NODE) {
                return false;
            }
            sheet.append(child.getNodeValue());
        }
        return safeCss(sheet.toString());
    }

    private static boolean acceptsAttribute(Attr attribute) {
        String namespace = attribute.getNamespaceURI();
        String name = attribute.getLocalName() != null ? attribute.getLocalName() : attribute.getName();
        String value = attribute.getValue();
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace)) {
            return true;   // a namespace declaration: elements of other namespaces are refused anyway
        }
        if (XMLConstants.XML_NS_URI.equals(namespace)) {
            return XML_ATTRIBUTES.contains(name) && safeText(value);   // never xml:base
        }
        if (XLINK_NS.equals(namespace)) {
            return "href".equals(name) && isLocalFragment(value);
        }
        if (namespace != null) {
            return false;
        }
        if (DATA_ATTRIBUTE.matcher(name).matches() || "title".equals(name)) {
            return safeText(value);
        }
        if (!ATTRIBUTES.contains(name)) {
            return false;
        }
        if ("href".equals(name)) {
            return isLocalFragment(value);
        }
        return safeCss(value);
    }

    /** {@code #name}: a local fragment of letters, digits, {@code _ - . :} only. */
    private static boolean isLocalFragment(String value) {
        return value != null && value.length() > 1 && value.charAt(0) == '#'
            && value.chars().skip(1).allMatch(c -> Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':');
    }

    /** Free text ({@code title}, {@code data-*}, {@code xml:lang}): no markup, no script or network reference. */
    private static boolean safeText(String value) {
        String text = value.toLowerCase(Locale.ROOT);
        return !text.contains("<") && !text.contains("javascript:") && !text.contains("//");
    }

    /**
     * CSS (style sheets, style and presentation attributes), checked as written and with its comments removed:
     * no forbidden sequence ({@code \}, {@code <}, {@code //} — every absolute or protocol-relative URL —,
     * script or data URLs, {@code expression(}, bindings), no at-rule but {@code @media}, no function outside
     * {@link #CSS_FUNCTIONS}, and every {@code url(} pointing to {@code #}.
     */
    static boolean safeCss(String value) {
        if (value == null) {
            return true;
        }
        String raw = value.toLowerCase(Locale.ROOT);
        return safeCssText(raw) && safeCssText(CSS_COMMENT.matcher(raw).replaceAll(""));
    }

    private static boolean safeCssText(String text) {
        for (String forbidden : CSS_FORBIDDEN) {
            if (text.contains(forbidden)) {
                return false;
            }
        }
        Matcher atRule = AT_RULE.matcher(text);
        while (atRule.find()) {
            if (!"media".equals(atRule.group(1))) {
                return false;
            }
        }
        Matcher function = CSS_FUNCTION.matcher(text);
        while (function.find()) {
            if (!CSS_FUNCTIONS.contains(function.group(1)) && !MEDIA_KEYWORDS.contains(function.group(1))) {
                return false;
            }
        }
        Matcher url = URL.matcher(text);
        while (url.find()) {
            if (!"#".equals(url.group(2))) {
                return false;
            }
        }
        return true;
    }

    private static Document parse(String svg) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(SILENT);
            return builder.parse(new InputSource(new StringReader(svg)));
        } catch (Exception e) {
            return null;   // unparseable or refused (DOCTYPE, entities): unsafe by definition
        }
    }

    /** Fails on every problem without printing it (the default handler writes to stderr). */
    private static final ErrorHandler SILENT = new ErrorHandler() {
        @Override
        public void warning(SAXParseException e) {
            // ignored
        }

        @Override
        public void error(SAXParseException e) throws SAXParseException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXParseException {
            throw e;
        }
    };
}
