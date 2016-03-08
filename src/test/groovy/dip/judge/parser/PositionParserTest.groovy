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

class PositionParserTest extends Specification {
    def "test"() {
        String input =
                """sdkflaakdljf fdakjd slkjdfsa klfd
dslkafflddfskj fdakjd slkjdfsa klfd
kldsjfkdskajfdsak fdakjd slkjdfsa klfd

Starting position for Spring of 1901.

Argentina:Army  Santa Cruz.
Argentina:Fleet Buenos Aires.
Argentina:Fleet Chile.

Brazil:  Army  Brasilia.
Brazil:  Army  Rio de Janeiro.
Brazil:  Fleet Recife.

Oz:      Fleet New South Wales.
Oz:      Fleet Victoria.
Oz:      Fleet Western Australia.

The deadline for the first movement orders is Tue Dec  4 2001 23:30:00 PST.
The next phase of 'ferret' will be Movement for Fall of 1901.
The deadline for orders will be Tue Jan 22 2002 23:30:00 -0500.
""";


        PositionParser pp = new PositionParser(input);

        def pi = pp.getPositionInfo();
        System.out.println("# of orders: " + pi.size());
        System.out.println("phase: " + pp.getPhase());

        expect:
        pi.size() == 9
        pp.getPhase().getYear() == 1901
        pp.getPhase().getBriefName() == "S1901M"
        pi[0].getPowerName() == 'Argentina'
        pi[0].getUnitName() == 'Army'
        pi[0].getLocationName() == 'Santa Cruz'

    }
}
