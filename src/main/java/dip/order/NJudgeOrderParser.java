// @(#)NJudgeOrderParser.java
//
// Copyright 2004 Zachary DelProposto. All rights reserved.
// Use is subject to license terms.
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;

import dip.misc.Utils;
import dip.order.result.DislodgedResult;
import dip.order.result.OrderResult;
import dip.order.result.OrderResult.ResultType;
import dip.order.result.Result;
import dip.order.result.SubstitutedResult;
import dip.world.Coast;
import dip.world.Location;
import dip.world.Phase.PhaseType;
import dip.world.Power;
import dip.world.Province;
import dip.world.Unit.Type;
import dip.world.WorldMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses nJudge-format orders into Orders and Results
 * <p>
 * This handles all 3 phases (Movement, Retreat, and Adjustment).
 * <p>
 * Please note that knowledge of the phase is required prior to
 * processing; for example, Retreat orders are used intead of Move
 * orders when the phase is PhaseType.RETREAT. Similarly, since
 * adjustment orders are in a format quite different from Movement
 * or Retreat phase orders, a similar hint PhaseType.ADJUSTMENT is
 * required.
 * <p>
 * Not all nJudge orders have a 1:1 mapping with jDip orders.
 * For example, jDip waive orders currently require a Province;
 * nJudge orders do not. Unusable waived builds and unusable pending
 * builds, as well as waived builds, are not converted to orders
 * but are denoted specifically in the returned NJudgeOrder.
 * An informative text Result describes this.
 * <p>
 * Note that DISLOGDGED order results do not have valid retreat locations
 * set. This cannot be done until further processing occurs.
 */
public final class NJudgeOrderParser {
    // regexes for adjustment-phase processing
    //
    // general regex
    private static final String ADJUSTMENT_REGEX = "(?i)^([\\p{Alnum}\\-\\_]+):.*(remove|build|default).*\\s(army|fleet|wing).*\\s(?:in\\sthe|over\\sthe|in|over)\\s+((.+))";

    // waived / unusable / pending regex
    private static final String ALTERNATE_ADJUSTMENT_REGEX = "(?i)^([\\p{Alnum}\\-\\_]+):\\s+(\\d*)\\s(unusable|unused)?.*(build).*((waived|pending)).*";

    // order tokens
    private static final String ORDER_HOLD = "HOLD";
    private static final String ORDER_MOVE = "->";
    private static final String ORDER_SUPPORT = "SUPPORT";
    private static final String ORDER_CONVOY = "CONVOY";
    private static final String ORDER_DISBAND = "DISBAND";
    private static final String ORDER_NO_ORDERS = "No";  // "England: Fleet Denmark, No Orders Processed."

    // all order tokens
    private static final String[] ORDER_NAME_TOKENS = {ORDER_HOLD, ORDER_MOVE, ORDER_SUPPORT, ORDER_CONVOY, ORDER_DISBAND, ORDER_NO_ORDERS};

    // unit delimiter array
    private static final String[] UNIT_DELIMS = {"army", "fleet", "wing"};

    // class variables
    private static final Pattern ADJUSTMENT_PATTERN = Pattern
            .compile(ADJUSTMENT_REGEX);
    private static final Pattern ALTERNATE_ADJUSTMENT_PATTERN = Pattern
            .compile(ALTERNATE_ADJUSTMENT_REGEX);


