package com.farmingnavigator.account;

import java.util.EnumMap;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

public class AccountStateService
{
    /*
     * Making Friends with My Arm uses quest-progress
     * varbit 6528.
     *
     * Current cache data also uses this exact varbit to
     * morph the Weiss fire pit between:
     *
     * Old fire pit
     * and
     * Fire of Nourishment.
     *
     * Quest completion occurs at >= 200.
     * Construction of the Fire of Nourishment advances
     * the persistent state to 205.
     */
    private static final int
            MAKING_FRIENDS_WITH_MY_ARM_PROGRESS_VARBIT =
            6528;

    private static final int
            FIRE_OF_NOURISHMENT_BUILT_PROGRESS =
            205;

    /*
     * Current Quest Helper maps the Fairytale II quest
     * progress varbit to the following progression:
     *
     * 0  = not started
     * 10 = returned to Martin
     * 20+ = investigating Fairy Nuff's disappearance
     * 40 = Fairy Godfather conversation completed and
     *      fairy-ring access granted
     *
     * Later quest values remain fairy-ring capable, so
     * the persistent unlock test is >= 40.
     */
    private static final int
            FAIRY_RING_ACCESS_PROGRESS =
            40;

    private static final Quest[] RELEVANT_QUESTS = {
            /*
             * Morytania / Harmony / Port Phasmatys routes.
             */
            Quest.PRIEST_IN_PERIL,
            Quest.GHOSTS_AHOY,
            Quest.CREATURE_OF_FENKENSTRAIN,
            Quest.CABIN_FEVER,
            Quest.THE_GREAT_BRAIN_ROBBERY,

            /*
             * Ardougne.
             */
            Quest.PLAGUE_CITY,

            /*
             * Kourend and Hosidius transportation.
             */
            Quest.CLIENT_OF_KOUREND,
            Quest.THE_DEPTHS_OF_DESPAIR,

            /*
             * Troll Stronghold and Weiss.
             */
            Quest.EADGARS_RUSE,
            Quest.MY_ARMS_BIG_ADVENTURE,
            Quest.MAKING_FRIENDS_WITH_MY_ARM,

            /*
             * Lunar transportation.
             */
            Quest.LUNAR_DIPLOMACY,

            /*
             * Varlamore transportation.
             */
            Quest.CHILDREN_OF_THE_SUN,
            Quest.TWILIGHTS_PROMISE,
            Quest.AT_FIRST_LIGHT,

            /*
             * Fairy rings and Morytania access chains.
             */
            Quest.THE_RESTLESS_GHOST,
            Quest.NATURE_SPIRIT,
            Quest.LOST_CITY,
            Quest.FAIRYTALE_I__GROWING_PAINS,
            Quest.FAIRYTALE_II__CURE_A_QUEEN,

            /*
             * Spirit tree network.
             */
            Quest.TREE_GNOME_VILLAGE,
            Quest.THE_GRAND_TREE
    };

    private final Client client;

    @Inject
    public AccountStateService(
            Client client
    )
    {
        this.client =
                client;
    }

