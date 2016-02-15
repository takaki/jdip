//
//  @(#)TestSuite.java		4/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/
//
package dip.test;

import dip.order.*;
import dip.order.result.ConvoyPathResult;
import dip.order.result.OrderResult;
import dip.order.result.OrderResult.ResultType;
import dip.process.StdAdjudicator;
import dip.world.*;
import dip.world.variant.VariantManager;
import dip.world.variant.data.Variant;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * A very hastily-programmed Test harness..
 * <p>
 * This will read in a file of cases (1 or more). All cases must use the same
 * variant. The variant is then loaded, orders are parsed, and adjudication then
 * occurs. After adjudication, the positions of units are checked with that of
 * the case file for discrepancies. If no discrepancies exist, the case passes.
 * <p>
 * Note that when in performance-testing mode, all logging is disabled and
 * comparison-checking is not performed; the goal is testing adjudicator code
 * only.
 * <p>
 * All output is printed to stdout
 * <p>
 * <b>Case File Format Notes:</b>
 * <ul>
 * <li>
 * Any line prefixed by a # is a comment line. A # may be placed after a line,
 * to comment out part of a line or make a comment about a particular line.
 * </li>
 * <li>
 * Empty lines / whitespace-only lines are ignored. Whitespace before keywords
 * and lines are also ignored by the parser.
 * </li>
 * <li>
 * Single Line keywords are a keyword, followed by whitespace, followed by
 * text; that text is parsed and associated with that keyword. Some keywords
 * (such as END) do not have any text that follows them.
 * </li>
 * <li>
 * Block keywords begin a block; DO NOT put text on the same line as a block
 * keyword; start text on the next line. A block ends when another keyword
 * (block or single line) is detected.
 * </li>
 * </ul>
 * <p>
 * <b>Case File Keywords:</b>
 * <ul>
 * <li><b>VARIANT_ALL: </b><i>Required</i>.
 * This must occur at the beginning of the case file. <i>All cases are
 * required to use the same variant</i>. Single line.
 * </li>
 * <li><b>CASE: (String)</b><i>Required</i>.
 * Begins a Case. The text following the case is the case name, and may
 * contain any printable character, including spaces, but must fit on
 * a single line.
 * </li>
 * <li><b>PRESTATE_SETPHASE: (phase)</b><i>Recommended</i>.
 * Set the phase (e.g., "Fall 1901, Movement" or "F1901M"). Single line.
 * </li>
 * <li><b>PRESTATE: </b><i>Recommended</i>.
 * Begins the non-dislodged unit setup block. Unit setups must consist of power, unit type,
 * and province, on the next line(s). e.g.: "England: F lon". Any orders to
 * non-dislodged units require a unit in the PRESTATE block.
 * </li>
 * <li><b>PRESTATE_DISLODGED: </b><i>Optional</i>.
 * If any dislodged units are to be positioned, set them in this block.
 * e.g.: "England: F lon" would create a dislodged Fleet in London.
 * </li>
 * <li><b>PRESTATE_RESULTS: </b><i>Optional</i>.
 * If a retreat phase is to be adjudicated, this sets up the "prior" phase.
 * Begins a block, where each order must be preceded by the keyword "SUCCESS:"
 * or "FAILURE:", followed by an order (i.e., Move, Hold, etc.).
 * </li>
 * <li><b>PRESTATE_SUPPLYCENTER_OWNERS: </b><i>Optional</i>.
 * Set owned, but not occupied, supply center owners in this block. If this is omitted,
 * the ownership is used from the initial variant settings. If it is supplied,
 * the variant information is erased and replaced with the given information.
 * <b>Note:</b> Currently you must use a unit too; e.g., "France: F lon" would set
 * the supply center in London to be owned by France. The unit type is required by
 * the parser but is ignored.
 * </li>
 * <li><b>ORDERS: </b><i>Recommended</i>.
 * One line, one order, in this block. e.g., "England: F lon-bel".
 * The orders are what will be adjudicated.
 * </li>
 * <li><b>POSTSTATE: </b><i>Recommended</i>.
 * A block of post-adjudication non-dislodged unit positions. The TestSuite tests
 * and make sure these match the post-adjudication state. Same format as PRESTATE.
 * </li>
 * <li><b>POSTSTATE_DISLODGED: </b><i>Recommended</i>.
 * A block of post-adjudication dislodged unit positions. The TestSuite tests
 * and make sure these match the post-adjudication state. Same format as PRESTATE
 * (or PRESTATE_DISLODGED for that matter).
 * </li>
 * <li><b>POSTSTATE_SAME: </b><i>Optional</i>.
 * If non-dislodged units do not change position, this may be used instead
 * of a POSTSTATE block and a list of non-dislodged unit positions.
 * </li>
 * <li><b>END: </b><i>Required</i>.
 * Ends a case. Must be the last line in a case.
 * </li>
 * </ul>
 * <p>
 * <b>An Example Case File:</b>
 * <pre>
 * VARIANT_ALL Standard
 * CASE Example Case 1 (illustrative example)
 * PRESTATE_SETPHASE Fall 1901, Movement
 * PRESTATE
 * Russia: F con
 * Russia: F bla
 * Turkey: F ank
 * ORDERS
 * Russia: F con S F bla-ank
 * Russia: F bla-ank
 * Turkey: F ank-con
 * POSTSTATE
 * Russia: F con
 * Russia: F ank
 * POSTSTATE_DISLODGED
 * Turkey: F ank
 * END
 * </pre>
 */
