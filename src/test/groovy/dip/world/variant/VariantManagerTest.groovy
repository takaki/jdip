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

package dip.world.variant

import dip.world.Coast
import dip.world.Unit
import dip.world.variant.data.VersionNumber
import spock.lang.Specification

import java.nio.file.Paths

class VariantManagerTest extends Specification {
    def "initialize"() {
        setup:
        VariantManager.getInstance().init([Paths.get(System.getProperty("user.dir"), "src/test/resources/variants").
                                                   toFile()] as File[])
        when:
        def variant = VariantManager.getInstance().getVariant("TEST_Borders", VersionNumber.parse('1.0'))
        then:
        variant.getName() == "TEST_Borders"

        when:
        def symbol = VariantManager.getInstance().getSymbolPack("Simple", VersionNumber.parse('1.0'))
        then:
        symbol.getName() == "Simple"

        when:
        def symbolPacks = VariantManager.getInstance().getSymbolPacks()
        then:
        symbolPacks.size() == 1
        def symbol0 = symbolPacks[0]
        symbol0.getName() == "Simple"
        symbol0.getThumbnailURI() == new URI("symbols.gif")


        when:
        def variants = VariantManager.getInstance().getVariants()
        then:
        variants.size() == 9
        def variant0 = variants[7]
        variant0.getName() == "DATC_Standard"
        variant0.getPowers()[0].getName() == "France"
        variant0.getSupplyCenters()[0].getHomePowerName() == "turkey"
        variant0.getSupplyCenters()[0].getProvinceName() == "ank"

        variant0.getInitialStates()[0].getProvinceName() == "ank"
        variant0.getInitialStates()[0].getCoast() == Coast.UNDEFINED
        variant0.getInitialStates()[0].getUnitType() == Unit.Type.FLEET
        variant0.getInitialStates()[17].getProvinceName() == "stp"
        variant0.getInitialStates()[17].getPowerName() == "russia"
        variant0.getInitialStates()[17].getCoast() == Coast.SOUTH
        variant0.getInitialStates()[17].getUnitType() == Unit.Type.FLEET

        variant0.getMapGraphics().size() == 1
        variant0.getMapGraphics()[0].isDefault()
        variant0.getMapGraphics()[0].getName() == "Simple"
        variant0.getMapGraphics()[0].getURI() == new URI("simple_std.svg")
        variant0.getMapGraphics()[0].getThumbnailURI() == new URI("simple_thumb.png")

        expect:
        VariantManager.getInstance().getSymbolPackVersions("Simple") == [VersionNumber.parse('1.0')] as VersionNumber[]
        VariantManager.getInstance().hasVariantVersion("TEST_Borders", VersionNumber.parse('1.0'))
        VariantManager.getInstance().hasSymbolPackVersion("Simple", VersionNumber.parse('1.0'))
        VariantManager.getInstance().getVariantVersions("TEST_Borders") == [VersionNumber.
                                                                                    parse('1.0')] as VersionNumber[]
        VariantManager.getInstance().getResource(variant, new URI("a")) == null
        VariantManager.getInstance().getResource(variant, new URI("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/testVariants.zip!/")) == new URL("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/testVariants.zip!/")
        VariantManager.getInstance().getResource(symbol, new URI("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/simpleSimbols.zip!/")) == new URL("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/simpleSimbols.zip!/")
        VariantManager.getInstance().getVariantPackageJarURL(variant) == new URL("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/testVariants.zip!/")


    }
}
