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

package dip.test

import dip.order.Order
import dip.order.OrderFactory
import dip.process.StdAdjudicator
import dip.test.TestSuite.UnitPos
import dip.world.Position
import dip.world.Province
import dip.world.TurnState
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Paths

class TestSuiteRunner extends Specification {
    static def files = ["datc_v2.4_06_remove6.e.4.txt", "datc_v2.4_09.txt","dipai.txt","explicitConvoys.txt","real.txt","wing.txt"]
    static def tss = files.collect { file -> new TestSuite(Paths.get("etc/test_data", file)) }.
            collectMany { ts -> ts.getCases().collect { c -> [ts, c] } }

    @Unroll
    def "#currentCase"() {
        when:
        def world = ts.getWorld()
        world.setTurnState(currentCase.getCurrentTurnState());
        world.setTurnState(currentCase.getPreviousTurnState());
        final StdAdjudicator stdJudge = new StdAdjudicator(
                OrderFactory.getDefault(),
                currentCase.getCurrentTurnState());
        stdJudge.process();

        then:
        compareState(currentCase, stdJudge.getNextTurnState());

        cleanup:
        world.removeAllTurnStates();
        currentCase.getCurrentTurnState().getResultList().clear();

        where:
        [ts, currentCase] << tss
    }

    private static boolean compareState(final TestSuite.Case c, final TurnState resolvedTS) {
        if (resolvedTS == null) {
            return true;
        }

        final Position pos = resolvedTS.getPosition();

        final Set<UnitPos> resolvedUnits = new HashSet<>();

        for (final Province prov : pos.getUnitProvinces()) {
            if (!resolvedUnits.add(new UnitPos(pos, prov, false))) {
                throw new IllegalStateException(
                        "CompareState: Internal error (non dislodged)");
            }
        }

        for (final Province prov : pos.getDislodgedUnitProvinces()) {
            if (!resolvedUnits.add(new UnitPos(pos, prov, true))) {
                throw new IllegalStateException(
                        "CompareState: Internal error (dislodged)");
            }
        }

        final Set<UnitPos> caseUnits = new HashSet<>();

        for (final Order dsOrd1 : c.getPostState()) {
            if (!caseUnits.add(new UnitPos(dsOrd1, false))) {
                throw new IllegalStateException(
                        String.format("duplicate POSTSTATE position: %s",
                                dsOrd1));
            }
        }

        for (final Order dsOrd : c.getPostDislodged()) {
            if (!caseUnits.add(new UnitPos(dsOrd, true))) {
                throw new IllegalStateException(String.format(
                        "duplicate POSTSTATE_DISLODGED position: %s", dsOrd));
            }
        }

        final Set<UnitPos> intersection = new HashSet<>(caseUnits);
        intersection.retainAll(resolvedUnits);

        final Set<UnitPos> added = new HashSet<>(resolvedUnits);
        added.removeAll(caseUnits);

        final Set<UnitPos> missing = new HashSet<>(caseUnits);
        missing.removeAll(resolvedUnits);

        if (!missing.isEmpty() || !added.isEmpty()) {
            return false;
        }

        return true;
    }// compareState()

}
