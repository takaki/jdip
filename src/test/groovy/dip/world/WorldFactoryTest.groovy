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
import dip.world.variant.data.Variant
import spock.lang.Specification

import java.nio.file.Paths

class WorldFactoryTest extends Specification {
    def "getInstance"() {
        expect:
        WorldFactory.getInstance() instanceof WorldFactory
        WorldFactory.getInstance() == WorldFactory.getInstance()
    }

    def "createWorld"() {
        def variant = new Variant()
        def world = WorldFactory.getInstance().createWorld(variant)
//        expect:
//        world.getMap() != null
    }

    def "check standard map"() {
        setup:
        VariantManager.init([Paths.get(System.getProperty("user.dir"), "src/test/resources/variants").
                                     toFile()] as File[], false)
        when:
        def variant = VariantManager.getVariants()[7]
        def world = WorldFactory.getInstance().createWorld(variant)
        world.getPhaseSet()
        def mos = world.getMap().getProvince("mos")
        def stp = world.getMap().getProvince("stp")
        def ank = world.getMap().getProvince("ank")
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
