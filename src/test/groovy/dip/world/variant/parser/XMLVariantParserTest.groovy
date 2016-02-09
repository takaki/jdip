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
import dip.world.variant.data.VersionNumber
import spock.lang.Specification

import java.nio.file.Paths

class XMLVariantParserTest extends Specification {

    def "parse"() {
        setup:
        VariantManager.getInstance().init([Paths.get(System.getProperty("user.dir"), "src/test/resources/variants").
                                     toFile()] as File[]) // TODO: fix
        def pluginUrl = getClass().getResource("/variants/testVariants.zip")
        URLClassLoader urlCL = new URLClassLoader(pluginUrl);
        URL variantXMLURL = urlCL.findResource("variants.xml")
        InputStream is = new BufferedInputStream(variantXMLURL.openStream());

        when:
        def instance = new XMLVariantParser(is, pluginUrl)
        def variants = instance.getVariants()
        def std = variants[0]
        then:
        variants.size() == 9
        std.getStartingPhase().getYear() == 1914
        std.getName() == "DATC_Standard"
        std.getDescription() == "<div style=\"font-family:arial,helvetica;color:red;\">\n" +
                "\t\t<b>NOTE: This map is for DATC and Regression Testing, and is not recommended for general game play.</b>\n" +
                "\t\t</div>\n" +
                "\t\t<div style=\"font-family:arial,helvetica;margin-top:10px;\">\n" +
                "\t\tTest Version of Standard Map. This version allows the user to optionally start in BC (e.g., 500 BC), \n" +
                "\t\tunlike the normal Standard variant. Wing units are also allowed, for testing purposes.\n" +
                "\t\t</div>"

        std.getNumSCForVictory() == 18
        std.getMaxGameTimeYears() == 35
        std.getMaxYearsNoSCChange() == 7

        std.getMapGraphics()[0].getName() == "Simple"
        std.getMapGraphics()[0].isDefault()
        std.getMapGraphics()[0].getURI() == new URI("simple_std.svg")

        std.getPowers().size() == 7
        std.getPowers()[0].getName() == "France"

        std.getSupplyCenters().size() == 34
        std.getSupplyCenters()[0].getProvinceName() == "ank"

        std.getInitialStates().size() == 22
        std.getInitialStates()[0].getProvinceName() == "ank"

        std.getRuleOptionNVPs().size() == 2

        std.getProvinceData().size() == 75
        std.getProvinceData()[19].getShortNames() == ["eas", "emed", "eastmed", "ems", "eme"] as String[]
        std.getProvinceData()[18].
                getAdjacentProvinceNames() == ["swe kie", "hel nth swe bal kie ska"] as String[]
        std.getProvinceData()[18].getAdjacentProvinceTypes() == ["mv", "xc"] as String[]


        std.getBorderData().size() == 0
        std.getVersion() == VersionNumber.parse('1.0')

        std.toString() == "dip.world.variant.data.Variant[name=DATC_Standard,isDefault=false" +
                "powers=France,Austria,Turkey,Russia,England,Germany,Italy,," +
                "phase=Spring, 1914 (Movement),istate=,supplyCenters=,provinceData=mapGraphics=," +
                "vcNumSCForVictory=18,vcMaxGameTimeYears=35,vcMaxYearsNoSCChange=7,version=1.0]"

    }
}