    // TEST harness
    /*
    public static void main(String args[])
	throws OrderException
	{
		final String variantName = "Chaos";
		World world = null;
		dip.world.Map map = null;
		
		// setup
		// get default variant directory. 
		final String VARIANT_DIR = "variants";
		File defaultVariantSearchDir = null;
		if(System.getProperty("user.dir") == null)
		{
			defaultVariantSearchDir = new File(".", VARIANT_DIR);
		}
		else
		{
			defaultVariantSearchDir = new File(System.getProperty("user.dir"), VARIANT_DIR );
		}
		
		try
		{
			// parse variants
			VariantManager.init(new File[]{defaultVariantSearchDir}, false);
			
			// load the default variant (Standard)
			// error if it cannot be found!!
			Variant variant = VariantManager.getVariant(variantName, VariantManager.VERSION_NEWEST);
			if(variant == null)
			{
				System.out.println("ERROR: cannot find variant: "+variantName);
				System.exit(1);
			}
			
			// create the world
			world = WorldFactory.getInstance().createWorld(variant);
			map = world.getMap();
			
			// set the RuleOptions in the World (this is normally done
			// by the GUI)
			world.setRuleOptions(RuleOptions.createFromVariant(variant));
		}
		catch(Exception e)
		{
			System.out.println("ERROR: could not create variant.");
			System.out.println(e);
			e.printStackTrace();
			System.exit(1);
		}
		
		
		
		// hold: OK
		// disband: OK
		// convoy: OK
		// support: OK
		// move: OK
		
		// ADJUST:
		String[] lines = 
		{
			"F-Bul: Removes the fleet in Bulgaria.",
			"F-Bul: Builds an army in Siberia.",
			"F-Bul: Removes the fleet in the Norwegian Sea.",
			"F-Bul: Remove fleet in the Norwegian Sea.",
			
			 "2-Spa:     Builds a fleet in Spain (south coast).",
			 "F-Bul:     Builds an army in Bulgaria.",
			 "H-Den:     Removes the fleet in the Norwegian Sea.",
			 "F-Bul:     Defaults, removing the fleet in the Black Sea.",
			 "F-Bul:     Defaults, removing the army in Siberia.",
			 "2-Spa:     Removes the wing over the St. Petersberg.",
			 "F-Bul:   	 Builds a wing in Sevastopol.",
			 
			"F-Bul:    	1 unusable build pending.",
			"2-Spa:     3 unusable builds pending.",
			"H-Den:     1 unused build pending.",
			"2-Spa:     2 unused builds pending.",
			"2-Spa:   	Build waived.",
			"F-Bul:    	1 unusable build waived.",
			"2-Spa:     3 unusable builds waived."
			 
		};
		
		// MOVE
		//String line = "A-Ank: Army Ankara -> Smyrna.  (*bounce, dislodged*)";
		//String line = "1-Smy: Army Constantinople -> Aegean Sea -> Greece.  (*bounce, dislodged*)";
		
		
		// HOLD / DISBAND
		//String line = "N-Lvp: Fleet Irish Sea HOLD.";
		//String line = "T-Nap:     Army  Naples DISBAND.";
		
		// CONVOY
		//String line = "1-Smy: Fleet Ionian Sea CONVOY Army Bulgaria -> Serbia.";
		//String line = "N-Lvp: Fleet Mid-Atlantic Ocean CONVOY Q-Mar Army Brest -> Spain.";
		
		// SUPPORTS
		//String line = "B-Bel: Army Belgium SUPPORT K-Hol Army Holland.";
		//String line = "H-Den: Fleet Berlin SUPPORT Army Kiel.";
		//String line = "N-Lvp: Fleet North Sea SUPPORT Fleet Barents Sea -> Norwegian Sea.";
		//String line = "2-Spa: Fleet Spain (south coast) SUPPORT W-Por Fleet Portugal -> Mid-Atlantic Ocean.";
		
		NJudgeOrderParser njp = new NJudgeOrderParser();
		
		for(int i=0; i<lines.length; i++)
		{
			System.out.println(">ORDER: "+lines[i]);
			NJudgeOrder njo = njp.parse(map, OrderFactory.getDefault(), Phase.PhaseType.ADJUSTMENT, lines[i]);
			System.out.println(">   "+njo);
		}
		
		System.out.println("DONE:");
	}
	*/


    /**
     * Create an NJudgeOrderParser
     */
    public NJudgeOrderParser() {
    }// NJudgeOrderParser()


    /**
     * Class that holds an Order and Text Results
     */
    public static class NJudgeOrder {
        private final Order order;
        private final List<Result> results;
        private final boolean isAdjustment;
        private final Power specialAdjustmentPower;
        private final boolean isWaive;
        private final int unusedPendingBuilds;
        private final int unusedPendingWaives;

        /**
         * Create an NJudgeOrder, which is an Orderable with
         * a dip.order.Result result(s).
         */
        public NJudgeOrder(final Order order, final List<Result> results,
                           final boolean isAdjustmentPhase) {
            this(order, results, isAdjustmentPhase, null, false, 0, 0);
        }// NJudgeOrder()

        /**
         * Create an NJudgeOrder
         */
        public NJudgeOrder(final Order order, final Result aResult,
                           final boolean isAdjustmentPhase) {
            this(order, createResultList(aResult), isAdjustmentPhase, null,
                    false, 0, 0);
        }// NJudgeOrder()

        /**
         * Create an Adjustment-phase NJudgeOrder for unused pending Builds,
         * or pending Waives, but not both.
         * Also creates a Result for this.
         */
        public NJudgeOrder(final Power power, final int unusedPendingBuilds,
                           final int unusedPendingWaives, final Result result) {
            this(null, createResultList(result), true, power, false,
                    unusedPendingBuilds, unusedPendingWaives);

            if (power == null || unusedPendingBuilds > 0 && unusedPendingWaives > 0 || unusedPendingBuilds < 0 || unusedPendingWaives < 0) {
                throw new IllegalArgumentException();
            }
        }// NJudgeOrder()


        /**
         * Create an Adjustment-phase NJudgeOrder for a Waived Build.
         * Also creates a Result for this.
         */
        public NJudgeOrder(final Power power, final Result result) {
            this(null, createResultList(result), true, power, true, 0, 0);
            Objects.requireNonNull(power);
        }// NJudgeOrder()

        /**
         * Create an NJudgeOrder
         */
        private NJudgeOrder(final Order order, final List<Result> results,
                            final boolean isAdjustment, final Power power,
                            final boolean isWaive,
                            final int unusedPendingBuilds,
                            final int unusedPendingWaives) {
            Objects.requireNonNull(results);

            this.order = order;
            this.results = Collections
                    .unmodifiableList(new ArrayList<>(results));
            this.isAdjustment = isAdjustment;
            specialAdjustmentPower = power;
            this.isWaive = isWaive;
            this.unusedPendingBuilds = unusedPendingBuilds;
            this.unusedPendingWaives = unusedPendingWaives;
        }// NJudgeOrder()


