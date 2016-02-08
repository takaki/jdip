//t
//  @(#)XMLVariantParser.java		7/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
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
package dip.world.variant.parser;

import dip.misc.LRUCache;
import dip.misc.Log;
import dip.misc.Utils;
import dip.world.variant.VariantManager;
import dip.world.variant.data.BorderData;
import dip.world.variant.data.ProvinceData;
import dip.world.variant.data.Variant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Parses an XML Variant description.
 */
public class XMLVariantParser implements VariantParser {
    // XML Element constants
    public static final String EL_VARIANT = "VARIANT";
    public static final String EL_MAP_DEFINITION = "MAP_DEFINITION";

    // il8n error message constants
    private static final String ERR_NO_ELEMENT = "XMLVariantParser.noelement";

    // instance variables
    private Document doc;
    private final DocumentBuilder docBuilder;
    private final List<Variant> variantList;


    /** Create an XMLVariantParser */
    /*
    public XMLVariantParser(boolean isValidating)
	throws ParserConfigurationException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(isValidating);
		dbf.setCoalescing(false);
		dbf.setIgnoringComments(true);

		docBuilder = dbf.newDocumentBuilder();
		docBuilder.setErrorHandler(new XMLErrorHandler());

		provinceParser = new XMLProvinceParser(dbf);

		variantList = new LinkedList();
		AdjCache.init(provinceParser);
	}// XMLVariantParser()
	*/

    /**
     * Create an XMLVariantParser
     */
    public XMLVariantParser(
            final DocumentBuilderFactory dbf) throws ParserConfigurationException {
        docBuilder = dbf.newDocumentBuilder();
        docBuilder.setErrorHandler(new XMLErrorHandler());
        FastEntityResolver.attach(docBuilder);
        final XMLProvinceParser provinceParser = new XMLProvinceParser(dbf);

        variantList = new LinkedList<>();
        AdjCache.init(provinceParser);
    }// XMLVariantParser()


    /**
     * Parse the given input stream; parsed data available via <code>getVariants()</code>
     * <p>
     * Note that when this method is called, any previous Variants (if any exist) are
     * cleared.
     */
    public void parse(final InputStream is,
                      final URL variantPackageURL) throws IOException, SAXException {
        Log.println("XMLVariantParser: Parsing: ", variantPackageURL);
        final long time = System.currentTimeMillis();

        // cleanup cache (very important to remove references!)
        AdjCache.clear();
        variantList.clear();

        if (variantPackageURL == null) {
            throw new IllegalArgumentException();
        }

        AdjCache.setVariantPackageURL(variantPackageURL);
        doc = docBuilder.parse(is);
        try {
            procVariants();
        } catch (final XPathExpressionException | JAXBException e) {
            throw new IllegalArgumentException(e);
        }
        Log.printTimed(time, "   time: ");
    }// parse()


    /**
     * Cleanup, clearing any references/resources
     */
    public void close() {
        AdjCache.clear();
        variantList.clear();
    }// close()


    /**
     * Returns an array of Variant objects.
     * <p>
     * Will never return null. Note that parse() must be called before
     * this will return any information.
     */
    @Override
    public Variant[] getVariants() {
        return variantList.toArray(new Variant[variantList.size()]);
    }// getVariants()


    @XmlRootElement(name = "VARIANTS")
    public static class RootVariants {
        @XmlElement(name = "DESCRIPTION")
        private String description;
        @XmlElement(name = "MAP_DEFINITION")
        private List<MapDef> mapDefinitions;
        @XmlElement(name = "VARIANT")
        private List<Variant> variants;
    }

    /**
     * Process the Variant list description file
     */
    private void procVariants() throws XPathExpressionException, JAXBException {
        final XPath xpath = XPathFactory.newInstance().newXPath();

        // find the root element (VARIANTS), and all VARIANT elements underneath.
        final Element root = doc.getDocumentElement();
        final Unmarshaller rootUnmarshaller = JAXBContext
                .newInstance(RootVariants.class).createUnmarshaller();
        final RootVariants rootVariants = (RootVariants) rootUnmarshaller
                .unmarshal(root);


        // get map definitions (at least one, under VARIANT)
        final NodeList mapDefEls = (NodeList) xpath
                .evaluate(EL_MAP_DEFINITION, root, XPathConstants.NODESET);

        // setup map definition ID hashmap
        final Unmarshaller mapDefUnmarshaller = JAXBContext
                .newInstance(MapDef.class).createUnmarshaller();
        final Map<String, MapDef> mapDefTable = rootVariants.mapDefinitions
                .stream()
                .collect(Collectors.toMap(MapDef::getID, Function.identity()));


        // search for variant data
        final Unmarshaller variantUnmarshaller = JAXBContext
                .newInstance(Variant.class).createUnmarshaller();
        final NodeList variantElements = (NodeList) xpath
                .evaluate(EL_VARIANT, root, XPathConstants.NODESET);
        variantList.addAll(IntStream.range(0, variantElements.getLength())
                .mapToObj(i -> (Element) variantElements.item(i))
                .map(elVariant -> {
                    try {
                        final Variant variant = (Variant) variantUnmarshaller
                                .unmarshal(elVariant);
                        variant.updateMapGraphics(mapDefTable);
                        return variant;
                    } catch (JAXBException e) {
                        throw new IllegalArgumentException(e);
                    }
                }).collect(Collectors.toList()));
    }// procVariants()


