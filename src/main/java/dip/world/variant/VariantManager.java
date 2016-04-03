//
//  @(#)VariantManager.java	1.00	7/2002
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
package dip.world.variant;

import dip.misc.Log;
import dip.misc.Resources;
import dip.world.variant.data.MapGraphic;
import dip.world.variant.data.SymbolPack;
import dip.world.variant.data.Variant;
import dip.world.variant.data.VersionNumber;
import dip.world.variant.parser.XMLSymbolParser;
import dip.world.variant.parser.XMLVariantParser;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Finds Variant-packs, which are of the format:
 * <ol>
 * <li>*Variant.zip</li>
 * <li>*Variants.zip</li>
 * <li>*Variant.jar</li>
 * <li>*Variants.jar</li>
 * </ol>
 * Faciliates loading of variant resources. Within the above file, the variants.xml
 * file is parsed to determine the required information.
 * <p>
 * Also finds all SymbolPacks, which end in:
 * <ol>
 * <li>*Symbols.zip</li>
 * <li>*Symbols.jar</li>
 * </ol>
 * <p>
 * <p>
 * TODO: deconflict code may work better if we include the preceding "/" before
 * the .jar name.<br>
 * <br>
 */
public final class VariantManager {
    public static final Logger LOG = LoggerFactory.getLogger(VariantManager.class);
    /**
     * Version Constant representing the most recent version of a Variant or SymbolPack
     */
    public static final VersionNumber VERSION_NEWEST = new VersionNumber(-1000,
            0);

    private static final String VARIANT_FILE_NAME = "variants.xml";
    private static final String SYMBOL_FILE_NAME = "symbols.xml";

    private final Map<String, MapRec<VRec>> variantMap;    // map of lowercased Variant names to MapRec objects (which contain VRecs)
    private final Map<String, MapRec<SPRec>> symbolMap;    // lowercase symbol names to MapRec objects (which contain SPRecs)


    /**
     * Initiaize the VariantManager.
     * <p>
     * An exception is thrown if no File paths are specified. A "." may be used
     * to specify th ecurrent directory.
     * <p>
     * Loaded XML may be validated if the isValidating flag is set to true.
     */
    /**
     * Singleton
     */
    public VariantManager() {
        // perform cleanup

        // find plugins, create plugin loader

        // for each plugin, attempt to find the "variants.xml" file inside.
        // if it does not exist, we will not load the file. If it does, we will parse it,
        // and associate the variant with the URL in a hashtable.

        final Map<String, MapRec<VRec>> collect = Resources
                .getResourceURLs(url -> {
                    return url.getPath().endsWith(VARIANT_FILE_NAME);
                }).stream().flatMap(variantXMLURL -> {
                    // parse variant description file, and create hash entry of variant object -> URL
                    // add variants; variants with same name (but older versions) are
                    // replaced with same-name newer versioned variants
                    return new XMLVariantParser(variantXMLURL).getVariants()
                            .stream().map(variant -> {// FIXME
                                final VRec vRec = new VRec(variantXMLURL,
                                        variantXMLURL.getFile(), variant);
                                return vRec;
                            });
                }).collect(Collectors.groupingBy(
                        vRec -> vRec.getVariant().getName().toLowerCase()))
                .entrySet().stream().collect(Collectors.toMap(Entry::getKey,
                        entry -> entry.getValue().stream()
                                .collect(MapRec<VRec>::new, MapRec::add,
                                        (mr0, mr1) -> {
                                        })));
        variantMap = collect.entrySet().stream().flatMap(entry -> {
            final List<String> keys = new ArrayList<>(
                    entry.getValue().getNewest().get().getVariant()
                            .getAliases());
            final String origin = entry.getKey();
            keys.add(origin);
            return keys.stream()
                    .map(key -> new Pair<>(key, collect.get(origin)));
        }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));


        // now, parse symbol packs

        symbolMap = Resources.getResourceURLs(url -> {
            return url.getPath().endsWith(SYMBOL_FILE_NAME);
        }).stream().map(symbolXMLURL -> {
            try { // FIXME pluginName
                return new SPRec(symbolXMLURL, symbolXMLURL.getFile(),
                        new XMLSymbolParser(symbolXMLURL).getSymbolPack());
            } catch (ParserConfigurationException | MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }).collect(Collectors.groupingBy(
                spRec -> spRec.getSymbolPack().getName().toLowerCase()))
                .entrySet().stream().collect(Collectors.toMap(Entry::getKey,
                        entry -> entry.getValue().stream()
                                .collect(MapRec<SPRec>::new, MapRec::add,
                                        (mr0, mr1) -> {
                                        })));
    }

