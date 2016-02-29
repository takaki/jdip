//
//  @(#)Map.java		4/2002
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
package dip.world;

import dip.order.OrderException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * A Map is a list of Provinces and Powers, and methods for obtaining and parsing
 * these Provinces and Powers.
 */
public final class Map implements Serializable {

    // internal constant arrays
    // all this data is serialized.
    private final List<Power> powers;
    private final List<Province> provinces;

    // None of the data below here is serialized; it can be derived from
    // the above (serialized) data.
    //
    // Province-related
    private transient java.util.Map<String, Province> nameMap;    // map of all (short & full) names to a province; names in lower case
    private transient List<String> names;    // list of all province names [short & full]; names in lower case

    // Power-related
    private transient java.util.Map<String, Power> powerNameMap;        // created by createMappings()

    // fields created on first-use (by a method)
    private transient List<String> lcPowerNames;        // lower case power names & adjectives
    private transient List<String> wsNames;            // list of all province names that contain whitespace, "-", or " "


    /**
     * Constructs a Map object.
     */
    protected Map(final Power[] powerArray, final Province[] provinceArray) {
        // define constant arrays.
        powers = Arrays.asList(powerArray);
        provinces = Arrays.asList(provinceArray);

        // check provinceArray: index must be >= 0 and < provinceArray.length
        final int len = provinceArray.length;
        IntStream.range(0, provinceArray.length).forEach(i -> {
            final int idx = provinceArray[i].getIndex();
            if (idx < 0 || idx >= len) {
                throw new IllegalArgumentException(
                        "Province: " + provinceArray[i] + ": illegal Index: " + idx);
            }
            if (idx != i) {
                throw new IllegalArgumentException(
                        "Province: " + provinceArray[i] + ": out of order (index: " + idx + "; position: " + i + ")");
            }
        });

        // create mappings
        createMappings();
    }// Map()


    /**
     * Creates the name->power and name->province mappings.
     * <p>
     * After de-serialization, this method MUST be called, since
     * the mappings aren't saved by default.
     */
    private void createMappings() {
        // create powerNameMap
        // also map adjectives
        powerNameMap = new HashMap<>(powers.stream().collect(Collectors
                .toMap(power -> power.getAdjective().toLowerCase(),
                        Function.identity())));
        powers.stream().forEach(power -> {
            powerNameMap.putAll(Arrays.stream(power.getNames()).collect(
                    Collectors.toMap(String::toLowerCase, aTmp -> power)));
        });
        // create lcPowerNameList
        lcPowerNames = createLCPowerNameList();

        // province-related namemap
        //
        // map long name
        nameMap = new HashMap<>(provinces.stream().collect(Collectors
                .toMap(province -> province.getFullName().toLowerCase(),
                        Function.identity())));
        provinces.stream().forEach(province -> nameMap
                .putAll(Arrays.stream(province.getShortNames()).collect(
                        Collectors.toMap(String::toLowerCase,
                                lcShortName -> province))));
        // add to List
        names = new ArrayList<>(provinces.stream().map(Province::getFullName)
                .map(String::toLowerCase).collect(Collectors.toList()));
        names.addAll(provinces.stream()
                .flatMap(province -> Arrays.stream(province.getShortNames()))
                .map(String::toLowerCase).collect(Collectors.toList()));

    }// createMappings()


    /**
     * Returns an Array of all Powers.
     */
    public final Power[] getPowers() {
        return powers.toArray(new Power[powers.size()]);
    }// getPowers()


    /**
     * Returns the power that matches name. Returns null if no
     * match found.
     * <p>
     * The match must be exact, but is case-insensitive.
     */
    public Power getPower(final String name) {
        return powerNameMap.get(name.toLowerCase());
    }// getPower()


