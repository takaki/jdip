//
//  @(#)Province.java		4/2002
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

import dip.order.Order;
import dip.world.Unit.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * A Province represents a region on the map.
 * <p>
 * Provinces may be sea, coastal, or landlocked. Their connectivity and type (sea, coastal,
 * or landlocked) is determined by the Adjacency data.
 * <p>
 * The types of provinces are:
 * <ul>
 * <li>Landlocked; adjacent only by land (e.g., warsaw)
 * <li>Sea; adjacent only by water (e.g., black sea)
 * <li>Single-Coast; (e.g., portugal)
 * <li>Multi-Coastal;	(e.g., spain)
 * </ul>
 * <p>
 * The adjacency information is similar to that used by the Ken Lowe Judge software.
 * <p>
 * It is illegal to have a Province without any Coasts, to combine single-coast and
 * multi-coast, or to have only multiple coasts without a land coast.
 * <p>
 * <pre>
 * L S N S W E 	(land / single / north / south / west / east)
 * ===========
 * x - - - - - 	landlocked
 * - x - - - - 	seaspace ("sealocked")
 * x x - - - - 	coastal land space (only 1 coast)
 * - - ? ? ? ? 	INVALID
 * - x ? ? ? ? 	INVALID
 * x x ? ? ? ? 	INVALID
 * x - ? ? ? ? 	see below
 *
 * other valid:
 * land + (!single) + (any combination of north/south/west/east)
 * </pre>
 * <p>
 * If Supply Centers become more complex in the future, they may
 * be handled as a separate object within the Province.
 */
public class Province implements Serializable, Comparable<Province> {

    // immutable persistent fields
    private final String fullName;                // fullName MUST BE UNIQUE
    private final List<String> shortNames;            // always has AT LEAST one, and all are globally unique
    private final int index;                    // contiguous index
    private final boolean isConvoyableCoast;    // 'true' if coast is convoyable
    private final Adjacency adjacency;            // adjacency data

    // publicly immutable non-final persistent fields
    // (because of difficulties with creation)
    private boolean supplyCenter;        // true if supply center exists here.
    private List<Border> borders;            // non-zero-length if any Borders exist


    /**
     * Adjacency maintains the connectivity graph between provinces.
     */
    protected static class Adjacency implements Serializable {
        private final EnumMap<Coast, List<Location>> adjLoc;

        /**
         * Creates a new Adjacency object.
         */
        private Adjacency() {
            adjLoc = new EnumMap<>(Coast.class);
        }// Adjacency()

        /**
         * Sets which locations are adjacent to the specified coast.
         */
        protected void setLocations(final Coast coast,
                                    final List<Location> locations) {
            adjLoc.put(coast, new ArrayList<>(locations));
        }// setLocations()


        /**
         * Gets the locations which are adjacent to the coast.
         * <p>
         * If no locations are adjacent, a zero-length array is returned.
         */
        protected List<Location> getLocations(final Coast coast) {
            return Collections.unmodifiableList(
                    adjLoc.getOrDefault(coast, Collections.emptyList()));
        }// getLocations()


        /**
         * Creates a WING coast from Province coastal data. All Coasts must
         * be set for this Province already. Note that a Wing coast is equiavalent
         * to 'touching' adjacency.
         */
        protected void createWingCoasts() {
            final List<Location> locList = Coast.ALL_COASTS.stream()
                    .flatMap(coast -> getLocations(coast).stream())
                    .map(Location::getProvince).distinct()
                    .map(prov -> new Location(prov, Coast.WING))
                    .collect(Collectors.toList());
            setLocations(Coast.WING, locList);
        }// createWingCoasts()


        /**
         * Ensure that adjacency data is consistent, and that there are
         * no illegal coast combinations.
         * <p>
         * The Province argument is required to correctly validate
         * convoyable coast regions. Convoyable coasts require a single
         * or multi-coast region with a defined land coast.
         * <code>
         * <p>
         * land / single / north / south / west / east
         * <p>
         * x = true; - = false; ? = true or false
         * <p>
         * L S N S W E
         * ===========================================
         * - - ? ? ? ? 	INVALID		(a)		where at least one ? is true
         * - x ? ? ? ? 	INVALID		(b)		where at least one ? is true
         * x x ? ? ? ? 	INVALID		(c) 	where at least one ? is true
         * - - - - - - 	INVALID		(d)
         * </code>
         */
        protected boolean validate(final Province p) {
            final boolean isDirectional = Coast.ANY_DIRECTIONAL.stream()
                    .anyMatch(coast -> adjLoc.get(coast) != null);

            final boolean isLand = adjLoc.get(Coast.LAND) != null;
            final boolean isSingle = adjLoc.get(Coast.SINGLE) != null;

            // covers cases (b) and (c)
            if (isDirectional && isSingle) {
                return false;
            }

            // covers case (d)
            if (!isDirectional && !isLand && !isSingle) {
                return false;
            }

            // covers case (a)
            if (isDirectional && !isLand && !isSingle) {
                return false;
            }

            // check convoyable coasts
            return !(p
                    .isConvoyableCoast() && (!isLand || !isSingle && !isDirectional));

        }// validate()
    }// inner class Adjacency()