    /**
     * Returns the known Variants. If multiple versions of a Variant
     * exist, only the latest version is returned. The list is
     * sorted in alphabetic order.
     */
    public synchronized List<Variant> getVariants() {
        // The sorted Variant list
        // note that we need to avoid putting duplicates
        // into the array.
        // fill variant list with variants.
        return variantMap.values().stream().distinct().flatMap(
                rec -> rec.getNewest().map(vrec -> Stream.of(vrec.getVariant()))
                        .orElse(Stream.empty())).sorted()
                .collect(Collectors.toList());
    }// getVariants()

    /**
     * Returns the known SymbolPacks. If multiple versions of a SymbolPack
     * exist, only the latest version is returned. The list is
     * sorted in alphabetic order.
     */
    public synchronized List<SymbolPack> getSymbolPacks() {
        // avoid putting duplicates into the array.
        // fill variant list with variants.
        return symbolMap.values().stream().distinct().flatMap(
                rec -> rec.getNewest()
                        .map(sprec -> Stream.of(sprec.getSymbolPack()))
                        .orElse(Stream.empty())).sorted()
                .collect(Collectors.toList());
    }// getSymbolPacks()


    /**
     * Finds Variant with the given name, or null if no Variant is found.
     * Attempts to find the version specified. Versions must be > 0.0f or
     * the version constants VERSION_NEWEST or VERSION_OLDEST.
     * <p>
     * Note: Name is <b>not</b> case-sensitive.
     */
    public synchronized Optional<Variant> getVariant(final String name,
                                                     final VersionNumber version) {
        return Optional.ofNullable(variantMap.get(name.toLowerCase()))
                .flatMap(mr -> mr.get(version).map(VRec::getVariant));
    }// getVariant()


    /**
     * Finds SymbolPack with the given name, or null if no SymbolPack is found.
     * Attempts to find the version specified. Versions must be > 0.0f or
     * the version constants VERSION_NEWEST or VERSION_OLDEST.
     * <p>
     * Note: Name is <b>not</b> case-sensitive.
     */
    public synchronized Optional<SymbolPack> getSymbolPack(final String name,
                                                           final VersionNumber version) {
        return Optional.ofNullable(name).flatMap(name0 -> Optional
                .ofNullable(symbolMap.get(name0.toLowerCase())))
                .flatMap(mr -> mr.get(version).map(SPRec::getSymbolPack));
    }// getSymbolPack()

    /**
     * Obtains a SymbolPack via the following criteria:
     * <ol>
     * <li>If matching SymbolPack name and Version found, that is returned; otherwise</li>
     * <li>Returns SymbolPack of same name but of the newest available version; otherwise</li>
     * <li>Returns the newest available SymbolPack preferred by the MapGraphic (if set); otherwise</li>
     * <li>Returns the first SymbolPack in the list of SymbolPacks.</li>
     * </ol>
     * <p>
     * Thus it is assured that a SymbolPack will always be obtained.
     */
    public synchronized Optional<SymbolPack> getSymbolPack(final MapGraphic mg,
                                                           final String symbolPackName,
                                                           final VersionNumber symbolPackVersion) {
        Objects.requireNonNull(mg);

        // safety:
        // if version is invalid (< 0.0f), convert to VERSION_NEWEST
        // automatically. Log this method, though
        VersionNumber spVersion = symbolPackVersion;
        if (spVersion.compareTo(new VersionNumber(0, 0)) <= 0) {
            Log.println(
                    "WARNING: VariantManager.getSymbolPack() called with symbolPackVersion of <= 0.0f. Check parameters.");
            spVersion = VERSION_NEWEST;
        }

        final Optional<SymbolPack> sp0 = getSymbolPack(symbolPackName,
                spVersion);
        if (!sp0.isPresent()) {
            final Optional<SymbolPack> sp = getSymbolPack(symbolPackName,
                    VERSION_NEWEST);
            if (!sp.isPresent() && mg.getPreferredSymbolPackName() != null) {
                return getSymbolPack(mg.getPreferredSymbolPackName(),
                        VERSION_NEWEST);
            }

            if (!sp.isPresent()) {
                return Optional.of(getSymbolPacks().get(0));
            }
        }

        return sp0;
    }// getSymbolPack()