public final class TestSuite {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(TestSuite.class);

    // constants
    private static final String VARIANT_ALL = "variant_all";
    private static final String CASE = "case";
    private static final String PRESTATE = "prestate";
    private static final String ORDERS = "orders";
    private static final String POSTSTATE = "poststate";
    private static final String POSTSTATE_SAME = "poststate_same";
    private static final String END = "end";
    private static final String PRESTATE_SETPHASE = "prestate_setphase";
    private static final String PRESTATE_SUPPLYCENTER_OWNERS = "prestate_supplycenter_owners";
    private static final String PRESTATE_DISLODGED = "prestate_dislodged";
    private static final String POSTSTATE_DISLODGED = "poststate_dislodged";
    private static final String PRESTATE_RESULTS = "prestate_results";

    // warning: POSTSTATE_SAME MUST come before POSTSTATE (since we use startsWith())
    // "other" == not CASE (begin) or END
    private static final List<String> KEY_TYPES_OTHER = Arrays
            .asList(ORDERS, POSTSTATE_SAME, PRESTATE_SETPHASE, PRESTATE_RESULTS,
                    PRESTATE_SUPPLYCENTER_OWNERS, PRESTATE_DISLODGED,
                    POSTSTATE_DISLODGED, PRESTATE, POSTSTATE, VARIANT_ALL);

    private static final List<String> KEY_TYPES_WITH_LIST = Arrays
            .asList(ORDERS, PRESTATE_SUPPLYCENTER_OWNERS, PRESTATE_RESULTS,
                    PRESTATE_DISLODGED, POSTSTATE_DISLODGED, POSTSTATE,
                    PRESTATE);


    // VARIANT_ALL name
    private static float parseTime = -1;

    private final Collection<Case> cases = new ArrayList<>(10);
    private World world;
    private TurnState templateTurnState;


    /**
     * Start the TestSuite
     */
    public static void main(final String[] args) {
        final Path inFileName = Paths
                .get("etc/test_data/datc_v2.4_06_remove6.e.4.txt");
// {"etc/test_data/datc_v2.4_09.txt","etc/test_data/dipai.txt","etc/test_data/explicitConvoys.txt","etc/test_data/real.txt","etc/test_data/wing.txt"};


        LOGGER.debug("TestSuite Results: ({})", new Date());
        LOGGER.debug(
                "=======================================================================");
        LOGGER.debug("  test case file: {}", inFileName);

        final TestSuite ts = new TestSuite(inFileName);
        ts.evaluate();
    }// main()