        /**
         * Returns the Order
         */
        public Order getOrder() {
            return order;
        }// getOrder()

        /**
         * Returns the Results of the order. Each item in the list
         * is a subclass of dip.order.Result.
         */
        public List<Result> getResults() {
            return Collections.unmodifiableList(results);
        }// getResults()


        /**
         * Returns true if this is an PhaseType.ADJUSTMENT phase order.
         */
        public boolean isAdjustmentPhase() {
            return isAdjustment;
        }// isAdjustmentPhase()


        /**
         * Returns the number of unused builds pending. This will be 0
         * if there are no unsed builds pending.
         * <p>
         * Note that this applies only to ADJUSTMENT phase orders. This will
         * throw an IllegalStateException if <code>!isAdjustmentPhase()</code>.
         */
        public int getUnusedPendingBuilds() {
            if (!isAdjustment) {
                throw new IllegalStateException();
            }

            return unusedPendingBuilds;
        }// getUnusedPendingBuilds()


        /**
         * Returns the number of unused builds waived. This will be 0
         * if no unused builds were waived.
         * <p>
         * Note that this applies only to ADJUSTMENT phase orders. This will
         * throw an IllegalStateException if <code>!isAdjustmentPhase()</code>.
         */
        public int getUnusedPendingWaives() {
            if (!isAdjustment) {
                throw new IllegalStateException();
            }

            return unusedPendingWaives;
        }// getUnusedPendingWaives()


        /**
         * Returns if a Build has been waived..
         * Because nJudge Waive orders do not support a Province,
         * and jDip Waive orders require a Province, Waive orders are not
         * (yet) fully supported.
         * <p>
         * If this is NOT a waived build, this will return <code>false</code>.
         * <p>
         * This will be removed in a future version, when jDip-style Waive
         * orders are fully supported.
         * <p>
         * Note that this applies only to ADJUSTMENT phase orders. This will
         * throw an IllegalStateException if <code>!isAdjustmentPhase()</code>.
         */
        public boolean isWaivedBuild() {
            if (!isAdjustment) {
                throw new IllegalStateException();
            }

            return isWaive;
        }// isWaivedBuild()


        /**
         * Returns the power for special adjustment orders, which are
         * obtained via the methods:
         * <ul>
         * <li><code>getUnusedPendingBuilds()</code></li>
         * <li><code>getUnusedPendingWaives()</code></li>
         * <li><code>isWaivedBuild()</code></li>
         * </ul>
         * This will throw an IllegalStateException() if we are not in the
         * adjustment phase. This will return <code>null</code> if we are
         * in the adjustment phase, but, none of isWaivedBuild() or
         * getUnusedXXX() methods have been set.
         */
        public Power getAdjustmentPower() {
            if (!isAdjustment) {
                throw new IllegalStateException();
            }

            return specialAdjustmentPower;
        }// getAdjustmentPower()


        /**
         * For debugging only
         */
        public String toString() {
            final StringBuffer sb = new StringBuffer(256);
            sb.append(getClass().getName());
            sb.append("[");
            sb.append(order);

            sb.append(";results=");
            sb.append(results);

            sb.append(",isAdjust=");
            sb.append(isAdjustment);

            sb.append(",adjPower=");
            sb.append(specialAdjustmentPower);

            sb.append(",waive=");
            sb.append(isWaive);

            sb.append(",unusedPendingBuilds=");
            sb.append(unusedPendingBuilds);

            sb.append(",unusedPendingWaives=");
            sb.append(unusedPendingWaives);

            sb.append("]");
            return sb.toString();
        }
    }// nested class NJudgeOrder


    /**
     * Parse a single line order.
     * <p>
     * Null arguments are not permitted, except for phaseType.
     * If phaseType is Phase.PhaseType.RETREAT, "Move" format orders will
     * be made into Retreat orders, and convoyed moves will be disallowed.
     */
    public NJudgeOrder parse(final WorldMap map,
                             final OrderFactory orderFactory,
                             final PhaseType phaseType,
                             final String line) throws OrderException {
        // create order parsing context
        final ParseContext pc = new ParseContext(map, orderFactory, phaseType,
                line);

        // parse results. This also removes the trailing '.' from the order
        final List<String> resultList = new ArrayList<>(5);
        final String newOrderLine = removeTrailingDot(
                parseResults(pc, resultList));

        if (PhaseType.ADJUSTMENT == phaseType) {
            return parseAdjustmentOrder(pc, newOrderLine);
        } else {
            // tokenize order
            final String[] tokens = tokenize(newOrderLine);

            // parse order prefix
            final OrderPrefix prefix = new OrderPrefix(pc, tokens);

            // parse predicate
            final Order order = parsePredicate(pc, prefix, tokens);

            // parse text results into real results
            final List<Result> results = createResults(pc, order, resultList);

            // create NJudgeOrder
            return new NJudgeOrder(order, results, pc.isAdjustmentPhase());
        }
    }// parse()


