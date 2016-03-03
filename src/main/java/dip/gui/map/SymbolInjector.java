//
//  @(#)SymbolInjector.java		11/2003
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
//  Or from http://www.gnu.org/
//
package dip.gui.map;

import dip.gui.ClientFrame;
import dip.misc.XMLUtils;
import dip.world.variant.VariantManager;
import dip.world.variant.data.MapGraphic;
import dip.world.variant.data.Symbol;
import dip.world.variant.data.SymbolPack;
import dip.world.variant.data.SymbolPack.CSSStyle;
import dip.world.variant.data.Variant;
import dip.world.variant.parser.FastEntityResolver;
import dip.world.variant.parser.XMLErrorHandler;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;


/**
 * Adds Symbols from a SymbolPack into a Variant map.
 * This should occur prior to Batik processing.
 */
public class SymbolInjector {
    // <defs> element name
    private static final String DEFS_ELEMENT_NAME = "defs";
    private static final String STYLE_ELEMENT_NAME = "style";
    private static final String ID_ATTRIBUTE = "id";
    private static final String ATT_TYPE = "type";
    private static final String CSS_TYPE_VALUE = "text/css";
    private static final String CDATA_NODE_NAME = "#cdata-section";

    private final Document doc;
    private final SymbolPack sp;


    /**
     * Create a SymbolInjector
     * <p>
     * Throws an IOException if URL resolving fails.
     */
    public SymbolInjector(final ClientFrame cf, final Variant variant, final MapGraphic mg,
                          final SymbolPack sp) throws IOException, SAXException, ParserConfigurationException {
        if (variant == null || mg == null || sp == null) {
            throw new IllegalArgumentException();
        }

        this.sp = sp;

        // resolve URL
        final URL url = new VariantManager().getResource(variant, mg.getURI()).orElse(null);
        if (url == null) {
            throw new IOException();
        }

        // load URL into DOM Document
        InputStream is = null;
        try {
            is = new BufferedInputStream(url.openStream());
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);    // essential!
            dbf.setValidating(cf.getValidating());
            final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            docBuilder.setErrorHandler(new XMLErrorHandler());
            FastEntityResolver.attach(docBuilder);

            doc = docBuilder.parse(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                }
            }
        }
    }// SymbolInjector()


    /**
     * Inject the Symbols into the Map SVG
     * <p>
     * An exception is thrown if no &lt;defs;&gt; element (section)
     * is found.
     */
    public void inject() throws IOException {
        // find <defs> element
        final Element root = doc.getDocumentElement();
        Element defs = null;
        Element style = null;

        defs = XMLUtils.findChildElementMatching(root, DEFS_ELEMENT_NAME);

        // found defs?
        if (defs == null) {
            throw new IOException("SVG is missing a <defs> section.");
        }

        // found style?
        style = XMLUtils.findChildElementMatching(defs, STYLE_ELEMENT_NAME);
        if (style != null && sp.hasCSSStyles()) {
            // check CSS type (must be "text/css")
            //
            final String type = style.getAttribute(ATT_TYPE).trim();
            if (!CSS_TYPE_VALUE.equals(type)) {
                throw new IOException(
                        "Only <style type=\"text/css\"> is accepted. Cannot merge CSS otherwise.");
            }

            style.normalize();

            // get style CDATA
            final CDATASection cdsNode = (CDATASection) XMLUtils
                    .findChildNodeMatching(style, CDATA_NODE_NAME,
                            Node.CDATA_SECTION_NODE);

            if (cdsNode == null) {
                throw new IOException("CDATA in <style> node is null.");
            }

            // append data (if any)
            mergeCSS(cdsNode, sp.getCSSStyles().toArray(new CSSStyle[0]));
        }

        // find all <g> or <symbol> under defs with same tag names.
        // if found, replace with our own symbols. If not found, add them.
        //
        final HashMap<String, Element> defsElementMap = elementMapper(defs, ID_ATTRIBUTE);

        final List<Symbol> symbols = sp.getSymbols();
        assert (symbols != null);
        assert (symbols.size() > 0);

        for (int i = 0; i < symbols.size(); i++) {
            final Symbol symbol = symbols.get(i);
            final Element element = defsElementMap.get(symbol.getName());
            if (element == null) {
                // does not exist! add
                defs.appendChild(getSymbolElement(symbol));
            } else {
                // already exists! replace
                final Element parent = (Element) element.getParentNode();
                parent.replaceChild(getSymbolElement(symbol), element);
            }
        }

    }// inject()


    /**
     * Get the XML DOM Document
     */
    public Document getDocument() {
        return doc;
    }// getDocument()

    /**
     * Get the SVG Data (element) from a Symbol, but, makes sure
     * that it can be imported correctly (this avoids the
     * "wrong document" DOMException), since the Symbol SVG data
     * originated within a different XML document.
     */
    private Element getSymbolElement(final Symbol symbol) {
        return (Element) doc.importNode(symbol.getSVGData(), true);
    }// getSymbolElement()


    /**
     * Searches an XML document from the given Element, recursively,
     * and returns elements that have the given non-empty attribute name.
     * <p>
     * A HashMap is then created, which maps the attribute <b>value</b> to
     * an org.w3c.Element. An Exception is thrown if an element with a
     * duplicate attribute value (case-sensitive) is found.
     */
    private HashMap<String, Element> elementMapper(final Element start,
                                                   final String attrName) throws IOException {
        final HashMap<String, Element> map = new HashMap<>(31);
        elementMapperWalker(map, start, attrName);
        return map;
    }// elementMapper()


    /**
     * Recursive portion of elementMapper
     */
    private void elementMapperWalker(final HashMap<String, Element> map, final Node node,
                                     final String attrName) throws IOException {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (node.hasAttributes()) {
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

                        map.put(attrValue, (Element) node);
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
     * Merge CSS data. Note that we DO NOT handle comments. So beware!
     * <p>
     * Throw an error if a duplicate CSS style is encountered.
     */
    private void mergeCSS(final CDATASection cdsNode,
                          final SymbolPack.CSSStyle[] cssStyles) throws IOException {
        final String oldCSS = cdsNode.getData();

        // collision check
        for (int i = 0; i < cssStyles.length; i++) {
            if (oldCSS.indexOf(cssStyles[i].getName()) >= 0) {
                throw new IOException(
                        "Map and SymbolPack contain same CSS style: \"" + cssStyles[i]
                                .getName() + "\"");
            }
        }

        // add (at end)
        final StringBuffer sb = new StringBuffer(oldCSS);


        sb.append("/* merged CSS from SymbolPack */\n");

        for (int i = 0; i < cssStyles.length; i++) {
            sb.append(cssStyles[i].getName());
            sb.append(' ');
            sb.append(cssStyles[i].getStyle());
            sb.append('\n');
        }

        cdsNode.setData(sb.toString());
    }// mergeCSS()


}// class SymbolInjector

