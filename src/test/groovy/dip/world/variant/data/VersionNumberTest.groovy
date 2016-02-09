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

class VersionNumberTest extends Specification {
    def v10 = new VersionNumber(1, 0)

    def "test compare"() {
        def v09 = new VersionNumber(0, 9)
        def v10a = new VersionNumber(1, 0)
        def v11 = new VersionNumber(1, 1)
        expect:
        v10.equals(v10a)
        v11.compareTo(v10) > 0
        v10.compareTo(v09) > 0
        v09.compareTo(v10) < 0
        v10.compareTo(v09) >= 0
    }

    def "test toString"() {
        expect:
        v10.toString() == "1.0"
    }

    def "test parse"() {
        expect:
        VersionNumber.parse("2.0") == new VersionNumber(2, 0)
    }
}
