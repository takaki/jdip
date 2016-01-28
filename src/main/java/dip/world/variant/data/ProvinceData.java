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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**




 */
public class ProvinceData {
    private String fullName;
    private List<String> shortNames;
    private List<String> adj_provinces;
    private List<String> adj_types;
    private boolean isConvoyableCoast;
    private List<String> borders;

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
     * Set full name of province.
     */
    public void setFullName(final String value) {
        fullName = value;
    }

    /**
     * Set all adjacent province names.
     */
    public void setAdjacentProvinceNames(final String[] values) {
        adj_provinces = Arrays.asList(values);
    }

    /**
     * Set all adjacent province types.
     */
    public void setAdjacentProvinceTypes(final String[] values) {
        adj_types = Arrays.asList(values);
    }

    /**
     * Set all short (abbreviated) names, from a List.
     */
    public void setShortNames(final List list) {
        shortNames = new ArrayList<>(list);
    }// setShortNames()

    /**
     * Sets whether this Province is a convoyable coastal province.
     */
    public void setConvoyableCoast(final boolean value) {
        isConvoyableCoast = value;
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