    /**
     * Tokenize input into whitespace-seperated strings.
     */
    private static String[] tokenize(final String input) {
        assert input != null;
        return input.trim().split("\\s+");
    }// tokenize()


    /**
     * Creates a List with a single result
     */
    private static List<Result> createResultList(final Result aResult) {
        final List<Result> list = new ArrayList<>(1);
        list.add(aResult);
        return list;
    }// createResultList()

    /**
     * Parse the rest of the order
     */
    private Order parsePredicate(final ParseContext pc, final OrderPrefix op,
                                 final String[] tokens) throws OrderException {
        final String type = op.orderName;

        if (Objects.equals(type, ORDER_HOLD) || Objects
                .equals(type, ORDER_DISBAND) || Objects
                .equals(type, ORDER_NO_ORDERS)) {
            return parseHoldOrDisband(pc, op, tokens, type);
        }
        if (Objects.equals(type, ORDER_MOVE)) {
            return parseMove(pc, op, tokens);
        }
        if (Objects.equals(type, ORDER_SUPPORT)) {
            return parseSupport(pc, op, tokens);
        }
        if (Objects.equals(type, ORDER_CONVOY)) {
            return parseConvoy(pc, op, tokens);
        }

        throw new IllegalStateException("unknown orderName type");
    }// parsePredicate()


    /**
     * All Orders begin with the following format:
     * <p>
     * POWER: UNIT LOCATION ORDERNAME
     * <p>
     * POWER: power name, followed by a ":"
     * UNIT: unit type (wing, army, fleet)
     * LOCATION: province name (may be multiple words), optionally
     * followed by a coast specifier
     * ORDERNAME: one of HOLD/SUPPORT/CONVOY/"->"/DISBAND
     */
    private class OrderPrefix {
        public final Power power;
        public final Type unit;
        public final Location location;
        public final String orderName;
        public final int tokenIndex;        // index at which to continue parsing

        /**
         * Parses the Prefix of an Order using the
         * tokenized input string.
         * <p>
         * Throws an exception if a processing error occurs.
         */
        public OrderPrefix(final ParseContext pc,
                           final String[] tokens) throws OrderException {
            String tok;
            int idx = 0;

            // search and parse power name; power name must be followed by a ":"
            tok = getToken(pc, idx, tokens);
            if (!tok.endsWith(":") || tok.length() <= 1) {
                throw new OrderException(
                        "Improper Power format: \"" + tok + "\" in order: " + pc.orderText);
            }

            final String powerNameText = tok.substring(0, tok.length() - 1);
            power = pc.map.getClosestPower(powerNameText).orElse(null);
            if (power == null) {
                throw new OrderException(
                        "Unknown Power: \"" + powerNameText + "\" in order: " + pc.orderText);
            }

            // search and parse unit type
            idx++;
            tok = getToken(pc, idx, tokens);
            unit = parseUnitType(pc, tok);


            // find the order name (order type)
            int tokIdx = idx;
            int orderNameIndex = -1;
            String tmpOrderName = null;
            while (tokIdx < tokens.length && orderNameIndex < 0) {
                final String aToken = tokens[tokIdx];
                for (final String ORDER_NAME_TOKEN : ORDER_NAME_TOKENS) {
                    if (ORDER_NAME_TOKEN.equals(aToken)) {
                        // error-check
                        orderNameIndex = tokIdx;
                        tmpOrderName = ORDER_NAME_TOKEN;
                        break;
                    }
                }

                tokIdx++;
            }

            tokenIndex = orderNameIndex + 1;
            orderName = tmpOrderName;

            if (orderNameIndex == -1) {
                throw new OrderException(
                        "Cannot determine order type for order: " + pc.orderText);
            }

            assert orderName != null;


            // increment index (points to token AFTER unit type)
            idx++;

            // parse location
            location = parseLocation(pc, idx, orderNameIndex, tokens);
        }// parseOrderPrefix()


    }// OrderPrefix


    /**
     * Parse a Location (or throw an OrderException) between the
     * given start and end tokens. Start is inclusive, end is exclusive.
     * <p>
     * This will never return null.
     */
    private static Location parseLocation(final ParseContext pc,
                                          final int start, final int end,
                                          final String[] tokens) throws OrderException {
        // check args
        if (start < 0 || end < 0 || end > tokens.length || start > end) {
            throw new IllegalArgumentException(
                    "invalid start/end: " + start + "," + end);
        }

        // parse location Name. This includes the province type (which may
        // be multiple tokens) and, optionally, the coast (also multiple
        // tokens). Periods are stripped.
        //
        final StringBuffer sb = new StringBuffer(64);
        for (int i = start; i < end; i++) {
            if (i > start) {
                sb.append(' ');
            }

            sb.append(tokens[i]);
        }

        return parseLocation(pc, sb.toString());
    }// parseLocation()


