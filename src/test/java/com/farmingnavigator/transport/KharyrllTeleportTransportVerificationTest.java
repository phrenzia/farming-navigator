package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RuneKind;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KharyrllTeleportTransportVerificationTest
{
    @Test
    public void catalogueAddsVerifiedKharyrllRoute()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        service
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.TELEPORT_SPELL,
                option.getVerificationKind()
        );

        assertEquals(
                TransportCategory.SPELL,
                option.getCategory()
        );

        assertEquals(
                TeleportSpellDefinition.KHARYRLL_TELEPORT,
                option.getTeleportSpellDefinition()
        );

        assertEquals(
                5,
                option.getBaselineRank()
        );
    }

    @Test
    public void definitionUsesExactKharyrllRequirements()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.KHARYRLL_TELEPORT;

        assertEquals(
                "Kharyrll Teleport",
                spell.getDisplayName()
        );

        assertEquals(
                66,
                spell.getRequiredMagicLevel()
        );

        assertEquals(
                SpellbookType.ANCIENT,
                spell.getSpellbook()
        );

        assertEquals(
                Quest.DESERT_TREASURE_I,
                spell.getRequiredQuest()
        );

        assertEquals(
                Integer.valueOf(
                        1
                ),
                spell.getRuneCost()
                        .getQuantities()
                        .get(
                                RuneKind.BLOOD
                        )
        );

        assertEquals(
                Integer.valueOf(
                        2
                ),
                spell.getRuneCost()
                        .getQuantities()
                        .get(
                                RuneKind.LAW
                        )
        );
    }

    @Test
    public void ancientSpellbookWithQuestAndCarriedRunesIsReadyNow()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.KHARYRLL_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        66,
                        66,
                        SpellbookType.ANCIENT,
                        QuestState.FINISHED,
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Kharyrll Teleport"
                        )
        );
    }

    @Test
    public void bankedRunesAreAvailableFromBank()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.KHARYRLL_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        66,
                        66,
                        SpellbookType.ANCIENT,
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        createBankRunes(
                                spell
                        )
                );

        assertEquals(
                VerifiedTransportState.AVAILABLE_FROM_BANK,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "AVAILABLE FROM BANK"
                        )
        );
    }

    @Test
    public void wrongSpellbookIsLocked()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.KHARYRLL_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        99,
                        99,
                        SpellbookType.STANDARD,
                        QuestState.FINISHED,
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Ancient Magicks spellbook"
                        )
        );
    }

    @Test
    public void unfinishedDesertTreasureIsLocked()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.KHARYRLL_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        99,
                        99,
                        SpellbookType.ANCIENT,
                        QuestState.IN_PROGRESS,
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Desert Treasure"
                        )
        );
    }

    @Test
    public void insufficientMagicIsLocked()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.KHARYRLL_TELEPORT;

        VerifiedTransportResult result =
                evaluate(
                        65,
                        65,
                        SpellbookType.ANCIENT,
                        QuestState.FINISHED,
                        createInventoryRunes(
                                spell
                        ),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );
    }

    @Test
    public void missingRunesAreMissing()
    {
        VerifiedTransportResult result =
                evaluate(
                        99,
                        99,
                        SpellbookType.ANCIENT,
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.MISSING,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void fenkenstrainRouteRemainsFuture()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        service,
                        "Fenkenstrain's Castle Teleport"
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.NOT_IMPLEMENTED,
                option.getVerificationKind()
        );
    }

    private TransportOption findOption(
            HerbPatchTransportService service
    )
    {
        return findOption(
                service,
                "Kharyrll Teleport"
        );
    }

    private TransportOption findOption(
            HerbPatchTransportService service,
            String displayName
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                HerbPatchDefinition.MORYTANIA
        )
        )
        {
            if (
                    displayName.equals(
                            option.getDisplayName()
                    )
            )
            {
                return option;
            }
        }

        return null;
    }

    private VerifiedTransportResult evaluate(
            int realMagicLevel,
            int boostedMagicLevel,
            SpellbookType spellbook,
            QuestState desertTreasureState,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
        HerbPatchTransportService transportService =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        transportService
                );

        assertNotNull(
                option
        );

        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.DESERT_TREASURE_I,
                desertTreasureState
        );

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
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        spellbook,
                        null,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );

        StorageSnapshot storageSnapshot =
                new StorageSnapshot(
                        convertToLongMap(
                                inventory
                        ),
                        Collections.emptyMap(),
                        bank,
                        true,
                        Instant.now()
                );

        Map<
                HerbPatchDefinition,
                List<TransportOption>
                > options =
                new EnumMap<>(
                        HerbPatchDefinition.class
                );

        options.put(
                HerbPatchDefinition.MORYTANIA,
                Collections.singletonList(
                        option
                )
        );

        Map<
                HerbPatchDefinition,
                List<VerifiedTransportResult>
                > results =
                new TransportVerificationService()
                        .evaluateAll(
                                accountState,
                                travelSnapshot,
                                storageSnapshot,
                                options
                        );

        return results
                .get(
                        HerbPatchDefinition.MORYTANIA
                )
                .get(
                        0
                );
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
                : spell.getRuneCost()
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
                : spell.getRuneCost()
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
}