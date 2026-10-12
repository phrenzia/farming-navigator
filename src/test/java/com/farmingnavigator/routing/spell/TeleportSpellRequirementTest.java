package com.farmingnavigator.routing.spell;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementReadiness;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RequirementState;
import com.farmingnavigator.routing.requirement.RuneKind;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TeleportSpellRequirementTest
{
    @Test
    public void faladorTeleportCanUseBoostedMagic()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .FALADOR_TELEPORT;

        RequirementContext context =
                createContext(
                        36,
                        37,
                        SpellbookType.STANDARD,
                        Collections.emptyMap(),
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap(),
                        true,
                        null
                );

        RequirementResult result =
                spell.createRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result.isSatisfied()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void catherbyRequiresLunarSpellbook()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .CATHERBY_TELEPORT;

        Map<Quest, QuestState> quests =
                finishedQuests(
                        Quest.LUNAR_DIPLOMACY
                );

        RequirementContext context =
                createContext(
                        99,
                        99,
                        SpellbookType.STANDARD,
                        quests,
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap(),
                        true,
                        null
                );

        RequirementResult result =
                spell.createRequirement()
                        .evaluate(
                                context
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Lunar spellbook"
                        )
        );
    }

    @Test
    public void catherbyRequiresLunarDiplomacy()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .CATHERBY_TELEPORT;

        RequirementContext context =
                createContext(
                        99,
                        99,
                        SpellbookType.LUNAR,
                        Collections.emptyMap(),
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap(),
                        true,
                        null
                );

        RequirementResult result =
                spell.createRequirement()
                        .evaluate(
                                context
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Lunar Diplomacy"
                        )
        );
    }

    @Test
    public void catherbyCanPrepareRunesFromBank()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .CATHERBY_TELEPORT;

        RequirementContext context =
                createContext(
                        99,
                        99,
                        SpellbookType.LUNAR,
                        finishedQuests(
                                Quest.LUNAR_DIPLOMACY
                        ),
                        Collections.emptyMap(),
                        createBankRunes(
                                spell
                        ),
                        true,
                        null
                );

        RequirementResult result =
                spell.createRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result.isSatisfied()
        );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertEquals(
                RequirementReadiness
                        .AVAILABLE_FROM_BANK,
                result.getReadiness()
        );
    }

    @Test
    public void battlefrontNeedsNoQuestWhenArceuusIsActive()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .BATTLEFRONT_TELEPORT;

        RequirementContext context =
                createContext(
                        23,
                        23,
                        SpellbookType.ARCEUUS,
                        Collections.emptyMap(),
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap(),
                        true,
                        null
                );

        RequirementResult result =
                spell.createRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void harmonyIslandRequiresGreatBrainRobbery()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .HARMONY_ISLAND_TELEPORT;

        RequirementContext context =
                createContext(
                        99,
                        99,
                        SpellbookType.ARCEUUS,
                        Collections.emptyMap(),
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap(),
                        true,
                        null
                );

        RequirementResult result =
                spell.createRequirement()
                        .evaluate(
                                context
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Great Brain Robbery"
                        )
        );
    }

    @Test
    public void trollheimRequiresEadgarsRuse()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .TROLLHEIM_TELEPORT;

        RequirementContext context =
                createContext(
                        99,
                        99,
                        SpellbookType.STANDARD,
                        Collections.emptyMap(),
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap(),
                        true,
                        null
                );

        RequirementResult result =
                spell.createRequirement()
                        .evaluate(
                                context
                        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Eadgar"
                        )
        );
    }

    @Test
    public void teleportToHouseRequiresConfiguredHouse()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .TELEPORT_TO_HOUSE;

        RequirementContext withoutHouse =
                createContext(
                        99,
                        99,
                        SpellbookType.STANDARD,
                        Collections.emptyMap(),
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap(),
                        true,
                        new FarmingNavigatorConfig()
                        {
                        }
                );

        RequirementResult blocked =
                spell.createRequirement()
                        .evaluate(
                                withoutHouse
                        );

        assertFalse(
                blocked.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                blocked.getState()
        );

        RequirementContext withHouse =
                createContext(
                        99,
                        99,
                        SpellbookType.STANDARD,
                        Collections.emptyMap(),
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap(),
                        true,
                        createHouseConfig()
                );

        RequirementResult ready =
                spell.createRequirement()
                        .evaluate(
                                withHouse
                        );

        assertTrue(
                ready.isReadyNow()
        );
    }

    @Test
    public void knownQuestBlockBeatsUnsyncedBankData()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition
                        .HARMONY_ISLAND_TELEPORT;

        RequirementContext context =
                createContext(
                        99,
                        99,
                        SpellbookType.ARCEUUS,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        null
                );

        RequirementResult result =
                spell.createRequirement()
                        .evaluate(
                                context
                        );

        /*
         * We already KNOW The Great Brain Robbery is not
         * completed.
         *
         * Missing bank data must not hide that definitive
         * blocker.
         */
        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Great Brain Robbery"
                        )
        );
    }

    @Test
    public void researchedDefinitionValuesArePreserved()
    {
        assertEquals(
                48,
                TeleportSpellDefinition
                        .KOUREND_CASTLE_TELEPORT
                        .getRequiredMagicLevel()
        );

        assertEquals(
                54,
                TeleportSpellDefinition
                        .CIVITAS_ILLA_FORTIS_TELEPORT
                        .getRequiredMagicLevel()
        );

        assertEquals(
                85,
                TeleportSpellDefinition
                        .FISHING_GUILD_TELEPORT
                        .getRequiredMagicLevel()
        );

        assertEquals(
                87,
                TeleportSpellDefinition
                        .CATHERBY_TELEPORT
                        .getRequiredMagicLevel()
        );

        assertEquals(
                2,
                TeleportSpellDefinition
                        .CIVITAS_ILLA_FORTIS_TELEPORT
                        .getRuneCost()
                        .getQuantity(
                                RuneKind.LAW
                        )
        );

        assertEquals(
                10,
                TeleportSpellDefinition
                        .CATHERBY_TELEPORT
                        .getRuneCost()
                        .getQuantity(
                                RuneKind.WATER
                        )
        );

        assertEquals(
                3,
                TeleportSpellDefinition
                        .CATHERBY_TELEPORT
                        .getRuneCost()
                        .getQuantity(
                                RuneKind.ASTRAL
                        )
        );
    }

    private RequirementContext createContext(
            int realMagicLevel,
            int boostedMagicLevel,
            SpellbookType spellbook,
            Map<Quest, QuestState> questStates,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank,
            boolean bankVerified,
            FarmingNavigatorConfig config
    )
    {
        AccountState accountState =
                new AccountState(
                        true,
                        99,
                        99,
                        realMagicLevel,
                        boostedMagicLevel,
                        99,
                        99,
                        99,
                        99,
                        questStates,
                        null,
                        null
                );

        TravelCapabilitySnapshot
                travelCapabilitySnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        spellbook,
                        null,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        StorageSnapshot storageSnapshot =
                new StorageSnapshot(
                        convertToLongMap(
                                inventory
                        ),
                        Collections.emptyMap(),
                        bank,
                        bankVerified,
                        bankVerified
                                ? Instant.now()
                                : null
                );

        return new RequirementContext(
                accountState,
                config,
                travelCapabilitySnapshot,
                storageSnapshot
        );
    }

    private Map<Quest, QuestState>
    finishedQuests(
            Quest... quests
    )
    {
        EnumMap<Quest, QuestState> states =
                new EnumMap<>(
                        Quest.class
                );

        if (quests != null)
        {
            for (
                    Quest quest
                    : quests
            )
            {
                states.put(
                        quest,
                        QuestState.FINISHED
                );
            }
        }

        return states;
    }

    private Map<Integer, Integer>
    createInventoryRunes(
            TeleportSpellDefinition spell
    )
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        for (
                Map.Entry<RuneKind, Integer> entry
                : spell
                .getRuneCost()
                .getQuantities()
                .entrySet()
        )
        {
            inventory.merge(
                    entry.getKey()
                            .getCanonicalItemId(),
                    entry.getValue(),
                    Integer::sum
            );
        }

        return inventory;
    }

    private Map<Integer, Long>
    createBankRunes(
            TeleportSpellDefinition spell
    )
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        for (
                Map.Entry<RuneKind, Integer> entry
                : spell
                .getRuneCost()
                .getQuantities()
                .entrySet()
        )
        {
            bank.merge(
                    entry.getKey()
                            .getCanonicalItemId(),
                    entry.getValue()
                            .longValue(),
                    Long::sum
            );
        }

        return bank;
    }

    private Map<Integer, Long>
    convertToLongMap(
            Map<Integer, Integer> source
    )
    {
        Map<Integer, Long> result =
                new LinkedHashMap<>();

        for (
                Map.Entry<Integer, Integer> entry
                : source.entrySet()
        )
        {
            result.put(
                    entry.getKey(),
                    entry.getValue()
                            .longValue()
            );
        }

        return result;
    }

    private FarmingNavigatorConfig
    createHouseConfig()
    {
        return new FarmingNavigatorConfig()
        {
            @Override
            public PohHouseLocation
            pohHouseLocation()
            {
                return PohHouseLocation
                        .RIMMINGTON;
            }
        };
    }
}