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

package dip.world.variant.parser

import spock.lang.Specification

class XMLProvinceParserTest extends Specification {


    def "parse test/std_adjacency"() {
        def url = XMLProvinceParser.class.getResource("/variants/test/std_adjacency.xml")
        when:
        def instance = new XMLProvinceParser(url)
        then:
        instance.getProvinceData().size() == 75
        instance.getBorderData().size() == 0
        instance.getProvinceData()[19].getShortNames() == ["eas", "emed", "eastmed", "ems", "eme"] as String[]
        instance.getProvinceData()[18].
                getAdjacentProvinceNames() == ["swe kie", "hel nth swe bal kie ska"] as String[]
        instance.getProvinceData()[18].getAdjacentProvinceTypes() == ["mv", "xc"] as String[]

//        when:
//        instance.close()
//        then:
//        instance.getProvinceData().size() == 0
//        instance.getBorderData().size() == 0
    }

    def "parse test/std_border"() {
        def url = XMLProvinceParser.class.getResource("/variants/test/std_borders.xml")
        when:
        def instance = new XMLProvinceParser(url)
        then:
        instance.getProvinceData().size() == 75
        instance.getBorderData().size() == 12

        instance.getProvinceData()[0].getFullName() == "Adriatic Sea"
        instance.getBorderData()[0].getSeason() == "Spring"

//        when:
//        instance.close()
//        then:
//        instance.getProvinceData().size() == 0
//        instance.getBorderData().size() == 0
    }
}
