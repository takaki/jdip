//
//  @(#)World.java		4/2002
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

import JSX.ObjectReader;
import JSX.ObjectWriter;
import dip.gui.undo.UndoRedoManager;
import dip.world.metadata.GameMetadata;
import dip.world.metadata.PlayerMetadata;
import dip.world.variant.data.VersionNumber;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * The entire game World. This contains the state of an entire game.
 * <p>
 * A World contains:
 * <ol>
 * <li>Map (dip.world.Map) object [constant]
 * <li>TurnState objects [in a linked hash map]
 * <li>HashMap of per-power and global state information (used to set various data)
 * </ol>
 */
public class World implements Serializable {
    // constants for non-turn-data lookup
    private static final String KEY_GLOBAL_DATA = "_global_data_";
    private static final String KEY_VICTORY_CONDITIONS = "_victory_conditions_";

    private static final String KEY_WORLD_METADATA = "_world_metadata_";
    private static final String KEY_UNDOREDOMANAGER = "_undo_redo_manager_";
    private static final String KEY_GAME_SETUP = "_game_setup_";
    private static final String KEY_VARIANT_INFO = "_variant_info_";

    // instance variables
    private SortedMap turnStates;            // turn data
    private Map nonTurnData;            // non-turn data (misc data & per-player data)
    private final WorldMap map;                        // the actual map (constant)


