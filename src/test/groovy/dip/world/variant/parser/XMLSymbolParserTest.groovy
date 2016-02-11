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

import dip.world.variant.data.VersionNumber
import spock.lang.Specification

class XMLSymbolParserTest extends Specification {

    def "parse"() {
        when:
        def url = getClass().getResource("/variants/symbols/simple/symbols.xml")
        def instance = new XMLSymbolParser(url)
        def symbolpack = instance.getSymbolPack()
        then:
        symbolpack.getName() == "Simple"
        symbolpack.getSVGURI() == new URI("symbols.svg")
        symbolpack.getVersion() == new VersionNumber(1,0)
        symbolpack.getCSSStyles()[0].getName() == ".symBuildShadow"
        symbolpack.getCSSStyles()[0].getStyle() == "{fill:none;stroke:black;opacity:0.5;stroke-width:7;}"
        symbolpack.getCSSStyles()[7].getName() == ".symThinBorder"
        symbolpack.getCSSStyles()[7].getStyle() == "{stroke:black;stroke-width:0.2;}"
        symbolpack.getSymbols()[0].getSVGData().getTagName() == "symbol"
        symbolpack.getSymbols().size() == 11
    }

}