    /**
     * Returns the closest Power to the given input String.
     * If no reasonable match is found, or multiple matches are found,
     * returns null.
     * <p>
     * This is different from getPowerMatching() in that this method
     * assumes <i>a priori</i> that the input is a power; it therefore
     * has looser parsing requirements. Likewise, if used on non-power tokens
     * (e.g., Provinces), it may be sufficiently close to a Power that it will
     * match; such improper (mis)matches would occur much LESS often
     * with getPowerMatching().
     * <p>
     * As few as a single character can be matched (if it's unique);
     * e.g., "E" for England.
     */
    public Optional<Power> getClosestPower(final String powerName) {
        // return 'null' if powerName is empty
        if (powerName == null || powerName.isEmpty()) {
            return Optional.empty();
        }

        // 1) check for an exact match.
        //
        final Power matchPower = getPower(powerName);
        if (matchPower != null) {
            return Optional.of(matchPower);
        }

        // make lowercase
        final String powerNameLower = powerName.toLowerCase();

        // 2) check for a unique partial match
        //
        final List<Power> list = findPartialPowerMatch(powerNameLower);
        if (list.size() == 1) {
            return Optional.of(list.get(0));
        }

        // 3) perform a Levenshtein match against power names.
        //
        final int bestMatch = lcPowerNames.stream()
                .mapToInt(name -> Distance.getLD(powerNameLower, name)).min()
                .orElse(Integer.MAX_VALUE);

        // if absolute error rate is too high, discard.
        if (bestMatch <= powerNameLower.length() / 2) {
            final Set<Power> matchPowers = lcPowerNames.stream()
                    .filter(name -> Distance
                            .getLD(powerNameLower, name) == bestMatch)
                    .map(this::getPower).collect(Collectors.toSet());
            return matchPowers.isEmpty() || matchPowers.size() > 1 ? Optional
                    .empty() : Optional.of(matchPowers.iterator().next());
        }

        // 4) nothing sufficiently close. Return null.
        return Optional.empty();
    }// getClosestPower()


    /**
     * Returns the Power that matches the powerName. Returns
     * null if no best match found.
     * <p>
     * This will match the closest power but requires at least
     * 5 characters for a match.
     */
    public Power getPowerMatching(String powerName) {
        // return 'null' if powerName is empty
        if (powerName == null || powerName.isEmpty()) {
            return null;
        }

        // first, check for exact match.
        final Power bestMatchingPower = getPower(powerName);
        if (bestMatchingPower != null) {
            return bestMatchingPower;
        }

        final String powerNameLC = powerName.toLowerCase();

        // no exact match.
        // otherwise we check for the 'max' matched characters, and go with this
        // if there are multiple equivalent matches (ties), without a clear winner,
        // return null.
        if (powerNameLC.length() >= 4) {
            final List<Power> list = findPartialPowerMatch(powerNameLC);
            if (list.size() == 1) {
                return list.get(0);
            }
        }

        // 3) perform a levenshtein match against power names.
        //
        final int bestMatch = lcPowerNames.stream()
                .mapToInt(name -> Distance.getLD(powerNameLC, name)).min()
                .orElse(Integer.MAX_VALUE);
        // we are stricter than in getClosestPower()
        if (bestMatch <= powerNameLC.length() / 3) {
            final Set<String> collect = lcPowerNames.stream()
                    .filter(name -> bestMatch == Distance
                            .getLD(powerNameLC, name))
                    .collect(Collectors.toSet());
            return collect.isEmpty() || collect.size() > 1 ? null : getPower(
                    collect.iterator().next());
        }

        // nothing is close
        return null;
    }// getPowerMatching()


    /**
     * Returns an Array of all Provinces.
     */
    public final Province[] getProvinces() {
        return provinces.toArray(new Province[provinces.size()]);
    }// getProvinces()


    /**
     * Returns the Province that matches name. Returns null if
     * no match found.
     * <p>
     * The match must be exact, but is case-insensitive.
     */
    public Province getProvince(final String name) {
        return nameMap.get(name.toLowerCase());
    }// getProvince()


    /**
     * Returns the Province that matches the input name. Returns
     * null if no best match found.
     * <p>
     * This will match the closest power but requires at least
     * 3 characters for a match. Ties result in no match at all.
     * This method uses the Levenshtein distance algorithm
     * to determine closeness.
     */
    public Province getProvinceMatching(final String input) {
        // return 'null' if input is empty
        if (input == null || input.isEmpty()) {
            return null;
        }

        // first, try exact match.
        // (fastest, if it works)
        final Province province = getProvince(input);
        if (province != null) {
            return province;
        }

        // we must be at least 3 chars
        if (input.length() < 3) {
            return null;
        }

        // input converted to lower case
        final String trimmed = input.toLowerCase().trim();

        // Do a partial match against the name list.
        // If we tie, return no match. This is a 'partial first match'
        // This is tried BEFORE we try Levenshtein
        //
        final List<Province> list = findPartialProvinceMatch(trimmed);
        if (list.size() == 1) {
            return list.get(0);
        }

        // tie list. Use a Set so that we get no dupes

        // compute Levenshteins on the match
        // if there are ties, keep them.. for now
        final int bestDist = names.stream()
                .mapToInt(name -> Distance.getLD(trimmed, name)).min()
                .orElse(Integer.MAX_VALUE);
        final Set<Province> ties = names.stream()
                .filter(name -> Distance.getLD(trimmed, name) == bestDist)
                .map(this::getProvince).collect(Collectors.toSet());
        /*
        System.out.println("LD input: "+input);
		System.out.println("   ties: "+ties);
		System.out.println("   bestDist: "+bestDist);
		System.out.println("   maxbest: "+((int) (input.length() / 2)));
		*/

        // if absolute error rate is too high, discard.
        // if we have >1 unique ties, (or none at all) no match
        if (bestDist <= trimmed.length() / 2 && ties.size() == 1) {
            // there is but one
            return ties.iterator().next();
        }

        return null;
    }// getProvinceMatching


