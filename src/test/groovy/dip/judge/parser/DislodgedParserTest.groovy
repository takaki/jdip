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

class DislodgedParserTest extends Specification {
    def "test"() {
        String input = """Italy: Fleet Naples SUPPORT Fleet Tunis -> Ionian Sea.
Italy: Fleet Tunis -> Ionian Sea.
Italy: Army Rome SUPPORT Army Venice -> Apulia.

Russia: Army Smyrna -> Constantinople.


The following units were dislodged:

The 1Austrian Army in Apulia with no valid retreats was destroyed.
The 2Austrian Fleet in the Ionian Sea can retreat to Tyrrhenian Sea or Albania
or Greece or Eastern Mediterranean.
The 3French Army in Paris with no valid retreats was destroyed.
The 4German Fleet in the North Sea can retreat to Norwegian Sea or Skagerrak or
Belgium or London.
The 5Italian Fleet in Spain (south coast) can retreat to Portugal (north coast) or Western
Mediterranean.
The 6Chinese Fleet in the South Atlantic Ocean with no valid retreats was
destroyed.
The 7Russian Army in Constantinople can retreat to Ankara or Smyrna.
The 8Soviet Army in St. Petersburg can retreat to Here or There.
The 9Unlucky Army in Badlands can retreat to St. Elsewhere or Picardy.
The 10Germany Army in AAAAAA can retreat to BBBBBBB.
The 11Germany Army in NewAAAA can retreat to St. BBBBBBB.
The 12Germany Army in ReallyNewAAAA can retreat to St.
BBBBBBB.

The next phase of 'ferret' will be Retreats for Fall of 1906.
The deadline for orders will be Wed Apr 10 2002 17:54:23 -0500.
""";
        def variant = new VariantManager().getVariants()[18]
        def world = WorldFactory.getInstance().createWorld(variant)

        def phase = world.getInitialTurnState().getPhase()
        def dp = new DislodgedParser(phase, input);
        def di = dp.getDislodgedInfo();
        expect:
        di.size() == 12
        di[0].getPowerName() == '1austrian'
        di[0].getSourceName() == 'apulia'
        di[0].getRetreatLocationNames().size() ==0
        di[0].getUnitName() == 'army'
    }
}
