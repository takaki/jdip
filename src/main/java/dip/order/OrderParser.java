//
// @(#)OrderParser.java	12/2002
//
// Copyright 2002 Zachary DelProposto. All rights reserved.
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

import dip.misc.Log;
import dip.misc.Utils;
import dip.world.Coast;
import dip.world.Location;
import dip.world.Phase;
import dip.world.Phase.PhaseType;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.TurnState;
import dip.world.Unit;
import dip.world.Unit.Type;
import dip.world.WorldMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * Parses text to create an Order object.
 * <p>
 * NOTE: this code is rather hackish (and in no way reflective of the
 * rest of the code base). It is not expandable, or modular. However, it
 * is a pretty flexible parser in terms of what it will accept. As a point
 * of history, it's the first piece of code written for jDip.
 * <p>
 * I am gradually replacing the most crusty parts with better code. For example, Coast now
 * normalizes coasts with regular expressions, and the code is far cleaner.
 * <p>
 * In the future, I anticipate we will have some sort of command-normalization (Order.normalize)
 * which will be implemented by Order subclasses, and some sort of pattern matching
 * that will allow the order to be matched to the tokens. This will allow Order classes to be modfied
 * or added without rewriting the OrderParser.
 * <p>
 * <b>note:</b> The parser is extremely tolerant of misspellings in single-word provinces. However,
 * it is not at all tolerant of multiword province misspellings. This is because it cannot easily
 * recognize multi-word provinces. A pattern based parser could, and would be more robust in this
 * regard.
 * <p>
 * <pre>
 * 	HOLD:
 * 	<power>: <type> <s-prov> h
 *
 * 	MOVE, RETREAT:
 * 	<power>: <type> <s-prov> m <d-prov>
 * 	<power>: m <s-prov> (to) <d-prov>
 *
 * 	SUPPORT:
 * 	<power>: <type> <s-prov> s <type> <s-prov>
 * 	<power>: <type> <s-prov> s <type> <s-prov> m <d-prov>
 *
 * 	CONVOY:
 * 	<power>: <type> <s-prov> c <type> <s-prov> m <d-prov>
 *
 * 	DISBAND:
 * 	<power>: <type> <s-prov> d
 *
 * 	BUILD:
 * 	<power>: <build> <type> <s-prov>
 *
 * 	REMOVE:
 * 	<power>: <remove> <type> <s-prov>
 *
 *
 * 	Where:
 *
 * 	<type> = "army", "a", "fleet", "f" or <empty>
 * 	<s-prov> = Source province.
 * 	<d-prov> = Destination province.
 * 	<power> = Power name or abbreviation of two or more characters.
 * 	<holds> = "h", "hold", "holds", "stand", "stands".
 * 	<moves> = "-", "->", "=>", "m", "move", "moves", "move to", "moves to".
 * 	<support> = "s", "support", "supports".
 * 	<convoy> = "c", "convoy", "convoys".
 * 	<disband> = "d", "disband".
 * 	<build> = "b", "build"
 * 	<remove> = "r", "remove"
 *
 * 	</pre>
 */
public class OrderParser {
    private static OrderParser instance;


    // il8n constants
    private static final String OF_POWER_NOT_RECOGNIZED = "OF_POWER_NOT_RECOGNIZED";
    private static final String OF_UNIT_NOT_RECOGNIZED = "OF_UNIT_NOT_RECOGNIZED";
    private static final String OF_PROVINCE_NOT_RECOGNIZED = "OF_PROVINCE_NOT_RECOGNIZED";
    private static final String OF_PROVINCE_UNCLEAR = "OF_PROVINCE_UNCLEAR";
    private static final String OF_NO_UNIT_IN_PROVINCE = "OF_NO_UNIT_IN_PROVINCE";
    private static final String OF_TOO_SHORT = "OF_TOO_SHORT";
    private static final String OF_INTERNAL_ERROR = "OF_INTERNAL_ERROR";
    private static final String OF_UNKNOWN_ORDER = "OF_UNKNOWN_ORDER";
    private static final String OF_CONVOY_NO_MOVE_OR_DEST = "OF_CONVOY_NO_MOVE_OR_DEST";
    private static final String OF_CONVOY_NO_DEST = "OF_CONVOY_NO_DEST";
    private static final String OF_CONVOY_NO_MOVE_SPEC = "OF_CONVOY_NO_MOVE_SPEC";
    private static final String OF_SUPPORT_NO_DEST = "OF_SUPPORT_NO_DEST";
    private static final String OF_SUPPORT_NO_MOVE = "OF_SUPPORT_NO_MOVE";
    private static final String OF_MISSING_DEST = "OF_MISSING_DEST";
    private static final String OF_BAD_FOR_POWER = "OF_BAD_FOR_POWER";
    private static final String OF_NO_ORDER_TYPE = "OF_NO_ORDER_TYPE";
    private static final String OF_POWER_LOCKED = "OF_POWER_LOCKED";
    private static final String OF_COAST_INVALID = "OF_COAST_INVALID";