    /**
     * Finds the Province(s) that best match the given input.
     * Returns a List of Provinces that match. If an empty list,
     * nothing was close (e.g., less than three characters).
     * If the list contains a single Province,
     * it is the closest match. If the list contains multiple Provinces,
     * there were several equally-close matches (ties).
     * <p>
     * This method uses the Levenshtein distance algorithm
     * to determine closeness.
     * <p>
     */
    public Collection<Province> getProvincesMatchingClosest(
            final String input) {
        // return empty list
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        // first, try exact match.
        // (fastest, if it works)
        final Province province = getProvince(input);
        if (province != null) {
            return Collections.singleton(province);
        }

        // input converted to lower case
        final String trimmed = input.toLowerCase().trim();

        // if 2 or less, do no processing
        if (input.length() <= 2) {
            return Collections.emptyList();
        }

        // tie list. Use a Set so that we get no dupes
        final Collection<Province> ties = new HashSet<>();
        if (input.length() == 3) {
            // if we are only 3 chars, do a partial-first match
            // against provinces and return that tie list (or,
            // if no tie, return the province)
            //
            // This works better than Levenshtein
            // which can return some very odd results.
            // for short strings...
            //
            ties.addAll(names.stream().filter(name -> name.startsWith(trimmed))
                    .map(this::getProvince).collect(Collectors.toSet()));
        } else {
            // compute Levenshteins on the match
            // if there are ties, keep them.. for now
            final int bestDist = names.stream()
                    .mapToInt(name -> Distance.getLD(trimmed, name)).min()
                    .orElse(Integer.MAX_VALUE);
            ties.addAll(names.stream()
                    .filter(name -> Distance.getLD(trimmed, name) == bestDist)
                    .map(this::getProvince).collect(Collectors.toSet()));
        }

        return ties;
    }// getProvincesMatchingClosest()


    /**
     * Parses text into a Location. This will discern coast
     * information, if present, as per Coast.normalize() followed
     * by Coast.parse().
     */
    public Location parseLocation(final String input) {
        try {
            final Coast coast = Coast.parse(Coast.normalize(input));
            final Province province = getProvinceMatching(
                    Coast.getProvinceName(input));
            return province != null ? new Location(province, coast) : null;
        } catch (final OrderException ignored) {
            return null;
        }
    }// parseLocation()


    /**
     * Searches the input string for any province names that contain
     * hyphens or whitespace ('-' or ' ') and replaces it with a short name.
     * this simplifies parsing, later, and allows the parser to better understand
     * multi-word names. ASSUMES input is all lower-case.
     * <p>
     * This is a special-purpose method for Order parsing.
     */
    public void replaceProvinceNames(final StringBuffer sb) {
        // create the whitespace list, if it doesn't exist.
        // TODO: delayed initialize
        if (wsNames == null) {
            // sort array from longest entries to shortest. This
            // eliminates errors in partial replacements.
            wsNames = names.stream()
                    .filter(name -> name.indexOf(' ') != -1 || name
                            .indexOf('-') != -1).map(String::toLowerCase)
                    .sorted(Comparator.comparing(String::length).reversed())
                    .collect(Collectors.toList());
        }

        // search & replace.
        for (final String currentName : wsNames) {
            int idx = 0;
            int start = sb.indexOf(currentName, idx);

            while (start != -1) {
                final int end = start + currentName.length();
                sb.replace(start, end, getProvince(currentName).getShortName());
                // repeat search
                idx = start + currentName.length();
                start = sb.indexOf(currentName, idx);
            }
        }
    }// replaceProvinceNames()


