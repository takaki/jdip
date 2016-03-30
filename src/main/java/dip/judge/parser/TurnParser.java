//
//  @(#)TurnParser.java	1.00	6/2002
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
package dip.judge.parser;

import dip.misc.Utils;
import dip.world.Phase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Turns of a History file.
 * <p>
 */
public class TurnParser {
    // il8n constants
    private static final String TP_NO_SUBJECT = "JP.turn.nosubject";
    private static final String TP_BAD_PHASE = "JP.turn.badphase";
    private static final String TP_BAD_SUBJECT = "JP.turn.badsubject";

    /**
     * Pattern for matching the phase in the Subject: line<br>
     * xDDDDx pattern. Entire group is used.
     */
    private static final String SUBJ_PHASE_REGEX = "\\s+\\p{Alpha}\\d{4}\\p{Alpha}\\s+";

    /**
     * Pattern for matching the phase in the Subject: line<br>
     * xDDDDx pattern. Entire group is used.
     * For older versions of nJudge
     */
    private static final String SUBJ_PHASE_REGEX_OLD = "\\s+\\p{Alpha}\\d{4}\\p{Alpha}\\s*$";

    /**
     * This text (or pattern) must be present in the Subject: line. If it is not present,
     * the turn is ignored.
     */
    private static final String RESULT_SUBJ_REGEX = "(?i)results";


    // instance variables
    private final List<Turn> turns;


    /**
     * Create the TurnParser and perform parsing.
     */
    public TurnParser(final String input) throws IOException {
        // patterns
        final Pattern subjPhasePattern = Pattern.compile(SUBJ_PHASE_REGEX);
        final Pattern subjPhasePatternOld = Pattern
                .compile(SUBJ_PHASE_REGEX_OLD);
        final Pattern isResultsPattern = Pattern.compile(RESULT_SUBJ_REGEX);


        final LinkedList<Turn> turnList = new LinkedList<>();
        final BufferedReader reader = new BufferedReader(
                new StringReader(input));

        String line = reader.readLine();
        StringBuffer sb = null;
        Turn turn = new Turn();            // current turn
        Turn lastTurn = null;            // previous turn

        while (line != null) {
            final int pos = line.toLowerCase().indexOf("date:");
            if (pos >= 0 && pos < 10) {
                turn.setDateLine(line);

                // set the subject line; if not present, throw an error (shouldn't occur)
                final String nextLine = reader.readLine();
                if (!nextLine.toLowerCase().contains("subject:")) {
                    throw new IOException(Utils.getLocalString(TP_NO_SUBJECT));
                } else if (isResultsPattern.matcher(nextLine).find()) {
                    turn.setSubjectLine(nextLine);

                    // regex parse the subject line
                    final Matcher m = subjPhasePattern.matcher(nextLine);
                    final Matcher m_o = subjPhasePatternOld.matcher(nextLine);
                    if (m.find()) {
                        final Phase phase = Phase.parse(m.group(0).trim())
                                .orElse(null);
                        if (phase == null) {
                            throw new IOException(
                                    Utils.getLocalString(TP_BAD_PHASE,
                                            m.group(0).trim()));
                        }

                        turn.setPhase(phase);
                    } else if (m_o.find()) {
                        final Phase phase = Phase.parse(m_o.group(0).trim())
                                .orElse(null);
                        if (phase == null) {
                            throw new IOException(
                                    Utils.getLocalString(TP_BAD_PHASE,
                                            m_o.group(0).trim()));
                        }

                        turn.setPhase(phase);
                    } else {
                        throw new IOException(
                                Utils.getLocalString(TP_BAD_SUBJECT, nextLine));
                    }

                    if (lastTurn != null) {
                        lastTurn.setText(sb.toString());
                        turnList.add(lastTurn);
                    }

                    lastTurn = turn;
                    turn = new Turn();
                    sb = new StringBuffer(512);
                }
            } else if (sb != null) {
                // accumulate text
                sb.append(line);
                sb.append('\n');
            }

            line = reader.readLine();
        }

        // add last turn
        if (lastTurn != null) {
            lastTurn.setText(sb.toString());
            turnList.add(lastTurn);
        }

        // convert to array
        turns = turnList;
    }// TurnParser()


    /**
     * Returns the turns. If not parsed, or an error occured, it may return null.
     */
    public List<Turn> getTurns() {
        return Collections.unmodifiableList(turns);
    }// getTurns()


    /**
     * A Turn object is created for each Turn detected in the History file.
     */
    public static class Turn {
        private String dateLine;
        private String subjectLine;
        private String text;
        private Phase phase;

        public Turn() {
        }


        /**
         * Sets the unparsed Date: line
         */
        public void setDateLine(final String value) {
            dateLine = value;
        }

        /**
         * Sets the unparsed Subject: line
         */
        public void setSubjectLine(final String value) {
            subjectLine = value;
        }

        /**
         * Sets the PhaseType
         */
        public void setPhase(final Phase value) {
            phase = value;
        }

        /**
         * Sets the text between Subject: and upto (but not including) the next Date: line
         */
        public void setText(final String value) {
            text = value;
        }


        /**
         * Returns the unparsed Date: line
         */
        public String getDateLine() {
            return dateLine;
        }

        /**
         * Returns the unparsed Subject: line
         */
        public String getSubjectLine() {
            return subjectLine;
        }

        /**
         * Returns the PhaseType, or null if it cannot be detected.
         */
        public Phase getPhase() {
            return phase;
        }

        /**
         * Returns the text between Subject: upto (but not including) the next Date: line
         */
        public String getText() {
            return text;
        }


    }// nested class Turn

}// class TurnParser
