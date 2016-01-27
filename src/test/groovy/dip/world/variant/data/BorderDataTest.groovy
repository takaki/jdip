/*
 * Copyright (C) 2016 TANIGUCHI Takaki
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package dip.world.variant.data

import spock.lang.Specification

class BorderDataTest extends Specification {
    def bd = new BorderData()

    def "getter and setter"() {
        bd.setID("id")
        bd.setDescription("description")
        bd.setUnitTypes("unittype")
        bd.setBaseMoveModifier("basemovemodifier")
        bd.setFrom("from")
        bd.setOrderTypes("ordertype")
        bd.setPhase("phase")
        bd.setSeason("season")
        bd.setYear("year")
        expect:
        bd.getBaseMoveModifier() == "basemovemodifier"
        bd.getDescription() == "description"
        bd.getFrom() == "from"
        bd.getID() == "id"
        bd.getOrderTypes() == "ordertype"
        bd.getPhase() == "phase"
        bd.getSeason() == "season"
        bd.getUnitTypes() == "unittype"
        bd.getYear() == "year"

    }
}
