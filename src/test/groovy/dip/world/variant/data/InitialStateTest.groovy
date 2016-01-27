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

import dip.world.Coast
import dip.world.Unit
import spock.lang.Specification

class InitialStateTest extends Specification {
    def instance = new InitialState()

    def "getter and setter"() {
        instance.setProvinceName("1")
        instance.setPowerName("2")
        instance.setUnitType(Unit.Type.ARMY)
        instance.setCoast(Coast.EAST)
        expect:
        instance.getProvinceName() == "1"
        instance.getPowerName() == "2"
        instance.getUnitType() == Unit.Type.ARMY
        instance.getCoast() == Coast.EAST
        instance.toString() == "dip.world.variant.data.InitialState[provinceName=1,power=2,unit=A,coast=East Coast]"
    }
}