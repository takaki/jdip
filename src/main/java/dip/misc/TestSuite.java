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
package dip.misc;

import dip.order.*;
import dip.order.result.ConvoyPathResult;
import dip.order.result.OrderResult;
import dip.order.result.OrderResult.ResultType;
import dip.order.result.Result;
import dip.process.StdAdjudicator;
import dip.world.*;
import dip.world.variant.VariantManager;
import dip.world.variant.data.Variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map;


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
    private static final String[] KEY_TYPES_OTHER = {ORDERS, POSTSTATE_SAME, PRESTATE_SETPHASE, PRESTATE_RESULTS, PRESTATE_SUPPLYCENTER_OWNERS, PRESTATE_DISLODGED, POSTSTATE_DISLODGED, PRESTATE, POSTSTATE, VARIANT_ALL};

    private static final String[] KEY_TYPES_WITH_LIST = {ORDERS, PRESTATE_SUPPLYCENTER_OWNERS, PRESTATE_RESULTS, PRESTATE_DISLODGED, POSTSTATE_DISLODGED, POSTSTATE, PRESTATE};


    private static float parseTime = -1;
    private static final String VARIANT_DIR = "variants";

    private Map<String, LinkedList<String>> keyMap = null;


    private static boolean isAdjudicatorLogged = true;
    private static boolean isLogging = true;
    private static boolean isPerfTest = false;
    private static boolean isRegression = false;

    private static String inFileName = null;

    private List<Case> cases = new ArrayList<>(10);
    private World world = null;
    private TurnState templateTurnState;
    private StdAdjudicator stdJudge = null;
    private List<String> failedCaseNames = new ArrayList<>(10);
    private static int benchTimes = 1;

    // VARIANT_ALL name
    private static String variantName = null;


    /**
     * Start the TestSuite
     */
    public static void main(final String[] args) {
        if (args.length < 1 || args.length > 2) {
            printUsageAndExit();
        }

        if (args.length == 2) {
            inFileName = args[1];

            final String firstArg = args[0].trim().toLowerCase();
            if (firstArg.startsWith("-perftest")) {
                isLogging = false;
                isAdjudicatorLogged = false;
                isPerfTest = true;
                if (firstArg.indexOf(":") != -1) {
                    benchTimes = getTimes(firstArg);
                } else {
                    printUsageAndExit();
                }
            } else if (firstArg.equals("-brief")) {
                isAdjudicatorLogged = false;
            } else if (firstArg.equals("-statsonly")) {
                isAdjudicatorLogged = false;
                isPerfTest = false;
                isLogging = false;
            } else if (firstArg.equals("-regress")) {
                isAdjudicatorLogged = false;
                isPerfTest = false;
                isLogging = false;
                isRegression = true;
            } else {
                printUsageAndExit();
            }
        } else {
            inFileName = args[0];
        }


        if (isAdjudicatorLogged) {
        }

        final TestSuite ts = new TestSuite();

        println("TestSuite Results: (", new Date(), ")");
        println("=======================================================================");
        println("  test case file: ", inFileName);


        final File file = new File(inFileName);

        final long startTime = System.currentTimeMillis();
        ts.parseCases(file);
        parseTime = (System.currentTimeMillis() - startTime) / 1000f;
        println("  initialization complete.");
        println("  variant: ", variantName);

        ts.evaluate();
    }// main()

    private static void printUsageAndExit() {
        System.out.println(
                "USAGE: TestSuite [-statsonly | -perftest | -brief] <test-input-file>");
        System.out.println("  All log output to stdout");
        System.out.println(
                "  -statsonly      disable all logging; only show statistics");
        System.out.println(
                "  -perftest:n     no logging or statistics; repeat all cases n times");
        System.out.println(
                "  -brief          disable internal adjudicator logging");
        System.out.println(
                "  -regress        run test cases in infinite loop; no logging or stats.");
        System.out.println("");
        System.out.println("  Examples:");
        System.out.println("      java dip.misc.TestSuite datc.txt >out");
        System.out
                .println("      java dip.misc.TestSuite -brief datc.txt >out");
        System.out.println(
                "      java dip.misc.TestSuite -perftest:1000 case.txt >out");
        System.exit(1);
    }


    private static int getTimes(final String in) {
        final String s = in.substring(in.indexOf(':') + 1);
        int n = -1;
        try {
            n = Integer.parseInt(s);
        } catch (final NumberFormatException e) {
            System.err.println("ERROR: invalid argument: " + in);
            printUsageAndExit();
        }

        if (n <= 0) {
            System.err.println(
                    "Benchmark repitition out of range; must be greater than 0");
            printUsageAndExit();
        }

        return n;
    }// getTimes()


    private TestSuite() {
    }// TestSuite()


    private void initVariant() {
        try {
            // get default variant directory.
            File defaultVariantSearchDir = null;
            if (System.getProperty("user.dir") == null) {
                defaultVariantSearchDir = new File(".", VARIANT_DIR);
            } else {
                defaultVariantSearchDir = new File(
                        System.getProperty("user.dir"), VARIANT_DIR);
            }


            // load the default variant (Standard)
            // error if it cannot be found!!
            final Variant variant = new VariantManager()
                    .getVariant(variantName, VariantManager.VERSION_NEWEST).orElse(null);
            if (variant == null) {
                throw new Exception("Cannot find variant " + variantName);
            }

            // create the world
            world = WorldFactory.createWorld(variant);
            templateTurnState = world.getLastTurnState();
            world.removeTurnState(templateTurnState);

            // set the RuleOptions in the World (this is normally done
            // by the GUI)
            world.setRuleOptions(RuleOptions.createFromVariant(variant));
        } catch (final Exception e) {
            println("Init error: ", e);
            e.printStackTrace();
            System.exit(1);
        }
    }// init()


    private void evaluate() {
        int nOrders = 0;
        int nPass = 0;
        int nFail = 0;
        int nCases = 0;

        final List<String> unRezParadoxes = new LinkedList<>();

        final long startMillis = System.currentTimeMillis();    // start timing!

        // all cases in an array
        final Case[] allCases = (Case[]) cases.toArray(new Case[cases.size()]);

        if (isRegression) {
            // no stats are kept in regression mode, because we're in an
            // infinite loop.
            //
            int rCount = 0;
            System.out.print("Running cases in an infinite loop");

            while (true) {
                for (final Case currentCase : allCases) {
                    // world: setup
                    world.setTurnState(currentCase.getCurrentTurnState());
                    world.setTurnState(currentCase.getPreviousTurnState());

                    stdJudge = new StdAdjudicator(OrderFactory.getDefault(),
                            currentCase.getCurrentTurnState());
                    stdJudge.process();

                    // cleanup: remove turnstates from world
                    world.removeAllTurnStates();

                    // cleanup: clear results in currentTurnSTate
                    // this is absolutely essential!!
                    currentCase.getCurrentTurnState().getResultList().clear();

                    // print a '.' every 1000 iterations
                    rCount++;
                    if (rCount == 1000) {
                        System.out.print('.');
                        rCount = 0;
                    }
                }
            }
        } else if (isPerfTest) {
            // performance mode. We need to track stats here,
            // but there is no logging or output except stats.
            //
            for (int i = 0; i < benchTimes; i++) {
                for (final Case currentCase : allCases) {
                    // world: setup
                    world.setTurnState(currentCase.getCurrentTurnState());
                    world.setTurnState(currentCase.getPreviousTurnState());

                    nOrders += currentCase.getOrders().length;

                    // adjudicate
                    // we don't check results when in performance mode.
                    //
                    stdJudge = new StdAdjudicator(OrderFactory.getDefault(),
                            currentCase.getCurrentTurnState());
                    stdJudge.process();

                    nCases++;

                    // cleanup: remove turnstates from world
                    world.removeAllTurnStates();

                    // cleanup: clear results in currentTurnSTate
                    // this is absolutely essential!!
                    currentCase.getCurrentTurnState().getResultList().clear();
                }
            }
        } else {
            // 'typical' mode (testing).
            // we keep stats and may or may not have logging
            //
            for (final Case currentCase : allCases) {
                // world: setup
                world.setTurnState(currentCase.getCurrentTurnState());
                world.setTurnState(currentCase.getPreviousTurnState());

                // print case name
                println("\n\n");
                println("=CASE==================================================================");
                println("  ", currentCase.getName());

                // print pre-state
                printState(currentCase);

                // print orders
                println("=ORDERS================================================================");
                printOrders(currentCase);
                nOrders += currentCase.getOrders().length;

                // adjudicate
                println("=ADJUDICATION==========================================================");

                stdJudge = new StdAdjudicator(OrderFactory.getDefault(),
                        currentCase.getCurrentTurnState());
                stdJudge.process();

                // print adjudication results, if not performance testing
                // also print & check post conditions, if not performance testing
                if (!isAdjudicatorLogged) {
                    println("  [adjudicator logging disabled]");
                }

                // add unresolved paradoxes to list, so we know which cases they are
                if (stdJudge.isUnresolvedParadox()) {
                    unRezParadoxes.add(currentCase.getName());
                }

                println("=ADJUDICATION RESULTS==================================================");
                if (stdJudge.getNextTurnState() == null) {
                    println("=NEXT PHASE: NONE. Game has been won.");
                } else {
                    println("=NEXT PHASE: ",
                            stdJudge.getNextTurnState().getPhase());
                }
                final List<Result> resultList = stdJudge.getTurnState()
                        .getResultList();
                final Iterator<Result> resultIter = resultList.iterator();
                while (resultIter.hasNext() && isLogging) {
                    final Result r = resultIter.next();
                    println("  ", r);
                }

                // check post conditions
                println("=POST-STATE============================================================");

                if (compareState(currentCase, stdJudge.getNextTurnState())) {
                    nPass++;
                } else {
                    nFail++;
                    failedCaseNames.add(currentCase.getName());
                }

                println("=======================================================================");

                nCases++;

                // cleanup: remove turnstates from world
                world.removeAllTurnStates();

                // cleanup: clear results in currentTurnSTate
                // this is absolutely essential!!
                currentCase.getCurrentTurnState().getResultList().clear();
            }
        }


        // print stats
        //
        final long time = System.currentTimeMillis() - startMillis;    // end timing!
        println("End: ", new Date());

        // total time: includes setup/adjudication/comparison
        final float orderTime = (float) time / (float) nOrders;
        final float thruPut = 1000.0f / orderTime;
        final float score = (float) nPass / (float) nCases * 100.0f;

        println("\nFailed Cases:");
        println("=============");
        Iterator<String> iter = failedCaseNames.iterator();
        while (iter.hasNext()) {
            println("   ", iter.next());
        }
        println("   [total: " + failedCaseNames.size() + "]");

        println("\nUnresolved Paradoxes:");
        println("=====================");
        iter = unRezParadoxes.iterator();
        while (iter.hasNext()) {
            println("   " + iter.next());
        }
        println("   [total: ", unRezParadoxes.size(), "]");

        // print to log
        println("\nStatistics:");
        println("===========");
        println("    Case parse time: " + parseTime + " seconds.");
        if (isPerfTest) {
            println("    " + nCases + " cases evaluated. Pass/Fail rate not available with -perftest option.");
            println("    Adjudication Performance for " + benchTimes + " iterations:");
        } else {
            println("    " + nCases + " cases evaluated. " + nPass + " passed, " + nFail + " failed; " + score + "%  pass rate.");
            println("    Times [includes setup, adjudication, and post-adjudication comparision]");
        }
        println("      " + nOrders + " orders processed in " + time + " ms; " + orderTime + " ms/order average");
        println("      Throughput: " + thruPut + " orders/second");

        // if in 'brief' mode, only print out summary statistics
        if (!isLogging) {
            System.out.println("\nStatistics for \"" + inFileName + "\":");
            System.out
                    .println("    Case parse time: " + parseTime + " seconds.");
            if (isPerfTest) {
                System.out.println(
                        "    " + nCases + " cases evaluated. Pass/Fail rate not available with -perftest option.");
                System.out.println(
                        "    Adjudication Performance for " + benchTimes + " iterations:");
            } else {
                System.out.println(
                        "    " + nCases + " cases evaluated. " + nPass + " passed, " + nFail + " failed; " + score + "% pass rate.");
                System.out.println(
                        "    Times [includes setup, adjudication, and post-adjudication comparision]");
            }
            System.out.println(
                    "      " + nOrders + " orders processed in " + time + " ms; " + orderTime + " ms/order average");
            System.out
                    .println("      Throughput: " + thruPut + " orders/second");

            if (isPerfTest) {
                printPerfStatsBrief(benchTimes, nOrders, time, thruPut);
            }
        }

        // exit
        System.exit(nFail);
    }// evaluate()

    /**
     * Briefly print performance stats for cut/paste
     */
    private void printPerfStatsBrief(final int nIter, final int nOrder, final float timeTotal,
                                     final float thruput) {
        final StringBuffer sb = new StringBuffer();
        sb.append("**\t");    // start line; asterisks

        // file name only [no path]
        final File file = new File(inFileName);
        sb.append(file.getName());
        sb.append("\t");

        // # of iterations
        sb.append(nIter);
        sb.append("\t");

        // # of orders
        sb.append(nOrder);
        sb.append("\t");

        // total time (ms)
        sb.append(timeTotal);
        sb.append("\t");

        // thruput (orders / second)
        sb.append(thruput);
        sb.append("\t");

        System.out.println(sb);
    }


    // prints state settings...
    private void printState(final Case c) {
        if (!isLogging) {
            return;
        }

        final TurnState turnState = c.getCurrentTurnState();
        //Position position = turnState.getPosition();

        println("=PHASE=================================================================");
        println("  ", turnState.getPhase());

        // if we have some results to display, for prior state, do that now.
        if (isLogging && c.getResults().length > 0) {
            // print
            println("=PRESTATE_RESULTS======================================================");
            println("  From ", c.getPreviousTurnState().getPhase());
            final OrderResult[] or = c.getResults();
            for (OrderResult anOr : or) {
                println("    ", anOr);
            }
        }


        // print non-dislodged units
        if (c.getPreState().length > 0) {
            println("=PRE-STATE=============================================================");
            final DefineState[] dsOrds = c.getPreState();
            for (DefineState dsOrd : dsOrds) {
                println("   ", dsOrd);
            }
        }

        // print dislodged units
        if (c.getPreDislodged().length > 0) {
            println("=PRE-STATE DISLODGED===================================================");
            final DefineState[] dsOrds = c.getPreDislodged();
            for (DefineState dsOrd : dsOrds) {
                println("   ", dsOrd);
            }
        }
    }// printState()


    /**
     * Prints the orders in a case
     */
    private void printOrders(final Case currentCase) {
        if (isLogging) {
            final Order[] orders = currentCase.getOrders();
            for (Order order : orders) {
                println("  ", order.toString());
            }

            if (orders.length == 0) {
                println("  [none]");
            }
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
    private boolean compareState(final Case c, final TurnState resolvedTS) {
        // special case: check for a win.
        if (resolvedTS == null) {
            println("The game has been won. No new TurnState object is created.");
            return true;
        }

        final Position pos = resolvedTS.getPosition();

        // create set of resolvedUnits
        //
        Set<UnitPos> resolvedUnits = new HashSet<>();

        Province[] provs = pos.getUnitProvinces().toArray(new Province[0]);
        for (Province prov1 : provs) {
            if (!resolvedUnits.add(new UnitPos(pos, prov1, false))) {
                throw new IllegalStateException(
                        "CompareState: Internal error (non dislodged)");
            }
        }

        provs = pos.getDislodgedUnitProvinces().toArray(new Province[0]);
        for (Province prov : provs) {
            if (!resolvedUnits.add(new UnitPos(pos, prov, true))) {
                throw new IllegalStateException(
                        "CompareState: Internal error (dislodged)");
            }
        }


        resolvedUnits = Collections
                .unmodifiableSet(resolvedUnits);    // for safety


        // create set of caseUnits
        //
        Set<UnitPos> caseUnits = new HashSet<>();

        DefineState[] dsOrds = c.getPostState();
        for (DefineState dsOrd1 : dsOrds) {
            if (!caseUnits.add(new UnitPos(dsOrd1, false))) {
                println("ERROR: duplicate POSTSTATE position: " + dsOrd1);
                return false;
            }
        }

        dsOrds = c.getPostDislodged();
        for (DefineState dsOrd : dsOrds) {
            if (!caseUnits.add(new UnitPos(dsOrd, true))) {
                println("ERROR: duplicate POSTSTATE_DISLODGED position: " + dsOrd);
                return false;
            }
        }
        caseUnits = Collections.unmodifiableSet(caseUnits);    // for safety

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
            println("  CompareState: FAILED: unit positions follow.");

            // print adds
            printSet(added, "+");

            // print subtracts
            printSet(missing, "-");

            // print units in correct position
            printSet(intersection, "=");

            return false;
        } else {
            println("  CompareState: PASSED");
        }

        return true;
    }// compareState()


    /**
     * Print all the UnitPos objects from a Set; prefixing with the given prefix
     */
    private void printSet(final Set<UnitPos> set, final String prefix) {
        final Iterator<UnitPos> iter = set.iterator();
        while (iter.hasNext()) {
            final UnitPos up = iter.next();

            String sb = "  " +
                    prefix +
                    " " +
                    up;

            println(sb);
        }
    }// printSet()


    /**
     * Private inner class, usually contained in Sets, that
     * is comparable, for determining if the end-state is
     * in fact correct.
     */
    private class UnitPos {
        private final Unit unit;            // owner/type/coast
        private final Province province;        // position
        private final boolean isDislodged;    // dislodged?

        /**
         * Create a UnitPos
         */
        public UnitPos(final DefineState ds, final boolean isDislodged) {
            unit = new Unit(ds.getPower(), ds.getSourceUnitType());
            unit.setCoast(ds.getSource().getCoast());
            province = ds.getSource().getProvince();
            this.isDislodged = isDislodged;
        }// UnitPos()

        /**
         * Create a UnitPos
         */
        public UnitPos(final Position pos, final Province prov, final boolean isDislodged) {
            province = prov;
            this.isDislodged = isDislodged;
            unit = isDislodged ? pos.getDislodgedUnit(prov).orElse(null) : pos
                    .getUnit(prov).orElse(null);
            if (unit == null) {
                throw new IllegalArgumentException();
            }
        }// UnitPos()

        /**
         * Print
         */
        public String toString() {
            final StringBuffer sb = new StringBuffer(32);
            sb.append(unit.getPower().getName());
            sb.append(' ');
            sb.append(unit.getType().getShortName());
            sb.append(' ');
            sb.append(province.getShortName());
            sb.append('/');
            sb.append(unit.getCoast().getAbbreviation());
            if (isDislodged) {
                sb.append(" [DISLODGED]");
            }
            return sb.toString();
        }// toString()

        /**
         * Compare
         */
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
        public int hashCode() {
            return 0;    // very very bad! just an easy shortcut
        }
    }// inner class UnitPos


    /**
     * Holds a Case
     */
    private final class Case {
        private DefineState[] preState = null;
        private DefineState[] postState = null;
        private DefineState[] preDislodged = null;
        private DefineState[] postDislodged = null;
        private DefineState[] supplySCOwners = null;    // all types are 'army'
        private OrderResult[] results = null;

        private Order[] orders = null;
        private String name;
        private Phase phase = null;
        private OrderParser of = null;
        private TurnState currentTS = null;
        private TurnState previousTS = null;

        // tsTemplate: template turnstate to create the current, and (if needed) previous
        // turnstates.
        public Case(final String name, final String phaseName, final List<String> pre, final List<String> ord,
                    final List<String> post, final List<String> supplySCOwnersList, final List<String> preDislodgedList,
                    final List<String> postDislodgedList, final List<String> orderResultList) {
            this.name = name;
            final List temp = new ArrayList<>(50);
            Iterator<String> iter;
            of = OrderParser.getInstance();


            // phase
            if (phaseName != null) {
                phase = Phase.parse(phaseName).orElse(null);
                if (phase == null) {
                    System.out.println("ERROR: case " + name);
                    System.out
                            .println("ERROR: cannot parse phase " + phaseName);
                    System.exit(1);
                }
            }

            // set phase to template phase, if no phase was assigned.
            phase = phaseName == null ? templateTurnState.getPhase() : phase;

            // setup current turnstate from template
            // use phase, if appropriate.
            currentTS = new TurnState(phase);
            currentTS.setPosition(
                    templateTurnState.getPosition().cloneExceptUnits());
            currentTS.setWorld(world);

            // setup previous phase, in case we need it.
            previousTS = new TurnState(phase.getPrevious());
            previousTS.setPosition(
                    templateTurnState.getPosition().cloneExceptUnits());
            previousTS.setWorld(world);

            // pre
            temp.clear();
            iter = pre.iterator();
            while (iter.hasNext()) {
                final String line = iter.next();
                final Order order = parseOrder(line, currentTS, true);
                temp.add(order);
            }
            preState = (DefineState[]) temp
                    .toArray(new DefineState[temp.size()]);


            // ord
            temp.clear();
            iter = ord.iterator();
            while (iter.hasNext()) {
                final String line = iter.next();
                final Order order = parseOrder(line, currentTS, false);
                temp.add(order);
            }
            orders = (Order[]) temp.toArray(new Order[temp.size()]);


            // post
            temp.clear();
            iter = post.iterator();
            while (iter.hasNext()) {
                final String line = iter.next();
                final Order order = parseOrder(line, currentTS, true);
                temp.add(order);
            }
            postState = (DefineState[]) temp
                    .toArray(new DefineState[temp.size()]);

            // prestate dislodged
            if (preDislodgedList != null) {
                temp.clear();
                iter = preDislodgedList.iterator();
                while (iter.hasNext()) {
                    final String line = iter.next();
                    final Order order = parseOrder(line, currentTS, true);
                    temp.add(order);
                }
                preDislodged = (DefineState[]) temp
                        .toArray(new DefineState[temp.size()]);
            }

            // poststate dislodged
            if (postDislodgedList != null) {
                temp.clear();
                iter = postDislodgedList.iterator();
                while (iter.hasNext()) {
                    final String line = iter.next();
                    final Order order = parseOrder(line, currentTS, true);
                    temp.add(order);
                }
                postDislodged = (DefineState[]) temp
                        .toArray(new DefineState[temp.size()]);
            }

            // supply-center owners
            if (supplySCOwnersList != null) {
                temp.clear();
                iter = supplySCOwnersList.iterator();
                while (iter.hasNext()) {
                    final String line = iter.next();
                    final Order order = parseOrder(line, currentTS, true);
                    temp.add(order);
                }
                supplySCOwners = (DefineState[]) temp
                        .toArray(new DefineState[temp.size()]);
            }


            // OrderResults
            //
            // THE BEST way to do this would be to setup the case, and then run
            // the adjudicator to get the results, checking the ajudicator results
            // against the 'prestate' positions. This way we would have all the same
            // results that the adjudicator would normally generate.
            //
            if (orderResultList != null) {
                temp.clear();
                iter = orderResultList.iterator();
                while (iter.hasNext()) {
                    String line = iter.next();
                    ResultType ordResultType = null;

                    // success or failure??
                    if (line.startsWith("success")) {
                        ordResultType = ResultType.SUCCESS;
                    } else if (line.startsWith("failure")) {
                        ordResultType = ResultType.FAILURE;
                    } else {
                        System.out.println("ERROR");
                        System.out.println("case: " + name);
                        System.out.println("line: " + line);
                        System.out.println(
                                "PRESTATE_RESULTS: must prepend orders with \"SUCCESS:\" or \"FAILURE:\".");
                        System.exit(1);
                    }

                    // remove after first colon, and parse the order
                    line = line.substring(line.indexOf(':') + 1);
                    final Order order = parseOrder(line, previousTS, false);

                    // was order a convoyed move? because then we have to add a
                    // convoyed move result.
                    //
                    if (order instanceof Move) {
                        final Move mv = (Move) order;
                        if (mv.isConvoying()) {
                            // NOTE: we cheat; path src/dest ok, middle is == src
                            final Province[] path = new Province[3];
                            path[0] = mv.getSource().getProvince();
                            path[1] = path[0];
                            path[2] = mv.getDest().getProvince();
                            temp.add(new ConvoyPathResult(order, path));
                        }
                    }


                    // create/add order result
                    temp.add(new OrderResult(order, ordResultType,
                            " (prestate)"));
                }
                results = (OrderResult[]) temp
                        .toArray(new OrderResult[temp.size()]);

                // add results to previous turnstate
                previousTS.setResultList(new ArrayList<>(temp));

                // add positions/ownership/orders to current turnstate
                //
                // add orders, first clearing any existing orders in the turnstate
                currentTS.clearAllOrders();
                for (Order order : orders) {
                    final List<Order> orderList = currentTS
                            .getOrders(order.getPower());
                    orderList.add(order);
                    currentTS.setOrders(order.getPower(), orderList);
                }

                // get position
                final Position position = currentTS.getPosition();

                // ensure all powers are active
                final Power[] powers = world.getMap().getPowers().toArray(new Power[0]);
                for (Power power : powers) {
                    position.setEliminated(power, false);
                }

                // Add non-dislodged units
                for (DefineState aPreState : preState) {
                    final Unit unit = new Unit(aPreState.getPower(),
                            aPreState.getSourceUnitType());
                    unit.setCoast(aPreState.getSource().getCoast());
                    position.setUnit(aPreState.getSource().getProvince(), unit);
                }

                // Add dislodged units
                for (DefineState aPreDislodged : preDislodged) {
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
                if (supplySCOwners.length > 0) {
                    // first erase old info
                    final Province[] provinces = position.getProvinces().toArray(new Province[0]);

                    for (final Province province : provinces) {
                        if (position.hasSupplyCenterOwner(province)) {
                            position.setSupplyCenterOwner(province, null);
                        }
                    }

                    // add new info
                    for (DefineState supplySCOwner : supplySCOwners) {
                        position.setSupplyCenterOwner(
                                supplySCOwner.getSource().getProvince(),
                                supplySCOwner.getPower());
                    }
                }
            }
        }// Case()


        public String getName() {
            return name;
        }

        public DefineState[] getPreState() {
            return preState;
        }

        public DefineState[] getPostState() {
            return postState;
        }

        public DefineState[] getPreDislodged() {
            return preDislodged;
        }

        public DefineState[] getPostDislodged() {
            return postDislodged;
        }

        public DefineState[] getSCOwners() {
            return supplySCOwners;
        }

        public Phase getPhase() {
            return phase;
        }

        public Order[] getOrders() {
            return orders;
        }

        public OrderResult[] getResults() {
            return results;
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
                Order o = of
                        .parse(OrderFactory.getDefault(), s, null, ts, false,
                                false);

                if (isDefineState) {
                    if (o instanceof DefineState) {
                        // we just want to check if the DefineState order does not have
                        // an undefined coast for a fleet unit.
                        final Location newLoc = o.getSource()
                                .getValidatedSetup(o.getSourceUnitType());

                        // create a new DefineState with a validated loc
                        o = OrderFactory.getDefault()
                                .createDefineState(o.getPower(), newLoc,
                                        o.getSourceUnitType());
                    } else {
                        throw new OrderException(
                                "A DefineState order is required here.");
                    }
                }

                return o;
            } catch (final OrderException e) {
                System.out.println("ERROR");
                System.out.println("parseOrder() OrderException: " + e);
                System.out.println("Case: " + name);
                System.out.println("failure line: " + s);
                System.exit(1);
            }
            return null;
        }// parseOrder()

    }// class Case


    // NEW case parser
    private void parseCases(final File caseFile) {
        BufferedReader br = null;

        // per case data that is NOT in List format
        String caseName = null;
        String phaseName = null;
        boolean inCase = false;        // we are in a CASE

        // setup reader
        try {
            br = new BufferedReader(new FileReader(caseFile));
        } catch (final IOException e) {
            System.out.println(
                    "ERROR: I/O error opening case file \"" + caseFile + "\"");
            System.out.println("EXCEPTION: " + e);
            System.exit(1);
        }

        try {
            String rawLine = br.readLine();
            String currentKey = null;
            int lineCount = 1;

            while (rawLine != null) {
                final String line = filterLine(rawLine);
                final String key = getKeyType(line);

                if (key != null) {
                    currentKey = key;
                }

                // only process non-null (after filtering)
                if (line != null) {
                    if (currentKey == null) {
                        // this can occur if a key is missing.
                        System.out.println("ERROR: missing a required key");
                        System.out
                                .println("Line " + lineCount + ": " + rawLine);
                        System.exit(1);
                    } else if (currentKey.equals(VARIANT_ALL)) {
                        // make sure nothing is defined yet
                        if (variantName == null) {
                            variantName = getAfterKeyword(line);
                        } else {
                            System.out.println(
                                    "ERROR: before cases are defined, the variant must");
                            System.out.println(
                                    "       be set with the VARIANT_ALL flag.");
                            System.exit(1);
                        }

                        // make sure we are not in a case!
                        if (inCase) {
                            System.out.println(
                                    "ERROR: VARIANT_ALL cannot be used within a CASE.");
                            System.exit(1);
                        }

                        // attempt to initialize the variant
                        initVariant();
                    } else if (currentKey.equals(CASE)) {
                        // begin a case; case name appears after keyword
                        //
                        // clear data
                        inCase = true;
                        clearAndSetupKeyMap();
                        phaseName = null;
                        currentKey = null;

                        // set case name
                        caseName = getAfterKeyword(line);

                        // make sure we have defined a variant!
                        if (variantName == null) {
                            System.out.println(
                                    "ERROR: before cases are defined, the variant must");
                            System.out.println(
                                    "       be set with the VARIANT_ALL flag.");
                            System.exit(1);
                        }
                    } else if (currentKey.equals(END)) {
                        // end a case
                        inCase = false;

                        // create the case
                        final Case aCase = new Case(caseName, phaseName,
                                getListForKeyType(PRESTATE),        // prestate
                                getListForKeyType(ORDERS),            // orders
                                getListForKeyType(POSTSTATE),
                                // poststate
                                getListForKeyType(PRESTATE_SUPPLYCENTER_OWNERS),
                                // pre-state: sc owners
                                getListForKeyType(PRESTATE_DISLODGED),
                                // pre-dislodged
                                getListForKeyType(POSTSTATE_DISLODGED),
                                // post-dislodged
                                getListForKeyType(PRESTATE_RESULTS)
                                // results (of prior phase)
                        );
                        cases.add(aCase);
                    } else {
                        if (inCase) {
                            if (currentKey.equals(POSTSTATE_SAME)) {
                                // just copy prestate data
                                final List<String> list = getListForKeyType(POSTSTATE);
                                list.addAll(getListForKeyType(PRESTATE));
                            } else if (currentKey.equals(PRESTATE_SETPHASE)) {
                                // phase appears after keyword
                                phaseName = getAfterKeyword(line);
                            } else if (key == null) // important: we don't want to add key lines to the lists
                            {
                                // we need to get a list.
                                final List<String> list = getListForKeyType(currentKey);
                                list.add(line);
                            }
                        } else {
                            System.out.println(
                                    "ERROR: line not enclosed within a CASE.");
                            System.out.println(
                                    "Line " + lineCount + ": " + rawLine);
                            System.exit(1);
                        }
                    }
                }

                rawLine = br.readLine();
                lineCount++;
            }// while()
        } catch (final IOException e) {
            println("ERROR: I/O error reading case file \"", caseFile, "\"");
            println("EXCEPTION: ", e);
            System.exit(1);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e2) {
                }
            }
        }

        println("  parsed " + cases.size() + " cases.");
    }// parseCases()


    // returns null if string is a comment line.
    private String filterLine(final String in) {
        // remove whitespace
        String out = in.trim();

        // find comment-character index, if it exists
        final int ccIdx = out.indexOf('#');

        // if entire line is a comment, or empty, return COMMENT_LINE now
        if (ccIdx == 0 || out.length() < 1) {
            return null;
        }

        // remove 'trailing' comments, if any
        // otherwise, it could interfere with order processing.
        if (ccIdx > 0) {
            out = out.substring(0, ccIdx);
        }

        // convert to lower case();
        out = out.toLowerCase();

        return out;
    }// filterLine

    // find first space this works, because the
    // preceding whitespace before a keyword has already been trimmed
    private String getAfterKeyword(final String in) {
        final int idxSpace = in.indexOf(' ');
        final int idxTab = in.indexOf('\t');

        if (idxSpace == -1 && idxTab == -1) {
            return null;
        }

        int idx;

        if (idxSpace == -1 || idxTab == -1) {
            idx = idxSpace > idxTab ? idxSpace : idxTab;        // return greater
        } else {
            idx = idxSpace < idxTab ? idxSpace : idxTab;        // return lesser
        }

        return in.substring(idx + 1);
    }// getAfterKeyword()


    private void clearAndSetupKeyMap() {
        if (keyMap == null) {
            keyMap = new HashMap<>(23);
        }

        keyMap.clear();

        for (String aKEY_TYPES_WITH_LIST : KEY_TYPES_WITH_LIST) {
            keyMap.put(aKEY_TYPES_WITH_LIST, new LinkedList<>());
        }
    }// setupKeyMap()


    /*
        returns:
            true key type type
    */
    private String getKeyType(final String line) {
        if (line == null) {
            return null;
        }

        if (line.startsWith(CASE)) {
            return CASE;
        } else if (line.startsWith(END)) {
            return END;
        } else {
            for (String aKEY_TYPES_OTHER : KEY_TYPES_OTHER) {
                if (line.startsWith(aKEY_TYPES_OTHER)) {
                    return aKEY_TYPES_OTHER;
                }
            }
        }

        return null;
    }// getKeyType()


    private List<String> getListForKeyType(final String keyType) {
        return keyMap.get(keyType);
    }// getListForKeyType()


    // fast internal logging
    // this allows just references to be passed. Although references are copied, this is a
    // fast operation in java. If logging is turned off, no string concatenation need be
    // performed.
    /*
        -perftest speed increased from 550 orders/second to about 700 orders/second. That's
		a HUGE speedup (~33%); this is just on the DATC.txt test case set.
	
	
	*/
    private static final void println(final String s1) {
        if (isLogging) {
            System.out.println(s1);
        }
    }

    private static final void println(final String s1, final int i1) {
        if (isLogging) {
            String sb = s1 + i1;
            System.out.println(sb);
        }
    }

    private static final void println(final String s1, final int i1, final String s2) {
        if (isLogging) {
            String sb = s1 +
                    i1 +
                    s2;
            System.out.println(sb);
        }
    }

    private static final void println(final String s1, final Object o2) {
        if (isLogging) {
            String sb = s1 + o2;
            System.out.println(sb);
        }
    }


    private static final void println(final String s1, final Object o2, final Object o3) {
        if (isLogging) {
            String sb = s1 +
                    o2 +
                    o3;
            System.out.println(sb);
        }
    }

    private static final void println(final String s1, final Object o2, final Object o3,
                                      final Object o4) {
        if (isLogging) {
            String sb = s1 +
                    o2 +
                    o3 +
                    o4;
            System.out.println(sb);
        }
    }

}// class TestSuite

