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

package dip.order

import dip.world.*
import spock.lang.Specification

class BuildTest extends Specification {
    static def power = new Power(["France"], "French", true)
    static def location = new Location(new Province("Spain", ["spa"], 0, false), Coast.LAND)

    def "illegal arguments throw a exception"() {
        when:
        new Build(pw, src, unit)

        then:
        thrown(NullPointerException)

        where:
        pw    | src      | unit
        null  | location | Unit.Type.FLEET
        power | null     | Unit.Type.FLEET
        power | location | null
    }

    def "getter"() {
        def build = new Build(power, location, Unit.Type.ARMY)
        expect:
        build.getBriefName() == "B"
        build.getFullName() == "Build"
        build.getDefaultFormat() == "{getPower()}: {_orderName_} {getSourceUnitType()} {getSource()}"
        build.toBriefString() == "France: B A spa"
        build.toFullString() == "France: Build Army Spain"
        build.equals(new Build(power, location, Unit.Type.ARMY))

    }

}
