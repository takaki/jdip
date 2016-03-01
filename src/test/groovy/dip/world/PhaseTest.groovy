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

class PhaseTest extends Specification {
    def phase = new Phase(Phase.SeasonType.SPRING, 1900, Phase.PhaseType.MOVEMENT)

    def "test instance"() {
        expect:
        phase.getYear() == 1900
        phase.getYearType() == new Phase.YearType(1900)
        phase.getPhaseType() == Phase.PhaseType.MOVEMENT
        phase.getSeasonType() == Phase.SeasonType.SPRING
        phase.getBriefName() == "S1900M"
        phase.toString() == "Spring, 1900 (Movement)"
    }

    def "test equals"() {
        def phase0 = new Phase(Phase.SeasonType.FALL, 1900, Phase.PhaseType.MOVEMENT)
        expect:
        phase.equals(phase)
        !phase.equals(phase0)
        !phase0.equals(phase)
        phase.compareTo(phase) == 0
        phase.compareTo(phase0) < 0
        phase0.compareTo(phase) > 0
    }

    def "serialize" (){
        def baos = new ByteArrayOutputStream()
        def os = new ObjectOutputStream(baos)
        os.writeObject(phase)
        def obj = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        expect:
        obj.readObject() == phase
    }

    def "getNext and Previout"() {
        def phase0 = new Phase(Phase.SeasonType.FALL, 1900, Phase.PhaseType.ADJUSTMENT)
        def phase1 = new Phase(Phase.SeasonType.SPRING, 1901, Phase.PhaseType.MOVEMENT)
        expect:
        phase0.getNext() == phase1
        phase0 == phase1.getPrevious()
    }

    def "test isValid"() {
        expect:
        Phase.isValid(Phase.SeasonType.FALL, Phase.PhaseType.MOVEMENT)
        !Phase.isValid(Phase.SeasonType.SPRING, Phase.PhaseType.ADJUSTMENT)
    }

    def "Phase#parse"() {
        expect:
        Phase.parse("Spring, 1900(Movement)").get() == phase
        Phase.parse("Spring, 1900[Movement)").get() == phase
        Phase.parse("1900 Spring Movement").get() == phase
        Phase.parse("Movement;1900 / Spring ").get() == phase
        Phase.parse("Spring bc 1000 Movement").get() == new Phase(Phase.SeasonType.SPRING, -1000, Phase.PhaseType.MOVEMENT)
    }

    def "test getAllSeasonPhaseCombos"() {
        expect:
        Phase.getAllSeasonPhaseCombos()[0] == "Spring Movement"
        Phase.getAllSeasonPhaseCombos()[2] == "Fall Movement"
        Phase.getAllSeasonPhaseCombos()[4] == "Fall Adjustment"
        Phase.getAllSeasonPhaseCombos().size() == 5
    }

    def "test SeasonType"() {
        def spring = Phase.SeasonType.SPRING
        def fall = Phase.SeasonType.FALL
        expect:
        spring.getNext() == fall
        fall.getNext() == spring
        spring.getPrevious() == fall
        fall.getPrevious() == spring
        !spring.equals(fall)
        spring.compareTo(fall) < 0
        fall.compareTo(spring) > 0
    }

    def "SeasonType#parse"() {
        def spring = Phase.SeasonType.SPRING
        def fall = Phase.SeasonType.FALL
        expect:
        Phase.SeasonType.parse("s") == spring
        Phase.SeasonType.parse("f") == fall
        Phase.SeasonType.parse("w") == fall
        Phase.SeasonType.parse("sPrIng") == spring
        Phase.SeasonType.parse("summer") == spring
        Phase.SeasonType.parse("faLL") == fall
        Phase.SeasonType.parse("Winter") == fall
    }

    def "PhaseType"() {
        def movement = Phase.PhaseType.MOVEMENT
        def retrieat = Phase.PhaseType.RETREAT
        def adjustment = Phase.PhaseType.ADJUSTMENT
        expect:
        movement.getNext() == retrieat
        retrieat.getNext() == adjustment
        adjustment.getNext() == movement
        movement == retrieat.getPrevious()
        retrieat == adjustment.getPrevious()
        adjustment == movement.getPrevious()
    }

    def "PhaseType#parse"() {
        def movement = Phase.PhaseType.MOVEMENT
        def retreat = Phase.PhaseType.RETREAT
        def adjustment = Phase.PhaseType.ADJUSTMENT
        expect:
        Phase.PhaseType.parse("M") == movement
        Phase.PhaseType.parse("A") == adjustment
        Phase.PhaseType.parse("B") == adjustment
        Phase.PhaseType.parse("R") == retreat
        Phase.PhaseType.parse("adjustment_") == adjustment
        Phase.PhaseType.parse("movement_") == movement
        Phase.PhaseType.parse("retreat_") == retreat

    }

    def "YearType#parse" () {
        expect:
        Phase.YearType.parse("1900").get().getYear() == 1900
        Phase.YearType.parse("100").get().getYear() == 100
        Phase.YearType.parse("1000 BC") == Optional.empty()
        Phase.YearType.parse("1000 bc").get().getYear() == -1000
        Phase.YearType.parse("-1000").get().getYear() == -1000
    }
    def "YserType#toString" (){
        expect:
        new Phase.YearType(100).toString() == "100 AD"
        new Phase.YearType(1000).toString() == "1000"
        new Phase.YearType(-100).toString() == "100 BC"
    }
}