    /**
     * Returns true if the desired version was found. Version must
     * be a positive floating point value, or, a defined constant.
     * Returns false if the version is not available or the variant
     * is not found.
     */
    public boolean hasVariantVersion(final String name,
                                     final VersionNumber version) {
        return getVariant(name, version) != null;
    }// hasVariantVersion()

    /**
     * Returns true if the desired version was found. Version must
     * be a positive floating point value, or, a defined constant.
     * Returns false if the version is not available or the SymbolPack
     * is not found.
     */
    public boolean hasSymbolPackVersion(final String name,
                                        final VersionNumber version) {
        return getSymbolPack(name, version) != null;
    }// hasVariantVersion()


    /**
     * Returns the versions of a variant that are available.
     * If the variant is not found, a zero-length array is returned.
     */
    public synchronized List<VersionNumber> getVariantVersions(
            final String name) {
        return Optional.ofNullable(variantMap.get(name.toLowerCase()))
                .map(MapRec::getVersions).orElse(Collections.emptyList());
    }// getVariantVersions()

    /**
     * Returns the versions of a SymbolPack that are available.
     * If the SymbolPack is not found, a zero-length array is returned.
     */
    public synchronized List<VersionNumber> getSymbolPackVersions(
            final String name) {
        return Optional.ofNullable(symbolMap.get(name.toLowerCase()))
                .map(MapRec::getVersions).orElse(Collections.emptyList());

    }// getSymbolPackVersions()


    /**
     * Gets a specific resource by properly resolving the URI
     * to this Variant. Null arguments are illegal. Returns
     * null if the resource cannot be resolved. Threadsafe.
     */
    public Optional<URL> getResource(final Variant variant, final URI uri) {
        Objects.requireNonNull(variant);
        return getVRec(variant).flatMap(vRec -> getResource(vRec, uri));
    }// getResource()


    /**
     * Gets a specific resource by properly resolving the URI
     * to this SymbolPack. Null arguments are illegal. Returns
     * null if the resource cannot be resolved. Threadsafe.
     */
    public Optional<URL> getResource(final SymbolPack symbolPack,
                                     final URI uri) {
        Objects.requireNonNull(symbolPack);
        return getSPRec(symbolPack).flatMap(spRec -> getResource(spRec, uri));
    }// getResource()


    /**
     * Gets the URL to the Variant package (plugin). This is typically
     * only needed in special circumstances. Returns null if null variant
     * input OR variant not found.
     * <p>
     * Note that this will always return a URL with a JAR prefix.
     * e.g.: <code>jar:http:/the.location/ajar.zip!/</code>
     * or <code>jar:file:/c:/plugins/ajar.zip!/</code>
     */
    public Optional<URL> getVariantPackageJarURL(final Variant variant) {
        return Optional.ofNullable(variant).flatMap(v -> getVRec(v)
                .flatMap(vrec -> Optional.ofNullable(vrec.getURL()))
                .flatMap(url -> {
                    final String txtUrl = url.toString();
                    if (txtUrl.startsWith("jar:")) {
                        return Optional.of(url);
                    }
                    try {
                        return Optional
                                .of(new URL(String.format("jar:%s!/", txtUrl)));
                    } catch (final MalformedURLException e) {
                        LOG.debug("Could not convert {} to a JAR url.", url);
                        LOG.debug("Exception: {}", e);
                        return Optional.empty();
                    }
                }));
    }// getVariantPackageURL()

