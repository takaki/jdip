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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A SymbolPack
 */
public class SymbolPack implements Comparable<SymbolPack> {
    private String name;
    private float version;
    private String description = "";
    private URI thumbURI;
    private URI svgURI;
    private List<Symbol> symbols;
    private List<CSSStyle> cssStyles = Collections.emptyList();

    /**
     * The name of the SymbolPack.
     */
    public String getName() {
        return name;
    }

    /**
     * Version of this SymbolPack
     */
    public float getVersion() {
        return version;
    }

    /**
     * The description of the SymbolPack.
     */
    public String getDescription() {
        return description;
    }


    /**
     * Set the SymbolPack name.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Set the SymbolPack of this variant
     */
    public void setVersion(final float value) {
        version = value;
    }

    /**
     * Set the SymbolPack description
     */
    public void setDescription(final String value) {
        description = value;
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
     * Set the URI for the thumbnail image
     */
    public void setThumbnailURI(final String value) {
        thumbURI = makeURI(value);
    }

    /**
     * Set the URI for the Symbol SVG data
     */
    public void setSVGURI(final String value) {
        svgURI = makeURI(value);
    }


    /**
     * Get the Symbols
     */
    public Symbol[] getSymbols() {
        return symbols.toArray(new Symbol[symbols.size()]);
    }

    /**
     * Get the CSS Style data (if any)
     */
    public CSSStyle[] getCSSStyles() {
        return cssStyles.toArray(new CSSStyle[cssStyles.size()]);
    }

    /**
     * Do we have any CSS data?
     */
    public boolean hasCSSStyles() {
        return cssStyles.size() > 0;
    }

    /**
     * Set the CSS Style data
     */
    public void setCSSStyles(final CSSStyle[] styles) {
        if (styles == null) {
            throw new IllegalArgumentException();
        }

        cssStyles = Arrays.asList(styles);
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
    public void setSymbols(final List list) {
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
        return getName().compareTo(o.getName());
    }// compareTo()

    /**
     * Make a URI from a String
     */
    private URI makeURI(final String uri) {
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
        private final String name;
        private final String style;

        /**
         * Create a CSS style
         */
        public CSSStyle(final String name, final String style) {
            if (name == null || style == null) {
                throw new IllegalArgumentException();
            }

            this.name = name;
            this.style = style;
        }// CSSStyle()

        /**
         * Get the CSS Style name. This will have the '.' in front of it.
         */
        public String getName() {
            return name;
        }

        /**
         * Get the CSS Style value.
         */
        public String getStyle() {
            return style;
        }
    }// nested class CSSStyle

}// class SymbolPack
