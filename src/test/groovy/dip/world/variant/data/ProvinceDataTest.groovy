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

import spock.lang.Specification

class ProvinceDataTest extends Specification {
    def instance = new ProvinceData()

    def "setter and getter"() {

        instance.setFullName("0")
        instance.setAdjacentProvinceNames("1")
        instance.setAdjacentProvinceTypes("2")
        instance.setShortNames(Arrays.asList("3"))
        instance.setConvoyableCoast(true)
        instance.setBorders(Arrays.asList("4"))

        expect:
        instance.getFullName() == "0"
        instance.getShortNames() == ["3"] as String[]
        instance.getAdjacentProvinceNames() == ["1"] as String[]
        instance.getAdjacentProvinceTypes() == ["2"] as String[]
        instance.getConvoyableCoast()
        instance.getBorders() == ["4"] as String[]
        instance.toString() == "dip.world.variant.data.ProvinceData[fullName=0,#shortNames=1,#adj_provinces=1,#adj_types=1,isConvoyableCoast=true,#borders=1]"
    }
}
