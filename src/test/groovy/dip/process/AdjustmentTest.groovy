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

package dip.process

import dip.world.WorldFactory
import dip.world.variant.VariantManager
import spock.lang.Specification

class AdjustmentTest extends Specification {

    def "getAdjustmentInfo"() {
        def variant = new VariantManager().getVariants()[18]
        def world = WorldFactory.getInstance().createWorld(variant)
        def ts = world.getInitialTurnState();
        def ro = world.getRuleOptions();
        def pr = world.getMap().getPower("Russia")
        expect:
        def rai = Adjustment.getAdjustmentInfo(ts, ro, pr)
        rai.getUnitCount() == 4
        rai.getDislodgedUnitCount() == 0
        rai.getHomeSupplyCenterCount() == 4
        rai.getSupplyCenterCount() == 4
    }

    def "getAdjustmentMap"() {
        def variant = new VariantManager().getVariants()[18]
        def world = WorldFactory.getInstance().createWorld(variant)
        def ts = world.getInitialTurnState();
        def ro = world.getRuleOptions();
        def powers = world.map.getPowers()
        def pr = world.getMap().getPower("Russia")
        def pe = world.getMap().getPower("England")
        expect:
        def aim = Adjustment.getAdjustmentInfo(ts, ro, powers)
        aim.get(pr).getUnitCount() == 4
        aim.get(pr).getDislodgedUnitCount() == 0
        aim.get(pr).getHomeSupplyCenterCount() == 4
        aim.get(pr).getSupplyCenterCount() == 4
        aim.get(pe).getUnitCount() == 3
        aim.get(pe).getDislodgedUnitCount() == 0
        aim.get(pe).getHomeSupplyCenterCount() == 3
        aim.get(pe).getSupplyCenterCount() == 3
    }
}