    /**
     * Reads a World object from a file.
     */
    public static World open(final File file) throws IOException {

        try (
                final GZIPInputStream gzi = new GZIPInputStream(
                        new BufferedInputStream(new FileInputStream(file),
                                4096));
                ObjectReader in = new ObjectReader(gzi);) {
            final World w = (World) in.readObject();
            return w;
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }// open()


    /**
     * Saves a World object to a file.
     */
    public static void save(final File file,
                            final World world) throws IOException {

        try (GZIPOutputStream gzos = new GZIPOutputStream(
                new FileOutputStream(file), 2048);
             final ObjectWriter out = new ObjectWriter(gzos)) {
            out.setPrettyPrint(false);
            out.writeObject(world);
            out.close();
            gzos.finish(); // this is key. otherwise data is not written.
        }
    }// save()


    /**
     * Constructs a World object.
     */
    protected World(final WorldMap map) {
        this.map = map;
        turnStates = Collections.synchronizedSortedMap(
                new TreeMap());    // synchronize on TreeMap
        nonTurnData = new HashMap(17);
    }// World()


    /**
     * Returns the Map (dip.world.Map) associated with this World.
     */
    public WorldMap getMap() {
        return map;
    }// getMap()


    /**
     * Set the Victory Conditions
     */
    public void setVictoryConditions(final VictoryConditions value) {
        nonTurnData.put(KEY_VICTORY_CONDITIONS, value);
    }// setVictoryConditions()

    /**
     * Get the Victory Conditions
     */
    public VictoryConditions getVictoryConditions() {
        return (VictoryConditions) nonTurnData.get(KEY_VICTORY_CONDITIONS);
    }// getVictoryConditions()


    /**
     * Gets the first TurnState object
     */
    public TurnState getInitialTurnState() {
        final TurnState ts = (TurnState) turnStates.get(turnStates.firstKey());
        if (ts != null) {
            ts.setWorld(this);
        }
        return ts;
    }// getInitialTurnState()


    /**
     * Gets the most current (last in the list) TurnState.
     */
    public TurnState getLastTurnState() {
        final TurnState ts = (TurnState) turnStates.get(turnStates.lastKey());
        if (ts != null) {
            ts.setWorld(this);
        }
        return ts;
    }// getLastTurnState()


    /**
     * Gets the TurnState associated with the specified Phase
     */
    public TurnState getTurnState(final Phase phase) {
        final TurnState ts = (TurnState) turnStates.get(phase);
        if (ts != null) {
            ts.setWorld(this);
        }
        return ts;
    }// getTurnState()


    /**
     * Gets the TurnState that comes after this phase (if it exists).
     * <p>
     * Note that the next phase may not be (due to phase skipping) the
     * same phase generated by phase.getNext(). This will return null
     * iff we are at the last Phase.
     */
    public Optional<TurnState> getNextTurnState(final TurnState state) {
        final Phase current = state.getPhase();
        if (current == null) {
            return Optional.empty();
        }

        Phase next = null;
        final Iterator iter = turnStates.keySet().iterator();
        while (iter.hasNext()) {
            final Phase phase = (Phase) iter.next();
            if (current.compareTo(phase) == 0) {
                if (iter.hasNext()) {
                    next = (Phase) iter.next();
                }

                break;
            }
        }

        if (next == null) {
            return Optional.empty();
        }

        final TurnState ts = (TurnState) turnStates.get(next);
        ts.setWorld(this);
        return Optional.of(ts);
    }// getNextTurnState()


    /**
     * Get all TurnStates. Note that the returned List
     * may be modified, without modifications being reflected
     * in the World object. However, modifications to individual
     * TurnState objects will be reflected in the World object
     * (TurnStates are not cloned here).
     */
    public List getAllTurnStates() {
        final Collection values = turnStates.values();
        final ArrayList al = new ArrayList(values.size());
        al.addAll(values);
        return al;
    }// getAllTurnStates()


    /**
     * Gets the TurnState that comes before the specified phase.
     * <p>
     * Note that the previous phase may not be (due to phase skipping) the
     * same phase generated by phase.getPrevious(). This will return null
     * iff we are at the first (initial) Phase.
     */
    public Optional<TurnState> getPreviousTurnState(final TurnState state) {
        final Phase current = state.getPhase();
        if (current == null) {
            return Optional.empty();
        }


        Phase previous = null;
        final Iterator iter = turnStates.keySet().iterator();
        while (iter.hasNext()) {
            final Phase phase = (Phase) iter.next();
            if (phase.compareTo(current) != 0) {
                previous = phase;
            } else {
                break;
            }
        }

        if (previous == null) {
            return Optional.empty();
        }

        final TurnState ts = (TurnState) turnStates.get(previous);
        ts.setWorld(this);
        return Optional.of(ts);
    }// getPreviousTurnState()


    /**
     * If a TurnState with the given phase already exists, it is replaced.
     */
    public void setTurnState(final TurnState turnState) {
        turnStates.put(turnState.getPhase(), turnState);
    }// setTurnState()


    /**
     * Removes a turnstate from the world. This should
     * be used with caution!
     */
    public void removeTurnState(final TurnState turnState) {
        turnStates.remove(turnState.getPhase());
    }// removeTurnState()


    /**
     * Removes <b>all</b> TurnStates from the World.
     */
    public void removeAllTurnStates() {
        turnStates.clear();
    }// removeAllTurnStates()


    /**
     * returns sorted (ascending) set of all Phases
     */
    public Set getPhaseSet() {
        return turnStates.keySet();
    }// getPhaseSet()


    /**
     * Sets the Game metadata
     */
    public void setGameMetadata(final GameMetadata gmd) {
        Objects.requireNonNull(gmd);
        nonTurnData.put(KEY_WORLD_METADATA, gmd);
    }// setGameMetadata()

    /**
     * Gets the Game metadata. Never returns null. Does not return a copy.
     */
    public GameMetadata getGameMetadata() {
        GameMetadata gmd = (GameMetadata) nonTurnData.get(KEY_WORLD_METADATA);
        if (gmd == null) {
            gmd = new GameMetadata();
            setGameMetadata(gmd);
        }
        return gmd;
    }// setGameMetadata()


    /**
     * Sets the metadata for a player, referenced by Power
     */
    public void setPlayerMetadata(final Power power, final PlayerMetadata pmd) {
        Objects.requireNonNull(power);
        Objects.requireNonNull(pmd);
        nonTurnData.put(power, pmd);
    }// setPlayerMetadata()

    /**
     * Gets the metadata for a power. Never returns null. Does not return a copy.
     */
    public PlayerMetadata getPlayerMetadata(final Power power) {
        Objects.requireNonNull(power);

        PlayerMetadata pmd = (PlayerMetadata) nonTurnData.get(power);
        if (pmd == null) {
            pmd = new PlayerMetadata();
            setPlayerMetadata(power, pmd);
        }
        return pmd;
    }// getPlayerMetadata()


    /**
     * Sets the UndoRedo manager to be saved. This may be set to null.
     */
    public void setUndoRedoManager(final UndoRedoManager urm) {
        nonTurnData.put(KEY_UNDOREDOMANAGER, urm);
    }// setGlobalState()


    /**
     * Gets the UndoRedo manager that was saved. Null if none was saved.
     */
    public UndoRedoManager getUndoRedoManager() {
        return (UndoRedoManager) nonTurnData.get(KEY_UNDOREDOMANAGER);
    }// getUndoRedoManager()


    /**
     * Sets the GameSetup object
     */
    public void setGameSetup(final GameSetup gs) {
        Objects.requireNonNull(gs);
        nonTurnData.put(KEY_GAME_SETUP, gs);
    }// setGameSetup()


    /**
     * Returns the GameSetup object
     */
    public GameSetup getGameSetup() {
        return (GameSetup) nonTurnData.get(KEY_GAME_SETUP);
    }// getGameSetup()


    /**
     * Get the Variant Info object. This returns a Reference to the Variant information.
     */
    public synchronized VariantInfo getVariantInfo() {
        VariantInfo vi = (VariantInfo) nonTurnData.get(KEY_VARIANT_INFO);

        if (vi == null) {
            vi = new VariantInfo();
            nonTurnData.put(KEY_VARIANT_INFO, vi);
        }

        return vi;
    }// getVariantInfo()

    /**
     * Set the Variant Info object.
     */
    public synchronized void setVariantInfo(final VariantInfo vi) {
        nonTurnData.put(KEY_VARIANT_INFO, vi);
    }// getVariantInfo()


    /**
     * Convenience method: gets RuleOptions from VariantInfo object.
     */
    public RuleOptions getRuleOptions() {
        return getVariantInfo().getRuleOptions();
    }// getRuleOptions()

    /**
     * Convenience method: sets RuleOptions in VariantInfo object.
     */
    public void setRuleOptions(final RuleOptions ruleOpts) {
        Objects.requireNonNull(ruleOpts);
        getVariantInfo().setRuleOptions(ruleOpts);
    }// getRuleOptions()


    /**
     * Variant Info is a class which holds information about
     * the variant, map, symbols, and symbol options.
     */
    public static final class VariantInfo {
        private String variantName;
        private String mapName;
        private String symbolsName;
        private VersionNumber variantVersion;
        private VersionNumber symbolsVersion;
        private RuleOptions ruleOptions = new RuleOptions();

        /**
         * Create a VariantInfo object
         */
        public VariantInfo() {
        }

        /**
         * Set the Variant name.
         */
        public void setVariantName(final String value) {
            variantName = value;
        }

        /**
         * Set the Map name.
         */
        public void setMapName(final String value) {
            mapName = value;
        }

        /**
         * Set the Symbol pack name.
         */
        public void setSymbolPackName(final String value) {
            symbolsName = value;
        }

        /**
         * Set the Variant version.
         *
         * @param value
         */
        public void setVariantVersion(final VersionNumber value) {
            checkVersion(value);
            variantVersion = value;
        }

        /**
         * Set the Symbol pack version.
         *
         * @param value
         */
        public void setSymbolPackVersion(final VersionNumber value) {
            checkVersion(value);
            symbolsVersion = value;
        }

        /**
         * <b>Replaces</b> the current RuleOptions with the given RuleOptions
         */
        public void setRuleOptions(final RuleOptions value) {
            ruleOptions = value;
        }


        /**
         * Get the Variant name.
         */
        public String getVariantName() {
            return variantName;
        }

        /**
         * Get the Map name.
         */
        public String getMapName() {
            return mapName;
        }

        /**
         * Get the Symbol pack name.
         */
        public String getSymbolPackName() {
            return symbolsName;
        }

        /**
         * Get the Variant version.
         */
        public VersionNumber getVariantVersion() {
            return variantVersion;
        }

        /**
         * Get the Symbol pack version.
         */
        public VersionNumber getSymbolPackVersion() {
            return symbolsVersion;
        }

        /**
         * Gets the RuleOptions
         */
        public RuleOptions getRuleOptions() {
            return ruleOptions;
        }// getRuleOptions()


        /**
         * ensures Version is a value &gt;0.0f
         *
         * @param v
         */
        private static void checkVersion(final VersionNumber v) {
            if (v.compareTo(new VersionNumber(0, 0)) <= 0) {
                throw new IllegalArgumentException("version: " + v);
            }
        }// checkVersion();
    }// nested class VariantInfo


}// class World
