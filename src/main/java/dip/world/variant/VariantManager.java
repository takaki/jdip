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
import dip.misc.Utils;
import dip.world.variant.data.MapGraphic;
import dip.world.variant.data.SymbolPack;
import dip.world.variant.data.Variant;
import dip.world.variant.parser.SymbolParser;
import dip.world.variant.parser.VariantParser;
import dip.world.variant.parser.XMLSymbolParser;
import dip.world.variant.parser.XMLVariantParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;


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
public class VariantManager {
    /**
     * Version Constant representing the most recent version of a Variant or SymbolPack
     */
    public static final double VERSION_NEWEST = -1000.0;
    /**
     * Version Constant representing the most oldest version of a Variant or SymbolPack
     */
    public static final double VERSION_OLDEST = -2000.0;


    // variant constants
    private static final List<String> VARIANT_EXTENSIONS = Arrays
            .asList("Variant.zip", "Variants.zip", "Variant.jar",
                    "Variants.jar");
    private static final String VARIANT_FILE_NAME = "variants.xml";

    // symbol constants
    private static final List<String> SYMBOL_EXTENSIONS = Arrays
            .asList("Symbols.zip", "Symbols.jar");
    private static final String SYMBOL_FILE_NAME = "symbols.xml";

    // class variables
    private static final VariantManager vm = new VariantManager();

    // instance variables
    private final boolean inWebstart;
    private final Map<String, MapRec<VRec>> variantMap;    // map of lowercased Variant names to MapRec objects (which contain VRecs)
    private final Map<String, MapRec<SPRec>> symbolMap;    // lowercase symbol names to MapRec objects (which contain SPRecs)

    // cached variables to enhance performance of getResource() methods
    private List<SymbolPack> symbolPacks = Collections
            .emptyList();// The sorted SymbolPack list

    public static VariantManager getInstance() {
        return vm;
    }

