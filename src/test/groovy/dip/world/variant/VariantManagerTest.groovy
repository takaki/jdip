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

import spock.lang.Specification

import java.nio.file.Paths

class VariantManagerTest extends Specification {
    def "initialize"() {
        setup:
        VariantManager.init([Paths.get(System.getProperty("user.dir"), "src/test/resources/variants").
                                     toFile()] as File[], false)
        when:
        def variant = VariantManager.getVariant("TEST_Borders", 1.0)
        then:
        variant.getName() == "TEST_Borders"

        when:
        def symbol = VariantManager.getSymbolPack("Simple", 1.0)
        then:
        symbol.getName() == "Simple"

        when:
        def symbolPacks = VariantManager.getSymbolPacks()
        then:
        symbolPacks.size() == 1
        def symbol0 = symbolPacks[0]
        symbol0.getName() == "Simple"
        symbol0.getThumbnailURI() == new URI("symbols.gif")


        when:
        def variants = VariantManager.getVariants()
        then:
        variants.size() == 9
        def variant0 = variants[8]
        variant0.getName() == "TEST_Borders"
        variant0.getPowers()[0].getName() == "France"
        variant0.getSupplyCenters()[0].getHomePowerName() == "turkey"
        variant0.getSupplyCenters()[0].getProvinceName() == "ank"

        expect:
        VariantManager.getSymbolPackVersions("Simple") == [1.0] as float[]
        VariantManager.hasVariantVersion("TEST_Borders", 1.0)
        VariantManager.hasSymbolPackVersion("Simple", 1.0)
        VariantManager.getVariantVersions("TEST_Borders") == [1.0] as float[]
        VariantManager.getResource(variant, new URI("a")) == null
        VariantManager.getResource(variant, new URI("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/testVariants.zip!/")) == new URL("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/testVariants.zip!/")
        VariantManager.getResource(symbol, new URI("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/simpleSimbols.zip!/")) == new URL("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/simpleSimbols.zip!/")
        VariantManager.getVariantPackageJarURL(variant) == new URL("jar:file:" + System.
                getProperty("user.dir") + "/src/test/resources/variants/testVariants.zip!/")


    }
}
