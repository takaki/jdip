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
import dip.world.variant.VariantManager;
import dip.world.variant.data.Symbol;
import dip.world.variant.data.SymbolPack;
import dip.world.variant.data.SymbolPack.CSSStyle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXB;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Parses a SymbolPack description.
 */
public class XMLSymbolParser implements SymbolParser {


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
                                   final URL symbolPackURL) throws IOException, SAXException, XPathExpressionException {
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
                                     final Map<String, Float> scaleMap) throws IOException, SAXException, XPathExpressionException {

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
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final String style = (String) xPath.evaluate(
                    "//*[local-name()='defs']/*[local-name()='style'][@type='text/css']/text()",
                    svgDoc, XPathConstants.STRING);

            // get style CDATA
            if (style != null && style.isEmpty()) {
                throw new IOException("CDATA in <style> node is null.");
            }

            symbolPack.setCSSStyles(parseCSS(style));

            // find all IDs
            // List of Symbols

            final NodeList evaluate = (NodeList) xPath.evaluate(
                    "(//*[local-name()='g' or local-name()='symbol' or local-name()='svg'])[@id]",
                    svgDoc, XPathConstants.NODESET);
            final List<Symbol> list = IntStream.range(0, evaluate.getLength())
                    .mapToObj(i -> {
                        Element me = (Element) evaluate.item(i);
                        String name = me.getAttribute("id");
                        final Float scale = scaleMap
                                .getOrDefault(name, Symbol.IDENTITY_SCALE);
                        return new Symbol(name, scale, me);
                    }).collect(Collectors.toList());
            if (list.stream().map(Symbol::getName).distinct().count() != list
                    .size()) {
                throw new IOException("ID is duplicated.");
            }
            symbolPack.setSymbols(list);
        }
    }// procAndAddSymbolSVG()


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
    private static CSSStyle[] parseCSS(final String input) throws IOException {
        // break input into lines
        try (final BufferedReader br = new BufferedReader(
                new StringReader(input))) {
            final List<CSSStyle> cssStyles = br.lines().map(String::trim)
                    .filter(line -> line.startsWith(".")).map(line -> {
                        final String[] tokens = line.split("\\s+", 2);
                        if (tokens.length < 2) {
                            throw new IllegalArgumentException(String.format(
                                    "Could not parse SymbolPack CSS. " +
                                            "Note that comments are not supported, " +
                                            "and that there may be only one CSS style per line.Error line text: \"%s\"",
                                    line));
                        }
                        if (!tokens[1].matches("\\{[^{}]+\\}$")) {
                            throw new IllegalArgumentException(String.format(
                                    "Could not parse SymbolPack CSS. " +
                                            "Note that comments are not supported, " +
                                            "and that there may be only one CSS style per line.Error line text: \"%s\"",
                                    line));
                        }
                        // create CSS Style
                        return new CSSStyle(tokens[0], tokens[1]);
                    }).collect(Collectors.toList());
            return cssStyles.toArray(new CSSStyle[cssStyles.size()]);
        }
    }// parseCSS()


}// class XMLSymbolParser