    public AccountState createSnapshot()
    {
        if (
                client.getGameState()
                        != GameState.LOGGED_IN
        )
        {
            return AccountState.loggedOut();
        }

        int realFarmingLevel =
                client.getRealSkillLevel(
                        Skill.FARMING
                );

        int boostedFarmingLevel =
                client.getBoostedSkillLevel(
                        Skill.FARMING
                );

        int realMagicLevel =
                client.getRealSkillLevel(
                        Skill.MAGIC
                );

        int boostedMagicLevel =
                client.getBoostedSkillLevel(
                        Skill.MAGIC
                );

        int realAgilityLevel =
                client.getRealSkillLevel(
                        Skill.AGILITY
                );

        int boostedAgilityLevel =
                client.getBoostedSkillLevel(
                        Skill.AGILITY
                );

        int realHunterLevel =
                client.getRealSkillLevel(
                        Skill.HUNTER
                );

        int boostedHunterLevel =
                client.getBoostedSkillLevel(
                        Skill.HUNTER
                );

        EnumMap<Quest, QuestState> questStates =
                readQuestStates();

        EnumMap<DiaryArea, DiaryTier>
                completedDiaryTiers =
                readCompletedDiaryTiers();

        EnumMap<DiaryArea, DiaryTier>
                claimedDiaryTiers =
                readClaimedDiaryTiers();

        boolean fireOfNourishmentBuilt =
                readFireOfNourishmentBuilt();

        boolean fairyRingAccessUnlocked =
                readFairyRingAccessUnlocked();

        boolean varlamoreVisited =
                readVarlamoreVisited();

        return new AccountState(
                true,
                realFarmingLevel,
                boostedFarmingLevel,
                realMagicLevel,
                boostedMagicLevel,
                realAgilityLevel,
                boostedAgilityLevel,
                realHunterLevel,
                boostedHunterLevel,
                questStates,
                completedDiaryTiers,
                claimedDiaryTiers,
                fireOfNourishmentBuilt,
                fairyRingAccessUnlocked,
                varlamoreVisited
        );
    }

    private EnumMap<Quest, QuestState>
    readQuestStates()
    {
        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        for (
                Quest quest
                : RELEVANT_QUESTS
        )
        {
            questStates.put(
                    quest,
                    quest.getState(
                            client
                    )
            );
        }

        return questStates;
    }

    private boolean readFireOfNourishmentBuilt()
    {
        int progress =
                client.getVarbitValue(
                        MAKING_FRIENDS_WITH_MY_ARM_PROGRESS_VARBIT
                );

        return progress
                >= FIRE_OF_NOURISHMENT_BUILT_PROGRESS;
    }

    private boolean readFairyRingAccessUnlocked()
    {
        int progress =
                client.getVarbitValue(
                        VarbitID.FAIRY2_QUEENCURE_QUEST
                );

        return hasFairyRingAccessFromProgress(
                progress
        );
    }

    static boolean hasFairyRingAccessFromProgress(
            int progress
    )
    {
        return progress
                >= FAIRY_RING_ACCESS_PROGRESS;
    }

    private boolean readVarlamoreVisited()
    {
        int visited =
                client.getVarbitValue(
                        VarbitID.VARLAMORE_VISITED
                );

        return hasVisitedVarlamoreFromVarbit(
                visited
        );
    }

    static boolean hasVisitedVarlamoreFromVarbit(
            int value
    )
    {
        return value > 0;
    }

    private EnumMap<DiaryArea, DiaryTier>
    readCompletedDiaryTiers()
    {
        EnumMap<DiaryArea, DiaryTier> tiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        tiers.put(
                DiaryArea.ARDOUGNE,
                readHighestTier(
                        VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE,
                        VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE,
                        VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,
                        VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE
                )
        );

        tiers.put(
                DiaryArea.FALADOR,
                readHighestTier(
                        VarbitID.FALADOR_DIARY_EASY_COMPLETE,
                        VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE,
                        VarbitID.FALADOR_DIARY_HARD_COMPLETE,
                        VarbitID.FALADOR_DIARY_ELITE_COMPLETE
                )
        );

        tiers.put(
                DiaryArea.FREMENNIK,
                readHighestTier(
                        VarbitID.FREMENNIK_DIARY_EASY_COMPLETE,
                        VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE,
                        VarbitID.FREMENNIK_DIARY_HARD_COMPLETE,
                        VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE
                )
        );

        tiers.put(
                DiaryArea.KANDARIN,
                readHighestTier(
                        VarbitID.KANDARIN_DIARY_EASY_COMPLETE,
                        VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE,
                        VarbitID.KANDARIN_DIARY_HARD_COMPLETE,
                        VarbitID.KANDARIN_DIARY_ELITE_COMPLETE
                )
        );

        tiers.put(
                DiaryArea.KOUREND_AND_KEBOS,
                readHighestTier(
                        VarbitID.KOUREND_DIARY_EASY_COMPLETE,
                        VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE,
                        VarbitID.KOUREND_DIARY_HARD_COMPLETE,
                        VarbitID.KOUREND_DIARY_ELITE_COMPLETE
                )
        );

        tiers.put(
                DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                readHighestTier(
                        VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE,
                        VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE,
                        VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE,
                        VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE
                )
        );

        tiers.put(
                DiaryArea.MORYTANIA,
                readHighestTier(
                        VarbitID.MORYTANIA_DIARY_EASY_COMPLETE,
                        VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE,
                        VarbitID.MORYTANIA_DIARY_HARD_COMPLETE,
                        VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE
                )
        );

        return tiers;
    }

