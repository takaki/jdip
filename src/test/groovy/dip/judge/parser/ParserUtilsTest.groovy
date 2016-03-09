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

import spock.lang.Specification

class ParserUtilsTest extends Specification {
    def "test parseBlock" ( ) {
        when:
        def input = """0
abcdeabcdeabcde
012345
abcdeabcdeabcde
123456789012345
012345
abcdeabcdeabcde
123456789012345
"""
        then:
        ParserUtils.parseBlock(new BufferedReader(new StringReader(input))) == """abcdeabcdeabcde
"""

        when:
        def input0 = """abcdeabcdeabcde
012345
abcdeabcdeabcde
123456789012345
012345
abcdeabcdeabcde
123456789012345
"""

        then:
        ParserUtils.parseBlock(new BufferedReader(new StringReader(input0))) == """abcdeabcdeabcde
abcdeabcdeabcde
123456789012345
"""
    }

    def "test getNextLongLine" (){
        when:
        def input = """0
abcdeabcdeabcde
012345
abcdeabcdeabcde
123456789012345
012345
abcdeabcdeabcde
123456789012345
"""
        then:
        ParserUtils.getNextLongLine(new BufferedReader(new StringReader(input))) == """abcdeabcdeabcde"""

        when:
        def input0 = """0abcdeabcdeabcde
012345
abcdeabcdeabcde
123456789012345
012345
abcdeabcdeabcde
123456789012345
"""

        then:
        ParserUtils.getNextLongLine(new BufferedReader(new StringReader(input0))) == """0abcdeabcdeabcde"""

    }

    def "test filter" (){
        expect:
        ParserUtils.filter("  ") == ""
        ParserUtils.filter("a b") == "ab"
        ParserUtils.filter("a  b") == "ab"
        ParserUtils.filter("  a  b  ") == "a b "

        ParserUtils.filter("   aa   b   ") == "aa b "
        ParserUtils.filter("   aa  b  ") == "aa b "
    }
}
