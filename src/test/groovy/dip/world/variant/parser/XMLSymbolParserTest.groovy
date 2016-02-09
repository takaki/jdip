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

import dip.world.variant.VariantManager
import spock.lang.Specification

import java.nio.file.Paths

class XMLSymbolParserTest extends Specification {

    def "parse"() {
        setup:
        VariantManager.init([Paths.get(System.getProperty("user.dir"), "src/test/resources/variants").
                                     toFile()] as File[]) // TODO: fix
        def pluginUrl = getClass().getResource("/variants/simpleSymbols.zip")
        URLClassLoader urlCL = new URLClassLoader(pluginUrl);
        URL variantXMLURL = urlCL.findResource("symbols.xml")
        InputStream is = new BufferedInputStream(variantXMLURL.openStream());

        when:
        def instance = new XMLSymbolParser(is, pluginUrl)
        def symbolpack = instance.getSymbolPack()
        then:
        symbolpack.getName() == "Simple"
        symbolpack.getSVGURI() == new URI("symbols.svg")
        symbolpack.getVersion() == 1.0
        symbolpack.getCSSStyles()[0].getName() == ".symBuildShadow"
        symbolpack.getCSSStyles()[0].getStyle() == "{fill:none;stroke:black;opacity:0.5;stroke-width:7;}"
        symbolpack.getCSSStyles()[7].getName() == ".symThinBorder"
        symbolpack.getCSSStyles()[7].getStyle() == "{stroke:black;stroke-width:0.2;}"
        symbolpack.getSymbols()[0].getSVGData().getTagName() == "symbol"
        symbolpack.getSymbols().size() == 11
    }

}
