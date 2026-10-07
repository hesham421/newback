package com.erp.file.domain;

import java.io.StringReader;
import java.util.Locale;
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
 * RULE-FILE-009 (tenant-maturity D.4, review round 1) — an SVG is accepted only when a hardened,
 * namespace-aware parse (no DOCTYPE, no entities, no XInclude) succeeds and every element is an
 * allow-listed SVG-namespace element, every attribute an allow-listed one, every {@code href} a local
 * {@code #fragment}, and every attribute value and {@code <style>} text free of {@code \}, {@code @import},
 * {@code javascript:}, {@code expression(} and any {@code url(} that is not {@code url(#…)}.
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
    private static final Pattern URL = Pattern.compile("url\\s*\\(\\s*([\"']?)\\s*(.)");

    private SvgAllowList() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** Whether {@code svg} (already strict-UTF-8 decoded) parses securely and passes the allow-list. */
    static boolean accepts(String svg) {
        Document document = parse(svg);
        if (document == null) {
            return false;
        }
        Element root = document.getDocumentElement();
        return root != null && "svg".equals(root.getLocalName()) && SVG_NS.equals(root.getNamespaceURI())
            && accepts(root);
    }

    private static boolean accepts(Element element) {
        if (!SVG_NS.equals(element.getNamespaceURI()) || !ELEMENTS.contains(element.getLocalName())) {
            return false;
        }
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            if (!acceptsAttribute((Attr) attributes.item(i))) {
                return false;
            }
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE -> {
                    if (!accepts((Element) child)) {
                        return false;
                    }
                }
                case Node.TEXT_NODE -> {
                    if ("style".equals(element.getLocalName()) && !safeCss(child.getNodeValue())) {
                        return false;
                    }
                }
                case Node.CDATA_SECTION_NODE -> {
                    if (!"style".equals(element.getLocalName()) || !safeCss(child.getNodeValue())) {
                        return false;   // CDATA only as style sheet text
                    }
                }
                case Node.COMMENT_NODE -> {
                    // inert
                }
                default -> {
                    return false;   // processing instructions, entity references, anything else
                }
            }
        }
        return true;
    }

    private static boolean acceptsAttribute(Attr attribute) {
        String namespace = attribute.getNamespaceURI();
        String name = attribute.getLocalName() != null ? attribute.getLocalName() : attribute.getName();
        String value = attribute.getValue();
        if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespace)) {
            return true;   // a namespace declaration: elements of other namespaces are refused anyway
        }
        if (XMLConstants.XML_NS_URI.equals(namespace)) {
            return XML_ATTRIBUTES.contains(name) && safeCss(value);   // never xml:base
        }
        if (XLINK_NS.equals(namespace)) {
            return "href".equals(name) && isLocalFragment(value);
        }
        if (namespace != null || !ATTRIBUTES.contains(name)) {
            return false;
        }
        if ("href".equals(name)) {
            return isLocalFragment(value);
        }
        return safeCss(value);
    }

    private static boolean isLocalFragment(String value) {
        return value != null && value.startsWith("#") && value.length() > 1 && safeCss(value);
    }

    /** No escapes, imports, script URLs or IE expressions; every {@code url(…)} points into the document. */
    static boolean safeCss(String value) {
        if (value == null) {
            return true;
        }
        String text = value.toLowerCase(Locale.ROOT);
        if (text.indexOf('\\') >= 0 || text.contains("@import") || text.contains("javascript:")
            || text.contains("expression(") || text.contains("<")) {
            return false;
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