    private TestSuite(final Path inFileName) {
        final long startTime = System.currentTimeMillis();
        parseCaseFile(inFileName);
        parseTime = (System.currentTimeMillis() - startTime) / 1000.0f;
    }// TestSuite()


    private void initVariant(final String variantName) {
        try {
            // get default variant directory.


            // load the default variant (Standard)
            // error if it cannot be found!!
            final Variant variant = new VariantManager()
                    .getVariant(variantName, VariantManager.VERSION_NEWEST)
                    .orElse(null);
            if (variant == null) {
                throw new IllegalArgumentException(
                        String.format("Cannot find variant %s", variantName));
            }

            // create the world
            world = WorldFactory.getInstance().createWorld(variant);
            templateTurnState = world.getLastTurnState();
            world.removeTurnState(templateTurnState);

            // set the RuleOptions in the World (this is normally done
            // by the GUI)
            world.setRuleOptions(RuleOptions.createFromVariant(variant));
            LOGGER.debug("  initialization complete.");
            LOGGER.debug("  variant: {}", variantName);
        } catch (final InvalidWorldException e) {
            throw new IllegalArgumentException("Init error", e);
        }
    }// init()


    public Collection<Case> getCases() {
        return Collections.unmodifiableCollection(cases);
    }

    public World getWorld() {
        return world;
    }


    public void evaluate() {
        int nOrders = 0;
        int nPass = 0;
        int nFail = 0;
        int nCases = 0;
        final List<String> failedCaseNames = new ArrayList<>(10);
        final List<String> unRezParadoxes = new LinkedList<>();

        final long startMillis = System.currentTimeMillis();    // start timing!

        // 'typical' mode (testing).
        // we keep stats and may or may not have logging
        //
        for (final Case currentCase : cases) {
            // world: setup
            world.setTurnState(currentCase.getCurrentTurnState());
            world.setTurnState(currentCase.getPreviousTurnState());

            // print case name
            LOGGER.debug("");
            LOGGER.debug("");
            LOGGER.debug(
                    "=CASE==================================================================");
            LOGGER.debug("  {}", currentCase.getName());

            // print pre-state
            printState(currentCase);

            // print orders
            LOGGER.debug(
                    "=ORDERS================================================================");
            printOrders(currentCase);
            nOrders += currentCase.getOrders().size();

            // adjudicate
            LOGGER.debug(
                    "=ADJUDICATION==========================================================");

            final StdAdjudicator stdJudge = new StdAdjudicator(
                    OrderFactory.getDefault(),
                    currentCase.getCurrentTurnState());
            stdJudge.process();

            // print adjudication results, if not performance testing
            // also print & check post conditions, if not performance testing

            // add unresolved paradoxes to list, so we know which cases they are
            if (stdJudge.isUnresolvedParadox()) {
                unRezParadoxes.add(currentCase.getName());
            }

            LOGGER.debug(
                    "=ADJUDICATION RESULTS==================================================");
            if (stdJudge.getNextTurnState() == null) {
                LOGGER.debug("=NEXT PHASE: NONE. Game has been won.");
            } else {
                LOGGER.debug("=NEXT PHASE: {}",
                        stdJudge.getNextTurnState().getPhase());
            }
            stdJudge.getTurnState().getResultList().stream()
                    .forEach(r -> LOGGER.debug("{}{}", "  ", r));

            // check post conditions
            LOGGER.debug(
                    "=POST-STATE============================================================");

            if (compareState(currentCase, stdJudge.getNextTurnState())) {
                nPass++;
            } else {
                nFail++;
                failedCaseNames.add(currentCase.getName());
            }

            LOGGER.debug(
                    "=======================================================================");

            nCases++;

            // cleanup: remove turnstates from world
            world.removeAllTurnStates();

            // cleanup: clear results in currentTurnSTate
            // this is absolutely essential!!
            currentCase.getCurrentTurnState().getResultList().clear();
        }


        // print stats
        //
        final long time = System
                .currentTimeMillis() - startMillis;    // end timing!
        LOGGER.debug("End: {}", new Date());

        // total time: includes setup/adjudication/comparison
        final float orderTime = (float) time / nOrders;
        final float thruPut = 1000.0f / orderTime;
        final float score = (float) nPass / nCases * 100.0f;

        LOGGER.debug("");
        LOGGER.debug("Failed Cases:");
        LOGGER.debug("=============");
        failedCaseNames.forEach(name -> LOGGER.debug("{}{}", "   ", name));
        LOGGER.debug("   [total: {}]", failedCaseNames.size());

        LOGGER.debug("");
        LOGGER.debug("Unresolved Paradoxes:");
        LOGGER.debug("=====================");
        unRezParadoxes.forEach(paradox -> LOGGER.debug("   {}", paradox));
        LOGGER.debug("   [total: {}]", Integer.toString(unRezParadoxes.size()));

        // print to log
        LOGGER.debug("");
        LOGGER.debug("Statistics:");
        LOGGER.debug("===========");
        LOGGER.debug("    Case parse time: {} seconds.", parseTime);
        LOGGER.debug(
                "    {} cases evaluated. {} passed, {} failed; {}%  pass rate.",
                nCases, nPass, nFail, score);
        LOGGER.debug(
                "    Times [includes setup, adjudication, and post-adjudication comparision]");
        LOGGER.debug("      {} orders processed in {} ms; {} ms/order average",
                nOrders, time, orderTime);
        LOGGER.debug("      Throughput: {} orders/second", thruPut);

        // if in 'brief' mode, only print out summary statistics

        // exit
        if (nFail > 0) {
            throw new RuntimeException("Did not pass tests.");
        }

    }// evaluate()


