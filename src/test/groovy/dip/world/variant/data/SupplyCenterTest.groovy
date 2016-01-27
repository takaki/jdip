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

class SupplyCenterTest extends Specification {
    def instance = new SupplyCenter()

    def "setter and getter"() {
        instance.setHomePowerName("0")
        instance.setProvinceName("1")
        instance.setOwnerName("2")
        expect:
        instance.getHomePowerName() == "0"
        instance.getProvinceName() == "1"
        instance.getOwnerName() == "2"
        instance.toString() == "dip.world.variant.data.SupplyCenter[provinceName=1,powerName=0,ownerName=2]"
    }

    def "check illegal arguments" () {
        when:
        instance.setOwnerName("any")
        then:
        thrown(IllegalArgumentException)
    }

}
