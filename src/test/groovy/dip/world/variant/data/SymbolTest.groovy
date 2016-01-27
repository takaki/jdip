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

import org.w3c.dom.Document
import org.w3c.dom.Element
import spock.lang.Specification

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class SymbolTest extends Specification {

    def "getter"() {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("test");
        def instance = new Symbol("name", 1, rootElement)
        expect:
        instance.getName() == "name"
        instance.scale == 1
        instance.getSVGData().toString() == rootElement.toString()
    }

    def "check illegal arguments"() {
        when:
        new Symbol(null, 1, null)
        then:
        thrown(IllegalArgumentException)

        when:
        new Symbol("name", -0.01, null)
        then:
        thrown(IllegalArgumentException)
    }


}