    // prints state settings...
    private void printState(final Case c) {

        final TurnState turnState = c.getCurrentTurnState();
        //Position position = turnState.getPosition();

        LOGGER.debug(
                "=PHASE=================================================================");
        LOGGER.debug("  {}", turnState.getPhase());

        // if we have some results to display, for prior state, do that now.
        if (!c.getResults().isEmpty()) {
            // print
            LOGGER.debug(
                    "=PRESTATE_RESULTS======================================================");
            LOGGER.debug("  From {}", c.getPreviousTurnState().getPhase());
            for (final OrderResult anOr : c.getResults()) {
                LOGGER.debug("    {}", anOr);
            }
        }


        // print non-dislodged units
        if (!c.getPreState().isEmpty()) {
            LOGGER.debug(
                    "=PRE-STATE=============================================================");
            for (final Order dsOrd : c.getPreState()) {
                LOGGER.debug("{}{}", "   ", dsOrd);
            }
        }

        // print dislodged units
        if (!c.getPreDislodged().isEmpty()) {
            LOGGER.debug(
                    "=PRE-STATE DISLODGED===================================================");
            for (final Order dsOrd : c.getPreDislodged()) {
                LOGGER.debug("{}{}", "   ", dsOrd);
            }
        }
    }// printState()


    /**
     * Prints the orders in a case
     */
    private static void printOrders(final Case currentCase) {
        for (final Order order : currentCase.getOrders()) {
            LOGGER.debug("  {}", order);
        }

        if (currentCase.getOrders().isEmpty()) {
            LOGGER.debug("  [none]");
        }
    }// printOrders()