    /**
     * Internal getResource() implementation
     */
    private synchronized Optional<URL> getResource(final MapRecObj mro,
                                                   final URI uri) {
        Objects.requireNonNull(mro);
        Objects.requireNonNull(uri);

        // if URI has a defined scheme, convert to a URL (if possible) and return it.
        if (uri.getScheme() != null) {
            try {
                return Optional.of(uri.toURL());
            } catch (final MalformedURLException ignored) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(mro.getURL())
                .map(getURL -> getClassLoader(getURL)
                        .findResource(uri.toString()));
    }// getResource()


    /**
     * Returns the URLClassLoader for a given URL, or creates a new one....
     */

    private URLClassLoader getClassLoader(final URL packageURL) {
        // WARNING: this method is not (itself) threadsafe
        Objects.requireNonNull(packageURL);
        return new URLClassLoader(new URL[]{packageURL});
    }// getClassLoader()


    /**
     * Returns the "file" part of the URL; e.g.: x/y/z.jar, returns z.jar
     */
    private static String getFile(final URL url) {
        final String s = url.toString();
        return s.substring(s.lastIndexOf("/") + 1, s.length());
    }// getFile()


    /**
     * Gets the VRec associated with a Variant (via name and version)
     */
    private Optional<VRec> getVRec(final Variant v) {
        final MapRec<VRec> mapRec = variantMap.get(v.getName().toLowerCase());
        return mapRec.get(v.getVersion());
    }// getVRec()


    /**
     * Gets the SPRec associated with a SymbolPack (via name and version)
     */
    private Optional<SPRec> getSPRec(final SymbolPack sp) {
        final MapRec<SPRec> mapRec = symbolMap.get(sp.getName().toLowerCase());
        return mapRec.get(sp.getVersion());
    }// getSPRec()

    /**
     * The value which is stored within the name mapping
     */
    private final class MapRec<T extends MapRecObj> {
        private final List<T> list = new ArrayList<>(2);

        MapRec() {

        }

        public int size() {
            return list.size();
        }

        /**
         * Adds the MapRecObj to this MapRec, but only if it is of
         * a unique version. If it is not, returns false. Otherwise,
         * the MapRecObj is added and returns true.
         */
        public void add(final T obj) {
            if (list.stream().noneMatch(aList -> (Objects
                    .equals(aList.getVersion(), obj.getVersion())))) {
                list.add(obj);
            } else {
                throw new IllegalArgumentException(String.format(
                        "Don't add the same version.[class=%s][%s]",
                        obj.getClass(), obj.toString()));
            }

        }// add()

        /**
         * Get all available versions
         */
        public List<VersionNumber> getVersions() {
            return list.stream().map(T::getVersion)
                    .collect(Collectors.toList());
        }// getVersions()

        /**
         * Get the desired version. Supports version constants.
         * Returns null if version not found (shouldn't occur if
         * version constants used, and at least one element exists)
         *
         * @param version
         */
        public Optional<T> get(final VersionNumber version) {
            if (version.equals(VERSION_NEWEST)) {
                return getNewest();
            }
            if (version.compareTo(new VersionNumber(0, 0)) < 0) {
                throw new IllegalArgumentException(
                        String.format("invalid version or version constant: %s",
                                version));
            }
            return list.stream()
                    .filter(list -> list.getVersion().equals(version))
                    .findFirst();

        }// get()

        public Optional<T> getNewest() {
            return list.stream()
                    .sorted(Comparator.comparing(MapRecObj::getVersion))
                    .reduce((a, b) -> b);
        }
    }// inner class VMRec


    /**
     * MapRec stores a list of ObjRecs
     */
    private abstract static class MapRecObj {
        private final URL fileURL;
        private final String pluginName;

        MapRecObj(final URL fileURL, final String pluginName) {
            this.fileURL = fileURL;
            this.pluginName = pluginName;
        }

        public final String getPluginName() {
            return pluginName;
        }

        public final URL getURL() {
            return fileURL;
        }

        public abstract VersionNumber getVersion();

    }// inner class ObjRec


    /**
     * An ObjRec for Variant objects
     */
    private static final class VRec extends MapRecObj {
        private final Variant variant;

        VRec(final URL pluginURL, final String pluginName, final Variant v) {
            super(pluginURL, pluginName);
            variant = v;
        }

        public Variant getVariant() {
            return variant;
        }

        @Override
        public VersionNumber getVersion() {
            return variant.getVersion();
        }

        @Override
        public String toString() {
            return String
                    .format("VRec[name=%s; version=%s; pluginName=%s; pluginURL=%s]",
                            variant.getName(), getVersion(), getPluginName(),
                            getURL());
        }
    }// inner class VRec

    /**
     * An ObjRec for SymbolPack objects
     */
    private static final class SPRec extends MapRecObj {
        private final SymbolPack symbolPack;

        SPRec(final URL pluginURL, final String pluginName,
              final SymbolPack sp) {
            super(pluginURL, pluginName);
            symbolPack = sp;
        }

        public SymbolPack getSymbolPack() {
            return symbolPack;
        }

        @Override
        public VersionNumber getVersion() {
            return symbolPack.getVersion();
        }

        @Override
        public String toString() {
            return String
                    .format("SPRec[name=%s; version=%s; pluginName=%s; pluginURL=%s]",
                            symbolPack.getName(), symbolPack.getVersion(),
                            getPluginName(), getURL());
        }


    }// inner class SPRec

}// class VariantManager
