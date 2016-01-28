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

class XMLSymbolParserTest extends Specification {
    def instance = new XMLSymbolParser(DocumentBuilderFactory.newInstance())

    def "parse"() {
        def pluginUrl = getClass().getResource("/variants/simpleSymbols.zip")
        URLClassLoader urlCL = new URLClassLoader(pluginUrl);
        URL variantXMLURL = urlCL.findResource("symbols.xml")
        InputStream is = new BufferedInputStream(variantXMLURL.openStream());
        expect:
        instance != null

        when:
        instance.parse(is, pluginUrl)
        then:
        def ex = thrown(IllegalArgumentException)
        ex.getMessage() == "not initialized"
    }

}