    /**
     * Eliminates any Power Names (e.g., "France") after the first whitespace
     * character or colon(this is done to prevent elimination of the first power,
     * which is required).
     * <p>
     * <b>NOTE: assumes StringBuffer is all lower-case.</b>
     * <p>
     * This is a special-purpose method for Order parsing.
     */
    public void filterPowerNames(final StringBuffer sb) {
        // find first white space or colon
        int wsIdx = -1;
        for (int i = 0; i < sb.length(); i++) {
            final char c = sb.charAt(i);
            if (c == ':' || Character.isWhitespace(c)) {
                wsIdx = i;
                break;
            }
        }

        // search / delete all names.
        // just looks for a single power name.
        //
        // preceding character MUST be a whitespace character.
        // thus "prussia" would not become "p"
        if (wsIdx >= 0) {
            for (String lcPowerName : lcPowerNames) {
                final int idx = sb.indexOf(lcPowerName, wsIdx);
                if (idx >= 0) {
                    if (idx != 0 && Character
                            .isWhitespace(sb.charAt(idx - 1))) {
                        sb.delete(idx, idx + lcPowerName.length());
                    }
                }
            }
        }
    }// filterPowerNames()


    /**
     * If a power token is specified (e.g., France), returns the token as a String.
     * If no token is specified, returns null. If a colon is present, this is
     * much looser than if no colon is present.
     * <p>
     * <b>NOTE: assumes StringBuffer is all lower-case, is trimmed, and
     * that power names DO NOT contain whitespace.</b>
     * <p>
     * This is a special-purpose method for Order parsing.
     * <p>
     * examples:
     * <code>
     * France: xxx-yyy     // returns "France"<br>
     * Fra: xxx-yyy		// returns "Fra" (assumed; it's before the colon)
     * Fra xxx-yyy			// returns null (Fra not recognized)
     * xxx-yyy				// returns null (xxx doesn't match a power)
     * </code>
     */
    public Optional<String> getFirstPowerToken(final StringBuffer sb) {
        assert lcPowerNames != null;

        // if we find a colon, we will ASSUME that the first token
        // is a power, and use getClosestPower(); otherwise, we will
        // just check against the lcPowerNames list.
        if (sb.length() == 0) {
            return Optional.empty();
        }

        // find first white space (or ':')
        final String[] colonTokens = sb.toString().split(":", -1);
        if (colonTokens.length >= 2) {
            return Optional.of(colonTokens[0].trim());
        }
        final String[] spaceTokens = sb.toString().split("\\s", -1);
        if (spaceTokens.length >= 2) {
            return lcPowerNames.stream()
                    .filter(spaceTokens[0].trim()::startsWith).findFirst();
        }
        return Optional.empty();
    }// getFirstPowerToken()


    /**
     * If a power token is specified (e.g., France), returns the token as a String.
     * If no token is specified, returns null. If a colon is present, this is
     * much looser than if no colon is present.
     * <p>
     * <b>NOTE: assumes StringBuffer is all lower-case, is trimmed, and
     * that power names DO NOT contain whitespace.</b>
     * <p>
     * This is a special-purpose method for Order parsing.
     * <p>
     * examples:
     * <code>
     * France: xxx-yyy     // returns "France"<br>
     * Fra: xxx-yyy		// returns "France" (assumed; it's before the colon)
     * Fra xxx-yyy			// returns null (Fra not recognized)
     * xxx-yyy				// returns null (xxx doesn't match a power)
     * </code>
     */
    public Optional<Power> getFirstPower(final String input) {
        assert lcPowerNames != null;

        // if we find a colon, we will ASSUME that the first token
        // is a power, and use getClosestPower(); otherwise, we will
        // just check against the lcPowerNames list.
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }

