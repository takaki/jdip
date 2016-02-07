//
//  @(#)XMLSymbolParser.java		11/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/19
//
package dip.world.variant.parser;

import dip.misc.Log;
import dip.misc.XMLUtils;
import dip.world.variant.VariantManager;
import dip.world.variant.data.Symbol;
import dip.world.variant.data.SymbolPack;
import dip.world.variant.data.SymbolPack.CSSStyle;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXB;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Parses a SymbolPack description.
 */
public class XMLSymbolParser implements SymbolParser {
    // Element constants
    private static final String EL_DEFS = "defs";
    private static final String EL_STYLE = "style";


    // Attribute constants
    private static final String ATT_ID = "id";
    private static final String ATT_TYPE = "type";

    // valid element tag names; case sensitive
    private static final String[] VALID_ELEMENTS = {"g", "symbol", "svg"};

    // misc
    private static final String CSS_TYPE_VALUE = "text/css";
    private static final String CDATA_NODE_NAME = "#cdata-section";


    private final DocumentBuilder docBuilder;
    private SymbolPack symbolPack;
    private URL symbolPackURL;


    /**
     * Create an XMLSymbolParser
     */
    public XMLSymbolParser(
            final DocumentBuilderFactory dbf) throws ParserConfigurationException {
        final boolean oldNSvalue = dbf.isNamespaceAware();

        dbf.setNamespaceAware(true);    // essential!
        docBuilder = dbf.newDocumentBuilder();
        docBuilder.setErrorHandler(new XMLErrorHandler());
        FastEntityResolver.attach(docBuilder);

        // cleanup
        dbf.setNamespaceAware(oldNSvalue);
    }// XMLProvinceParser()


    /**
     * Parse the given input stream
     */
    public synchronized void parse(final InputStream is,
                                   final URL symbolPackURL) throws IOException, SAXException {
        Log.println("XMLSymbolParser: Parsing: ", symbolPackURL);
        final long time = System.currentTimeMillis();
        this.symbolPackURL = symbolPackURL;
        symbolPack = JAXB.unmarshal(is, SymbolPack.class);
        // extract symbol SVG into symbols
        // add symbols to SymbolPack
        procAndAddSymbolSVG(symbolPack, symbolPack.getScaleMap());
        Log.printTimed(time, "    time: ");
    }// parse()


    /**
     * Cleanup, clearing any references/resources
     */
    public void close() {
        symbolPack = null;
    }// close()

    /**
     * Returns the SymbolPack, or null, if parse()
     * has not yet been called.
     */
    @Override
    public SymbolPack getSymbolPack() {
        return symbolPack;
    }// getSymbolPacks()


    /**
     * Parse the symbol data into Symbols and SymbolPacks
     */
    private void procAndAddSymbolSVG(final SymbolPack symbolPack,
                                     final Map<String, Float> scaleMap) throws IOException, SAXException {

        // resolve SVG URI
        final URL url = VariantManager
                .getResource(symbolPackURL, symbolPack.getSVGURI());
        if (url == null) {
            throw new IOException(String.format(
                    "Could not convert URI: %s from SymbolPack: %s",
                    symbolPack.getSVGURI(), symbolPackURL));
        }

        // parse resolved URI into a Document

        try (InputStream is = new BufferedInputStream(url.openStream())) {
            final Document svgDoc = docBuilder.parse(is);

            // find defs section, if any, and style attribute
            //
            final Element defs = XMLUtils
                    .findChildElementMatching(svgDoc.getDocumentElement(),
                            EL_DEFS);
            if (defs != null) {
                final Element style = XMLUtils
                        .findChildElementMatching(defs, EL_STYLE);
                if (style != null) {
                    // check CSS type (must be "text/css")
                    //
                    final String type = style.getAttribute(ATT_TYPE).trim();
                    if (!CSS_TYPE_VALUE.equals(type)) {
                        throw new IOException(
                                "Only <style type=\"text/css\"> is accepted. Cannot parse CSS otherwise.");
                    }

                    style.normalize();

                    // get style CDATA
                    final CDATASection cdsNode = (CDATASection) XMLUtils
                            .findChildNodeMatching(style, CDATA_NODE_NAME,
                                    Node.CDATA_SECTION_NODE);

                    if (cdsNode == null) {
                        throw new IOException("CDATA in <style> node is null.");
                    }

                    symbolPack.setCSSStyles(parseCSS(cdsNode.getData()));
                }
            }

            // find all IDs
            // List of Symbols
            // iterate over hashmap finding all symbols with IDs
            final List<Symbol> list = elementMapper(svgDoc.getDocumentElement(),
                    ATT_ID).entrySet().stream().map(me -> {
                final String name = me.getKey();
                final Float scale = scaleMap.get(name);
                return new Symbol(name,
                        scale == null ? Symbol.IDENTITY_SCALE : scale,
                        (Element) me.getValue());
            }).collect(Collectors.toList());

            // add symbols to symbolpack
            symbolPack.setSymbols(list);
        }
    }// procAndAddSymbolSVG()