    private static final String WHITESPACE = ": \t\n\r";


    // the order of replacements is very important!
    // all must be in lower case!
    private static final String REPLACEMENTS[][] = {
            // misc tiny words that people add
            // should NOT include 'to' because to can mean move; it's not always extraneous
            // must have spaces before and after
            {" in ", " "}, {" an ", " "}, {" of ", " "}, {" on ", " "},
            // WARNING: if a province is named 'the', this will create a problem.
            {" the ", " "},
            // convert unit-type specifiers
            {"fleet", " f "}, {"army", " a "}, {"wing", " w "},
            // WAIVE orders. Waive may NOT be abbreviated as "W"; otherwise,
            // w xxx (build a wing) may be confused as 'waive'
            {"waives builds ", " waive "}, {"waives build ", " waive "}, {"waive builds ", " waive "},    // e.g., waive build [province]; must come before "BUILD"
            {"waive build ", " waive "},        // e.g., waive build [province]; must come before "BUILD"
            {"waives", " waive "},
            // adjustment order Remove (since it contains "move", must come before)
            {"removes a ", " r "}, {"removes", " r "},        // plurals FIRST
            {"remove", " r "},
            // for MOVE orders; note that "->" must come before "-"
            // also, we MUST replace any "-" in coasts with a "/" first.
            {"-=>", " m "}, {"=->", " m "}, {"==>", " m "}, {"-->", " m "}, {"->", " m "}, {"=>", " m "}, {"-", " m "}, {"\u2192", " m "},            // unicode ARROW as used by jDip
            {"retreats to ", " m "}, // NOTE: space after "to" to avoid ambiguity e.g., "army bre retreats tol"
            {"retreat to ", " m "}, {"retreats", " m "},    // plural first
            {"retreat", " m "}, {"moving to ", " m "},    // NOTE: space after "to" ...
            {"moves to ", " m "},    // NOTE: space after "to" to avoid ambiguity e.g., "army bre moves tol"
            {"move to ", " m "},    // NOTE: plurals and longer entries MUST come before shorter entries
            {"moves", " m "}, {"move", " m "}, {" mv ", " m "},        // for those that like unix
            {" attacks on ", " m "},    // we precede the following with a space, since they are nonstandard keywords
            {" attacks to ", " m "}, {" attacks into ", " m "}, {" attacks of ", " m "}, {" attack on ", " m "}, {" attack to ", " m "}, {" attack into ", " m "}, {" attack of ", " m "}, {" attacks ", " m "}, {" attack ", " m "}, {" into ", " m "},        // prefixed with space (don't want to get the end of a province)
            {" to ", " m "},        // used as a substitute for 'move to'; space prefix here is also important
            // SUPPORT orders
            {"supports", " s "},    // plurals FIRST
            {"support", " s "}, {" to support", " s "},    // prefixed with space (to not get the end of another word)
            // HOLD orders
            {"holds", " h "}, {"hold", " h "}, {"stands", " h "}, {"stand", " h "},
            // CONVOY orders
            {"convoys", " c "}, {"convoy", " c "}, {"transports", " c "}, {"transport", " c "},
            // DISBAND orders	NOTE: 'remove' is up above (before 'move')
            {"disbands a ", " d "}, {"disbands", " d "}, {"disband", " d "},
            // various adjustment orders
            {"builds a ", " b "}, {"builds", " b "},    // plurals FIRST
            {"build a ", " b "}, {"build", " b "},
            // this occurs after coast-normalization, so convert parens to spaces.
            {"(", " "}, {")", " "}};