        final String[] colonTokens = input.split(":", -1);
        if (colonTokens.length >= 2) {
            return getClosestPower(colonTokens[0].trim());
        }
        final String[] spaceTokens = input.split("\\s", -1);
        if (spaceTokens.length >= 2) {
            return lcPowerNames.stream()
                    .filter(spaceTokens[0].trim()::startsWith).findFirst()
                    .flatMap(this::getClosestPower);
        }
        return Optional.empty();
    }// getFirstPower()


    /**
     * Given an index, returns the Province to which that index corresponds.
     */
    public final Province reverseIndex(final int i) {
        return provinces.get(i);
    }// reverseIndex()


    /**
     * Creats the reverse-sorted power name list required by
     * getFirstPowerToken(), filterPowerNames(), and other methods.
     * <p>
     * Includes power adjectives.
     */
    private List<String> createLCPowerNameList() {
        final List<String> tmpNames = new ArrayList<>(powers.stream()
                .flatMap(power -> Arrays.stream(power.getNames()))
                .map(String::toLowerCase).collect(Collectors.toList()));
        tmpNames.addAll(
                powers.stream().map(power -> power.getAdjective().toLowerCase())
                        .collect(Collectors.toList()));

        // sort collection, in reverse alpha order.
        // Why? because we need to ensure power names (and adjectives) like
        // "Russian" come before "Russia"; otherwise, the replacement will be f'd up.
        Collections.sort(tmpNames, Collections.reverseOrder());
        return tmpNames;
    }// createLCPowerNameList()

	
	
	
	/*
        Deprecated
		
		match string against another.
		if src > dest, -1
		higher number == closer!
		not ideal for checking exact match.
		we stop checking at the first letter that doesn't compare.
		assumes: SRC is lower case
		DEST lower case (now...)
	
	
	
	private int getCloseness(String src, String dest)
	{
		if(src.length() > dest.length())
		{
			return -1;
		}
				
		int numCharsMatching = 0;
		for(int i=0; i<src.length(); i++)
		{
			//if(src.charAt(i) != Character.toLowerCase(dest.charAt(i)))		// OLD
			if(src.charAt(i) != dest.charAt(i))
			{
				break;
			}
			
			numCharsMatching++;
		}
		
		return numCharsMatching;
	}// getCloseness()
	*/

    /**
     * Performs a 'best partial match' with a province name (trimmed, all
     * lower case). Returns a List which will be:
     * <ol>
     * <li>Empty, if no match occurs</li>
     * <li>One item, if a single ("best") match occured</li>
     * <li>Multiple items, if ties occur</li>
     * </ol>
     * Null is never returned.
     * <p>
     * For example: given provinces "Liverpool" and "Livonia", and "Loveland":<br>
     * "Li", and "Liv" will return a List of 2 items<br>
     * "Liver" will return a List of 1 item (Liverpool)<br>
     * "Xsdf" will return a List of 0 items.<br>
     * <p>
     * If there are multiple provinces with alternate names that
     * completely match, (different names, same object), only ONE reference
     * to the object will be returned in the collection.
     * <p>
     * The reason this is important is it is more reliable than Levenshtein
     * for matching some types of short strings
     * <p>
     * THIS METHOD REPLACES getCloseness() FOR PROVINCE MATCHING.
     */
    private List<Province> findPartialProvinceMatch(final String input) {
        final Set<Province> ties = IntStream.range(0, lcPowerNames.size())
                .mapToObj(i -> names.get(i))
                .filter(provName -> provName.startsWith(input))
                .map(this::getProvince).collect(Collectors.toSet());
        return new ArrayList<>(ties);
    }// findClosestProvince()


    /**
     * Same as findPartialProvinceMatch(), but for matching powers.
     * <p>
     * THIS METHOD REPLACES getCloseness() FOR POWER MATCHING.
     */
    private List<Power> findPartialPowerMatch(final String input) {
        final Set<Power> ties = lcPowerNames.stream()
                .filter(powerName -> powerName.startsWith(input))
                .map(this::getPower).collect(Collectors.toSet());
        return new ArrayList<>(ties);

    }// findPartialPowerMatch()


    /**
     * Gets a Levenshtein Edit Distance
     * Code by Michael Gilleland, Merriam Park Software
     */
    private static class Distance {
        /**
         * Get minimum of three values
         */
        private static int getMin(final int a, final int b, final int c) {
            int mi;

            mi = a;
            if (b < mi) {
                mi = b;
            }

            if (c < mi) {
                mi = c;
            }

            return mi;
        }// getMin()

        /**
         * Compute Levenshtein distance
         */
        public static int getLD(final String s, final String t) {
            final int[][] d; // matrix
            final int n; // length of s
            final int m; // length of t
            int i; // iterates through s
            int j; // iterates through t
            char s_i; // ith character of s
            char t_j; // jth character of t
            int cost; // cost

            // Step 1
            n = s.length();
            m = t.length();
            if (n == 0) {
                return m;
            }

            if (m == 0) {
                return n;
            }

            d = new int[n + 1][m + 1];

            // Step 2

            for (i = 0; i <= n; i++) {
                d[i][0] = i;
            }

            for (j = 0; j <= m; j++) {
                d[0][j] = j;
            }

            // Step 3
            for (i = 1; i <= n; i++) {
                s_i = s.charAt(i - 1);

                // Step 4
                for (j = 1; j <= m; j++) {
                    t_j = t.charAt(j - 1);

                    // Step 5
                    cost = s_i == t_j ? 0 : 1;

                    // Step 6
                    d[i][j] = getMin(d[i - 1][j] + 1, d[i][j - 1] + 1,
                            d[i - 1][j - 1] + cost);
                }// for(j)
            }// for(i)

            // Step 7
            return d[n][m];
        }// getLD()

    }// inner class Distance


    // reserialization: re-create mappings
    private void readObject(
            final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // re-create transient data.
        createMappings();
    }// readObject()


}// class Map
///////
