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

package dip.world.variant.data

import dip.world.Phase
import dip.world.Power
import spock.lang.Specification

class VariantTest extends Specification {
    def instance = new Variant()

    def "setter and getter"() {
        def phase = Phase.parse("S1900M")
        def map = new MapGraphic("uri", true, "name", "description", "thumbURI", "prefSPName")
        def power = new Power(["France"] as String[], "French", true)
        def england = new Power(["England"] as String[], "English", true)
        setup:
        instance.setName("0")
        instance.setAliases(["a"] as String[])
        instance.setVersion(1.0)
        instance.setDefault(true)
        instance.setDescription("d")
        instance.setStartingPhase(phase)
        instance.setNumSCForVictory(10)
        instance.setMaxYearsNoSCChange(20)
        instance.setMaxGameTimeYears(30)
        instance.setProvinceData()
        instance.setBorderData()
        instance.setBCYearsAllowed(true)
        instance.setMapGraphics([map])
        instance.setPowers([power, england])
        instance.setInitialStates([])
        instance.setSupplyCenters([])
        instance.setRuleOptionNVPs([])
        instance.setActiveState([true, false] as boolean[])

        expect:
        instance.getName() == "0"
        instance.getAliases() == ["a"] as String[]
        instance.getVersion() == 1.0f
        instance.isDefault()
        instance.getDescription() == "d"
        instance.getStartingPhase() == phase
        instance.getInitialStates().size() == 0
        instance.getPowers() == [power, england] as Power[]
        instance.getSupplyCenters().size() == 0
        instance.getNumSCForVictory()
        instance.getMaxYearsNoSCChange()
        instance.getMaxGameTimeYears()
        instance.getMapGraphics()
        instance.getProvinceData().size() == 0
        instance.getRuleOptionNVPs().size() == 0
        instance.getBorderData().size() == 0
        instance.getBCYearsAllowed()
        instance.getMapGrapic("name") == map
        instance.getMapGrapic("hoge") == null
        instance.getDefaultMapGraphic() == map
        instance.getHTMLSummaryArguments().
                toString() == (["0", "d", "10", "Spring", "1900", "Movement", "France, (England)", "2"] as Object[]).
                toString()
        instance.toString() != null
        instance.toString() == instance.clone().toString()


        when:
        instance.setActiveState([true] as boolean[])
        then:
        thrown(IllegalArgumentException)
    }

    def "clone"() {
        instance.setName("a")
        def clone = instance.clone()
        expect:
        clone != null
        instance == clone
    }

    def "NameValuePair"() {
//        when:
//        new Variant.NameValuePair("1", null)
//        then:
//        thrown(IllegalArgumentException)
//
//        when:
//        new Variant.NameValuePair(null, "1")
//        then:
//        thrown(IllegalArgumentException)
//
//        def pair = new Variant.NameValuePair("key", "val")
//        expect:
//        pair.getName() == "key"
//        pair.getValue() == "val"

    }
}
