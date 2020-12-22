package com.gmail.nossr50.datatypes.player;

import com.gmail.nossr50.config.AdvancedConfig;
import com.gmail.nossr50.datatypes.skills.CoreSkillConstants;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.datatypes.validation.NonNullRule;
import com.gmail.nossr50.datatypes.validation.PositiveIntegerRule;
import com.gmail.nossr50.datatypes.validation.Validator;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.experience.MMOExperienceBarManager;
import com.google.common.collect.ImmutableMap;
import com.neetgames.mcmmo.MobHealthBarType;
import com.neetgames.mcmmo.UniqueDataType;
import com.neetgames.mcmmo.exceptions.UnexpectedValueException;
import com.neetgames.mcmmo.skill.*;
import com.neetgames.mcmmo.player.MMOPlayerData;
import com.neetgames.neetlib.dirtydata.DirtyData;
import com.neetgames.neetlib.dirtydata.DirtyMap;
import com.neetgames.neetlib.mutableprimitives.MutableBoolean;
import com.neetgames.neetlib.mutableprimitives.MutableInteger;
import com.neetgames.neetlib.mutableprimitives.MutableLong;
import com.neetgames.neetlib.mutableprimitives.MutableString;
import org.apache.commons.lang.NullArgumentException;
import org.jetbrains.annotations.NotNull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PersistentPlayerData implements MMOPlayerData {

    private final @NotNull MutableBoolean dirtyFlag; //Dirty values in this class will change this flag as needed

    /* Player Stuff */
    private final @NotNull DirtyData<MutableString> playerName;
    private final @NotNull UUID playerUUID;

    /* Records */
    private final DirtyData<MutableLong> lastLogin;

    /* Skill Data */
    private final @NotNull DirtyMap<SkillIdentity, Integer> skillLevelValues;
    private final @NotNull DirtyMap<SkillIdentity, Float> skillExperienceValues;
    private final @NotNull DirtyMap<SkillIdentity, Integer> abilityDeactivationTimestamps; // Ability & Cooldown
    private final @NotNull DirtyMap<UniqueDataType, Integer> uniquePlayerData; //Misc data that doesn't fit into other categories (chimaera wing, etc..)
    private final @NotNull DirtyMap<SkillIdentity, SkillBossBarState> barStateMap;

    /* Special Flags */
    private final @NotNull DirtyData<MutableBoolean> partyChatSpying;
    private final @NotNull DirtyData<MutableBoolean> leaderBoardExclusion;

    /* Scoreboards */
    private final @NotNull DirtyData<MutableInteger> scoreboardTipsShown;

    /**
     * Create new persistent player data for a player
     * Initialized with default values
     *
     * @param playerUUID target player's UUID
     * @param playerName target player's name
     * @throws NullArgumentException thrown when never null arguments are null
     */
    public PersistentPlayerData(@NotNull UUID playerUUID, @NotNull String playerName) throws NullArgumentException {
        /*
         * New Data
         */
        this.dirtyFlag = new MutableBoolean(false); //Set this one first
        this.playerUUID = playerUUID;
        this.playerName = new DirtyData<>(new MutableString(playerName), dirtyFlag);

        this.skillLevelValues = new DirtyMap<>(new HashMap<>(), dirtyFlag);
        this.skillExperienceValues = new DirtyMap<>(new HashMap<>(), dirtyFlag);
        this.abilityDeactivationTimestamps = new DirtyMap<>(new HashMap<>(), dirtyFlag);
        this.uniquePlayerData = new DirtyMap<>(new EnumMap<>(UniqueDataType.class), dirtyFlag);

        this.scoreboardTipsShown = new DirtyData<>(new MutableInteger(0), dirtyFlag);

        for(RootSkill rootSkill : mcMMO.p.getSkillRegister().getRootSkills()) {
            abilityDeactivationTimestamps.put(rootSkill.getSkillIdentity(), 0);
        }

        //Core skills
        //TODO: Don't store values for disabled skills
        for(RootSkill rootSkill : CoreSkillConstants.getImmutableCoreRootSkillSet()) {
            skillLevelValues.put(rootSkill.getSkillIdentity(), AdvancedConfig.getInstance().getStartingLevel());
            skillExperienceValues.put(rootSkill.getSkillIdentity(), 0F);
        }

        //Unique Player Data
        this.uniquePlayerData.put(UniqueDataType.CHIMAERA_WING_DATS, 0);

        this.partyChatSpying = new DirtyData<>(new MutableBoolean(false), dirtyFlag);

        this.barStateMap = new DirtyMap<>(MMOExperienceBarManager.generateDefaultBarStateMap(), dirtyFlag);
        this.lastLogin = new DirtyData<>(new MutableLong(0), dirtyFlag); //Value of 0 will represent that the user hasn't been seen online
        this.leaderBoardExclusion = new DirtyData<>(new MutableBoolean(false), dirtyFlag);
    }

    /**
     * Create persistent player data for a player using existing values
     * Typically this will be used when loading data
     * @param playerUUID target player's {@link UUID}
     * @param playerName target player's saved name
     * @param partyChatSpying target player's chat spy toggle
     * @param skillLevelValues target player's skill levels
     * @param skillExperienceValues target player's skill experience levels
     * @param abilityDeactivationTimestamps target player's ability deactivation time stamps
     * @param uniquePlayerData target player's misc unique data
     * @param barStateMap target player's XP bar state settings
     * @param scoreboardTipsShown target player's scoreboard tip view count
     * @param lastLogin target player's last login
     * @param leaderBoardExclusion target player's leaderboard exemption status
     */
    public PersistentPlayerData(@NotNull UUID playerUUID,
                                @NotNull String playerName,
                                boolean partyChatSpying,
                                @NotNull EnumMap<PrimarySkillType, Integer> skillLevelValues,
                                @NotNull EnumMap<PrimarySkillType, Float> skillExperienceValues,
                                @NotNull EnumMap<SuperAbilityType, Integer> abilityDeactivationTimestamps,
                                @NotNull EnumMap<UniqueDataType, Integer> uniquePlayerData,
                                @NotNull EnumMap<PrimarySkillType, SkillBossBarState> barStateMap,
                                int scoreboardTipsShown,
                                long lastLogin,
                                boolean leaderBoardExclusion) throws Exception {

        /*
         * Skills Data
         */
        this.dirtyFlag = new MutableBoolean(false); //Set this one first

        validateMap(skillLevelValues);
        this.skillLevelValues = new DirtyMap<>(skillLevelValues, dirtyFlag);

        validateMap(skillExperienceValues);
        this.skillExperienceValues = new DirtyMap<>(skillExperienceValues, dirtyFlag);

        validateMap(abilityDeactivationTimestamps);
        this.abilityDeactivationTimestamps = new DirtyMap<>(abilityDeactivationTimestamps, dirtyFlag);

        this.uniquePlayerData = new DirtyMap<>(uniquePlayerData, dirtyFlag);

        this.scoreboardTipsShown = new DirtyData<>(new MutableInteger(scoreboardTipsShown), dirtyFlag);

        this.playerUUID = playerUUID;
        this.playerName = new DirtyData<>(new MutableString(playerName), dirtyFlag);
        this.barStateMap = new DirtyMap<>(barStateMap, dirtyFlag);

        this.partyChatSpying = new DirtyData<>(new MutableBoolean(partyChatSpying), dirtyFlag);
        this.lastLogin = new DirtyData<>(new MutableLong(lastLogin), dirtyFlag);

        this.leaderBoardExclusion = new DirtyData<>(new MutableBoolean(leaderBoardExclusion), dirtyFlag);
    }

    /**
     * Makes sure a target map only contains positive numbers and no null values for its keyset
     * @param hashMap target map
     * @throws UnexpectedValueException when values are outside of expected norms
     * @throws Exception when values are outside of expected norms
     */
    private void validateMap(Map<? extends Enum<?>, ? extends Number> hashMap) throws UnexpectedValueException, Exception {
        Validator<Number> validator = new Validator<>();

        validator.addRule(new PositiveIntegerRule<>());
        validator.addRule(new NonNullRule<>());

        for(PrimarySkillType primarySkillType : PrimarySkillType.values()) {
            validator.validate(hashMap.get(primarySkillType));
        }
    }

    /**
     * Set the level of a Primary Skill for the Player
     * @param primarySkillType target Primary Skill
     * @param newSkillLevel the new value of the skill
     */
    @Override
    public void setSkillLevel(@NotNull SkillIdentity skillIdentity, int newSkillLevel) {
        skillLevelValues.put(skillIdentity, newSkillLevel);
    }

    /**
     * Get the skill level the player currently has for target Primary Skill
     * @param primarySkillType target Primary Skill
     * @return the current level value of target Primary Skill
     */
    public Integer getSkillLevel(PrimarySkillType primarySkillType) {
        return skillLevelValues.get(primarySkillType);
    }

    @Override
    public void setSkillLevel(@NotNull RootSkill rootSkill, int i) {

    }

    @Override
    public int getSkillLevel(@NotNull RootSkill rootSkill) {
        return 0;
    }

    /**
     * True if the persistent data has changed state and not yet saved to DB
     * @return true if data is dirty (not saved)
     */
    public boolean isDirtyProfile() {
        return dirtyFlag.getImmutableCopy();
    }

    /**
     * Set the dirty flag back to false
     * Should be called after saving the player data to avoid unnecessary saves
     */
    public void resetDirtyFlag() {
        dirtyFlag.setBoolean(false);
    }

    /**
     * The saved player name for the player associated with this data
     * @return the saved player name for the player associated with this data
     */
    public @NotNull String getPlayerName() {
        return playerName.getData().getImmutableCopy();
    }

    /**
     * The {@link UUID} for the player associated with this data
     * @return the UUID for the player associated with this data
     */
    public @NotNull UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * This player's saved mob health bar type
     * @return the saved mob health bar type for this player
     */
    public @NotNull MobHealthBarType getMobHealthBarType() {
        return mobHealthBarType.getData();
    }

    /**
     * Change the mob health bar type for this player
     * @param mobHealthBarType the new mob health bar type for this player
     */
    public void setMobHealthBarType(@NotNull MobHealthBarType mobHealthBarType) {
        this.mobHealthBarType.setData(mobHealthBarType);
    }

    /*
     * Party Chat Spy
     */

    /**
     * Whether or not this player is currently spying on all party chat
     * @return true if this player is spying on party chat
     */
    public boolean isPartyChatSpying() { return partyChatSpying.getData().getImmutableCopy(); }

    /**
     * Toggle this player's party chat spying
     */
    public void togglePartyChatSpying() {
        partyChatSpying.getData().setBoolean(!partyChatSpying.getData().getImmutableCopy());
    }

    /**
     * Modify whether or not this player is spying on party chat
     * @param bool the new value of party chat spying (true for spying, false for not spying)
     */
    public void setPartyChatSpying(boolean bool) {
        this.partyChatSpying.getData().setBoolean(bool);
    }

    /*
     * Scoreboards
     */

    /**
     * The currently tracked number of times scoreboard tips have been viewed for this player
     * @return the currently tracked number of times scoreboard tips have been viewed for this player
     */
    public int getScoreboardTipsShown() {
        return scoreboardTipsShown.getData(false).getImmutableCopy();
    }

    /**
     * Modify the count of how many times scoreboard tips have been displayed to this player
     * @param newValue the new value
     */
    public void setScoreboardTipsShown(int newValue) {
        scoreboardTipsShown.getData(true).setInt(newValue);
    }

    /*
     * Cooldowns
     */

    /**
     * The time stamp for the last Chimaera Wing use for this player
     * @return the Chimaera Wing last use time stamp for this player
     */
    public int getChimaeraWingDATS() {
        return uniquePlayerData.get((UniqueDataType.CHIMAERA_WING_DATS));
    }

    /**
     * Set the time stamp for Chimaera Wing's last use for this player
     * @param DATS the new time stamp
     */
    public void setChimaeraWingDATS(int DATS) {
        uniquePlayerData.put(UniqueDataType.CHIMAERA_WING_DATS, DATS);
    }

    /**
     * Change one of the unique data map entries
     * @param uniqueDataType target unique data
     * @param newData new unique data value
     */
    public void setUniqueData(@NotNull UniqueDataType uniqueDataType, int newData) {
        uniquePlayerData.put(uniqueDataType, newData);
    }

    /**
     * Get the value associated with a specific {@link UniqueDataType}
     * @param uniqueDataType target unique data
     * @return associated value of this unique data
     */
    public long getUniqueData(@NotNull UniqueDataType uniqueDataType) { return uniquePlayerData.get(uniqueDataType); }

    @Override
    public long getAbilityDATS(@NotNull SuperSkill superSkill) {
        return 0;
    }

    @Override
    public void setAbilityDATS(@NotNull SuperSkill superSkill, long l) {

    }

    /**
     * Get the current deactivation timestamp of an superAbilityType.
     *
     * @param superAbilityType The {@link SuperAbilityType} to get the DATS for
     * @return the deactivation timestamp for the superAbilityType
     */
    public long getAbilityDATS(@NotNull SuperAbilityType superAbilityType) {
        return abilityDeactivationTimestamps.get(superAbilityType);
    }

    /**
     * Set the current deactivation timestamp of an superAbilityType.
     *
     * @param superAbilityType The {@link SuperAbilityType} to set the DATS for
     * @param DATS the DATS of the superAbilityType
     */
    public void setAbilityDATS(@NotNull SuperAbilityType superAbilityType, long DATS) {
        abilityDeactivationTimestamps.put(superAbilityType, (int) (DATS * .001D));
    }

    /**
     * Reset all ability cooldowns.
     */
    public void resetCooldowns() {
        abilityDeactivationTimestamps.replaceAll((a, v) -> 0);
    }

    /**
     * Get the {@link Map} for the related {@link SkillBossBarState}'s of this player
     * @return the bar state map for this player
     */
    public @NotNull Map<PrimarySkillType, SkillBossBarState> getBarStateMap() {
        return barStateMap;
    }

    /**
     * Get the {@link DirtyMap} for the related {@link SkillBossBarState}'s of this player
     * @return the dirty bar state map for this player
     */
    public @NotNull DirtyMap<PrimarySkillType, SkillBossBarState> getDirtyBarStateMap() {
        return barStateMap;
    }

    /**
     * Get the {@link DirtyMap} for the skill levels of this player
     * @return the dirty skill level map for this player
     */
    public @NotNull DirtyMap<PrimarySkillType, Integer> getDirtySkillLevelMap() {
        return skillLevelValues;
    }

    /**
     * Get the {@link DirtyMap} for the skill experience values of this player
     * @return the dirty skill experience values map for this player
     */
    public @NotNull DirtyMap<PrimarySkillType, Float> getDirtyExperienceValueMap() {
        return skillExperienceValues;
    }

    /**
     * Get the {@link DirtyData<MutableBoolean>} for the party chat toggle for this player
     * @return the dirty data for the party chat toggle for this player
     */
    public @NotNull DirtyData<MutableBoolean> getDirtyPartyChatSpying() {
        return partyChatSpying;
    }

    /**
     * Get the skill level map for this player
     * @return the map of skill levels for this player
     */
    public @NotNull Map<PrimarySkillType, Integer> getSkillLevelsMap() {
        return skillLevelValues;
    }

    /**
     * Get the map of experience values for skills for this player
     * @return the experience values map for this player
     */
    public @NotNull Map<PrimarySkillType, Float> getSkillsExperienceMap() {
        return skillExperienceValues;
    }

    /**
     * Get the map of timestamps representing the last use of abilities for this player
     * @return the ability deactivation timestamps map for this player
     */
    public @NotNull Map<SuperAbilityType, Integer> getAbilityDeactivationTimestamps() {
        return abilityDeactivationTimestamps;
    }

    /**
     * Get a map of various unique data for this player
     * @return a map of unique data for this player
     */
    public @NotNull Map<UniqueDataType, Integer> getUniquePlayerData() {
        return uniquePlayerData;
    }

    /**
     * Mark this data as dirty which will flag this data for the next appropriate save
     * Saves happen periodically, they also can happen on server shutdown and when the player disconnects from the server
     */
    public void setDirtyProfile() {
        this.dirtyFlag.setBoolean(true);
    }

    /**
     * The timestamp of when this player last logged in
     * @return the timestamp of when this player last logged in
     */
    public long getLastLogin() {
        return lastLogin.getData().getImmutableCopy();
    }

    /**
     * Set the value of when this player last logged in
     * @param newValue the new time stamp
     */
    public void setLastLogin(long newValue) {
        lastLogin.getData().setLong(newValue);
    }

    /**
     * Whether or not this player is exempt from leader boards
     * @return true if excluded from leader boards
     */
    public boolean isLeaderBoardExcluded() {
        return leaderBoardExclusion.getData().getImmutableCopy();
    }

    /**
     * Set whether or not this player is excluded from leader boards
     * @param bool new value
     */
    public void setLeaderBoardExclusion(boolean bool) {
        leaderBoardExclusion.getData(true).setBoolean(bool);
    }

    public ImmutableMap<PrimarySkillType, Integer> copyPrimarySkillLevelsMap() {
        return ImmutableMap.copyOf(getSkillLevelsMap());
    }

    public ImmutableMap<PrimarySkillType, Float> copyPrimarySkillExperienceValuesMap() {
        return ImmutableMap.copyOf(getSkillsExperienceMap());
    }
}