    // DELETION strings for preprocessor; must occur after coast normalization
    private static final String TODELETE[] = {".",    // periods often occur in coast specifiers (e.g., "n.c.")
            ",",    // shouldn't have any commas, but shouldn't be harmful, either.
            "\"",    // double-quotes filtered out
            "\'s",    // filter out possesives
            "\'",    // filter out possesives / single quotes
            "(",    // parentheses will only get in the way.
            ")",};


    private OrderParser() {
    }// OrderParser()


    /**
     * Gets an OrderParser instance
     */
    public static synchronized OrderParser getInstance() {
        if (instance == null) {
            instance = new OrderParser();
        }

        return instance;
    }// getInstance()


    /**
     * Parse an order to an Order object.
     * <p>
     * There are several options to control parsing.
     * <pre>
     * 	power:
     * 			"default" power to assume; null if not required
     * 	World:
     * 			current world; needed for province/power matching
     *
     * 	locked:
     * 			if true, only orders for the specified power are legal.
     * 			if power==null then an IAE is thrown.
     * 	guess:
     * 			only works if (power==null) and power is NOT locked
     * 			guesses power based upon source province
     * 			Position (derived from world) must be accurate; since guessing depends
     * 			upon knowing the current position information, and phase information.
     *
     *
     * 	States:
     *
     * 	Power		Locked	Guess	Result
     * 	=====   	======	=====	=================================================
     * 	null		false	false	VALID: but power must always be present in text to parse!
     * 	null		false	true	VALID: power is based on source province, whether specified or not
     * 	null		true	false	illegal
     * 	null		true	true	illegal
     *
     * 	(defined)	false	false	VALID: power must be specified, if not, assumes "Power" given
     * 	(defined)	false	true	VALID: if power not specified, it is based on source province
     * 	(defined)	true	false	VALID: power *always* is "Power" given
     * 	(defined)	true	true	illegal
     * 	</pre>
     */
    public Order parse(final OrderFactory orderFactory, final String text,
                       final Power power, final TurnState turnState,
                       final boolean locked,
                       final boolean guess) throws OrderException {
        Objects.requireNonNull(orderFactory);

        // check arguments
        if (locked && Objects.isNull(power)) {
            throw new IllegalArgumentException("power/lock disagreement");
        }

        if (guess && (Objects.nonNull(power) || Objects.isNull(turnState))) {
            throw new IllegalArgumentException(
                    "if guess == true, conditions: turnState != null, power == null, and !locked must all be true");
        }

        final Position position = turnState.getPosition();
        final WorldMap map = turnState.getWorld().getMap();
        final String preText = preprocess(text, map);

        Log.println("OP: Input:", text);
        Log.println("OP: preprocessed:", preText);

        return parse(preText, position, map, power, turnState, orderFactory,
                locked, guess);
    }// parse()


    /**
     * The preprocessor normalizes the orders, converting various order entry
     * formats to a single order entry format that is more easily parsed.
     */
    private String preprocess(final String ord,
                              final WorldMap map) throws OrderException {
        // create StringBuffer, after filtering the input string.
        // note that this step includes lower-case conversion.
        final StringBuffer sb = filterInput(ord);

        // replace any long (2-word, via space or hyphen) province names
        // with shorter version.
        // NOTE: this may be overkill, especially since it won't replace
        // *partial* province names, like "North-atl"
        map.replaceProvinceNames(sb);

        // filter out power names [required at beginning to filter out power names
        // with odd characters such as hyphens]. Excludes first token.
        map.filterPowerNames(sb);

        // normalize coasts (Converts to /Xc format)
        //Log.println("OP: pre-coast normalization:", sb);

        final String ncOrd = Coast.normalize(sb.toString()).orElseThrow(
                () -> new OrderException(
                        Utils.getLocalString(OF_COAST_INVALID, "empty")));
        sb.setLength(0);
        sb.append(ncOrd);

        //Log.println("OP: post-coast normalization:", sb);


        // get the 'power token' (or null).
        // this is so if a power name has odd characters in it (e.g., chaos map)
        // they do not undergo replacement.
        final String ptok = map.getFirstPowerToken(sb.toString()).get();
        final int startIdx = ptok == null ? 0 : ptok.length();

        // string replacement
        for (final String[] replacement : REPLACEMENTS) {
            int idx = startIdx;
            int start = sb.indexOf(replacement[0], idx);

            while (start != -1) {
                final int end = start + replacement[0].length();
                sb.replace(start, end, replacement[1]);

                // repeat search
                idx = start + replacement[1].length();
                start = sb.indexOf(replacement[0], idx);
            }
        }

        // delete unwanted characters
        delChars(sb, TODELETE);

        // re-replace, after conversion
        map.replaceProvinceNames(sb);

        // filter out power names; often occurs in 'support' orders.
        // could also appear in a convoy order as well
        // e.g.: France: F gas SUPPORT British F iri HOLD
        // or 						   "Britain's"   which would be converted to "Britain" by delChars()
        // this does NOT filter out the first power name!! (which may be required)
        map.filterPowerNames(sb);

        return sb.toString();
    }// preprocess()


