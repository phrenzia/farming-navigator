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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TeleportSpellTransportVerificationTest
{
    @Test
    public void transportCataloguePromotesOnlyVerifiedSpellRoutes()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        assertSpellRoute(
                service,
                HerbPatchDefinition.FALADOR,
                "Falador Teleport",
                TeleportSpellDefinition.FALADOR_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.CATHERBY,
                "Catherby Teleport",
                TeleportSpellDefinition.CATHERBY_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.CATHERBY,
                "Camelot Teleport",
                TeleportSpellDefinition.CAMELOT_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.ARDOUGNE,
                "Fishing Guild Teleport",
                TeleportSpellDefinition.FISHING_GUILD_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.MORYTANIA,
                "Fenkenstrain's Castle Teleport",
                TeleportSpellDefinition.FENKENSTRAINS_CASTLE_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.ARDOUGNE,
                "Ardougne Teleport",
                TeleportSpellDefinition.ARDOUGNE_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.TROLL_STRONGHOLD,
                "Trollheim Teleport",
                TeleportSpellDefinition.TROLLHEIM_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.FARMING_GUILD,
                "Battlefront Teleport",
                TeleportSpellDefinition.BATTLEFRONT_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.HARMONY_ISLAND,
                "Harmony Island Teleport",
                TeleportSpellDefinition.HARMONY_ISLAND_TELEPORT
        );

        assertSpellRoute(
                service,
                HerbPatchDefinition.ORTUS_FARM,
                "Civitas illa Fortis Teleport",
                TeleportSpellDefinition.CIVITAS_ILLA_FORTIS_TELEPORT
        );

    }

    @Test
    public void faladorTeleportIsReadyNowWithLiveRunes()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.FALADOR_TELEPORT;

        VerifiedTransportResult result =
                evaluateSpell(
                        spell,
                        createAccountState(
                                37,
                                37,
                                Collections.emptyMap()
                        ),
                        createTravelSnapshot(
                                SpellbookType.STANDARD,
                                createInventoryRunes(
                                        spell
                                )
                        ),
                        createStorageSnapshot(
                                Collections.emptyMap()
                        )
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );
    }

    @Test
    public void catherbyTeleportCanPrepareRunesFromBank()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.CATHERBY_TELEPORT;

        VerifiedTransportResult result =
                evaluateSpell(
                        spell,
                        createAccountState(
                                87,
                                87,
                                finishedQuests(
                                        Quest.LUNAR_DIPLOMACY
                                )
                        ),
                        createTravelSnapshot(
                                SpellbookType.LUNAR,
                                Collections.emptyMap()
                        ),
                        createStorageSnapshot(
                                createBankRunes(
                                        spell
                                )
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
    public void missingFaladorRunesReportMissing()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.FALADOR_TELEPORT;

        VerifiedTransportResult result =
                evaluateSpell(
                        spell,
                        createAccountState(
                                99,
                                99,
                                Collections.emptyMap()
                        ),
                        createTravelSnapshot(
                                SpellbookType.STANDARD,
                                Collections.emptyMap()
                        ),
                        createStorageSnapshot(
                                Collections.emptyMap()
                        )
                );

        assertEquals(
                VerifiedTransportState.MISSING,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void wrongSpellbookReportsLocked()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.CATHERBY_TELEPORT;

        VerifiedTransportResult result =
                evaluateSpell(
                        spell,
                        createAccountState(
                                99,
                                99,
                                finishedQuests(
                                        Quest.LUNAR_DIPLOMACY
                                )
                        ),
                        createTravelSnapshot(
                                SpellbookType.STANDARD,
                                createInventoryRunes(
                                        spell
                                )
                        ),
                        createStorageSnapshot(
                                Collections.emptyMap()
                        )
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
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
    public void unfinishedTrollheimQuestReportsLocked()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.TROLLHEIM_TELEPORT;

        VerifiedTransportResult result =
                evaluateSpell(
                        spell,
                        createAccountState(
                                99,
                                99,
                                Collections.emptyMap()
                        ),
                        createTravelSnapshot(
                                SpellbookType.STANDARD,
                                createInventoryRunes(
                                        spell
                                )
                        ),
                        createStorageSnapshot(
                                Collections.emptyMap()
                        )
                );

        assertEquals(
                VerifiedTransportState.LOCKED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Eadgar"
                        )
        );
    }

    @Test
    public void unknownSpellbookReportsDataRequired()
    {
        TeleportSpellDefinition spell =
                TeleportSpellDefinition.FALADOR_TELEPORT;

        VerifiedTransportResult result =
                evaluateSpell(
                        spell,
                        createAccountState(
                                99,
                                99,
                                Collections.emptyMap()
                        ),
                        createTravelSnapshot(
                                SpellbookType.UNKNOWN,
                                createInventoryRunes(
                                        spell
                                )
                        ),
                        createStorageSnapshot(
                                Collections.emptyMap()
                        )
                );

        assertEquals(
                VerifiedTransportState.DATA_REQUIRED,
                result.getState()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "could not verify the active spellbook"
                        )
        );
    }

    private void assertSpellRoute(
            HerbPatchTransportService service,
            HerbPatchDefinition patch,
            String displayName,
            TeleportSpellDefinition expectedSpell
    )
    {
        TransportOption option =
                findOption(
                        service,
                        patch,
                        displayName
                );

        assertNotNull(
                option
        );

        assertEquals(
                TransportVerificationKind.TELEPORT_SPELL,
                option.getVerificationKind()
        );

        assertEquals(
                expectedSpell,
                option.getTeleportSpellDefinition()
        );
    }

    private TransportOption findOption(
            HerbPatchTransportService service,
            HerbPatchDefinition patch,
            String displayName
    )
    {
        for (
                TransportOption option
                : service.getOptions(
                patch
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

    private VerifiedTransportResult evaluateSpell(
            TeleportSpellDefinition spell,
            AccountState accountState,
            TravelCapabilitySnapshot travelSnapshot,
            StorageSnapshot storageSnapshot
    )
    {
        TransportOption option =
                new TransportOption(
                        1,
                        spell.getDisplayName(),
                        "Test spell route.",
                        TransportCategory.SPELL,
                        TransportVerificationKind.TELEPORT_SPELL,
                        spell
                );

        Map<
                HerbPatchDefinition,
                List<TransportOption>
                > options =
                new EnumMap<>(
                        HerbPatchDefinition.class
                );

        options.put(
                HerbPatchDefinition.FALADOR,
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
                        HerbPatchDefinition.FALADOR
                )
                .get(
                        0
                );
    }

    private AccountState createAccountState(
            int realMagicLevel,
            int boostedMagicLevel,
            Map<Quest, QuestState> questStates
    )
    {
        return new AccountState(
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
    }

    private TravelCapabilitySnapshot
    createTravelSnapshot(
            SpellbookType spellbook,
            Map<Integer, Integer> inventory
    )
    {
        return new TravelCapabilitySnapshot(
                true,
                spellbook,
                null,
                inventory,
                Collections.emptyMap(),
                Collections.emptyMap()
        );
    }

    private StorageSnapshot createStorageSnapshot(
            Map<Integer, Long> bank
    )
    {
        return new StorageSnapshot(
                Collections.emptyMap(),
                Collections.emptyMap(),
                bank,
                true,
                Instant.now()
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
}