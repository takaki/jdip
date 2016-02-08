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
import dip.world.Phase;
import dip.world.variant.VariantManager;
import dip.world.variant.data.*;
import dip.world.variant.data.Variant.NameValuePair;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Parses an XML Variant description.
 */
public class XMLVariantParser implements VariantParser {
    // XML Element constants
    public static final String EL_VARIANTS = "VARIANTS";
    public static final String EL_VARIANT = "VARIANT";
    public static final String EL_DESCRIPTION = "DESCRIPTION";
    public static final String EL_MAP = "MAP";
    public static final String EL_STARTINGTIME = "STARTINGTIME";
    public static final String EL_INITIALSTATE = "INITIALSTATE";
    public static final String EL_SUPPLYCENTER = "SUPPLYCENTER";
    public static final String EL_POWER = "POWER";
    public static final String EL_MAP_DEFINITION = "MAP_DEFINITION";
    public static final String EL_MAP_GRAPHIC = "MAP_GRAPHIC";
    public static final String EL_VICTORYCONDITIONS = "VICTORYCONDITIONS";
    public static final String EL_GAME_LENGTH = "GAME_LENGTH";
    public static final String EL_YEARS_WITHOUT_SC_CAPTURE = "YEARS_WITHOUT_SC_CAPTURE";
    public static final String EL_WINNING_SUPPLY_CENTERS = "WINNING_SUPPLY_CENTERS";
    public static final String EL_RULEOPTIONS = "RULEOPTIONS";
    public static final String EL_RULEOPTION = "RULEOPTION";


    // XML Attribute constants
    public static final String ATT_ALIASES = "aliases";
    public static final String ATT_VERSION = "version";
    public static final String ATT_URI = "URI";
    public static final String ATT_DEFAULT = "default";
    public static final String ATT_TITLE = "title";
    public static final String ATT_DESCRIPTION = "description";
    public static final String ATT_THUMBURI = "thumbURI";
    public static final String ATT_ADJACENCYURI = "adjacencyURI";
    public static final String ATT_NAME = "name";
    public static final String ATT_ACTIVE = "active";
    public static final String ATT_ADJECTIVE = "adjective";
    public static final String ATT_ALTNAMES = "altnames";
    public static final String ATT_TURN = "turn";
    public static final String ATT_VALUE = "value";
    public static final String ATT_PROVINCE = "province";
    public static final String ATT_HOMEPOWER = "homepower";
    public static final String ATT_OWNER = "owner";
    public static final String ATT_POWER = "power";
    public static final String ATT_UNIT = "unit";
    public static final String ATT_UNITCOAST = "unitcoast";
    public static final String ATT_ALLOW_BC_YEARS = "allowBCYears";
    public static final String ATT_PREFERRED_UNIT_STYLE = "preferredUnitStyle";
    public static final String ATT_ID = "id";
    public static final String ATT_REF = "ref";


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


