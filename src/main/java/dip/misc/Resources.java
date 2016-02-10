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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// reference http://www.codeproject.com/Articles/512501/Recursive-Resource-Gathering-in-Java

public enum Resources {
    ;

    private static Set<URL> iterateFileSystem(final Path r,
                                              final Predicate<URL> f) {
        try (final Stream<Path> list = Files.list(r)) {
            return list.flatMap(path -> {
                if (Files.isDirectory(path)) {
                    return iterateFileSystem(path, f).stream();
                }
                if (Files.isRegularFile(path)) {
                    try {
                        return Stream.of(path.toUri().toURL()).filter(f::test);
                    } catch (MalformedURLException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                return Stream.empty();
            }).collect(Collectors.toSet());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<URL> iterateJarFile(final Path path,
                                           final Predicate<URL> f) {
        try (final JarFile jFile = new JarFile(path.toFile())) {
            return jFile.stream().filter(j -> !j.isDirectory()).map(j -> {
                try {
                    return new URL("jar", "",
                            path.toUri() + "!/" + j.getName());
                } catch (MalformedURLException e) {
                    throw new UncheckedIOException(e);
                }
            }).filter(f::test).collect(Collectors.toSet());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<URL> iterateEntry(final Path path,
                                         final Predicate<URL> f) {
        if (Files.isDirectory(path)) {
            return iterateFileSystem(path, f);
        }
        if (Files.isRegularFile(path) && path.getFileName().toString()
                .toLowerCase().endsWith(".jar")) {
            return iterateJarFile(path, f);
        }
        return Collections.emptySet();
    }

    public static Set<URL> getResourceURLs() {
        return getResourceURLs(u -> true);
    }

    public static Set<URL> getResourceURLs(
            final Class<?> rootClass) throws URISyntaxException {
        return getResourceURLs(rootClass, null);
    }

    public static Set<URL> getResourceURLs(final Predicate<URL> filter) {
        final URLClassLoader ucl = (URLClassLoader) ClassLoader
                .getSystemClassLoader();
        return Arrays.stream(ucl.getURLs()).flatMap(
                url -> iterateEntry(Paths.get(url.getPath()), filter).stream())
                .collect(Collectors.toSet());
    }

    public static Set<URL> getResourceURLs(final Class<?> rootClass,
                                           final Predicate<URL> filter) throws URISyntaxException {
        final CodeSource src = rootClass.getProtectionDomain().getCodeSource();
        return iterateEntry(Paths.get(src.getLocation().toURI()), filter);
    }
}