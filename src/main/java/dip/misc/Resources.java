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

// reference http://www.codeproject.com/Articles/512501/Recursive-Resource-Gathering-in-Java

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Resources {
    FileSystems;

    private static Collection<URL> iterateFileSystem(final Path r,
                                                     final Predicate<URL> f) {
        final Collection<URL> us = new HashSet<>();
        try {
            try (final Stream<Path> list = Files.list(r)) {
                us.addAll(list.filter(path -> Files.isDirectory(path))
                        .flatMap(path -> iterateFileSystem(path, f).stream())
                        .collect(Collectors.toList()));
            }
            try (final Stream<Path> list = Files.list(r)) {
                us.addAll(list.filter(path -> Files.isRegularFile(path))
                        .map(path -> {
                            try {
                                return path.toUri().toURL();
                            } catch (MalformedURLException e) {
                                throw new UncheckedIOException(e);
                            }
                        }).filter(f::test).collect(Collectors.toList()));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return us;
    }

    private static Collection<URL> iterateJarFile(final File file,
                                                  final Predicate<URL> f) throws IOException {
        final Collection<URL> us = new HashSet<>();
        try (final JarFile jFile = new JarFile(file)) {
            us.addAll(jFile.stream().filter(j -> !j.isDirectory()).map(j -> {
                try {
                    return new URL("jar", "",
                            file.toURI() + "!/" + j.getName());
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
            }).filter(f::test).collect(Collectors.toList()));
        }
        return us;
    }

    private static void iterateEntry(final File p, final Predicate<URL> f,
                                     final Set<URL> s) throws IOException {
        if (p.isDirectory()) {
            s.addAll(iterateFileSystem(p.toPath(), f));
        } else if (p.isFile() && p.getName().toLowerCase().endsWith(".jar")) {
            s.addAll(iterateJarFile(p, f));
        }
    }

    public static Set<URL> getResourceURLs() {
        return getResourceURLs(u -> true);
    }

    public static Set<URL> getResourceURLs(
            final Class<?> rootClass) throws IOException, URISyntaxException {
        return getResourceURLs(rootClass, null);
    }

    public static Set<URL> getResourceURLs(final Predicate<URL> filter) {
        final Set<URL> collectedURLs = new HashSet<>();
        final URLClassLoader ucl = (URLClassLoader) ClassLoader
                .getSystemClassLoader();
        Arrays.stream(ucl.getURLs()).forEach(url -> {
            try {
                iterateEntry(new File(url.toURI()), filter, collectedURLs);
            } catch (URISyntaxException | IOException e) {
                throw new IllegalArgumentException(e);
            }
        });
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