    /**
     * compareState: checks to see if resolved state matches,
     * unit for unit, the Case POSTSTATEs. Units that match
     * are prepended with an '='. Units that are not found in the
     * case POSTSTATE/POSTSTATE_DISLODGED are prepended with a '+',
     * and units in POSTSTATE/POSTSTATE_DISLODGED not found in
     * the resolved turnstate are prepended with a '-'.
     * <p>
     * This is a more strict comparison than the old compareState,
     * w.r.t. dislodged units and coast checking. The implementation
     * is fairly simple and is not optimized for performance.
     * <p>
     * If no POSTSTATE or POSTSTATE_DISLODGED results are given,
     * it is assumed that there are no units for the omitted section.
     * <p>
     * Returns true if the states match (or game has been won);
     * otherwise, returns false.
     */
    private static boolean compareState(final Case c,
                                        final TurnState resolvedTS) {
        // special case: check for a win.
        if (resolvedTS == null) {
            LOGGER.debug(
                    "The game has been won. No new TurnState object is created.");
            return true;
        }

        final Position pos = resolvedTS.getPosition();

        // create set of resolvedUnits
        //
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

        // create set of caseUnits
        //
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

        // compare sets.
        //
        // first, we must make a duplicate of one set.
        // these are the units that are in the correct position (intersection)
        //
        final Set<UnitPos> intersection = new HashSet<>(caseUnits);
        intersection.retainAll(resolvedUnits);

        // now, create subtraction sets
        final Set<UnitPos> added = new HashSet<>(resolvedUnits);
        added.removeAll(caseUnits);

        final Set<UnitPos> missing = new HashSet<>(caseUnits);
        missing.removeAll(resolvedUnits);

        // if subtraction sets have no units, we are done. Otherwise, we must print
        // the differences.
        //
        if (!missing.isEmpty() || !added.isEmpty()) {
            LOGGER.debug("  CompareState: FAILED: unit positions follow.");

            // print adds
            added.forEach(up -> LOGGER.debug("  + {}", up));

            // print subtracts
            missing.forEach(up -> LOGGER.debug("  - {}", up));

            // print units in correct position
            intersection.forEach(up -> LOGGER.debug("  = {}", up));

            return false;
        }
        LOGGER.debug("  CompareState: PASSED");

        return true;
    }// compareState()


    /**
     * Private inner class, usually contained in Sets, that
     * is comparable, for determining if the end-state is
     * in fact correct.
     */
    public static final class UnitPos {
        private final Unit unit;            // owner/type/coast
        private final Province province;        // position
        private final boolean isDislodged;    // dislodged?

        /**
         * Create a UnitPos
         */
        UnitPos(final Order ds, final boolean isDislodged) {
            unit = new Unit(ds.getPower(), ds.getSourceUnitType());
            unit.setCoast(ds.getSource().getCoast());
            province = ds.getSource().getProvince();
            this.isDislodged = isDislodged;
        }// UnitPos()

        /**
         * Create a UnitPos
         */
        UnitPos(final Position pos, final Province prov,
                final boolean isDislodged) {
            province = prov;
            this.isDislodged = isDislodged;
            unit = isDislodged ? pos.getDislodgedUnit(prov) : pos.getUnit(prov);
            if (unit == null) {
                throw new IllegalArgumentException();
            }
        }// UnitPos()

        /**
         * Print
         */
        @Override
        public String toString() {
            return String.join("", unit.getPower().getName(), " ",
                    unit.getType().getShortName(), " ", province.getShortName(),
                    "/", unit.getCoast().getAbbreviation(),
                    isDislodged ? " [DISLODGED]" : "");
        }// toString()

        /**
         * Compare
         */
        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof UnitPos) {
                final UnitPos up = (UnitPos) obj;
                if (isDislodged == up.isDislodged && province == up.province && unit
                        .equals(up.unit)) {
                    return true;
                }
            }

