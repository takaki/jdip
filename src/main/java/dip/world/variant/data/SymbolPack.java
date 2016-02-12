//
//  @(#)SymbolPack.java		10/2003
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
//  Or from http://www.gnu.org/
//
package dip.world.variant.data;

import javafx.util.Pair;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A SymbolPack
 */
@XmlRootElement(name = "SYMBOLS")
public final class SymbolPack implements Comparable<SymbolPack> {
    @XmlAttribute(name = "name", required = true)
    private String name;
    @XmlAttribute(name = "version", required = true)
    private String version;
    @XmlAttribute(required = true)
    private URI thumbURI;
    @XmlAttribute(required = true)
    private URI svgURI;
    @XmlElement(name = "description", required = true)
    private String description = "";


    @XmlElementWrapper(name = "SCALING")
    @XmlElement(name = "SCALE")
    private List<Scale> scales = Collections.emptyList();

    private List<Symbol> symbols;
    private List<CSSStyle> cssStyles = Collections.emptyList();

    @XmlRootElement(name = "SCALE")
    public static class Scale {
        @XmlAttribute(required = true)
        private String symbolName;
        @XmlAttribute(required = true)
        private float value;

        @SuppressWarnings("unused")
        public void afterUnmarshal(final Unmarshaller unmarshaller,
                                   final Object parent) {
            if (value <= 0.0f) {
                throw new IllegalArgumentException(
                        "scale attribute value cannot be negative or zero.");
            }
        }

        public String getSymbolName() {
            return symbolName;
        }

        public float getValue() {
            return value;
        }
    }

    public Map<String, Float> getScaleMap() {
        return scales.stream().collect(
                Collectors.toMap(Scale::getSymbolName, Scale::getValue));
    }


    /**
     * The name of the SymbolPack.
     */
    public String getName() {
        return name;
    }


    /**
     * Version of this SymbolPack
     */
    public VersionNumber getVersion() {
        return VersionNumber.parse(version);
    }

    /**
     * The description of the SymbolPack.
     */
    public String getDescription() {
        return description;
    }


    /**
     * Get the URI for the thumbnail image
     */
    public URI getThumbnailURI() {
        return thumbURI;
    }

    /**
     * Get the URI for the Symbol SVG data
     */
    public URI getSVGURI() {
        return svgURI;
    }

    /**
     * Get the Symbols
     */
    public List<Symbol> getSymbols() {
        return Collections.unmodifiableList(symbols);
    }

    /**
     * Get the CSS Style data (if any)
     */
    public List<CSSStyle> getCSSStyles() {
        return Collections.unmodifiableList(cssStyles);
    }

    /**
     * Do we have any CSS data?
     */
    public boolean hasCSSStyles() {
        return !cssStyles.isEmpty();
    }

    /**
     * Set the CSS Style data
     */
    public void setCSSStyles(final List<CSSStyle> styles) {
        if (styles == null) {
            throw new IllegalArgumentException();
        }

        cssStyles = new ArrayList<>(styles);
    }// setCSSStyles()

    /**
     * Set the Symbols
     */
    public void setSymbols(final Symbol[] symbols) {
        this.symbols = Arrays.asList(symbols);
    }

    /**
     * Set the Symbols
     */
    public void setSymbols(final List<Symbol> list) {
        symbols = new ArrayList<>(list);
    }

    /**
     * Find the Symbol with the given Name (case sensitive); returns null if name not found.
     */
    public Symbol getSymbol(final String name) {
        return symbols.stream().filter(symbol -> symbol.getName().equals(name))
                .findFirst().orElse(null);
    }// getSymbol()

    /**
     * Comparison, based on Name. Only compares to other SymbolPack objects.
     */
    @Override
    public int compareTo(final SymbolPack o) {
        return name.compareTo(o.name);
    }// compareTo()

    /**
     * Make a URI from a String
     */
    private static URI makeURI(final String uri) {
        try {
            return new URI(uri);
        } catch (final URISyntaxException ignored) {
            return null;
        }
    }// makeURI()


    /**
     * SymbolPack CSS data styles.
     */
    public static class CSSStyle {
        private final Pair<String, String> pair;

        /**
         * Create a CSS style
         */
        public CSSStyle(final String name, final String style) {
            if (name == null || style == null) {
                throw new IllegalArgumentException();
            }
            pair = new Pair<>(name, style);
        }// CSSStyle()

        /**
         * Get the CSS Style name. This will have the '.' in front of it.
         */
        public String getName() {
            return pair.getKey();
        }

        /**
         * Get the CSS Style value.
         */
        public String getStyle() {
            return pair.getValue();
        }
    }// nested class CSSStyle

}// class SymbolPack