    /**
     * Parse a Location from the given text.
     * <p>
     * This will never return null.
     */
    private static Location parseLocation(final ParseContext pc,
                                          final String text) throws OrderException {
        final String replaceFrom[] = {".", ","};
        final String replaceTo[] = {"", ""};
        final Optional<String> locationText = Coast
                .normalize(Utils.replaceAll(text, replaceFrom, replaceTo));

        final Location loc = locationText.flatMap(pc.map::parseLocation)
                .orElse(null);

        if (loc == null) {
            throw new OrderException(
                    "Invalid location \"" + locationText + "\" in order: " + pc.orderText);
        }

        return loc;
    }// parseLocation()


    /**
     * Parse a Unit Type, throw an exception if not recognized.
     * Never returns null.
     */
    private static Type parseUnitType(final ParseContext pc,
                                      final String input) {
        return Type.parse(input);
    }// parseUnitType()


    /**
     * Parse a Power. Throw an exception if not recognized.
     * Never returns null.
     */
    private static Power parsePower(final ParseContext pc,
                                    final String input) throws OrderException {
        final Power power = pc.map.getPower(input);
        if (power == null) {
            throw new OrderException(
                    "Unknown power: \"" + input + "\" in order: " + pc.orderText);
        }

        return power;
    }// parseUnitType()


    /**
     * Parse a HOLD or DISBAND order/
     * <p>
     * Format: [prefix] (HOLD || DISBAND).
     */
    private Order parseHoldOrDisband(final ParseContext pc,
                                     final OrderPrefix op,
                                     final String[] tokens, final String type) {
        // NO additional parsing
        //
        if (Objects.equals(type, ORDER_HOLD)) {
            return pc.orderFactory.createHold(op.power, op.location, op.unit);
        }
        if (Objects.equals(type, ORDER_DISBAND)) {
            return pc.orderFactory
                    .createDisband(op.power, op.location, op.unit);
        }
        if (Objects.equals(type, ORDER_NO_ORDERS)) {
            // FIXME: create own order type for "No Orders Processed"
            return pc.orderFactory.createHold(op.power, op.location, op.unit);
        }

        throw new IllegalStateException("expected HOLD or DISBAND");
    }// parseHoldOrDisband()


    private Order parseMove(final ParseContext pc, final OrderPrefix op,
                            final String[] tokens) throws OrderException {
        /*

			3-StP: Army St Petersburg -> Moscow.  (*bounce*)
			
			1-Smy: Army Constantinople -> Aegean Sea -> Greece.		
			
			
			keep parsing Locations until END or a -> is reached.
			so we keep looking for a -> until no more are found.
			
			if we have more than one, we'll add them to a list
			and then add that to the order Move order.
		*/

        final LinkedList<Province> pathList = new LinkedList<>();
        int idx = op.tokenIndex;
        int movTokIdx = findNextMoveToken(idx, tokens);

        while (movTokIdx != -1) {
            final Location loc = parseLocation(pc, idx, movTokIdx, tokens);
            pathList.addLast(loc.getProvince());

            idx = movTokIdx + 1;
            movTokIdx = findNextMoveToken(idx, tokens);
        }

        // add last location
        final Location loc = parseLocation(pc, idx, tokens.length, tokens);
        pathList.addLast(loc.getProvince());

        // create Move order
        if (pathList.size() == 1) {
            if (pc.isRetreatPhase()) {
                return pc.orderFactory
                        .createRetreat(op.power, op.location, op.unit, loc);
            } else {
                return pc.orderFactory
                        .createMove(op.power, op.location, op.unit, loc);
            }
        } else if (pathList.size() > 1) {
            if (pc.isRetreatPhase()) {
                throw new OrderException(
                        "Convoyed Retreat orders are not allowed. Order: " + pc.orderText);
            }

            // add source location at beginning of move list
            pathList.addFirst(op.location.getProvince());

            final Province[] route = pathList
                    .toArray(new Province[pathList.size()]);

            return pc.orderFactory
                    .createMove(op.power, op.location, op.unit, loc, route);
        } else {
            // this probably will not occur....
            throw new OrderException(
                    "Invalid movement path in Move order: " + pc.orderText);
        }
    }// parseMove()


    /**
     * Finds the next "->" (ORDER_MOVE) token index; returns -1 if
     * none is found.
     */
    private static int findNextMoveToken(final int startIndex,
                                         final String[] tokens) {
        if (startIndex < 0) {
            throw new IllegalArgumentException();
        }

        for (int i = startIndex; i < tokens.length; i++) {
            if (ORDER_MOVE.equals(tokens[i])) {
                return i;
            }
        }

        return -1;
    }// findNextMoveToken()


