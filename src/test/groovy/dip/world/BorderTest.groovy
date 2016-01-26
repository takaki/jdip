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

package dip.world

import dip.order.Move
import spock.lang.Specification
import spock.lang.Unroll

class BorderTest extends Specification {
    def loc0 = new Location(new Province("Moscow", ["Mos"] as String[], 0, false), Coast.NONE)
    def loc1 = new Location(new Province("Ukraine", ["Ukr"] as String[], 1, false), Coast.NONE)
    def border = new Border("id", "description", "Army  ", [loc0] as Location[], "dip.order.Move", "1", "Spring   Fall", "Movement", "1900, 2000")

    def "test canTransit"() {
        def phase = new Phase(Phase.SeasonType.SPRING, 1900, Phase.PhaseType.MOVEMENT)
        expect:
        border.canTransit(loc0, Unit.Type.FLEET, phase, Move.class)
        border.canTransit(loc1, Unit.Type.ARMY, phase, Move.class)
        border.canTransit(loc1, Unit.Type.FLEET, phase, Move.class)
        !border.canTransit(loc0, Unit.Type.ARMY, phase, Move.class)
        border.canTransit(loc0, Unit.Type.ARMY, new Phase(Phase.SeasonType.SPRING, 1899, Phase.PhaseType.MOVEMENT), Move.class)
    }

    def "test getBaseMoveModifier"() {
        expect:
        border.getBaseMoveModifier(loc0) == 1
        border.getBaseMoveModifier(loc1) == 0
    }

    def "test getDescription"() {
        expect:
        border.getDescription() == "description"
    }

    def "null from"() {
        def border = new Border("id", "description", "Army", null, "dip.order.Move", "1", "Spring Fall", "Movement", "1900,2000")
        def phase = new Phase(Phase.SeasonType.SPRING, 1900, Phase.PhaseType.MOVEMENT)
        expect:
        border.getBaseMoveModifier(loc0) == 1
        border.canTransit(loc0, Unit.Type.FLEET, phase, Move.class)
        !border.canTransit(loc1, Unit.Type.ARMY, phase, Move.class)
        !border.canTransit(loc0, Unit.Type.ARMY, phase, Move.class)
    }

    def "throw Exception"() {
        when:
        def border = new Border(null, "description", "Army  ", [loc0] as Location[], "dip.order.Move", "1", "Spring   Fall", "Movement", "1900, 2000")
        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "illegal '#id' throw InvalidBorderException"() {
        def loc0 = new Location(new Province("Moscow", ["Mos"] as String[], 0, false), Coast.NONE)
        when:
        new Border(id, description, units, from, orders, bMM, season, phase, year)

        then:
        thrown(InvalidBorderException)

        where:
        // "id" | "description" | "Army  " | [loc0] as Location[] | "dip.order.Move" | "1"              | "Spring   Fall" | "Movement" | "1900,2000"
        id            | description   | units    | from                 | orders           | bMM | season          | phase      | year
        "phase"       | "description" | "Army  " | [loc0] as Location[] | "dip.order.Move" | "1" | "Spring   Fall" | "phase"    | "1900,2000"
        "year"        | "description" | "Army  " | [loc0] as Location[] | "dip.order.Move" | "1" | "Spring   Fall" | "Movement" | "1900 1950 2000"
        "odd year"    | "description" | "Army  " | [loc0] as Location[] | "dip.order.Move" | "1" | "Spring   Fall" | "Movement" | "odd  1900"
        "even year"   | "description" | "Army  " | [loc0] as Location[] | "dip.order.Move" | "1" | "Spring   Fall" | "Movement" | "even 1900"
        "splitter"    | "description" | "Army  " | [loc0] as Location[] | "dip.order.Move" | "1" | "Spring   Fall" | "Movement" | "1900|2000"
        "season"      | "description" | "Army  " | [loc0] as Location[] | "dip.order.Move" | "1" | "season"        | "Movement" | "1900,2000"
        "year"        | "description" | "Army  " | [loc0] as Location[] | "dip.order.Move" | "1" | "Spring   Fall" | "Movement" | "11900,2000"
        "units"       | "description" | "Hoge  " | [loc0] as Location[] | "dip.order.Move" | "1" | "Spring   Fall" | "Movement" | "1900,2000"
        "orders"      | "description" | "Army  " | [loc0] as Location[] | "dip.order.Hoge" | "1" | "Spring   Fall" | "Movement" | "1900,2000"
//        "empty order" | "description" | "Army  " | [loc0] as Location[] | ""               | "1" | "Spring   Fall" | "Movement" | "1900,2000"
    }
}