    /**
     * Checks that an element is present
     */
    private void checkElement(final Element element, final String name) {
        if (element == null) {
            throw new IllegalArgumentException(
                    Utils.getLocalString(ERR_NO_ELEMENT, name));
        }
    }// checkElement()

    /**
     * Get an Element by name; only returns a single element.
     */
    private Element getSingleElementByName(final Element parent,
                                           final String name) {
        final NodeList nodes = parent.getElementsByTagName(name);
        return (Element) nodes.item(0);
    }// getSingleElementByName()


    /**
     * Integer parser; throws an exception if number cannot be parsed.
     */
    private static int parseInt(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }


    }// parseInt()


    /**
     * Float parser; throws an exception if number cannot be parsed. Value must be >= 0.0
     */
    private static float parseFloat(final String value) {
        try {
            final float floatValue = Float.parseFloat(value);
            if (floatValue < 0.0f) {
                throw new NumberFormatException("Value must be >= 0");
            }

            return floatValue;
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }

    }// parseInt()


    /**
     * Inner class which caches XML adjacency data (ProvinceData and BorderData),
     * which may be shared between different variants (if the variants use the
     * same adjacency data).
     * <p>
     * NOTE: this depends on the XMLVariantParser variable "adjCache", since inner classes
     * cannot have statics (unless they inner class is static, which just creates more problems;
     * this is a simpler solution)
     */
    public static class AdjCache {
        private static URL vpURL;
        private static XMLProvinceParser pp;
        private static LRUCache adjCache;    // URI -> AdjCache objects

        // instance variables
        private List<ProvinceData> provinceData;
        private List<BorderData> borderData;


        AdjCache() {
        }// AdjCache()

        /**
         * initialization
         */
        public static void init(final XMLProvinceParser provinceParser) {
            pp = provinceParser;
            adjCache = new LRUCache(6);
        }// AdjCache()

        /**
         * Sets the variant package URL
         */
        public static void setVariantPackageURL(final URL variantPackageURL) {
            vpURL = variantPackageURL;
        }// setVariantPackageURL()


        /**
         * Clears the cache.
         */
        public static void clear() {
            adjCache.clear();
        }// clear()


        /**
         * Gets the ProvinceData for a given adjacency URI
         */
        public static ProvinceData[] getProvinceData(
                final URI adjacencyURI) throws IOException, SAXException {
            final AdjCache ac = get(adjacencyURI);
            return ac.provinceData
                    .toArray(new ProvinceData[ac.provinceData.size()]);
        }// getProvinceData()


        /**
         * Gets the BorderData for a given adjacency URI
         */
        public static BorderData[] getBorderData(
                final URI adjacencyURI) throws IOException, SAXException {
            final AdjCache ac = get(adjacencyURI);
            return ac.borderData.toArray(new BorderData[ac.borderData.size()]);
        }// getBorderData()


        /**
         * Gets the AdjCache object from the cache, or parses from the URI, as appropriate
         */
        private static AdjCache get(
                final URI adjacencyURI) throws IOException, SAXException {
            // see if we already have the URI data cached.
            if (adjCache.get(adjacencyURI) != null) {
                //Log.println("  AdjCache: using cached adjacency data: ", adjacencyURI);
                return (AdjCache) adjCache.get(adjacencyURI);
            }

            // it's not cached. resolve URI.
            final URL url = VariantManager.getResource(vpURL, adjacencyURI);
            if (url == null) {
                throw new IOException(
                        "Could not convert URI: " + adjacencyURI + " from variant package: " + vpURL);
            }

            // parse resolved URI
            //Log.println("  AdjCache: not in cache: ", adjacencyURI);

            try (InputStream is = new BufferedInputStream(url.openStream())) {
                pp.parse(is);
            }

            // cache and return parsed data.
            final AdjCache ac = new AdjCache();
            ac.provinceData = Arrays.asList(pp.getProvinceData());
            ac.borderData = Arrays.asList(pp.getBorderData());
            adjCache.put(adjacencyURI, ac);
            return ac;
        }// get()

    }// inner class AdjCache


    /**
     * Class that holds MAP_DEFINITION data, which is
     * inserted into a hashtable for later recall.
     */
    @XmlRootElement(name = "MAP_DEFINITION")
    public static class MapDef {
        @XmlAttribute(required = true)
        private String id;
        @XmlAttribute(required = true)
        private String title;
        @XmlAttribute(name = "URI", required = true)
        private String mapURI;
        @XmlAttribute(required = true)
        private String thumbURI;
        @XmlAttribute(required = false)
        private String preferredUnitStyle;
        @XmlElement
        private String description;

        @SuppressWarnings("unused")
        void afterUnmarshal(final Unmarshaller unmarshaller,
                            final Object parent) {
            if (title != null && title.isEmpty()) {
                throw new IllegalArgumentException(
                        "map id=" + id + " missing a title (name)");
            }
        }

        public String getID() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getMapURI() {
            return mapURI;
        }

        public String getThumbURI() {
            return thumbURI;
        }

        public String getPrefUnitStyle() {
            return preferredUnitStyle;
        }

        public String getDescription() {
            return description;
        }
    }// inner class MapDef

}// class XMLVariantParser



