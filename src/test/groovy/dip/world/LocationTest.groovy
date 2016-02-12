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

package dip.world

import dip.order.OrderException
import spock.lang.Specification

class LocationTest extends Specification {
    def spain = new Province("Spain", ["spa"], 0, false)
    def spa_sc = new Location(spain, Coast.SOUTH)
    def spa_sc0 = new Location(spain, Coast.UNDEFINED)

    def "check constructor parameters"() {
        when:
        new Location(province, coast)
        then:
        thrown(IllegalArgumentException)
        where:
        province                           | coast
        null                               | Coast.EAST
        new Province("S", ["s"], 0, false) | null
    }

    def "instance"() {
        expect:
        spa_sc.getCoast() == Coast.SOUTH
        spa_sc.getProvince() == spain
        !spa_sc.isConvoyableCoast()
        spa_sc.toLongString() == "Spain(South Coast)"
        spa_sc.toString() == "spa/sc"
        spa_sc.isProvinceEqual(spa_sc0)
        spa_sc.isProvinceEqual(spain)
        !spa_sc.equals(spa_sc0)
        !spa_sc.equals("spa_sc")
        spa_sc.equals(spa_sc)
        spa_sc.clone().equals(spa_sc)
        spa_sc.equalsLoosely(spa_sc0)
    }

    def "OrderException"() {
        when:
        spa_sc.getValidated(Unit.Type.ARMY)
        then:
        thrown(OrderException)
    }

    def "IllegalArgumentError"() {
        when:
        spa_sc.getValidated(Unit.Type.UNDEFINED)
        then:
        thrown(IllegalArgumentException)
    }
}
