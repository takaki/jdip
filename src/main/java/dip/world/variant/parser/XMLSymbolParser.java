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
import dip.world.variant.data.Symbol;
import dip.world.variant.data.SymbolPack;
import dip.world.variant.data.SymbolPack.CSSStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Parses a SymbolPack description.
 */
public class XMLSymbolParser implements SymbolParser {
    private static final Logger LOG = LoggerFactory.getLogger(
            XMLSymbolParser.class);

    private final SymbolPack symbolPack;

    /**
     * Create an XMLSymbolParser
     */
    public XMLSymbolParser(
            final URL symbolXMLURL) throws ParserConfigurationException, MalformedURLException {
        // setup document builder
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // this may improve performance, and really only apply to Xerces
            dbf.setAttribute(
                    "http://apache.org/xml/features/dom/defer-node-expansion",
                    Boolean.FALSE);
            dbf.setAttribute(
                    "http://apache.org/xml/properties/input-buffer-size", 4096);
            dbf.setAttribute(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    Boolean.FALSE);
        } catch (final Exception e) {
            LOG.debug("VM: Could not set XML feature.", e);
        }

        dbf.setValidating(false);
        dbf.setCoalescing(false);
        dbf.setIgnoringComments(true);
        dbf.setNamespaceAware(true);    // essential!

        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        docBuilder.setErrorHandler(new XMLErrorHandler());
        FastEntityResolver.attach(docBuilder);

        LOG.debug("XMLSymbolParser: Parsing: {}", symbolXMLURL);
        final long time = System.currentTimeMillis();

        symbolPack = JAXB.unmarshal(symbolXMLURL, SymbolPack.class);
        // extract symbol SVG into symbols
        // add symbols to SymbolPack

        // resolve SVG URI
        final URL url = new URL(symbolXMLURL,
                symbolPack.getSVGURI().toString());

        // parse resolved URI into a Document

        try (InputStream is1 = new BufferedInputStream(url.openStream())) {
            final Document svgDoc = docBuilder.parse(is1);
            final XPath xPath = XPathFactory.newInstance().newXPath();
            final String style = (String) xPath.evaluate(
                    "//*[local-name()='defs']/*[local-name()='style'][@type='text/css']/text()",
                    svgDoc, XPathConstants.STRING);

            // get style CDATA
            Objects.requireNonNull(style);
            // break input into lines
            try (final BufferedReader br = new BufferedReader(
                    new StringReader(style))) {
                symbolPack.setCSSStyles(br.lines().map(String::trim)
                        .filter(line -> line.startsWith(".")).map(line -> {
                            final String[] tokens = line.split("\\s+", 2);
                            if (tokens.length < 2 || !tokens[1]
                                    .matches("\\{[^{}]+\\}$")) {
                                throw new IllegalArgumentException(
                                        String.format(
                                                "Could not parse SymbolPack CSS. " +
                                                        "Note that comments are not supported, " +
                                                        "and that there may be only one CSS style per line.Error line text: \"%s\"",
                                                line));
                            }
                            // create CSS Style
                            return new CSSStyle(tokens[0], tokens[1]);
                        }).collect(Collectors.toList()));
            }


            // find all IDs
            // List of Symbols
            final NodeList evaluate = (NodeList) xPath.evaluate(
                    "(//*[local-name()='g' or local-name()='symbol' or local-name()='svg'])[@id]",
                    svgDoc, XPathConstants.NODESET);
            final List<Symbol> list = IntStream.range(0, evaluate.getLength())
                    .mapToObj(i -> (Element) evaluate.item(i)).map(me -> {
                        String name = me.getAttribute("id");
                        final Float scale = symbolPack.getScaleMap()
                                .getOrDefault(name, Symbol.IDENTITY_SCALE);
                        return new Symbol(name, scale, me);
                    }).collect(Collectors.toList());
            if (list.stream().map(Symbol::getName).distinct().count() != list
                    .size()) {
                throw new IllegalArgumentException("ID is duplicated.");
            }
            symbolPack.setSymbols(list);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (SAXException | XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
        LOG.debug(Log.printTimed(time, "    time: "));

    }// XMLProvinceParser()


    /**
     * Returns the SymbolPack, or null, if parse()
     * has not yet been called.
     */
    @Override
    public SymbolPack getSymbolPack() {
        return symbolPack;
    }// getSymbolPacks()


}// class XMLSymbolParser
