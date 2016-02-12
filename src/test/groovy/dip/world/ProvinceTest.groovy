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

import spock.lang.Specification

class ProvinceTest extends Specification {
    def mos = new Province("Moscow", ["mos"], 0, false)
    def aaa = new Province("AAA000", ["aaa"], 0, false)

    def bbb = new Province("BBB000", ["bbb"], 0, false)
    def ccc = new Province("CCC000", ["ccc"], 0, false)
    def ddd = new Province("DDD000", ["ddd"], 0, false)
    def locb = new Location(bbb, Coast.LAND)
    def locc = new Location(ccc, Coast.LAND)
    def locd = new Location(ddd, Coast.LAND)

    def setup() {
        def locs = [locb, locc] as Location[]
        def adj = aaa.getAdjacency()
        adj.setLocations(Coast.LAND, locs)
    }

    def "test instance"() {
        expect:
        mos.getIndex() == 0
        mos.getAdjacentLocations(Coast.EAST).length == 0
        mos.getFullName() == "Moscow"
        mos.getShortName() == "mos"
        mos.getShortNames()[0] == "mos"
    }

    def "illegal construct parameters throw exception"() {
        when:
        new Province(fullname, shortnames, index, false)

        then:
        thrown(IllegalArgumentException)

        where:
        fullname | shortnames | index
        null     | ["mos"]    | 0
        "Moscow" | null       | 0
        "Moscow" | ["mos"]    | -1
        "Moscow" | []         | 0

    }

//    def "canTransit"() {
//        expect:
//        aaa.canTransit(locb, Unit.Type.FLEET, Phase.parse("S1900M"), Move.class)
//        aaa.canTransit(locb, Unit.Type.ARMY, Phase.parse("S1900M"), Move.class)
//        aaa.canTransit(locc, Unit.Type.FLEET, Phase.parse("S1900M"), Move.class)
//        aaa.canTransit(locc, Unit.Type.ARMY, Phase.parse("S1900M"), Move.class)
//        aaa.canTransit(locd, Unit.Type.ARMY, Phase.parse("S1900M"), Move.class)
//
//    }
}