            return false;
        }// equals()

        /**
         * Force all hashes to be the same, so equals() is used
         */
        @Override
        public int hashCode() {
            return Objects.hash(unit, province, isDislodged);
        }
    }// inner class UnitPos


    /**
     * Holds a Case
     */
    public static final class Case {
        private final List<Order> preState;
        private final List<Order> postState;
        private final List<Order> preDislodged;
        private final List<Order> postDislodged;
        private final List<Order> supplySCOwners;    // all types are 'army'
        private final List<OrderResult> results;

        private final List<Order> orders;
        private final String name;
        private final Phase phase;
        private final OrderParser of;
        private final TurnState currentTS;
        private final TurnState previousTS;

        // tsTemplate: template turnstate to create the current, and (if needed) previous
        // turnstates.
        Case(final World world, final TurnState turnState, final String name,
             final String phaseName, final List<String> pre,
             final List<String> ord, final List<String> post,
             final List<String> supplySCOwnersList,
             final List<String> preDislodgedList,
             final List<String> postDislodgedList,
             final List<String> orderResultList) {
            this.name = name;

            of = OrderParser.getInstance();
            // phase
            if (phaseName != null) {
                phase = Phase.parse(phaseName);
                if (phase == null) {
                    throw new IllegalArgumentException(
                            String.format("case %s, cannot parse phase %s",
                                    name, phaseName));
                }
            } else {
                phase = turnState.getPhase();
            }

            // set phase to template phase, if no phase was assigned.


            // setup current turnstate from template
            // use phase, if appropriate.
            currentTS = new TurnState(phase);
            currentTS.setPosition(turnState.getPosition().cloneExceptUnits());
            currentTS.setWorld(world);

            // setup previous phase, in case we need it.
            previousTS = new TurnState(phase.getPrevious());
            previousTS.setPosition(turnState.getPosition().cloneExceptUnits());
            previousTS.setWorld(world);

            // pre
            preState = pre.stream()
                    .map(line -> parseOrder(line, currentTS, true))
                    .collect(Collectors.toList());


            // ord
            orders = ord.stream()
                    .map(line -> parseOrder(line, currentTS, false))
                    .collect(Collectors.toList());


            // post
            postState = post.stream()
                    .map(line -> parseOrder(line, currentTS, true))
                    .collect(Collectors.toList());

            // prestate dislodged

            preDislodged = preDislodgedList.stream()
                    .map(line -> parseOrder(line, currentTS, true))
                    .collect(Collectors.toList());

            // poststate dislodged
            postDislodged = postDislodgedList.stream()
                    .map(line -> parseOrder(line, currentTS, true))
                    .collect(Collectors.toList());

            // supply-center owners
            supplySCOwners = supplySCOwnersList.stream()
                    .map(line -> parseOrder(line, currentTS, true))
                    .collect(Collectors.toList());


            // OrderResults
            //
            // THE BEST way to do this would be to setup the case, and then run
            // the adjudicator to get the results, checking the ajudicator results
            // against the 'prestate' positions. This way we would have all the same
            // results that the adjudicator would normally generate.
            //
            results = orderResultList.stream().flatMap(line -> {
                final ResultType ordResultType;

                // success or failure??
                if (line.startsWith("success")) {
                    ordResultType = ResultType.SUCCESS;
                } else if (line.startsWith("failure")) {
                    ordResultType = ResultType.FAILURE;
                } else {
                    throw new IllegalArgumentException(String.format(
                            "PRESTATE_RESULTS: must prepend orders with \"SUCCESS:\" or \"FAILURE:\".: case: %s, line: %s",
                            name, line));
                }

                // remove after first colon, and parse the order
                final String orderPart = line.split(":", 2)[1];
                final Order order = parseOrder(orderPart, previousTS, false);

                // was order a convoyed move? because then we have to add a
                // convoyed move result.
                //
                Collection<OrderResult> rv = new ArrayList<>();
                if (order instanceof Move) {
                    final Move mv = (Move) order;
                    if (mv.isConvoying()) {
                        // NOTE: we cheat; path src/dest ok, middle is == src
                        final Province[] path = new Province[3];
                        path[0] = mv.getSource().getProvince();
                        path[1] = path[0];
                        path[2] = mv.getDest().getProvince();
                        rv.add(new ConvoyPathResult(order, path));
                    }
                }

                // create/add order result
                rv.add(new OrderResult(order, ordResultType, " (prestate)"));
                return rv.stream();
            }).collect(Collectors.toList());


            // add results to previous turnstate
            previousTS.setResultList(results);

            // add positions/ownership/orders to current turnstate
            //
            // add orders, first clearing any existing orders in the turnstate
            currentTS.clearAllOrders();
            for (final Order order : orders) {
                final List orderList = currentTS.getOrders(order.getPower());
                orderList.add(order);
                currentTS.setOrders(order.getPower(), orderList);
            }

            // get position
            final Position position = currentTS.getPosition();

            // ensure all powers are active
            for (final Power power : world.getMap().getPowers()) {
                position.setEliminated(power, false);
            }

            // Add non-dislodged units
            for (final Order aPreState : preState) {
                final Unit unit = new Unit(aPreState.getPower(),
                        aPreState.getSourceUnitType());
                unit.setCoast(aPreState.getSource().getCoast());
                position.setUnit(aPreState.getSource().getProvince(), unit);
            }

            // Add dislodged units
            for (final Order aPreDislodged : preDislodged) {
                final Unit unit = new Unit(aPreDislodged.getPower(),
                        aPreDislodged.getSourceUnitType());
                unit.setCoast(aPreDislodged.getSource().getCoast());
                position.setDislodgedUnit(
                        aPreDislodged.getSource().getProvince(), unit);
            }

            // Set supply center owners
            // if we have ANY supply center owners, we erase the template
            // if we do not have any, we assume the template is correct
            // no need to validate units
            if (!supplySCOwners.isEmpty()) {
                // first erase old info

                for (final Province province : position.getProvinces()) {
                    if (position.hasSupplyCenterOwner(province)) {
                        position.setSupplyCenterOwner(province, null);
                    }
                }

                // add new info
                for (final Order supplySCOwner : supplySCOwners) {
                    position.setSupplyCenterOwner(
                            supplySCOwner.getSource().getProvince(),
                            supplySCOwner.getPower());
                }
            }
        }// Case()


        public String getName() {
            return name;
        }

        public List<Order> getPreState() {
            return Collections.unmodifiableList(preState);
        }

        public List<Order> getPostState() {
            return Collections.unmodifiableList(postState);
        }

        public List<Order> getPreDislodged() {
            return Collections.unmodifiableList(preDislodged);
        }

        public List<Order> getPostDislodged() {
            return Collections.unmodifiableList(postDislodged);
        }

        public List<Order> getOrders() {
            return Collections.unmodifiableList(orders);
        }

        public List<OrderResult> getResults() {
            return Collections.unmodifiableList(results);
        }

        public TurnState getCurrentTurnState() {
            return currentTS;
        }

        public TurnState getPreviousTurnState() {
            return previousTS;
        }

        private Order parseOrder(final String s, final TurnState ts,
                                 final boolean isDefineState) {
            try {
                // no guessing (but not locked); we must ALWAYS specify the power.
                final Order o = of
                        .parse(OrderFactory.getDefault(), s, null, ts, false,
                                false);

                if (isDefineState) {
                    if (o instanceof DefineState) {
                        // we just want to check if the DefineState order does not have
                        // an undefined coast for a fleet unit.
                        final Location newLoc = o.getSource()
                                .getValidatedSetup(o.getSourceUnitType());

                        // create a new DefineState with a validated loc
                        return OrderFactory.getDefault()
                                .createDefineState(o.getPower(), newLoc,
                                        o.getSourceUnitType());
                    } else {
                        throw new IllegalArgumentException(
                                "A DefineState order is required here.");
                    }
                }

                return o;
            } catch (final OrderException e) {
                throw new IllegalArgumentException(
                        String.format("Case: %s, failure line: %s", name, s),
                        e);
            }
        }// parseOrder()

    }// class Case


    private void parseCaseFile(final Path caseFile) {
        try (BufferedReader br = Files.newBufferedReader(caseFile)) {
            // zip!!!
            final List<String> lines = br.lines()
                    .map(rawLine -> rawLine.trim().replaceAll("#.*", "")
                            .toLowerCase()).collect(Collectors.toList());
            final Queue<Pair<Integer, String>> tokens = new LinkedList<>(
                    IntStream.range(0, lines.size())
                            .mapToObj(i -> new Pair<>(i, lines.get(i)))
                            .filter(p -> !p.getValue().isEmpty())
                            .collect(Collectors.toList()));
            final Pair<Integer, String> head = tokens.poll();
            if (head == null) {
                throw new IllegalArgumentException("Unexpected EOF");
            }
            if (!getKeyType(head.getValue()).get().equals(VARIANT_ALL)) {
                throw new IllegalArgumentException(
                        "Before cases are defined, the variant must be set with the VARIANT_ALL flag.");
            }
            initVariant(getAfterKeyword(head.getValue()));
            cases.addAll(parseCase(tokens));
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private LinkedList<Case> parseCase(
            final Queue<Pair<Integer, String>> tokens) {
        final LinkedList<Case> caseList = new LinkedList<>();
        while (true) {
            final Pair<Integer, String> head = tokens.poll();
            if (head == null) { // EOF
                break;
            }
            final String line = head.getValue();
            if (!getKeyType(line).get().equals(CASE)) {
                throw new IllegalArgumentException(
                        String.format("Not expected line [%d:%s]",
                                head.getKey(), head.getValue()));
            }
            final String caseName = getAfterKeyword(line);
            String phaseName = null;
            final Map<String, LinkedList<String>> keyMap = KEY_TYPES_WITH_LIST
                    .stream().collect(Collectors.toMap(Function.identity(),
                            key -> new LinkedList<>()));
            while (true) {
                final Pair<Integer, String> head1 = tokens.poll();
                if (head1 == null) {
                    throw new IllegalArgumentException("Unexpected EOF");
                }
                final String line1 = head1.getValue();
                final Optional<String> currentKey = getKeyType(line1);
                if (!currentKey.isPresent()) {
                    throw new IllegalArgumentException(
                            String.format("Key is not defined. [%d:%s]",
                                    head.getKey(), head.getValue()));
                }
                final String key = currentKey.get();
                if (key.equals(END)) {
                    break;
                } else if (key.equals(PRESTATE_SETPHASE)) {
                    phaseName = getAfterKeyword(line1);
                } else if (key.equals(POSTSTATE_SAME)) {
                    final List<String> list = keyMap.get(POSTSTATE);
                    list.addAll(keyMap.get(PRESTATE));
                } else {
                    final Deque<String> list = keyMap.get(key);
                    list.addAll(parseStatus(tokens));
                }
            }

            final Case aCase = new Case(world, templateTurnState, caseName,
                    phaseName, keyMap.get(PRESTATE),        // prestate
                    keyMap.get(ORDERS),            // orders
                    keyMap.get(POSTSTATE),
                    // poststate
                    keyMap.get(PRESTATE_SUPPLYCENTER_OWNERS),
                    // pre-state: sc owners
                    keyMap.get(PRESTATE_DISLODGED),
                    // pre-dislodged
                    keyMap.get(POSTSTATE_DISLODGED),
                    // post-dislodged
                    keyMap.get(PRESTATE_RESULTS)
                    // results (of prior phase)
            );
            caseList.add(aCase);
        }
        return caseList;
    }

    private static Collection<String> parseStatus(
            final Queue<Pair<Integer, String>> tokens) {
        final List<String> list = new LinkedList<>();
        while (true) {
            final Pair<Integer, String> head = tokens.peek();
            if (head == null) {
                throw new IllegalArgumentException("Unexpected EOF");
            }
            if (getKeyType(head.getValue()).isPresent()) {
                return list;
            }
            list.add(tokens.remove().getValue());
        }

    }


    // find first space this works, because the
    // preceding whitespace before a keyword has already been trimmed
    private static String getAfterKeyword(final String in) {
        final String[] tokens = in.split("[ \t]", 2);
        return tokens.length < 2 ? null : tokens[1];
    }// getAfterKeyword()


    /*
        returns:
            true key type type
    */
    private static Optional<String> getKeyType(final String line) {
        if (line != null && line.isEmpty()) {
            return Optional.empty();
        }
        if (line.startsWith(CASE)) {
            return Optional.of(CASE);
        }
        if (line.startsWith(END)) {
            return Optional.of(END);
        }
        return KEY_TYPES_OTHER.stream().filter(line::startsWith).findFirst();
    }// getKeyType()


}// class TestSuite

