// Copyright (C) 2016 TANIGUCHI Takaki
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//


package dip.misc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Resources {
    private static void collectURL(final Predicate<URL> f, final Set<URL> s,
                                   final URL u) {
        if (f == null || f.test(u)) {
            s.add(u);
        }
    }

    private static void iterateFileSystem(final File r, final Predicate<URL> f,
                                          final Set<URL> s) throws IOException {
        final File[] files = r.listFiles();
        for (final File file : files) {
            if (file.isDirectory()) {
                iterateFileSystem(file, f, s);
            } else if (file.isFile()) {
                collectURL(f, s, file.toURI().toURL());
            }
        }
    }

    private static void iterateJarFile(final File file, final Predicate<URL> f,
                                       final Set<URL> s) throws IOException {
        final JarFile jFile = new JarFile(file);
        for (final Enumeration<JarEntry> je = jFile.entries(); je
                .hasMoreElements(); ) {
            final JarEntry j = je.nextElement();
            if (!j.isDirectory()) {
                collectURL(f, s,
                        new URL("jar", "", file.toURI() + "!/" + j.getName()));
            }
        }
    }

    private static void iterateEntry(final File p, final Predicate<URL> f,
                                     final Set<URL> s) throws IOException {
        if (p.isDirectory()) {
            iterateFileSystem(p, f, s);
        } else if (p.isFile() && p.getName().toLowerCase().endsWith(".jar")) {
            iterateJarFile(p, f, s);
        }
    }

    public static Set<URL> getResourceURLs() throws IOException, URISyntaxException {
        return getResourceURLs((Predicate<URL>) null);
    }

    public static Set<URL> getResourceURLs(
            final Class rootClass) throws IOException, URISyntaxException {
        return getResourceURLs(rootClass, null);
    }

    public static Set<URL> getResourceURLs(
            final Predicate<URL> filter) throws IOException, URISyntaxException {
        final Set<URL> collectedURLs = new HashSet<>();
        final URLClassLoader ucl = (URLClassLoader) ClassLoader
                .getSystemClassLoader();
        for (final URL url : ucl.getURLs()) {
            iterateEntry(new File(url.toURI()), filter, collectedURLs);
        }
        return collectedURLs;
    }

    public static Set<URL> getResourceURLs(final Class rootClass,
                                           final Predicate<URL> filter) throws IOException, URISyntaxException {
        final Set<URL> collectedURLs = new HashSet<>();
        final CodeSource src = rootClass.getProtectionDomain().getCodeSource();
        iterateEntry(new File(src.getLocation().toURI()), filter,
                collectedURLs);
        return collectedURLs;
    }
}