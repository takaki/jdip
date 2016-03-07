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

package dip.judge.parser

import dip.world.WorldFactory
import dip.world.variant.VariantManager
import spock.lang.Specification

class AdjustmentParserTest extends Specification {

    def "test"() {
        def variant = new VariantManager().getVariants()[18]
        def world = WorldFactory.getInstance().createWorld(variant)
        world.getPhaseSet()

        def map = world.getMap()

        def input =
                """The deadline for orders will be Wed Apr 10 2002 17:54:23 -0500.
asdfjafsdkjfadslk: sdljfjldksfkjfdlsls
fadslkfkjdlsafslkj

Ownership of supply centers:

Austria:   Greece, Serbia.
England:   Edinburgh, Liverpool, London.
France:    Belgium, Brest, Marseilles, Paris,
           Portugal, Spain.
Germany:   Berlin, Denmark, Holland, Kiel, Munich, Norway.
Italy:     Naples, Rome, Trieste, Tunis, Venice, St.
           Petersburg.
Russia:    Moscow, Norway, Rumania, Sevastopol,
	    St. Petersburg, Vienna, Warsaw.
Turkey:    Ankara, Bulgaria, Constantinople, Smyrna.

Austria:   2 Supply centers,  3 Units:  Removes  1 unit.
England:   3 Supply centers,  4 Units:  Removes  1 unit.
France:    6 Supply centers,  4 Units:  Builds   2 units.
Germany:   6 Supply centers,  5 Units:  Builds   1 unit.
Italy:     5 Supply centers,  5 Units:  Builds   0 units.
Russia:    8 Supply centers,  7 Units:  Builds   1 unit.
Turkey:    4 Supply centers,  4 Units:  Builds   0 units.

The next phase of 'delphi' will be Adjustments for Winter of 1902.
""";

        def ap = new AdjustmentParser(map, input);
        def oi = ap.getOwnership();
        def ai = ap.getAdjustments();
        def of = oi.find {o -> o.getPowerName() == "France"}
        def oa = oi.find {o -> o.getPowerName() == "Austria"}

        def af = ai.find {o -> o.getPowerName() == "France"}
        def aa = ai.find {o -> o.getPowerName() == "Austria"}

        expect:
        ai.length == 7
        of.getProvinces().size() == 6
        oa.getProvinces().size() == 2

        af.getNumUnits() == 4
        af.getNumBuildOrRemove() == 2
        af.getNumSupplyCenters() == 6

        aa.getNumUnits() == 3
        aa.getNumBuildOrRemove() == 1
        aa.getNumSupplyCenters() == 2



    }

}