    /**
     * Searches an XML document for Elements that have a given non-empty attribute.
     * The Elements are then put into a HashMap, which is indexed by the attribute
     * value. This starts from the given Element and recurses downward. Throws an
     * exception if an element with a duplicate attribute name is found.
     */
    private Map<String, Node> elementMapper(final Element start,
                                            final String attrName) throws IOException {
        final Map<String, Node> map = new HashMap<>(31);
        elementMapperWalker(map, start, attrName);
        return map;
    }// elementMapper()


    /**
     * Recursive portion of elementMapper
     */
    private void elementMapperWalker(final Map<String, Node> map,
                                     final Node node,
                                     final String attrName) throws IOException {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            // node MUST be one of the following:
            // <g>, <symbol>, or <svg>
            //
            final String name = ((Element) node).getTagName();
            if (node.hasAttributes() && isValidElement(name)) {
                final NamedNodeMap attributes = node.getAttributes();
                final Node attrNode = attributes.getNamedItem(attrName);
                if (attrNode != null) {
                    final String attrValue = attrNode.getNodeValue();
                    if (!"".equals(attrValue)) {
                        if (map.containsKey(attrValue)) {
                            throw new IOException(
                                    "The " + attrName + " attribute has duplicate " +
                                            "values: " + attrValue);
                        }

                        map.put(attrValue, node);
                    }
                }
            }
        }

        // check if current node has any children
        // if so, iterate through & recursively call this method
        final NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                elementMapperWalker(map, children.item(i), attrName);
            }
        }
    }// elementMapperWalker()


    /**
     * See if name is a valid element tag name
     */
    private boolean isValidElement(final String name) {
        for (String VALID_ELEMENT : VALID_ELEMENTS) {
            if (VALID_ELEMENT.equals(name)) {
                return true;
            }
        }

        return false;
    }// isValidElement()


    /**
     * Very Simple CSS parser. Does not handle comments.
     * Assumes that the beginning of a line has a CSS property,
     * and is followed by a braced CSS style information.
     * <p>
     * <pre>
     * 		.hello 		{style:lala;this:that}		// handled OK
     * 		    .goodbye {fill:red;}				// handled OK
     * 	.multiline {fill:red;
     * 			opacity:some;}						// not handled
     * 	</pre>
     */
    private CSSStyle[] parseCSS(final String input) throws IOException {
        final List<CSSStyle> cssStyles = new ArrayList<CSSStyle>(20);

        // break input into lines
        final BufferedReader br = new BufferedReader(new StringReader(input));
        String line = br.readLine();
        while (line != null) {
            // first non-whitespace must be a '.'
            line = line.trim();
            if (line.startsWith(".")) {
                int idxEndName = -1;    // end of the style name
                int idxCBStart = -1;    // position of '{'
                int idxCBEnd = -1;        // position of '}'
                for (int i = 0; i < line.length(); i++) {
                    final char c = line.charAt(i);
                    if (idxEndName < 0 && Character.isWhitespace(c)) {
                        idxEndName = i;
                    }

                    if (idxEndName > 0 && c == '{') {
                        if (idxCBStart < 0) {
                            idxCBStart = i;
                        } else {
                            // error!
                            idxCBStart = -1;
                            break;
                        }
                    }

                    if (idxCBStart > 0 && c == '}') {
                        idxCBEnd = i;
                        break;
                    }
                }

                // validate
                if (idxEndName < 0 || idxCBStart < 0 || idxCBEnd < 0) {
                    throw new IOException(
                            "Could not parse SymbolPack CSS. Note that comments are not " +
                                    "supported, and that there may be only one CSS style per line." +
                                    "Error line text: \"" + line + "\"");
                }

                // parse
                final String name = line.substring(0, idxEndName);
                final String value = line.substring(idxCBStart, idxCBEnd + 1);

                // create CSS Style
                cssStyles.add(new CSSStyle(name, value));
            }

            line = br.readLine();
        }

        return cssStyles.toArray(new CSSStyle[cssStyles.size()]);
    }// parseCSS()


}// class XMLSymbolParser
