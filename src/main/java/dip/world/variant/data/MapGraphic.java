//
//  @(#)MapGraphic.java	1.00	7/2002
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
package dip.world.variant.data;

import dip.world.variant.parser.XMLVariantParser.MapDef;
import org.xml.sax.SAXException;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

@XmlRootElement
public class MapGraphic {
    @XmlAttribute(name = "default")
    private boolean isDefault;
    @XmlAttribute(name = "preferredUnitStyle")
    private String prefSPName;

    @XmlIDREF
    @XmlAttribute(name = "ref")
    private MapDef mapDef;

    private String name;
    private URI uri;
    private String desc;
    private URI thumbURI;


    /**
     * Constructs a MapGraphic object.
     * <p>
     * If the preferred Symbol Pack Name (prefSPName) is an empty string, it will
     * be converted to a null String.
     */
    public MapGraphic() {
    }

    void afterUnmarshal(final Unmarshaller unmarshaller,
                        final Object parent) throws IOException, SAXException {
        // create the MapGraphic object
        try {
            name = mapDef.getTitle();
            uri = new URI(mapDef.getMapURI());
            desc = mapDef.getDescription();
            thumbURI = new URI(mapDef.getThumbURI());
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


    public MapGraphic(final String uri, final boolean isDefault,
                      final String name, final String description,
                      final String thumbURI, final String prefSPName) {
        if (name == null) {
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.isDefault = isDefault;
        desc = description;
        this.prefSPName = prefSPName != null && prefSPName
                .isEmpty() ? null : prefSPName;

        // set URI
        Function<String, URI> setURI = u -> {
            try {
                return new URI(u);
            } catch (final URISyntaxException ignored) {
                return null;
            }
        };

        this.uri = setURI.apply(uri);
        this.thumbURI = setURI.apply(thumbURI);
    }// MapGraphic()

    /**
     * The URI for a map SVG file.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Whether this is the default graphic to use.
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * The name of this map. <p> Should not be null.
     */
    public String getName() {
        return name;
    }

    /**
     * The description of this map. <p> May return null.
     */
    public String getDescription() {
        return desc;
    }

    /**
     * The URI for the thumbnail graphic of this map.
     */
    public URI getThumbnailURI() {
        return thumbURI;
    }

    /**
     * The Preferred SymbolPack name, or null if none.
     */
    public String getPreferredSymbolPackName() {
        return prefSPName;
    }

    /**
     * For debugging only!
     */
    @Override
    public String toString() {
        return String
                .join("", getClass().getName(), "[", "uri=", uri.toString(),
                        ",isDefault=", Boolean.toString(isDefault), ",name=",
                        name, ",desc=", desc, ",thumbURI=", thumbURI.toString(),
                        "]");
    }// toString()

}// nested class MapGraphic


