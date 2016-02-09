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

class SymbolPackTest extends Specification {
    def instance = new SymbolPack()

    def "getter and setter"() {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("test");
        def symbol = new Symbol("name", 1, rootElement)
        def css = new SymbolPack.CSSStyle("name", "style")
        //def styles = [css] as SymbolPack.CSSStyle[]
        def styles = Arrays.asList(css)

//        instance.setName("0")
//        instance.setVersion(1)
//        instance.setDescription("2")
//        instance.setThumbnailURI("3")
//        instance.setSVGURI("4")
        //instance.setCSSStyles(styles)
        instance.setSymbols([symbol] as Symbol[])
        instance.setSymbols([symbol])
        expect:
//        instance.getName() == "0"
//        instance.getVersion() == 1
//        instance.getDescription() == "2"
//        instance.getThumbnailURI() == new URI("3")
//        instance.getSVGURI() == new URI("4")
        instance.getSymbols() == [symbol] as Symbol[]
        instance.getSymbol("name") == symbol
        instance.getSymbol("foo") == null
        //instance.getCSSStyles() == styles
        !instance.hasCSSStyles()
    }

    def "compareTo"() {
//        instance.setName("0")
//        def i0 = new SymbolPack()
//        i0.setName("1")
//        expect:
//        instance.compareTo(i0) < 0


    }

    def "strage uri"() {
//        instance.setThumbnailURI("%1")
//        instance.setSVGURI("%1")
//        expect:
//        instance.getThumbnailURI() == null
//        instance.getSVGURI() == null
    }

    def "CSSStyle" () {
        when:
        new SymbolPack.CSSStyle(null, "value")
        then:
        thrown(IllegalArgumentException)

        def css = new SymbolPack.CSSStyle("name", "value")
        expect:
        css.getName() == "name"
        css.getStyle() == "value"
    }
}