    private Order parse(final String ord, final Position position,
                        final WorldMap map, final Power defaultPower,
                        final TurnState turnState,
                        final OrderFactory orderFactory, final boolean locked,
                        final boolean guessing) throws OrderException {
        // Objects common to ALL order types.

        // current token for parsing

        final StringTokenizer st = new StringTokenizer(ord, WHITESPACE, false);


        // Power parsing

        // see if first token is a power; if so, parse it
        Power power = map.getFirstPower(ord).orElse(null);
        //Log.println("OP:parse(): first token a power? ", power);

        // eat up the token (we don't want to reparse it), but
        // only if it's NOT null (probably not a power)
        if (power != null) {
            getToken(st);    // eat token
            //String pTok = getToken(st);
            //Log.println("  OP:parse(): eating token: ", pTok);
        }

        // if we're not allowed to guess, and power is null, error.
        if (!guessing && power == null) {
            Log.println("OrderException: order: ", ord);
            final String pTok = getToken(st);
            throw new OrderException(
                    Utils.getLocalString(OF_POWER_NOT_RECOGNIZED, pTok));
        }

        // reset power, if null, to default (if specified)
        power = power == null ? defaultPower : power;

        // if we are locked, the power must be the default power
        if (locked) {
            assert power != null;

            if (!power.equals(defaultPower)) {
                Log.println("OrderException: order: ", ord);
                throw new OrderException(
                        Utils.getLocalString(OF_POWER_LOCKED, defaultPower));
            }
        }

        // NOTE: power may be null at this point, iff guessing==true.
        // in this case, it is upto the order-processing logic to parse
        // get the correct order from the source region.
        // decide if first token is src, src type, or adjustment order
        // adjustment orders have a different syntax from other orders
        // parse the src type [if any]
        //
        String token = getToken(st);
        String srcUnitTypeName = null;
        if (isTypeToken(token)) {
            srcUnitTypeName = token;
            token = getToken(st);
        } else if (isCommandPrefixed(token)) {
            return parseCommandPrefixedOrders(orderFactory, position, map,
                    power, token, st, guessing, turnState);
        }


        // parse the src province
        final String srcName = token;

        // parse the order type -- if this is missing, we
        // have a 'defineState' order type
        final String orderType = st.hasMoreTokens() ? getToken(st,
                Utils.getLocalString(OF_NO_ORDER_TYPE)) : "definestate";


        // create objects for Source, and SourceUnit which
        // occur for all orders.
        final Type srcUnitType = parseUnitType(srcUnitTypeName);
        final Location src = parseLocation(map, srcName);
        assert src != null;
        assert srcUnitType != null;

        // if we are guessing, guess the power from the source.
        // return an error if we cannot..
        //
        // ALSO, if user specified a power, and "guess" is true,
        // and the guessed power != specified, throw an exception.
        if (guessing) {
            // getPowerFromLocation() should throw an exception if no unit present.
            final Power tempPower = getPowerFromLocation(false, position,
                    turnState, src);
            if (power != null) {
                if (!tempPower.equals(power)) {
                    Log.println("OrderException: order: ", ord);
                    throw new OrderException(
                            Utils.getLocalString(OF_BAD_FOR_POWER, power));
                }
            }

            power = tempPower;
        }

        assert power != null;

        // create order based on order type
        switch (orderType) {
            case "h":
                // HOLD order
                // <power>: <type> <s-prov> h
                return orderFactory.createHold(power, src, srcUnitType);
            case "m":
                return parseMoveOrder(map, turnState, position, orderFactory,
                        st, power, src, srcUnitType, false);

            case "s": {
                // SUPPORT order
                // <power>: <type> <s-prov> s <type> <s-prov>
                // <power>: <type> <s-prov> s <type> <s-prov> [h]
                // <power>: <type> <s-prov> s <type> <s-prov> m <d-prov>
                //
                // get type and/or support source names
                final TypeAndSource tas = getTypeAndSource(st);

                // parse supSrc / supUnit
                final Type supUnitType = parseUnitType(tas.type);
                final Location supSrc = parseLocation(map, tas.src);

                assert supUnitType != null;
                assert supSrc != null;

                // get power from unit, if possible
                Power supPower = null;
                if (position.hasUnit(supSrc.getProvince())) {
                    supPower = position.getUnit(supSrc.getProvince())
                            .orElse(null).getPower();
                }

                // support a MOVE [if specified]
                if (st.hasMoreTokens()) {
                    token = st.nextToken();

                    if (token.equals("m")) {
                        final String supDestName = getToken(st,
                                Utils.getLocalString(OF_SUPPORT_NO_DEST));
                        final Location supDest = parseLocation(map,
                                supDestName);
                        assert supDest != null;
                        return orderFactory
                                .createSupport(power, src, srcUnitType, supSrc,
                                        supPower, supUnitType, supDest);
                    } else if (!token.equals("h")) {
                        // anything BUT a hold is ok.
                        Log.println("OrderException: order: ", ord);
                        throw new OrderException(
                                Utils.getLocalString(OF_SUPPORT_NO_MOVE));
                    }
                }

                // support a HOLD
                return orderFactory
                        .createSupport(power, src, srcUnitType, supSrc,
                                supPower, supUnitType);
            }
            case "c":
                // CONVOY order
                // <power>: <type> <s-prov> c <type> <s-prov> m <d-prov>
                // get type and/or support source
                final TypeAndSource tas = getTypeAndSource(st);
                final String conSrcName = tas.src;
                final String conUnitName = tas.type;

                // verify that there is an "m"
                token = getToken(st,
                        Utils.getLocalString(OF_CONVOY_NO_MOVE_OR_DEST));
                if (!token.equalsIgnoreCase("m")) {
                    Log.println("OrderException: order: ", ord);
                    throw new OrderException(
                            Utils.getLocalString(OF_CONVOY_NO_MOVE_SPEC));
                }

                // get the destination
                final String conDestName = getToken(st,
                        Utils.getLocalString(OF_CONVOY_NO_DEST));

                // parse convoy src/dest/type
                final Location conSrc = parseLocation(map, conSrcName);
                final Location conDest = parseLocation(map, conDestName);
                final Type conUnitType = parseUnitType(conUnitName);

                // get power, from unit
                Power conPower = null;
                if (position.hasUnit(conSrc.getProvince())) {
                    conPower = position.getUnit(conSrc.getProvince())
                            .orElse(null).getPower();
                }

                // create order.
                return orderFactory
                        .createConvoy(power, src, srcUnitType, conSrc, conPower,
                                conUnitType, conDest);
            case "d":
                // DISBAND order
                return createDisbandOrRemove(orderFactory, turnState, true,
                        power, src, srcUnitType);
            case "definestate":
                return orderFactory.createDefineState(power, src, srcUnitType);
            default:
                Log.println("OrderException: order: ", ord);
                throw new OrderException(
                        Utils.getLocalString(OF_UNKNOWN_ORDER, orderType));
        }
    }// parse