    /**
     * Process the Variant list description file
     */
    private void procVariants() throws XPathExpressionException, JAXBException {
        final XPath xpath = XPathFactory.newInstance().newXPath();


        // find the root element (VARIANTS), and all VARIANT elements underneath.
        final Element root = doc.getDocumentElement();

        // get map definitions (at least one, under VARIANT)
        final NodeList mapDefEls = (NodeList) xpath
                .evaluate(EL_MAP_DEFINITION, root, XPathConstants.NODESET);

        // setup map definition ID hashmap
        final Unmarshaller mapDefUnmarshaller = JAXBContext
                .newInstance(MapDef.class).createUnmarshaller();
        final Map<String, MapDef> mapDefTable = IntStream
                .range(0, mapDefEls.getLength())
                .mapToObj(i -> (Element) mapDefEls.item(i)).map(elMapDef -> {
                    try {
                        return (MapDef) mapDefUnmarshaller.unmarshal(elMapDef);
                    } catch (JAXBException e) {
                        throw new IllegalArgumentException(e);
                    }

                })
                .collect(Collectors.toMap(MapDef::getID, Function.identity()));

        // search for variant data
        final Unmarshaller variantUnmarshaller = JAXBContext
                .newInstance(Variant.class).createUnmarshaller();
        final NodeList variantElements = (NodeList) xpath
                .evaluate(EL_VARIANT, root, XPathConstants.NODESET);
        IntStream.range(0, variantElements.getLength())
                .mapToObj(i -> (Element) variantElements.item(i))
                .forEach(elVariant -> {
                    try {
                        final Variant variant = (Variant) variantUnmarshaller
                                .unmarshal(elVariant);

                        // starting time
                        final Element elStartingtime = getSingleElementByName(
                                elVariant, "STARTINGTIME");
                        checkElement(elStartingtime, "STARTINGTIME");
                        variant.setStartingPhase(Phase.parse(
                                elStartingtime.getAttribute("turn")));
                        variant.setBCYearsAllowed(Boolean.valueOf(
                                elStartingtime.getAttribute("allowBCYears")));

                        // if start is BC, and BC years are not allowed, then BC years ARE allowed.
                        if (variant.getStartingPhase().getYear() < 0) {
                            variant.setBCYearsAllowed(true);
                        }


                        // =====================================


//                        // supply centers (multiple)
//                        final Unmarshaller unmarshaller1;
//                        try {
//                            unmarshaller1 = JAXBContext
//                                    .newInstance(SupplyCenter.class)
//                                    .createUnmarshaller();
//                        } catch (final JAXBException e1) {
//                            throw new IllegalArgumentException(e1);
//                        }
//
//                        final NodeList nodes2 = elVariant
//                                .getElementsByTagName(EL_SUPPLYCENTER);
//                        variant.setSupplyCenters(
//                                IntStream.range(0, nodes2.getLength())
//                                        .mapToObj(j -> (Element) nodes2.item(j))
//                                        .map(element -> {
//                                            try {
//                                                return (SupplyCenter) unmarshaller1
//                                                        .unmarshal(element);
//                                            } catch (JAXBException e1) {
//                                                throw new IllegalArgumentException(
//                                                        e1);
//                                            }
//                                        }).collect(Collectors.toList()));
//
//                        // initial state (multiple)
//                        final Unmarshaller unmarshaller2;
//                        try {
//                            unmarshaller2 = JAXBContext
//                                    .newInstance(InitialState.class)
//                                    .createUnmarshaller();
//                        } catch (final JAXBException e1) {
//                            throw new IllegalArgumentException(e1);
//                        }
//
//                        final NodeList nodes3 = elVariant
//                                .getElementsByTagName(EL_INITIALSTATE);
//                        variant.setInitialStates(
//                                IntStream.range(0, nodes3.getLength())
//                                        .mapToObj(j -> (Element) nodes3.item(j))
//                                        .map(element -> {
//                                            try {
//                                                return (InitialState) unmarshaller2
//                                                        .unmarshal(element);
//                                            } catch (JAXBException e1) {
//                                                throw new IllegalArgumentException(
//                                                        e1);
//                                            }
//                                        }).collect(Collectors.toList()));

                        // MAP element and children
                        final Element elMap = getSingleElementByName(elVariant,
                                EL_MAP);
                        // MAP adjacency URI; process it using ProvinceData parser
                        try {
                            final URI adjacencyURI = new URI(
                                    elMap.getAttribute(ATT_ADJACENCYURI));
                            variant.setProvinceData(
                                    AdjCache.getProvinceData(adjacencyURI));
                            variant.setBorderData(
                                    AdjCache.getBorderData(adjacencyURI));
                        } catch (final URISyntaxException | SAXException | IOException e) {
                            throw new IllegalArgumentException(e);
                        }

                        // MAP_GRAPHIC element (multiple)
                        final NodeList nodes = elMap
                                .getElementsByTagName(EL_MAP_GRAPHIC);
                        variant.setMapGraphics(
                                makeMapGraphic(mapDefTable, nodes));

                        // rule options (if any have been set)
                        // this element is optional.
                        variant.setRuleOptionNVPs(
                                makeRuleOptionNVPs(elVariant));

                        // add variant to list of variants
                        variantList.add(variant);
                    } catch (JAXBException e) {
                        throw new IllegalArgumentException(e);
                    }
                });// for(i)
    }// procVariants()

    private List<MapGraphic> makeMapGraphic(
            final Map<String, MapDef> mapDefTable, final NodeList nodes) {
        return IntStream.range(0, nodes.getLength())
                .mapToObj(j -> (Element) nodes.item(j)).map(mgElement -> {
                    final String refID = mgElement.getAttribute(ATT_REF);
                    final boolean isDefault = Boolean
                            .valueOf(mgElement.getAttribute(ATT_DEFAULT));
                    final String preferredUnitStyle = mgElement
                            .getAttribute(ATT_PREFERRED_UNIT_STYLE);

                    // lookup; if we didn't find it, throw an exception
                    final MapDef md = mapDefTable.get(refID);
                    if (md == null) {
                        throw new IllegalArgumentException(
                                "MAP_GRAPHIC refers to unknown ID: \"" + refID + "\"");
                    }

                    // create the MapGraphic object
                    return new MapGraphic(md.getMapURI(), isDefault,
                            md.getTitle(), md.getDescription(),
                            md.getThumbURI(),
                            preferredUnitStyle != null && preferredUnitStyle
                                    .isEmpty() ? md
                                    .getPrefUnitStyle() : preferredUnitStyle);
                }).collect(Collectors.toList());
    }

    private List<NameValuePair> makeRuleOptionNVPs(final Element elVariant) {
        final Element element = getSingleElementByName(elVariant,
                EL_RULEOPTIONS);
        if (element == null) {
            return Collections.emptyList();
        } else {
            final NodeList nodes = element.getElementsByTagName(EL_RULEOPTION);
            return IntStream.range(0, nodes.getLength())
                    .mapToObj(j -> (Element) nodes.item(j)).map(rElement -> {
                        return new NameValuePair(
                                rElement.getAttribute(ATT_NAME),
                                rElement.getAttribute(ATT_VALUE));
                    }).collect(Collectors.toList());
        }
    }


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
    private static class AdjCache {
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
    private static class MapDef {
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



