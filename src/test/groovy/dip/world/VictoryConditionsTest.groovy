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

import spock.lang.Shared
import spock.lang.Specification

class VictoryConditionsTest extends Specification {
    @Shared
    def phase = new Phase(Phase.SeasonType.SPRING, 1900, Phase.PhaseType.MOVEMENT)
    def vc = new VictoryConditions(10, 20, 30, phase)

    def "constructor check arguments"() {
        when:
        new VictoryConditions(sc, ncyear, gtyear, init)
        then:
        thrown(IllegalArgumentException)
        where:
        sc | ncyear | gtyear | init
        -1 | 0      | 0      | phase
        0  | -1     | 0      | phase
        0  | 0      | -1     | phase
        0  | 0      | 0      | phase
    }

    def "constructor null check arguments"() {
        when:
        new VictoryConditions(sc, ncyear, gtyear, init)
        then:
        thrown(NullPointerException)
        where:
        sc | ncyear | gtyear | init
        0  | 0      | 0      | null
    }

}