    /**
     * Parses the "rest" of a move/retreat order; this finds
     * the destination location, and the location-list if it is
     * a convoyed move. It also checks for the "by convoy" or "via convoy"
     * phrase that signfies a convoyed move.
     * <p>
     * ignoreFirstM will ignore a token called "m" if set to true.
     * <p>
     * This will return a Move or Retreat order, or throw an OrderException.
     */
    private Order parseMoveOrder(final WorldMap map, final TurnState turnState,
                                 final Position position,
                                 final OrderFactory orderFactory,
                                 final StringTokenizer st, final Power srcPower,
                                 final Location srcLoc, final Type srcUnitType,
                                 final boolean ignoreFirstM) throws OrderException {
        // MOVE order, or RETREAT order, if we are in RETREAT phase. If so, we can ignore the convoy stuff.
        // <power>: <type> <s-prov> m <d-prov>
        //
        String destName = getToken(st, Utils.getLocalString(OF_MISSING_DEST));

        // eat possible first "M" if allowed and repeat dest-getting attempt
        if (ignoreFirstM && destName.equals("m")) {
            destName = getToken(st, Utils.getLocalString(OF_MISSING_DEST));
        }

        //
        // We do 2 things in this loop:
        // (1) JUDGE compatibility:
        // 		check and see if we have 'multiple' destinations; e.g.,
        // 		"A STP-BAR-NRG-NTH-YOR" we BAR is not the dest, YOR is.
        // 		so we will keep parsing until we come across the last valid
        // 		province. A move specifier ('m') *MUST* occur before each province.
        // (2) "via convoy" or "by convoy" checking
        //
        ArrayList<Province> al = null;
        if (st.hasMoreTokens()) {
            al = new ArrayList<>();
            al.add(srcLoc.getProvince());
            // parse first destination (and add to array list)
            al.add(parseLocation(map, destName).getProvince());
        }

        boolean isConvoyedMove = false;        // multiple 'move' locations
        boolean isExplicitConvoy = false;    // "by convoy" or "via convoy" present
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals("m")) {
                destName = getToken(st, Utils.getLocalString(OF_MISSING_DEST));
                final Location pathLoc = parseLocation(map, destName);
                assert pathLoc != null;
                al.add(pathLoc.getProvince());
                isConvoyedMove = true;
            } else if ((token.equals("via") || token.equals("by")) && st
                    .hasMoreTokens()) {
                token = st.nextToken();
                if (token.equals("c")) {
                    isExplicitConvoy = true;  // we are convoying!
                }
            }
        }

        // final destination
        final Location dest = parseLocation(map, destName);
        assert dest != null;

        if (turnState.getPhase().getPhaseType() == PhaseType.RETREAT) {
            return orderFactory
                    .createRetreat(srcPower, srcLoc, srcUnitType, dest);
        } else {
            if (isConvoyedMove) {// MUST test this first -- it overrides isExplicitConvoy
                final Province[] convoyRoute = al
                        .toArray(new Province[al.size()]);
                return orderFactory
                        .createMove(srcPower, srcLoc, srcUnitType, dest,
                                convoyRoute);
            } else if (isExplicitConvoy) {
                return orderFactory
                        .createMove(srcPower, srcLoc, srcUnitType, dest,
                                isExplicitConvoy);
            } else {
                // implicit convoy [determiend by Move.validate()] or nonconvoyed move order
                return orderFactory
                        .createMove(srcPower, srcLoc, srcUnitType, dest);
            }
        }
    }// parseMoveOrder()


    /**
     * Parse command-prefixed orders (e.g., "Build army paris"). This typically
     * applies to adjustment orders, however, we also allow Move orders to be
     * specified this way.
     */
    private Order parseCommandPrefixedOrders(final OrderFactory orderFactory,
                                             final Position position,
                                             final WorldMap map, Power power,
                                             final String orderType,
                                             final StringTokenizer st,
                                             final boolean guessing,
                                             final TurnState turnState) throws OrderException {
        // these orders have a command-specifier BEFORE unit/src information
        switch (orderType) {
            case "waive": {
                // WAIVE order
                // <power>: <waive> <province>
                // we ignore 'type', but let it be specified
                final TypeAndSource tas = getTypeAndSource(st);
                final Location src = parseLocation(map, tas.src);
                if (guessing) {
                    power = getPowerFromLocation(true, position, turnState,
                            src);
                }
                return orderFactory.createWaive(power, src);
            }
            case "b": {
                // BUILD order
                // <power>: BUILD <type> <s-prov>
                final TypeAndSource tas = getTypeAndSource(st);
                final Location src = parseLocation(map, tas.src);
                final Type unitType = parseUnitType(tas.type);
                if (guessing) {
                    power = getPowerFromLocation(true, position, turnState,
                            src);
                }

                return orderFactory.createBuild(power, src, unitType);
            }
            case "r": {
                // REMOVE order
                // <power>: REMOVE <type> <s-prov>
                final TypeAndSource tas = getTypeAndSource(st);
                final Location src = parseLocation(map, tas.src);
                final Type unitType = parseUnitType(tas.type);
                if (guessing) {
                    power = getPowerFromLocation(true, position, turnState,
                            src);
                }

                return createDisbandOrRemove(orderFactory, turnState, false,
                        power, src, unitType);
            }
            case "m": {
                // MOVE order: command-first version
                // <power>: m <unit> <location> m <location>
                // example: "france: move army paris to gascony"
                // or: "move army paris-gascony"
                final TypeAndSource srcTas = getTypeAndSource(st);
                final Location src = parseLocation(map, srcTas.src);
                final Type srcUnitType = parseUnitType(srcTas.type);
                if (guessing) {
                    power = getPowerFromLocation(true, position, turnState,
                            src);
                }

                return parseMoveOrder(map, turnState, position, orderFactory,
                        st, power, src, srcUnitType, true);
            }
            case "d":
                // DISBAND: command-first version
                // <power>: DISBAND <type> <s-prov>
                final TypeAndSource tas = getTypeAndSource(st);
                final Location src = parseLocation(map, tas.src);
                final Type unitType = parseUnitType(tas.type);
                if (guessing) {
                    power = getPowerFromLocation(true, position, turnState,
                            src);
                }

                return createDisbandOrRemove(orderFactory, turnState, true,
                        power, src, unitType);
            default:
                throw new IllegalArgumentException(
                        Utils.getLocalString(OF_INTERNAL_ERROR, orderType));
        }
    }// parseCommandPrefixedOrders()


    private static String getToken(final StringTokenizer st,
                                   final String error) throws OrderException {
        if (st.hasMoreTokens()) {
            return st.nextToken();
        } else {
            throw new OrderException(error);
        }
    }// getToken()


    private static String getToken(
            final StringTokenizer st) throws OrderException {
        return getToken(st, Utils.getLocalString(OF_TOO_SHORT));
    }// getToken()}


    /**
     * Derives the power based upon the location of the source unit. We have a
     * special flag (isAdjToken) which should be set to TRUE if we are parsing
     * an adjustment-phase order, and false otherwise.
     */
    private static Power getPowerFromLocation(final boolean isAdjToken,
                                              final Position position,
                                              final TurnState turnState,
                                              final Location source) throws OrderException {
        final Province province = source.getProvince();
        final Phase phase = turnState.getPhase();

        if (phase.getPhaseType() == PhaseType.ADJUSTMENT && isAdjToken) {
            // adjustment phase
            // we are supposed to 'guess', so we guess by getting the owner of the supply center chosen.
            // NOTE: this is loose (supply center, rather than home supply center); validation will take
            // care of details.
            //
            // if a unit exists, assume remove, and use that power; otherwise, assume a build.
            //
            if (position.hasUnit(province)) {
                return position.getUnit(province).orElse(null).getPower();
            } else {
                assert position.getSupplyCenterOwner(province) != null;
                return position.getSupplyCenterOwner(province).orElse(null);
            }
        }
        // retreat / movement phases:
        final Unit unit = phase.getPhaseType() == PhaseType.RETREAT ? position
                .getDislodgedUnit(province).orElse(null) : position
                .getUnit(province).orElse(null);
        if (unit != null) {
            return unit.getPower();
        }

        throw new OrderException(
                Utils.getLocalString(OF_NO_UNIT_IN_PROVINCE, province));
    }// getPowerFromLocation()


    /**
     * Determine if a Token is a Unit.Type token
     */
    private static boolean isTypeToken(final String s) {
        switch (s) {
            case "f":
            case "a":
            case "w":
                return true;
            default:
                return false;
        }
    }// isTypeToken

    // deletes any strings in the stringBuffer that match
    // strings specified in toDelete
    private static void delChars(final StringBuffer sb,
                                 final String[] toDelete) {
        for (final String aToDelete : toDelete) {
            int idx = sb.indexOf(aToDelete);
            while (idx != -1) {
                sb.delete(idx, idx + aToDelete.length());
                idx = sb.indexOf(aToDelete, idx);
            }
        }
    }// delChars()

    /**
     * Filters out any ISO control characters; improves the
     * robustness of pasted text parsing. Also replaces any
     * whitespace with a true space character. Returns a new
     * StringBuffer.
     * <p>
     * Also trims and lowercases the input, too
     */
    private static StringBuffer filterInput(final String input) {
        final String input1 = input.trim();

        final StringBuffer sb = new StringBuffer(input1.length());

        // delete control chars and whitespace conversion
        for (int i = 0; i < input1.length(); i++) {
            final char c = input1.charAt(i);

            if (Character.isWhitespace(c)) {
                sb.append(' ');
            } else if (!Character.isIdentifierIgnorable(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }

        return sb;
    }// delChars()

    /**
     * Some orders have the verb (command) at the beginning; e.g.:
     * "Build army france". We also allow move orders
     * to be specified this way, but most commonly
     * adjustment orders are specified this way.
     */
    private static boolean isCommandPrefixed(final String s) {
        // b,r,w = build, remove, waive
        // m = move
        switch (s.toLowerCase()) {
            case "b":
            case "r":
            case "d":
            case "m":
            case "waive":
                return true;
            default:
                return false;
        }
    }// isCommandPrefixed

    private TypeAndSource getTypeAndSource(
            final StringTokenizer st) throws OrderException {
        // given a StringTokenize, parse the next token
        // to determine if it is a type (Army or Fleet).
        // if it is missing, sets token to null, and sets
        // source token.
        final TypeAndSource tas = new TypeAndSource();
        final String token = getToken(st);

        if (isTypeToken(token)) {
            tas.type = token;
            tas.src = getToken(st);
        } else {
            tas.src = token;
        }

        return tas;
    }// getTypeAndSource()

    private static class TypeAndSource {
        public String type;
        public String src;
    }// inner class TypeAndSource


    /**
     * Parses a Location; never returns a null Location;
     * will throw an exception if the Location is unclear
     * or not recognized. This is similar to Map.parseLocation()
     * except that more detailed error information is returned.
     * <p>
     * <b>THIS ASSUMES COASTS HAVE ALREADY BEEN NORMALIZED WITH
     * Coast.normalize()</b>
     */
    private static Location parseLocation(final WorldMap map,
                                          final String locName) throws OrderException {
        // parse the coast
        // will return Coast.UNDEFINED at worst
        final Coast coast = Coast.parse(locName);

        // parse the province. if there are 'ties', we return the result.
        final List<Province> provinces = new ArrayList<>(
                map.getProvincesMatchingClosest(locName));

        switch (provinces.size()) {
            case 0:
                // nothing matched! we didn't recognize.
                throw new OrderException(
                        Utils.getLocalString(OF_PROVINCE_NOT_RECOGNIZED,
                                locName));
            case 1:
                return new Location(provinces.get(0), coast);
            case 2:
                // 2 matches... means it's unclear!
                throw new OrderException(
                        Utils.getLocalString(OF_PROVINCE_UNCLEAR, locName,
                                provinces.get(0), provinces.get(1)));
            default:
                // multiple matches! unclear. give a more detailed error message.
                // create a comma-separated list of all but the last.
                throw new OrderException(
                        Utils.getLocalString(OF_PROVINCE_UNCLEAR, locName,
                                provinces.stream().map(Province::toString)
                                        .collect(Collectors.joining(", ")),
                                ""));
        }
    }// parseLocation()


    //
    // uses unit.Type.parse()
    //	f/fleet -> FLEET
    //	a/army -> ARMY
    //  w/wing -> WING
    //	null -> UNDEFINED
    //	any other	-> null
    //
    private static Type parseUnitType(final String unitName) {
        return Type.parse(unitName);
    }// parseUnitType()

    private static Power parsePower(final WorldMap map,
                                    final String powerName) throws OrderException {
        return map.getPowerMatching(powerName).orElseThrow(
                () -> new OrderException(
                        Utils.getLocalString(OF_POWER_NOT_RECOGNIZED,
                                powerName)));
    }// parsePower()


    /**
     * Creates a Disband or Remove order, depending upon the phase.
     * Since some people use "Disband" to mean "Remove" and vice-versa,
     * but jDip interprets them differently (Disband is for retreat
     * phase, Remove is for adjustment phase). If the phase is not
     * adjustment or retreat, we create the 'desired' order, so that
     * the error message is correct.
     */
    private static Order createDisbandOrRemove(final OrderFactory orderFactory,
                                               final TurnState ts,
                                               final boolean disbandPreferred,
                                               final Power power,
                                               final Location src,
                                               final Type unitType) {
        if (ts.getPhase().getPhaseType() == PhaseType.RETREAT) {
            return orderFactory.createDisband(power, src, unitType);
        }
        if (ts.getPhase().getPhaseType() == PhaseType.ADJUSTMENT) {
            return orderFactory.createRemove(power, src, unitType);
        }

        return disbandPreferred ? orderFactory
                .createDisband(power, src, unitType) : orderFactory
                .createRemove(power, src, unitType);
    }// createDisbandOrRemove()


}// class OrderParser
