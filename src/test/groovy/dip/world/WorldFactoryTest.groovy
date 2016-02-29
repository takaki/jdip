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
import dip.world.variant.VariantManager
import spock.lang.Specification

class WorldFactoryTest extends Specification {
    def "getInstance"() {
        expect:
        WorldFactory.getInstance() instanceof WorldFactory
        WorldFactory.getInstance() == WorldFactory.getInstance()
    }

    def "check standard map"() {
        when:
        def vm = new VariantManager()

        then:
        vm.getVariants().size() == 43

        when:
        def variant = new VariantManager().getVariants()[18]
        def world = WorldFactory.getInstance().createWorld(variant)
        world.getPhaseSet()

        def map = world.getMap()
        then:
        map.getProvinces().size() == 75

        map.getClosestPower("England").get().toString() == "England"
        map.getClosestPower("Engband").get().toString() == "England"
        map.getPowerMatching("Englang").getName() == "England"
        map.getPowerMatching("Engbang").getName() == "England"

        map.getProvinceMatching("Mosccc").getFullName() == "Moscow"
        map.getProvinceMatching("Moscaw").getFullName() == "Moscow"
        map.getProvinceMatching("Xyz") == null
        map.getProvinceMatching("Xyzabc") == null

        map.getFirstPower("France: xxx-yyy").get().getName() == "France"
        map.getFirstPower("Fra: xxx-yyy").get().getName() == "France"
        map.getFirstPower("Fra xxx-yyy") == Optional.empty()
        map.getFirstPower("xxx-yyy") == Optional.empty()

        map.getFirstPowerToken(new StringBuffer("France: xxx-yyy")) == "France"
        map.getFirstPowerToken(new StringBuffer("Fra: xxx-yyy")) == "Fra"
        map.getFirstPowerToken(new StringBuffer("Fra xxx-yyy")) == null
        map.getFirstPowerToken(new StringBuffer("xxx-yyy")) == null


        map.getProvincesMatchingClosest("Moscow").size() == 1
        map.getProvincesMatchingClosest("Moscaw").size() == 1
        map.getProvincesMatchingClosest("Xyz").size() == 0
        map.getProvincesMatchingClosest("abc").size() == 0
        map.getProvincesMatchingClosest("def").size() == 0
        map.getProvincesMatchingClosest("Xyzabc").size() == 32
        map.getProvincesMatchingClosest("abcdef").size() == 9
        map.getProvincesMatchingClosest("defghi").size() == 4

        map.parseLocation("stp/nc").getCoast() == Coast.NORTH

        def mossb = new StringBuffer("moscow")
        map.replaceProvinceNames(mossb)
        mossb.toString() == "moscow"
        def bla0sb = new StringBuffer("black sea")
        map.replaceProvinceNames(bla0sb)
        bla0sb.toString() == "bla"
        def blasb = new StringBuffer("Black Sea")
        map.replaceProvinceNames(blasb)
        blasb.toString() == "Black Sea"

        def frasb = new StringBuffer("france france england italy")
        map.filterPowerNames(frasb)
        frasb.toString() == "france   "
        def fra2sb = new StringBuffer("france: france england italy")
        map.filterPowerNames(fra2sb)
        fra2sb.toString() == "france:   "
        def fra3sb = new StringBuffer("france:france england italy")
        map.filterPowerNames(fra3sb)
        fra3sb.toString() == "france:france  "

        when:
        def mos = map.getProvince("mos")
        def stp = map.getProvince("stp")
        def ank = map.getProvince("ank")
        then:
        mos.getFullName() == "Moscow"
        mos.isAdjacent(Coast.NONE, stp)
        !mos.isAdjacent(Coast.NONE, ank)
        !stp.isAdjacent(Coast.SINGLE, mos)
        mos.getAllAdjacent().size() == 5
        stp.getAllAdjacent().size() == 9
        stp.getAdjacentLocations(Coast.SOUTH).size() == 3
        mos.isTouching(stp)
        !mos.isTouching(ank)
        mos.isLandLocked()
        !mos.isCoastal()
        stp.isCoastal()
        stp.getValidDirectionalCoasts() == [Coast.NORTH, Coast.SOUTH] as Coast[]
        mos.getValidDirectionalCoasts() == [] as Coast[]
        !mos.isCoastValid(Coast.NORTH)
        stp.isCoastValid(Coast.NORTH)
        !stp.isCoastValid(Coast.EAST)
        mos.getBaseMoveModifier() == 0

        mos.canTransit(new Location(stp, Coast.SOUTH), Unit.Type.ARMY, Phase.parse("S1900M"), Move.class)
        mos.canTransit(new Location(stp, Coast.SOUTH), Unit.Type.FLEET, Phase.parse("S1900M"), Move.class)

        stp.canTransit(new Location(mos, Coast.LAND), Unit.Type.ARMY, Phase.parse("S1900M"), Move.class)
        stp.canTransit(new Location(mos, Coast.LAND), Unit.Type.FLEET, Phase.parse("S1900M"), Move.class)
    }
}
