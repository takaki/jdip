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

package dip.judge.parser

import dip.world.Power
import dip.world.Province
import spock.lang.Specification

class AdjustmentParserTest extends Specification {
    def power = new Power(["France"] as String[], "French", true)
    def spain = new Province("Spain", ["spa"] as String[], 0, false)
    def map = new dip.world.Map([power] as Power[], [spain] as Province[])

    def "check arguments"() {
        expect:
        map == null
    }
}