    private EnumMap<DiaryArea, DiaryTier>
    readClaimedDiaryTiers()
    {
        EnumMap<DiaryArea, DiaryTier> tiers =
                new EnumMap<>(
                        DiaryArea.class
                );

        tiers.put(
                DiaryArea.ARDOUGNE,
                readHighestTier(
                        VarbitID.ARDOUGNE_EASY_REWARD,
                        VarbitID.ARDOUGNE_MEDIUM_REWARD,
                        VarbitID.ARDOUGNE_HARD_REWARD,
                        VarbitID.ARDOUGNE_ELITE_REWARD
                )
        );

        tiers.put(
                DiaryArea.FALADOR,
                readHighestTier(
                        VarbitID.FALADOR_EASY_REWARD,
                        VarbitID.FALADOR_MEDIUM_REWARD,
                        VarbitID.FALADOR_HARD_REWARD,
                        VarbitID.FALADOR_ELITE_REWARD
                )
        );

        tiers.put(
                DiaryArea.FREMENNIK,
                readHighestTier(
                        VarbitID.FREMENNIK_EASY_REWARD,
                        VarbitID.FREMENNIK_MEDIUM_REWARD,
                        VarbitID.FREMENNIK_HARD_REWARD,
                        VarbitID.FREMENNIK_ELITE_REWARD
                )
        );

        tiers.put(
                DiaryArea.KANDARIN,
                readHighestTier(
                        VarbitID.KANDARIN_EASY_REWARD,
                        VarbitID.KANDARIN_MEDIUM_REWARD,
                        VarbitID.KANDARIN_HARD_REWARD,
                        VarbitID.KANDARIN_ELITE_REWARD
                )
        );

        tiers.put(
                DiaryArea.KOUREND_AND_KEBOS,
                readHighestTier(
                        VarbitID.KOUREND_EASY_REWARD,
                        VarbitID.KOUREND_MEDIUM_REWARD,
                        VarbitID.KOUREND_HARD_REWARD,
                        VarbitID.KOUREND_ELITE_REWARD
                )
        );

        tiers.put(
                DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                readHighestTier(
                        VarbitID.LUMBRIDGE_EASY_REWARD,
                        VarbitID.LUMBRIDGE_MEDIUM_REWARD,
                        VarbitID.LUMBRIDGE_HARD_REWARD,
                        VarbitID.LUMBRIDGE_ELITE_REWARD
                )
        );

        tiers.put(
                DiaryArea.MORYTANIA,
                readHighestTier(
                        VarbitID.MORYTANIA_EASY_REWARD,
                        VarbitID.MORYTANIA_MEDIUM_REWARD,
                        VarbitID.MORYTANIA_HARD_REWARD,
                        VarbitID.MORYTANIA_ELITE_REWARD
                )
        );

        return tiers;
    }

    private DiaryTier readHighestTier(
            int easyVarbit,
            int mediumVarbit,
            int hardVarbit,
            int eliteVarbit
    )
    {
        if (
                client.getVarbitValue(
                        eliteVarbit
                ) > 0
        )
        {
            return DiaryTier.ELITE;
        }

        if (
                client.getVarbitValue(
                        hardVarbit
                ) > 0
        )
        {
            return DiaryTier.HARD;
        }

        if (
                client.getVarbitValue(
                        mediumVarbit
                ) > 0
        )
        {
            return DiaryTier.MEDIUM;
        }

        if (
                client.getVarbitValue(
                        easyVarbit
                ) > 0
        )
        {
            return DiaryTier.EASY;
        }

        return DiaryTier.NONE;
    }
}