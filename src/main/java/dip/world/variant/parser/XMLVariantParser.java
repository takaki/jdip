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
import dip.world.variant.data.BorderData;
import dip.world.variant.data.ProvinceData;
import dip.world.variant.data.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/**
 * Parses an XML Variant description.
 */
public class XMLVariantParser implements VariantParser {
    private static final Logger LOG = LoggerFactory.getLogger(
            XMLVariantParser.class);

    private final List<Variant> variantList;

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
     * Parse the given input stream; parsed data available via <code>getVariants()</code>
     * <p>
     * Note that when this method is called, any previous Variants (if any exist) are
     * cleared.
     */
    public XMLVariantParser(final URL variantsXMLURL) {
        Objects.requireNonNull(variantsXMLURL);

        LOG.debug("XMLVariantParser: Parsing: {}", variantsXMLURL);
        final long time = System.currentTimeMillis();

        // cleanup cache (very important to remove references!)

        final RootVariants rootVariants = JAXB
                .unmarshal(variantsXMLURL, RootVariants.class);
        variantList = rootVariants.variants;
        variantList.stream().forEach(
                variant -> variant.setBaseURL(variantsXMLURL)); // FIXME
        LOG.debug(Log.printTimed(time, "   time: "));
    }// parse()


    /**
     * Returns an array of Variant objects.
     * <p>
     * Will never return null. Note that parse() must be called before
     * this will return any information.
     */
    @Override
    public List<Variant> getVariants() {
        return Collections.unmodifiableList(variantList);
    }// getVariants()


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
        private static final LRUCache<URL, AdjCache> adjCache = new LRUCache<>(
                6); // URI -> AdjCache objects

        // instance variables
        private final List<ProvinceData> provinceData;
        private final List<BorderData> borderData;

        public AdjCache(final List<ProvinceData> provinceData,
                        final List<BorderData> borderData) {
            this.provinceData = new ArrayList<>(provinceData);
            this.borderData = new ArrayList<>(borderData);
        }

        /**
         * Gets the ProvinceData for a given adjacency URI
         *
         * @param adjacencyURL
         */
        public static List<ProvinceData> getProvinceData(
                final URL adjacencyURL) {
            final AdjCache ac = get(adjacencyURL);
            return Collections.unmodifiableList(ac.provinceData);
        }// getProvinceData()


        /**
         * Gets the BorderData for a given adjacency URI
         *
         * @param adjacencyURL
         */
        public static List<BorderData> getBorderData(final URL adjacencyURL) {
            final AdjCache ac = get(adjacencyURL);
            return Collections.unmodifiableList(ac.borderData);
        }// getBorderData()


        /**
         * Gets the AdjCache object from the cache, or parses from the URI, as appropriate
         */
        private static AdjCache get(final URL url) {
            // see if we already have the URI data cached.
            return adjCache.computeIfAbsent(url, adjacencyURI -> {
                // final URL url = new URL(vpURL, adjacencyURI.toString());
                final XMLProvinceParser pp = new XMLProvinceParser(
                        adjacencyURI);
                return new AdjCache(pp.getProvinceData(), pp.getBorderData());
            });
        }// get()
    }// inner class AdjCache


    /**
     * Class that holds MAP_DEFINITION data, which is
     * inserted into a hashtable for later recall.
     */
    @XmlRootElement(name = "MAP_DEFINITION")
    public static final class MapDef {
        @XmlID
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
            if (title == null || title.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("map id=%s missing a title (name)", id));
            }
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

        public String getDescription() {
            return description;
        }
    }// inner class MapDef

}// class XMLVariantParser



