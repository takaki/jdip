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

class TurnStateTest extends Specification {
    def ts = new TurnState(new Phase(Phase.SeasonType.SPRING, 1900, Phase.PhaseType.MOVEMENT))

    def "check illegal arguments"() {
        when:
        new TurnState(null)
        then:
        thrown(NullPointerException)

        when:
        ts.setWorld(null)
        then:
        thrown(NullPointerException)

        when:
        ts.setPhase(null)
        then:
        thrown(NullPointerException)

        when:
        ts.setPosition(null)
        then:
        thrown(NullPointerException)

        when:
        ts.setResultList(null)
        then:
        thrown(NullPointerException)

        when:
        ts.setOrders(null, new ArrayList())
        then:
        thrown(NullPointerException)

        when:
        ts.setOrders(new Power(["a"] as String[], "adj", true), null)
        then:
        thrown(NullPointerException)
    }

    def "setter and getter" () {
        def ts = new TurnState()
        def phase = new Phase(Phase.SeasonType.SPRING, 1900, Phase.PhaseType.MOVEMENT)
        ts.setPhase(phase)

        expect:
        ts.getPhase() == phase

    }
}