    private Order parseSupport(final ParseContext pc, final OrderPrefix op,
                               final String[] tokens) throws OrderException {
        /*

		"B-Bel: Army Belgium SUPPORT K-Hol Army Holland.";
		"H-Den: Fleet Berlin SUPPORT Army Kiel.";
		
		
		
		"N-Lvp: Fleet North Sea SUPPORT Fleet Barents Sea -> Norwegian Sea.";
		"2-Spa: Fleet Spain (south coast) SUPPORT W-Por Fleet Portugal -> Mid-Atlantic Ocean.";
		
		if a "->" is NOT found, we are supporting a hold
		otherwise, supproting a move.
		
		POWER / UNITTYPE parsing as in CONVOY order.
		
		*/
        // token-index
        int idx = op.tokenIndex;

        Power supPower = null;

        // parse next token; may be a power (or power adjective), or null
        final String[] toks = getTokenUpto(pc, idx, tokens, UNIT_DELIMS);
        if (toks != null) {
            // conjugate strings before parsing with getPower()
            final StringBuffer sb = new StringBuffer(64);
            sb.append(toks[0]);
            for (int i = 1; i < toks.length; i++) {
                sb.append(' ');
                sb.append(toks[i]);
            }

            final String supPowerName = sb.toString();
            supPower = pc.map.getPower(supPowerName);

            // increment index appropriately
            idx += toks.length;

            // if toks is not null, we should have a valid power
            if (supPower == null) {
                throw new OrderException(
                        "Unrecognized Possesive Power \"" + supPowerName + "\" in order: " + pc.orderText);
            }
        }

        // toks was null; thus, next token should be a unit.
        final String tok = getToken(pc, idx, tokens);
        final Type supUnit = parseUnitType(pc, tok);

        // now parsing at token AFTER unit type token
        idx++;

        // now, find the "->" delimiter, if any.
        int delimIdx = -1;
        for (int i = idx; i < tokens.length; i++) {
            if (ORDER_MOVE.equals(tokens[i])) {
                delimIdx = i;
                break;
            }
        }

        // finish parsing order
        if (delimIdx == -1) {
            // support for a unit in place
            //
            // parse the support src
            final Location supSrc = parseLocation(pc, idx, tokens.length,
                    tokens);

            // create the support order
            return pc.orderFactory
                    .createSupport(op.power, op.location, op.unit, supSrc,
                            supPower, supUnit);
        } else {
            // support for a moving unit
            //
            // parse the support src
            final Location supSrc = parseLocation(pc, idx, delimIdx, tokens);

            // parse the support dest, if any
            if (delimIdx + 1 >= tokens.length) {
                throw new OrderException(
                        "Missing support destination in order: " + pc.orderText);
            }

            final Location supDest = parseLocation(pc, delimIdx + 1,
                    tokens.length, tokens);

            // create the support order
            return pc.orderFactory
                    .createSupport(op.power, op.location, op.unit, supSrc,
                            supPower, supUnit, supDest);
        }
    }// parseSupport()

    /**
     * parse a Convoy order:
     * PREFIX CONVOY [power] unit location -> location
     * remember, power is optional!
     */
    private Order parseConvoy(final ParseContext pc, final OrderPrefix op,
                              final String[] tokens) throws OrderException {
        /*

		"1-Smy: Fleet Ionian Sea CONVOY 			Army Bulgaria -> Serbia.";
		"N-Lvp: Fleet Mid-Atlantic Ocean CONVOY 	Q-Mar Army Brest -> Spain.";
		
		*/

        // token-index
        int idx = op.tokenIndex;

        Power convoyPower = null;

        // parse next token; may be a power (or power adjective), or null
        final String[] toks = getTokenUpto(pc, idx, tokens, UNIT_DELIMS);
        if (toks != null) {
            // conjugate strings before parsing with getPower()
            final StringBuffer sb = new StringBuffer(64);
            sb.append(toks[0]);
            for (int i = 1; i < toks.length; i++) {
                sb.append(' ');
                sb.append(toks[i]);
            }

            final String convoyPowerName = sb.toString();
            convoyPower = pc.map.getPower(convoyPowerName);

            // increment index appropriately
            idx += toks.length;

            // if toks is not null, we should have a valid power
            if (convoyPower == null) {
                throw new OrderException(
                        "Unrecognized Possesive Power \"" + convoyPowerName + "\" in order: " + pc.orderText);
            }
        }

        // toks was null; thus, next token should be a unit.
        final String tok = getToken(pc, idx, tokens);
        final Type convoyUnit = parseUnitType(pc, tok);

        // now parsing at token AFTER unit type token
        idx++;

        // now, find the "->" delimiter. There should be only one.
        int delimIdx = -1;
        for (int i = idx; i < tokens.length; i++) {
            if (ORDER_MOVE.equals(tokens[i])) {
                delimIdx = i;
                break;
            }
        }

        if (delimIdx == -1) {
            throw new OrderException(
                    "Missing \"->\" in Convoy order: " + pc.orderText);
        }


        // parse the convoy src
        final Location convoySrc = parseLocation(pc, idx, delimIdx, tokens);

        // parse the convoy dest
        if (delimIdx + 1 >= tokens.length) {
            throw new OrderException(
                    "Missing convoy destination in order: " + pc.orderText);
        }

        final Location convoyDest = parseLocation(pc, delimIdx + 1,
                tokens.length, tokens);


        // create the convoy order
        return pc.orderFactory
                .createConvoy(op.power, op.location, op.unit, convoySrc,
                        convoyPower, convoyUnit, convoyDest);
    }// parseConvoy()

