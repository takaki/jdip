//
//  @(#)ProvinceData.java	1.00	7/2002
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

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**




 */
@XmlRootElement(name = "PROVINCE")
public final class ProvinceData {

//    borders IDREFS #IMPLIED

    //    shortname CDATA #REQUIRED
    @XmlAttribute(name = "shortname", required = true)
    private String shortName;
    private List<String> shortNames = new ArrayList<>();
    @XmlAttribute(name = "fullname", required = true)
    private String fullName;
    //    isConvoyableCoast (true|false) "false"
    @XmlAttribute(name = "isConvoyableCoast", required = true)
    private boolean isConvoyableCoast;
    //    borders IDREFS #IMPLIED

    private List<String> borders = Collections.emptyList();

    @XmlElement(name = "UNIQUENAME")
    private List<UniqueName> uniqueNames = new ArrayList<>();

    @XmlRootElement
    public static class UniqueName {
        @XmlAttribute
        private String name;
    }

    @XmlRootElement(name = "ADJACENCY")
    public static class Adjacency {
        @XmlAttribute
        private String type;
        @XmlAttribute
        private String refs;
    }

    @XmlElement(name = "ADJACENCY")
    private List<Adjacency> adjacencies;

    private List<String> adj_provinces;
    private List<String> adj_types;

    void afterUnmarshal(final Unmarshaller unmarshaller, final Object parent) {
        adj_provinces = adjacencies.stream().map(a -> a.refs)
                .collect(Collectors.toList());
        adj_types = adjacencies.stream().map(a -> a.type)
                .collect(Collectors.toList());
        if (shortName != null && !shortName.isEmpty()) {
            shortNames.add(shortName);
        }
        shortNames.addAll(uniqueNames.stream().map(u -> u.name)
                .collect(Collectors.toList()));
    }

    /**
     * Full name of Province (e.g., Mid-Atlantic Ocean)
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Short (abbreviated) name of Province; (e.g., "mao" or "mid-atlantic")
     */
    public String[] getShortNames() {
        return shortNames.toArray(new String[shortNames.size()]);
    }

    /**
     * Province Adjacency array.
     */
    public String[] getAdjacentProvinceNames() {
        return adj_provinces.toArray(new String[adj_provinces.size()]);
    }

    /**
     * Prvoince Adjacency type array.
     */
    public String[] getAdjacentProvinceTypes() {
        return adj_types.toArray(new String[adj_types.size()]);
    }

    /**
     * Gets whether this Province is a convoyable coastal province.
     */
    public boolean getConvoyableCoast() {
        return isConvoyableCoast;
    }

    /**
     * Sets the Border ID names for this province (if any)
     */
    public void setBorders(final List list) {
        borders = new ArrayList<>(list);
    }// setBorders()

    // TODO: right?
    @XmlAttribute(name = "borders")
    public void setBorders(final String borders) {
        this.borders = borders.isEmpty() ? Collections
                .emptyList() : new ArrayList<>(
                Arrays.asList(borders.split("[, ]+")));
    }// setBorders()

    /**
     * Gets the Border ID names for this province (if any)
     */
    public String[] getBorders() {
        return borders.toArray(new String[borders.size()]);
    }


    /**
     * For debugging only!
     */
    @Override
    public String toString() {
        return String.join("", getClass().getName(), "[", "fullName=", fullName,
                ",#shortNames=", Integer.toString(shortNames.size()),
                ",#adj_provinces=", Integer.toString(adj_provinces.size()),
                ",#adj_types=", Integer.toString(adj_types.size()),
                ",isConvoyableCoast=", Boolean.toString(isConvoyableCoast),
                ",#borders=", Integer.toString(borders.size()), "]");
    }// toString()
}// nested class ProvinceData	

