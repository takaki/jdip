//
//  @(#)Power.java		4/2002
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
package dip.world;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Power represents player in the game.
 */
@XmlRootElement(name = "POWER")
public class Power implements Comparable<Power>, Serializable {
    /**
     * An empty array of Power objects.
     */
    public static final Power[] EMPTY_ARRAY = new Power[0];

    // constants for name array; always stored in this order.
    private static final int FULL_NAME = 0;    // required


    // immutable fields
    @XmlAttribute(required = true)
    private String name;
    @XmlAttribute(name = "active", required = true)
    private boolean active;
    @XmlAttribute(required = true)
    private String adjective;
    @XmlAttribute
    private List<String> altnames = new ArrayList<>();

    public Power() {
    }

    /**
     * Create a new Power.
     * <p>
     * The first name in the names array (index position 0) must be the full display
     * name of the power. Names beyond index 0 are optional, and are "alternate" names
     * (e.g., "Britain" instead of "England").
     * <p>
     * All fields are required and <b>must</b> be non-null/non-zero-length;
     * Names (not adjectives) should not contain whitespace, and must not
     * be empty ("") or null.
     * <p>
     * This should generally not be used, except for when a game is first created. Note that
     * Power uses instance equality, so two Power() objects created with the same arguments
     * will NOT be the same.
     */
    public Power(final String[] names, final String adjective,
                 final boolean active) {
        if (names == null || adjective == null) {
            throw new IllegalArgumentException("null argument(s)");
        }

        if (names.length == 0) {
            throw new IllegalArgumentException("no names");
        }

        if (adjective.isEmpty()) {
            throw new IllegalArgumentException("empty adjective");
        }

        name = names[0];
        altnames = Arrays.asList(Arrays.copyOfRange(names, 1, names.length));
        this.adjective = adjective;
        this.active = active;
    }// Power()


    /**
     * Returns the name of the power. Never returns null.
     */
    public String getName() {
        return name;
    }// getName()

    /**
     * Returns the adjective of the power (e.g., power France, adjective is French)
     */
    public String getAdjective() {
        return adjective;
    }// getAdjective()

    /**
     * Get all names. There is always at least one. Does not include adjectives.
     */
    public String[] getNames() {
        final List<String> names = new ArrayList<>();
        names.add(name);
        names.addAll(altnames);
        return names.toArray(new String[names.size()]);
    }// getAllNames()


    /**
     * Determines if this power is active. Only active powers can order units.
     */
    public boolean isActive() {
        return active;
    }


    /**
     * Implementation of Object.hashCode()
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }// hashCode()

	
	/* 	
        Implementation of Object.equals()
	 	NOTE: we just use default referential equality, since these objects are immutable!
	*/

    /**
     * Implementation of Object.toString()
     */
    @Override
    public String toString() {
        return getName();
    }// toString()


    /**
     * Implementation of Comparable interface
     */
    @Override
    public int compareTo(final Power obj) {
        return name.compareTo(obj.name);
    }// compareTo()


}// class Power
