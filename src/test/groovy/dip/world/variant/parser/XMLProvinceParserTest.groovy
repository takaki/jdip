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

import javax.xml.parsers.DocumentBuilderFactory

class XMLProvinceParserTest extends Specification {
    def instance = new XMLProvinceParser(DocumentBuilderFactory.newInstance())

    def "parse stringstream"() {
        def input = '<PROVINCES>' +
                '<BORDER_DEFINITIONS>' +
                '<!-- individual test -->' +
                '<BORDER id="seasonBorder" description="Season: Spring" season="Spring"/></BORDER_DEFINITIONS>' +

                '<!-- name, abbreviation, and adjacency data for all provinces -->' +
                '<!-- virtually identical to Judge format -->' +
                '<PROVINCE shortname="swi" fullname="Switzerland">' +
                '<ADJACENCY type="mv" refs="swi" />' +
                '</PROVINCE></PROVINCES>'
        when:
        instance.parse(new ByteArrayInputStream(input.getBytes()))
        def provinceData = instance.getProvinceData()
        def borderData = instance.getBorderData()
        then:
        provinceData.size() == 1
        provinceData[0].getFullName() == "Switzerland"
        borderData.size() == 1
        borderData[0].getSeason() == "Spring"

    }

    def "parse test/std_adjacency"() {
        def stream = XMLProvinceParser.class.getResourceAsStream("/variants/test/std_adjacency.xml")
        when:
        instance.parse(stream)
        then:
        instance.getProvinceData().size() == 75
        instance.getBorderData().size() == 0
        instance.getProvinceData()[19].getShortNames() == ["eas", "emed", "eastmed", "ems", "eme"] as String[]
        instance.getProvinceData()[18].
                getAdjacentProvinceNames() == ["swe kie", "hel nth swe bal kie ska"] as String[]
        instance.getProvinceData()[18].getAdjacentProvinceTypes() == ["mv", "xc"] as String[]

        when:
        instance.close()
        then:
        instance.getProvinceData().size() == 0
        instance.getBorderData().size() == 0
    }

    def "parse test/std_border"() {
        def stream = XMLProvinceParser.class.getResourceAsStream("/variants/test/std_borders.xml")
        when:
        instance.parse(stream)
        then:
        instance.getProvinceData().size() == 75
        instance.getBorderData().size() == 12

        when:
        instance.close()
        then:
        instance.getProvinceData().size() == 0
        instance.getBorderData().size() == 0
    }
}