    /**
     * Checks to see if index is within bounds of token length.
     * If not, throws an OrderException. If so, return the
     * token at that index.
     */
    private static String getToken(final ParseContext pc, final int index,
                                   final String[] tokens) throws OrderException {
        if (index < 0) {
            throw new IllegalArgumentException();
        }

        if (index >= tokens.length) {
            throw new OrderException("Truncated order: " + pc.orderText);
        }

        return tokens[index];
    }// getToken()


    /**
     * Checks to see if index is within bounds of token length,
     * as getToken does.
     * <p>
     * If the delimiter is found, it will return all tokens upto
     * the delimiter. If the delimiter is found at the index,
     * 'null' will be returned. If no delimiter is found, an
     * exception is thrown.
     */
    private static String[] getTokenUpto(final ParseContext pc, final int index,
                                         final String[] tokens,
                                         final String[] delim) throws OrderException {
        if (index < 0) {
            throw new IllegalArgumentException();
        }

        if (index >= tokens.length) {
            throw new OrderException("Truncated order: " + pc.orderText);
        }

        // is delim at start? if so, return null.
        String tok = tokens[index];
        for (final String aDelim1 : delim) {
            if (tok.equalsIgnoreCase(aDelim1)) {
                return null;
            }
        }

        final ArrayList<String> al = new ArrayList<>(3);
        al.add(tok);

        boolean foundDelim = false;
        for (int i = index + 1; i < tokens.length; i++) {
            tok = tokens[i];

            for (final String aDelim : delim) {
                if (tok.equalsIgnoreCase(aDelim)) {
                    foundDelim = true;
                    break;
                }
            }

            if (foundDelim) {
                break;
            } else {
                al.add(tok);
            }
        }


        if (!foundDelim) {
            throw new OrderException("Truncated order: " + pc.orderText);
        }

        assert !al.isEmpty();
        return al.toArray(new String[al.size()]);
    }// getTokenUpto()


    /**
     * Finds and removes all text between (and including)
     * the "(*" and "*)".
     * <p>
     * <p>
     * Sets each result (comma-delim) in the given list.
     * <p>
     * Returns the cleaned-up order text.
     */
    private static String parseResults(final ParseContext pc,
                                       final List<String> results) throws OrderException {
        final String line = pc.orderText;

        final int rStart = line.indexOf("(*");
        final int rEnd = line.indexOf("*)");

        // no results. Return original order text.
        if (rStart == -1 && rEnd == -1) {
            results.clear();
            return line;
        }

        // bad or missing (* or *) delimiters
        if (rEnd <= rStart || rStart == -1) {
            throw new OrderException(
                    "Invalid result \"(* *)\" delimiters for order: " + line);
        }

        final String resultText = line.substring(rStart + 2, rEnd);
        final String[] resultStrings = resultText.split("\\s*,\\s*");

        for (final String resultString : resultStrings) {
            results.add(resultString);
        }

        // return the order, without the result text.
        return line.substring(0, rStart);
    }// parseResults()


    /**
     * Create proper Result objects from Move/Retreat phase
     * results. The supported result types are:
     * <ul>
     * <li>(no results) -> SUCCESS order result</li>
     * <li>bounce -> FAILURE w/bounce message</li>
     * <li>cut -> FAILURE w/cut message</li>
     * <li>void -> FAILURE w/voit message</li>
     * <li>dislodged -> DISLOGED result</li>
     * <li>?unknown? -> OrderException </li>
     * </ul>
     * The given results are returned in the List.
     */
    private static List<Result> createResults(final ParseContext pc,
                                              final Orderable order,
                                              final List<String> stringResults) throws OrderException {
        final List<Result> results = new ArrayList<>(stringResults.size());

        for (final String stringResult : stringResults) {
            final String textResult = stringResult.trim();
            if (textResult.equalsIgnoreCase("bounce")) {
                results.add(
                        new OrderResult(order, ResultType.FAILURE, "Bounce"));
            } else if (textResult.equalsIgnoreCase("cut")) {
                results.add(new OrderResult(order, ResultType.FAILURE, "Cut"));
            } else if (textResult.equalsIgnoreCase("no convoy")) {
                results.add(new OrderResult(order, ResultType.FAILURE,
                        "No Convoy"));
            } else if (textResult.equalsIgnoreCase("dislodged")) {
                // create a failure result (if we were only dislodged)
                if (stringResults.size() == 1) {
                    results.add(
                            new OrderResult(order, ResultType.FAILURE, null));
                }

                // create a TEMPORARY dislodged result here
                results.add(new OrderResult(order, ResultType.DISLODGED,
                        "**TEMP**"));
            } else if (textResult.equalsIgnoreCase("destroyed")) {
                // create a failure result (if we were only dislodged)
                if (stringResults.size() == 1) {
                    results.add(
                            new OrderResult(order, ResultType.FAILURE, null));
                }

                // destroyed result
                results.add(new DislodgedResult(order, null));
            } else if (textResult.equalsIgnoreCase("void")) {
                results.add(
                        new OrderResult(order, ResultType.VALIDATION_FAILURE,
                                "Void"));
            } else {
                // unknown result type! Assume failure.
                throw new OrderException(
                        "Unknown result \"" + textResult + "\" for order: " + pc.orderText);
            }
        }

        // if no result created, create a success result.
        if (results.isEmpty()) {
            results.add(new OrderResult(order, ResultType.SUCCESS, null));
        }

        return results;
    }// createResults()


