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

import javax.xml.bind.JAXB;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Parses an XML Variant description.
 */
public class XMLVariantParser implements VariantParser {

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
        if (variantsXMLURL == null) {
            throw new IllegalArgumentException();
        }

        Log.println("XMLVariantParser: Parsing: ", variantsXMLURL);
        final long time = System.currentTimeMillis();

        // cleanup cache (very important to remove references!)
        AdjCache.clear();
        AdjCache.setVariantPackageURL(variantsXMLURL);

        final RootVariants rootVariants = JAXB
                .unmarshal(variantsXMLURL, RootVariants.class);
        variantList = rootVariants.variants;
        Log.printTimed(time, "   time: ");
    }// parse()


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
        private static final LRUCache<URI, AdjCache> adjCache = new LRUCache<>(
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
        public static ProvinceData[] getProvinceData(final URI adjacencyURI) {
            final AdjCache ac = get(adjacencyURI);
            return ac.provinceData
                    .toArray(new ProvinceData[ac.provinceData.size()]);
        }// getProvinceData()


        /**
         * Gets the BorderData for a given adjacency URI
         */
        public static BorderData[] getBorderData(final URI adjacencyURI) {
            final AdjCache ac = get(adjacencyURI);
            return ac.borderData.toArray(new BorderData[ac.borderData.size()]);
        }// getBorderData()


        /**
         * Gets the AdjCache object from the cache, or parses from the URI, as appropriate
         */
        private static AdjCache get(final URI aURI) {
            // see if we already have the URI data cached.
            return adjCache.computeIfAbsent(aURI, adjacencyURI -> {
                URL url
//                        VariantManager.getInstance()
//                        .getResource(vpURL, adjacencyURI);
                        ;
                try {
                    url = vpURL.toURI().resolve(adjacencyURI).toURL();
                } catch (URISyntaxException | MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
                if (url == null) {
                    throw new IllegalArgumentException(String.format(
                            "Could not convert URI: [%s] from variant package: [%s]",
                            adjacencyURI, vpURL));
                }

                //Log.println("  AdjCache: not in cache: ", adjacencyURI);

                try (InputStream is = new BufferedInputStream(
                        url.openStream())) {
                    final XMLProvinceParser pp = new XMLProvinceParser(is);
                    return new AdjCache(Arrays.asList(pp.getProvinceData()),
                            Arrays.asList(pp.getBorderData()));
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
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
            if (title != null && title.isEmpty()) {
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