    /**
     * Creates a new Province object.
     * <b>Unless you are a WorldFactory (or subclass), it should (almost) never be nescessary
     * to create a Province using new. In fact, to do so will break referential equality.
     * to get a Province, use the Map.getProvince() and similar methods.</b>
     * <p>
     * These are created by a WorldFactory, or through de-serialization.
     * Null names are not allowed. At least one shortName is required.
     */
    public Province(final String fullName, final List<String> shortNames,
                    final int index, final boolean isConvoyableCoast) {
        Objects.requireNonNull(fullName);
        Objects.requireNonNull(shortNames);

        if (shortNames.size() < 1) {
            throw new IllegalArgumentException(
                    "at least one shortName required");
        }

        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }

        this.fullName = fullName;
        this.shortNames = new ArrayList<>(shortNames);
        this.index = index;
        this.isConvoyableCoast = isConvoyableCoast;
        adjacency = new Adjacency();
    }// Province()


    /**
     * Sets the Border data for this province.
     */
    protected void setBorders(final List<Border> value) {
        borders = new ArrayList<>(value);
    }// setBorders()


    /**
     * Sets if this province has a supply center.
     */
    protected void setSupplyCenter(final boolean value) {
        supplyCenter = value;
    }// setSupplyCenter()


    /**
     * Returns the Province index; this is an int between 0 (inclusive) and
     * the total number of provinces (exclusive). It is never negative.
     */
    public final int getIndex() {
        return index;
    }// getIndex()


    /**
     * Gets the Adjacency data for this Province
     */
    protected final Adjacency getAdjacency() {
        return adjacency;
    }// getAdjacency()


    /**
     * Get all the Locations that are adjacent to this province.
     * Note that if you are only interested in adjacent provinces,
     * getAdjacentLocations(Coast.WING or Coast.TOUCHING) is more appropriate
     * and faster; however, all Locations returned will be of Coast.WING.
     * This method will return all truly adjacent locations, which have their
     * correct coasts. No duplicate Locations will be present in the returned
     * array. All arrays will be zero-length or higher; a null array is never
     * returned.
     */
    public List<Location> getAllAdjacent() {
        return Coast.ALL_COASTS.stream()
                .flatMap(coast -> adjacency.getLocations(coast).stream())
                .distinct().collect(Collectors.toList());
    }// getAllAdjacent()


    /**
     * Gets the Locations adjacent to this province, given the
     * specified coast.
     */
    public List<Location> getAdjacentLocations(final Coast coast) {
        return adjacency.getLocations(coast);
    }// getAdjacency()


    /**
     * Gets the full name (long name) of the Province
     */
    public final String getFullName() {
        return fullName;
    }

    /**
     * Gets the short name of the Province
     * <p> This returns the first short name if there are more than one.
     */
    public final String getShortName() {
        return shortNames.get(0);
    }

    /**
     * Gets all short names of the Province
     */
    public final List<String> getShortNames() {
        return Collections.unmodifiableList(shortNames);
    }

    /**
     * Determine if this Province contains a supply center
     */
    public boolean hasSupplyCenter() {
        return supplyCenter;
    }


    /**
     * Determines if two provinces are in any way adjacent (connected).
     * <p>
     * If two provinces are adjacent, by any coast, this will return true. This
     * implies connectivity in the broadest sense. No coast information is required
     * or needed in this or the Province that is compared. <b>because Coasts are
     * ignored, this method should generally not be used to determine adjacency for the movement
     * of units.</b>
     * <p>
     * This now uses the "Wing" ("Touching") Coast which is equivalent.
     */
    public boolean isTouching(final Province province) {
        return adjacency.getLocations(Coast.TOUCHING).stream()
                .anyMatch(location -> location.isProvinceEqual(province));
    }// isTouching()


    /**
     * Checks connectivity between this and another province
     * <p>
     * This method only determines if the current Province with the specified
     * coast is connected to the destination Province.
     */
    public boolean isAdjacent(final Coast sourceCoast, final Province dest) {
        return adjacency.getLocations(sourceCoast).stream()
                .anyMatch(location -> location.getProvince().equals(dest));
    }// isAdjacent()


    /**
     * Checks connectivity between this and another province
     * <p>
     * This method only determines if the current Province with the specified
     * coast is connected to the destination Province and Coast.
     * <p>
     * This is a stricter version of isAdjacent(Coast, Province)
     */
    public boolean isAdjacent(final Coast sourceCoast, final Location dest) {
        return adjacency.getLocations(sourceCoast).stream()
                .anyMatch(location -> location.equals(dest));
    }// isAdjacent()


    /**
     * Determines if this Province is landlocked.
     */
    public boolean isLandLocked() {
        return !Coast.ANY_SEA.stream()
                .anyMatch(coast -> !adjacency.getLocations(coast).isEmpty());
    }// isLandLocked()

    // NOTE: this could be made more efficient

    /**
     * Determines if this Province is coastal (including multi-coastal).
     */
    public boolean isCoastal() {
        return !adjacency.getLocations(Coast.LAND).isEmpty() && Coast.ANY_SEA
                .stream()
                .anyMatch(coast -> !adjacency.getLocations(coast).isEmpty());

    }// isCoastal()


    /**
     * Determines if this Province is a Land province (landlocked OR coastal)
     */
    public boolean isLand() {
        return !adjacency.getLocations(Coast.LAND).isEmpty();
    }// isLand()


    /**
     * Determines if this Province is a Sea province (no land, not coastal).
     */
    public boolean isSea() {
        return adjacency.getLocations(Coast.LAND)
                .isEmpty() && !Coast.ANY_DIRECTIONAL.stream()
                .anyMatch(coast -> !adjacency.getLocations(coast).isEmpty());

    }// isSea()


    /**
     * Determines if this Province has multiple coasts (e.g., Spain).
     */
    public boolean isMultiCoastal() {
        return adjacency.getLocations(Coast.SEA)
                .isEmpty() && Coast.ANY_DIRECTIONAL.stream()
                .anyMatch(coast -> !adjacency.getLocations(coast).isEmpty());
    }// isMultiCoastal()


    /**
     * Return the coasts supported by this province.
     * If not multicoastal, returns an empty Coast array.
     */
    public List<Coast> getValidDirectionalCoasts() {
        if (adjacency.getLocations(Coast.SEA).isEmpty()) {
            return Coast.ANY_DIRECTIONAL.stream()
                    .filter(coast -> !adjacency.getLocations(coast).isEmpty())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }// getValidCoasts()


    /**
     * Determines if specified coast is allowed for this Province
     */
    public boolean isCoastValid(final Coast coast) {
        return !adjacency.getLocations(coast).isEmpty();
    }// isCoastValid()


    /**
     * Implementation of Object.hashCode()
     */
    @Override
    public int hashCode() {
        return fullName.hashCode();
    }// hashCode()


    /**
     * Checks if unit can transit from a Location to this Province.
     */
    public boolean canTransit(final Location fromLoc, final Type unit,
                              final Phase phase, final Class<?> orderClass) {
        return !getTransit(fromLoc, unit, phase, orderClass).isPresent();
    }// canTransit()

    /**
     * Convenient version of canTransit()
     */
    public boolean canTransit(final Phase phase, final Order order) {
        return canTransit(order.getSource(), order.getSourceUnitType(), phase,
                order.getClass());
    }// canTransit()


    /**
     * Checks if unit can transit from a Location to this Province. Returns the first
     * failing Border order; returns null if Transit is successfull.
     */
    public Optional<Border> getTransit(final Location fromLoc, final Type unit,
                                       final Phase phase,
                                       final Class<?> orderClass) {
        return borders != null ? borders.stream().filter(border -> !border
                .canTransit(fromLoc, unit, phase, orderClass))
                .findFirst() : Optional.empty();
    }// getTransit()

    /**
     * Convenient version of getTransit()
     */
    public Optional<Border> getTransit(final Phase phase, final Order order) {
        return getTransit(order.getSource(), order.getSourceUnitType(), phase,
                order.getClass());
    }// getTransit()


    /**
     * Looks through borders to determine if there is a baseMoveModifier.
     * that fits. Note that the first matching non-zero baseMoveModifier is returned
     * if there are more than one, which is not recommended.
     */
    public int getBaseMoveModifier(final Location fromLoc) {
        return borders != null ? borders.stream()
                .map(border -> border.getBaseMoveModifier(fromLoc))
                .filter(baseMoveMod -> (baseMoveMod != 0)).findFirst()
                .orElse(0) : 0;
    }// getBaseMoveModifier()


    /**
     * If this province is a convoyable coastal Province, this will return <code>true</code>.
     */
    public boolean isConvoyableCoast() {
        return isConvoyableCoast;
    }// isConvoyableCoast()


    /**
     * Indicates if this province is convoyable, either because it is
     * a Sea province or a convoyable coast.
     * <p>
     * No transit checking is performed.
     */
    public boolean isConvoyable() {
        return isConvoyableCoast() || isSea();
    }// isConvoyable()


    /**
     * Implementation of equals(). Note that this <b>only</b> compares
     * the Province (full) name and index for equality, since they are assumed
     * be unique. Other fields/adjacency are not compared.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Province) {
            final Province p = (Province) obj;
            return index == p.index && fullName.equals(p.fullName);
        }

        return false;
    }// equals()


    /**
     * Returns the full name of the province
     */
    @Override
    public String toString() {
        return fullName;
    }// toString();


    /**
     * Compares this province to another, by the full name, ignoring case
     */
    @Override
    public int compareTo(final Province obj) {
        return fullName.compareToIgnoreCase(obj.fullName);
    }// compareTo()

}// class Province