    /**
     * Removes the trailing '.' from the order, and any leading
     * or trailing spaces.
     */
    private static String removeTrailingDot(final String input) {
        final String line = input.trim();
        if (line.endsWith(".")) {
            return line.substring(0, line.length() - 1);
        }

        return line;
    }// removeTrailingDot()


    private static NJudgeOrder parseAdjustmentOrder(final ParseContext pc,
                                                    final String line) throws OrderException {
        Matcher m = ADJUSTMENT_PATTERN.matcher(line);

        // attempt
        if (m.find()) {
            /*
                Groups:
					1: Power
					2: remove|build|default	(default is also a remove!)
					3: fleet|army|wing
					4: location
			
			*/

            // parse power
            final Power power = parsePower(pc, m.group(1).trim());

            // parse unit type
            final Type unit = parseUnitType(pc, m.group(3));

            // parse location
            final Location location = parseLocation(pc, m.group(4).trim());

            // parse action
            final boolean isDefault = "default".equalsIgnoreCase(m.group(2));

            final Order order;
            if ("build".equalsIgnoreCase(m.group(2))) {
                order = pc.orderFactory.createBuild(power, location, unit);
            } else {
                // remove or default
                order = pc.orderFactory.createRemove(power, location, unit);
            }

            final NJudgeOrder njo;

            if (isDefault) {
                // power defaulted; order is contained in a substitutedResult
                final SubstitutedResult substResult = new SubstitutedResult(
                        null, order, "Power defaulted; unit removed.");

                njo = new NJudgeOrder(null, substResult,
                        pc.isAdjustmentPhase());
            } else {
                njo = new NJudgeOrder(order,
                        new OrderResult(order, ResultType.SUCCESS, null),
                        pc.isAdjustmentPhase());
            }

            return njo;
        } else {
            // reuse variable 'm'
            m = ALTERNATE_ADJUSTMENT_PATTERN.matcher(line);
            if (m.find()) {
                // parse power
                final Power power = parsePower(pc, m.group(1).trim());

                // parse # (may be empty)
                // (if it is empty, set to 0)
                int numBuilds = 0;
                try {
                    if (m.group(2).length() >= 1) {
                        numBuilds = Integer.parseInt(m.group(2));
                    }
                } catch (final NumberFormatException e) {
                    throw new OrderException("Expected valid integer at \"" + m
                            .group(2) + "\" for order: " + pc.orderText);
                }

                // parse if unused/unusable
                boolean isUnusable = false;
                final String group3Tok = m.group(3);
                if ("unused".equalsIgnoreCase(group3Tok) || "unusable"
                        .equalsIgnoreCase(group3Tok)) {
                    isUnusable = true;
                }

                // parse if pending/waived
                final boolean isPending = "pending"
                        .equalsIgnoreCase(m.group(5));
                final boolean isWaived = "waived".equalsIgnoreCase(m.group(5));

                if (m.group(3) == null) {
                    // Group3 is null when it is a 'regular' waive order.
                    final Result result = new Result(power, "Build waived.");
                    return new NJudgeOrder(power, result);
                } else {
                    // since we depend on external input, these should
                    // be exceptions rather than asserts
                    //
                    assert isUnusable;
                    if (!isUnusable) {
                        throw new IllegalStateException();
                    }

                    assert isPending != isWaived;
                    if (isPending == isWaived) {
                        throw new IllegalStateException();
                    }

                    if (isPending) {
                        // pending builds
                        final Result result = new Result(power,
                                numBuilds + " unusable build(s) pending.");
                        return new NJudgeOrder(power, numBuilds, 0, result);
                    } else {
                        // waived builds
                        final Result result = new Result(power,
                                numBuilds + " unusable build(s) waived.");
                        return new NJudgeOrder(power, 0, numBuilds, result);
                    }
                }
            } else {
                throw new OrderException(
                        "Cannot parse adjustment order: " + pc.orderText);
            }
        }
    }// parseAdjustmentOrder()


    /**
     * Info needed by parsing methods
     */
    private static final class ParseContext {
        public final WorldMap map;
        public final OrderFactory orderFactory;
        public final PhaseType phaseType;
        public final String orderText;

        ParseContext(final WorldMap map, final OrderFactory orderFactory,
                     final PhaseType phaseType, final String orderText) {
            this.map = map;
            this.orderFactory = orderFactory;
            this.phaseType = phaseType;
            this.orderText = orderText;
        }

        public boolean isRetreatPhase() {
            return PhaseType.RETREAT == phaseType;
        }// isRetreatPhase()


        public boolean isAdjustmentPhase() {
            return PhaseType.ADJUSTMENT == phaseType;
        }// isAdjustmentPhase()
    }// ParseContext()
}// class NJudgeOrderParser