    /**
     * Initiaize the VariantManager.
     * <p>
     * An exception is thrown if no File paths are specified. A "." may be used
     * to specify th ecurrent directory.
     * <p>
     * Loaded XML may be validated if the isValidating flag is set to true.
     */
    public synchronized void init(
            final File[] searchPaths) throws ParserConfigurationException, NoVariantsException {
        final long ttime = System.currentTimeMillis();
        final long vptime = ttime;
        Log.println("VariantManager.init()");

        if (searchPaths == null || searchPaths.length == 0) {
            throw new IllegalArgumentException();
        }

        // perform cleanup
        variantMap.clear();
        symbolPacks = Collections.emptyList();
        symbolMap.clear();


        // find plugins, create plugin loader


        // for each plugin, attempt to find the "variants.xml" file inside.
        // if it does not exist, we will not load the file. If it does, we will parse it,
        // and associate the variant with the URL in a hashtable.
        for (final URL pluginURL1 : searchForFiles(Arrays.asList(searchPaths),
                VARIANT_EXTENSIONS)) {
            try (final URLClassLoader urlCL = new URLClassLoader(
                    new URL[]{pluginURL1})) {
                final URL variantXMLURL = urlCL.findResource(VARIANT_FILE_NAME);
                if (variantXMLURL != null) {
                    final String pluginName = getFile(pluginURL1);

                    // parse variant description file, and create hash entry of variant object -> URL
                    try (InputStream is = new BufferedInputStream(
                            variantXMLURL.openStream())) {
                        final VariantParser variantParser = new XMLVariantParser(
                                is, pluginURL1);
                        // add variants; variants with same name (but older versions) are
                        // replaced with same-name newer versioned variants
                        for (final Variant variant : variantParser
                                .getVariants()) {
                            addVariant(variant, pluginName, pluginURL1);
                        }
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }


        // if we are in webstart, search for variants within webstart jars

        if (inWebstart) {
            try {
                Collections.list(getClass().getClassLoader()
                        .getResources(VARIANT_FILE_NAME)).stream()
                        .forEach(variantURL -> {
                            // parse variant description file, and create hash entry of variant object -> URL
                            try (InputStream is = new BufferedInputStream(
                                    variantURL.openStream())) {
                                final String pluginName = getWSPluginName(
                                        variantURL);
                                // setup variant parser
                                final VariantParser variantParser = new XMLVariantParser(
                                        is, variantURL);
                                // add variants; variants with same name (but older versions) are
                                // replaced with same-name newer versioned variants
                                for (final Variant variant : variantParser
                                        .getVariants()) {
                                    addVariant(variant, pluginName, variantURL);
                                }
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

        }

        // check: did we find *any* variants? Throw an exception.
        if (variantMap.isEmpty()) {
            throw new NoVariantsException(
                    String.join("", "No variants found on path: ",
                            Arrays.stream(searchPaths).map(File::toString)
                                    .collect(Collectors.joining("; ")), "; "));
        }

        Log.printTimed(vptime, "VariantManager: variant parsing time: ");

        ///////////////// SYMBOLS /////////////////////////


        // now, parse symbol packs

        // find plugins, create plugin loader

        // for each plugin, attempt to find the "variants.xml" file inside.
        // if it does not exist, we will not load the file. If it does, we will parse it,
        // and associate the variant with the URL in a hashtable.
        for (final URL pluginURL : searchForFiles(Arrays.asList(searchPaths),
                SYMBOL_EXTENSIONS)) {
            final URLClassLoader urlCL = new URLClassLoader(
                    new URL[]{pluginURL});
            final URL symbolXMLURL = urlCL.findResource(SYMBOL_FILE_NAME);
            if (symbolXMLURL != null) {
                final String pluginName = getFile(pluginURL);

                // parse variant description file, and create hash entry of variant object -> URL
                try (InputStream is = new BufferedInputStream(
                        symbolXMLURL.openStream())) {
                    final SymbolParser symbolParser = new XMLSymbolParser(is,
                            pluginURL);
                    addSymbolPack(symbolParser.getSymbolPack(), pluginName,
                            pluginURL);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } catch (final SAXException | XPathExpressionException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        // if we are in webstart, search for variants within webstart jars

        if (inWebstart) {

            try {
                Collections.list(getClass().getClassLoader()
                        .getResources(SYMBOL_FILE_NAME)).stream()
                        .forEach(symbolURL -> {
                            // parse variant description file, and create hash entry of variant object -> URL
                            try (InputStream is = new BufferedInputStream(
                                    symbolURL.openStream())) {
                                final SymbolParser symbolParser = new XMLSymbolParser(
                                        is, symbolURL);
                                addSymbolPack(symbolParser.getSymbolPack(),
                                        getWSPluginName(symbolURL), symbolURL);
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            } catch (final SAXException | XPathExpressionException | ParserConfigurationException e) {
                                throw new IllegalArgumentException(e);
                            }
                        });
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }

        }// if(isInWebStart)


        // check: did we find *any* symbol packs? Throw an exception.
        if (symbolMap.isEmpty()) {
            throw new NoVariantsException(
                    String.join("", "No SymbolPacks found on path: ",
                            Arrays.stream(searchPaths).map(File::toString)
                                    .collect(Collectors.joining("; ")), "; "));
        }
        Log.printTimed(ttime, "VariantManager: total parsing time: ");
    }// init()


    /**
     * Returns the known Variants. If multiple versions of a Variant
     * exist, only the latest version is returned. The list is
     * sorted in alphabetic order.
     */
    public synchronized Variant[] getVariants() {

        // The sorted Variant list

        // note that we need to avoid putting duplicates
        // into the array.

        // fill variant list with variants.

        return variantMap.values().stream().distinct()
                .map(mr -> {
                    final VRec mro = mr.get(VERSION_NEWEST);
                    assert mro != null;
                    return mro.getVariant();
                }).sorted().toArray(Variant[]::new);
    }// getVariants()

    /**
     * Returns the known SymbolPacks. If multiple versions of a SymbolPack
     * exist, only the latest version is returned. The list is
     * sorted in alphabetic order.
     */
    public synchronized SymbolPack[] getSymbolPacks() {

        if (symbolPacks.size() != symbolMap.size()) {
            // avoid putting duplicates into the array.

            // fill variant list with variants.
            symbolPacks = symbolMap.values().stream().distinct().map(mr -> {
                final SPRec mro = mr.get(VERSION_NEWEST);
                assert mro != null;
                return mro.getSymbolPack();
            }).sorted().collect(Collectors.toList());
        }

        return symbolPacks.toArray(new SymbolPack[symbolPacks.size()]);
    }// getSymbolPacks()


    /**
     * Finds Variant with the given name, or null if no Variant is found.
     * Attempts to find the version specified. Versions must be > 0.0f or
     * the version constants VERSION_NEWEST or VERSION_OLDEST.
     * <p>
     * Note: Name is <b>not</b> case-sensitive.
     */
    public synchronized Variant getVariant(final String name,
                                           final double version) {
        final MapRec<VRec> mr = variantMap.get(name.toLowerCase());
        if (mr != null) {
            return mr.get(version).getVariant();
        }

        return null;
    }// getVariant()


    /**
     * Finds SymbolPack with the given name, or null if no SymbolPack is found.
     * Attempts to find the version specified. Versions must be > 0.0f or
     * the version constants VERSION_NEWEST or VERSION_OLDEST.
     * <p>
     * Note: Name is <b>not</b> case-sensitive.
     */
    public synchronized SymbolPack getSymbolPack(final String name,
                                                 final double version) {
        if (name == null) {
            return null;
        }

        final MapRec<SPRec> mr = symbolMap.get(name.toLowerCase());
        if (mr != null) {
            return mr.get(version).getSymbolPack();
        }

        return null;
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
    public synchronized SymbolPack getSymbolPack(final MapGraphic mg,
                                                 final String symbolPackName,
                                                 final double symbolPackVersion) {
        if (mg == null) {
            throw new IllegalArgumentException();
        }

        // safety:
        // if version is invalid (< 0.0f), convert to VERSION_NEWEST
        // automatically. Log this method, though
        double spVersion = symbolPackVersion;
        if (spVersion <= 0.0f) {
            Log.println(
                    "WARNING: VariantManager.getSymbolPack() called with symbolPackVersion of <= 0.0f. Check parameters.");
            spVersion = VERSION_NEWEST;
        }

        SymbolPack sp = getSymbolPack(symbolPackName, spVersion);
        if (sp == null) {
            sp = getSymbolPack(symbolPackName, VERSION_NEWEST);
            if (sp == null && mg.getPreferredSymbolPackName() != null) {
                sp = getSymbolPack(mg.getPreferredSymbolPackName(),
                        VERSION_NEWEST);
            }

            if (sp == null) {
                sp = getSymbolPacks()[0];
            }
        }

        return sp;
    }// getSymbolPack()


    /**
     * Returns true if the desired version was found. Version must
     * be a positive floating point value, or, a defined constant.
     * Returns false if the version is not available or the variant
     * is not found.
     */
    public boolean hasVariantVersion(final String name, final double version) {
        return getVariant(name, version) != null;
    }// hasVariantVersion()

    /**
     * Returns true if the desired version was found. Version must
     * be a positive floating point value, or, a defined constant.
     * Returns false if the version is not available or the SymbolPack
     * is not found.
     */
    public boolean hasSymbolPackVersion(final String name,
                                        final double version) {
        return getSymbolPack(name, version) != null;
    }// hasVariantVersion()


    /**
     * Returns the versions of a variant that are available.
     * If the variant is not found, a zero-length array is returned.
     */
    public synchronized double[] getVariantVersions(final String name) {
        final MapRec<VRec> mr = variantMap.get(name.toLowerCase());
        return mr == null ? new double[0] : mr.getVersions();

    }// getVariantVersions()

    /**
     * Returns the versions of a SymbolPack that are available.
     * If the SymbolPack is not found, a zero-length array is returned.
     */
    public synchronized double[] getSymbolPackVersions(final String name) {
        final MapRec<SPRec> mr = symbolMap.get(name.toLowerCase());
        return mr == null ? new double[0] : mr.getVersions();

    }// getSymbolPackVersions()


    /**
     * Ensures version is positive OR VERSION_NEWEST or VERSION_OLDEST
     *
     * @param version
     */
    private void checkVersionConstant(final double version) {
        if (version <= 0.0f && version != VERSION_NEWEST && version != VERSION_OLDEST) {
            throw new IllegalArgumentException(
                    String.format("invalid version or version constant: %s",
                            version));
        }
    }// checkVersionConstant()

    /**
     * Gets a specific resource for a Variant or a SymbolPack, given a URL to
     * the package and a reference URI. Threadsafe.
     * <p>
     * Typically, getResource(Variant, URI) or getResource(SymbolPack, URI) is
     * preferred to this method.
     */
    public synchronized URL getResource(final URL packURL, final URI uri) {
        // ensure we have been initialized...

        // if we are in webstart, assume that this is a webstart jar.
        if (inWebstart) {
            final URL url = getWSResource(packURL, uri);

            // if cannot get it, fall through.
            if (url != null) {
                return url;
            }
        }

        // if URI has a defined scheme, convert to a URL (if possible) and return it.
        if (uri.getScheme() != null) {
            try {
                return uri.toURL();
            } catch (final MalformedURLException e) {
                return null;
            }
        }

        // resolve & load.
        final URLClassLoader classLoader = getClassLoader(packURL);
        return classLoader.findResource(uri.toString());
    }// getResource()


    /**
     * Gets a specific resource by properly resolving the URI
     * to this Variant. Null arguments are illegal. Returns
     * null if the resource cannot be resolved. Threadsafe.
     */
    public URL getResource(final Variant variant, final URI uri) {
        if (variant == null) {
            throw new IllegalArgumentException();
        }
        return getResource(getVRec(variant), uri);
    }// getResource()


    /**
     * Gets a specific resource by properly resolving the URI
     * to this SymbolPack. Null arguments are illegal. Returns
     * null if the resource cannot be resolved. Threadsafe.
     */
    public URL getResource(final SymbolPack symbolPack, final URI uri) {
        if (symbolPack == null) {
            throw new IllegalArgumentException();
        }
        return getResource(getSPRec(symbolPack), uri);
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
    public URL getVariantPackageJarURL(final Variant variant) {
        if (variant != null) {
            final VRec vr = getVRec(variant);
            if (vr != null) {
                assert vr.getURL() != null;

                final URL url = vr.getURL();
                final String txtUrl = url.toString();

                if (txtUrl.startsWith("jar:")) {
                    return url;
                } else {
                    final StringBuffer sb = new StringBuffer(
                            txtUrl.length() + 8);
                    sb.append("jar:");
                    sb.append(txtUrl);
                    sb.append("!/");

                    try {
                        return new URL(sb.toString());
                    } catch (final MalformedURLException e) {
                        Log.println("Could not convert ", url,
                                " to a JAR url.");
                        Log.println("Exception: ", e);
                    }
                }
            }
        }

        return null;
    }// getVariantPackageURL()

    /**
     * Internal getResource() implementation
     */
    private synchronized URL getResource(final MapRecObj mro, final URI uri) {
        // ensure we have been initialized...
        assert mro != null;

        if (uri == null) {
            throw new IllegalArgumentException("null URI");
        }

        // if we are in webstart, assume that this is a webstart jar.
        if (inWebstart) {
            final URL url = getWSResource(mro, uri);

            // if cannot get it, fall through.
            if (url != null) {
                return url;
            }
        }


        // if URI has a defined scheme, convert to a URL (if possible) and return it.
        if (uri.getScheme() != null) {
            try {
                return uri.toURL();
            } catch (final MalformedURLException e) {
                return null;
            }
        }

        // find the URL
        if (mro.getURL() != null) {
            return getClassLoader(mro.getURL()).findResource(uri.toString());
        }

        return null;
    }// getResource()


    /**
     * Singleton
     */
    private VariantManager() {
        variantMap = new HashMap<>(53);
        symbolMap = new HashMap<>(17);
        inWebstart = Utils.isInWebstart();
    }// VariantManager()


    /**
     * Searches the given paths for files ending with the given extension(s).
     * Returns URLs.
     */
    private static List<URL> searchForFiles(final Collection<File> searchPaths,
                                            final Collection<String> extensions) {

        // internal error if list == null; means that
        // searchPaths[] is not a directory!
        return searchPaths.stream().map(File::listFiles)
                .filter(list -> list != null).flatMap(Arrays::stream)
                .filter(File::isFile)
                .filter(aList -> checkFileName(aList.getPath(), extensions))
                .map(aList -> {
                    try {
                        return aList.toURI().toURL();
                    } catch (final MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                        // do nothing; we just won't add it
                    }
                }).collect(Collectors.toList());
    }// searchForFiles()


    /**
     * Returns the URLClassLoader for a given URL, or creates a new one....
     */

    private URLClassLoader getClassLoader(final URL packageURL) {
        // WARNING: this method is not (itself) threadsafe
        if (packageURL == null) {
            throw new IllegalArgumentException();
        }
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
     * Get the webstart plugin name
     */
    private static String getWSPluginName(final URL url) {
        final String s = url.toString();
        final int idxExclam = s.indexOf('!');
        if (idxExclam >= 0) {
            return s.substring(s.lastIndexOf("/", idxExclam) + 1, idxExclam);
        } else {
            return s;
        }
    }// getWSPluginName()

    /**
     * Checks if the fileName ends with an allowed extension; if so, returns true.
     */
    private static boolean checkFileName(final String fileName,
                                         final Collection<String> extensions) {
        return extensions.stream().anyMatch(fileName::endsWith);
    }// checkFileName()


    /**
     * Get a resource for a variant. This uses the variantName to
     * deconflict, if multiple resources exist with the same name.
     * <p>
     * Conflict occur when plugins are loaded under the same ClassLoader,
     * because variant plugin namespace is not unique.
     * <p>
     * This primarily applies to Webstart resources
     */
    private URL getWSResource(final MapRecObj mro, final URI uri) {
        assert inWebstart;
        if (uri == null || mro == null) {
            return null;
        }

        try {
// deconflict. Note that this is not, and cannot be, foolproof;
// due to name-mangling by webstart. For example, if two plugins
// called "test" and "Supertest" exist, test may find the data
// file within Supertest because indexOf(test, SuperTest) >= 0
//
// however, if we can get the mangled name and set it as the
// 'pluginName', we can be foolproof.
//
            return Collections.list(getClass().getClassLoader()
                    .getResources(uri.toString())).stream().filter(url -> {
                final String lcPath = url.getPath();
                final String search = mro.getPluginName() + "!";
                return lcPath.contains(search);
            }).findFirst().orElse(null);
        } catch (final IOException ignored) {
            return null;
        }
    }// getWSResource()


    /**
     * Get a resource for a variant. This uses the variantName to
     * deconflict, if multiple resources exist with the same name.
     * <p>
     * Conflict occur when plugins are loaded under the same ClassLoader,
     * because variant plugin namespace is not unique.
     * <p>
     * This primarily applies to Webstart resources
     */
    private URL getWSResource(final URL packURL, final URI uri) {
        /*
            NOTE: this method is used by getResource(URL, URI), which is
			chiefly used by VariantManager and associated parsers; a VariantRecord
			has not yet been created. So we cannot use that; the internal
			logic here is slightly different.
		*/
        assert inWebstart;

// deconflict. Note that this is not, and cannot be, foolproof;
// due to name-mangling by webstart. For example, if two plugins
// called "test" and "Supertest" exist, test may find the data
// file within Supertest because indexOf(test, SuperTest) >= 0
//
// however, if we can get the mangled name and set it as the
// 'pluginName', we can be foolproof.
        try {
            return Collections.list(getClass().getClassLoader()
                    .getResources(uri.toString())).stream().filter(url -> {
                final String lcPath = url.getPath();
                return lcPath.contains(getWSPluginName(packURL));
            }).findAny().orElse(null);
        } catch (final IOException ignored) {
            return null;
        }
    }// getWSResource()


    /**
     * Adds a Variant. If the variant already exists with the same
     * name, checks the version. If the same version already exists,
     * an exception is thrown. If not, the new version is also added.
     * If we are in Web Start, however, no exception is thrown.
     * <p>
     * All names and aliases are mapped to the MapRec, not the VRec.
     * When mapping an alias, if it corresponds to a DIFFERENT
     * MapRec, an exception is thrown (this represents a non-unique
     * alias).
     * <p>
     * NOTE: names and aliases are always mapped in all lower case.
     */
    private void addVariant(final Variant variant, final String pluginName,
                            final URL pluginURL) {
        if (variant == null || pluginName == null || pluginURL == null) {
            throw new IllegalArgumentException("null argument(s).");
        }

        final VRec vRec = new VRec(pluginURL, pluginName, variant);

        final String name = variant.getName().toLowerCase();

        // see if we are mapped to a MapRec already.
        final MapRec<VRec> mapVRec = variantMap
                .computeIfAbsent(name, key -> new MapRec<>());
        // we are mapped. See if this version has been added.
        // If not, we'll add it.
        mapVRec.add(vRec);

        // map the aliases and/or check that aliases refer to the
        // same MapRec (this prevents two different Variants with the same
        // alias from causing a subtle error)
        //
        Arrays.stream(variant.getAliases())
                .filter(alias -> alias != null && !alias.isEmpty())
                .map(String::toLowerCase).forEach(alias -> {
            final MapRec<VRec> testMapVRec = variantMap
                    .computeIfAbsent(alias, key -> mapVRec);
            if (!Objects.equals(testMapVRec, mapVRec)) {
                // ERROR! incorrect alias map
                throw new IllegalArgumentException(String.format(
                        "Two variants have a conflicting (non-unique) alias.\n" +
                                "VRec 1: %s\n" +
                                "VRec 2: %s\n" +
                                "(must check all variants with this name)\n",
                        mapVRec.toString(), testMapVRec.toString()));
            }
        });
    }// addVariant()

    /**
     * Adds a SymbolPack. If the SymbolPack already exists with the same
     * name, checks the version. If the same version already exists,
     * an exception is thrown. If not, the new version is also added.
     * <p>
     * SymbolPacks do not support aliases.
     * <p>
     * Names are always mapped in all lower case.
     */
    private void addSymbolPack(final SymbolPack sp, final String pluginName,
                               final URL pluginURL) {
        if (sp == null || pluginName == null || pluginURL == null) {
            throw new IllegalArgumentException("Null argument(s)");
        }
        final SPRec spRec = new SPRec(pluginURL, pluginName, sp);
        final String spName = sp.getName().toLowerCase();
        // see if we are mapped to a MapRec already.
        final MapRec<SPRec> mapSPRec = symbolMap
                .computeIfAbsent(spName, sn -> new MapRec<>());
        // we are mapped. See if this version has been added.
        mapSPRec.add(spRec);
    }// addSymbolPack()


    /**
     * Gets the VRec associated with a Variant (via name and version)
     */
    private VRec getVRec(final Variant v) {
        final MapRec<VRec> mapRec = variantMap.get(v.getName().toLowerCase());
        return mapRec.get(v.getVersion());
    }// getVRec()


    /**
     * Gets the SPRec associated with a SymbolPack (via name and version)
     */
    private SPRec getSPRec(final SymbolPack sp) {
        final MapRec<SPRec> mapRec = symbolMap.get(sp.getName().toLowerCase());
        return mapRec.get(sp.getVersion());
    }// getSPRec()

    /**
     * The value which is stored within the name mapping
     */
    private final class MapRec<T extends MapRecObj> {
        private final List<T> list = new ArrayList<>(2);

        // this constructor prevents us from having an empty list.
        MapRec(final T obj) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }
            list.add(obj);
        }

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
            if (list.stream().noneMatch(
                    aList -> (aList.getVersion() == obj.getVersion()))) {
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
        public double[] getVersions() {
            return list.stream().mapToDouble(T::getVersion).toArray();
        }// getVersions()

        /**
         * Get the desired version. Supports version constants.
         * Returns null if version not found (shouldn't occur if
         * version constants used, and at least one element exists)
         *
         * @param version
         */
        public T get(final double version) {
            checkVersionConstant(version);

            final int size = list.size();

            // typical-case
            if (size == 1 && (version == VERSION_OLDEST || version == VERSION_NEWEST)) {
                return list.get(0);
            }

            T selected = null;
            for (final T aList : list) {
                selected = selected == null ? aList : selected;

                if (version == VERSION_OLDEST && aList.getVersion() < selected
                        .getVersion() || version == VERSION_NEWEST && aList
                        .getVersion() > selected.getVersion()) {
                    selected = aList;
                } else if (aList.getVersion() == version) {
                    return aList;
                }
            }

            return selected;
        }// get()

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

        public abstract double getVersion();

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
        public double getVersion() {
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
        public double getVersion() {
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
