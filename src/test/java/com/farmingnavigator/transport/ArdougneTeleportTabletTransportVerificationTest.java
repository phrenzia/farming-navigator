package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArdougneTeleportTabletTransportVerificationTest
{
    @Test
    public void catalogueKeepsTabletAndSpellSeparate()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption tablet =
                findOption(
                        service,
                        "Ardougne teleport tablet"
                );

        assertNotNull(
                tablet
        );

        assertEquals(
                TransportVerificationKind.ARDOUGNE_TELEPORT_TABLET,
                tablet.getVerificationKind()
        );

        assertEquals(
                TransportCategory.ITEM,
                tablet.getCategory()
        );

        assertEquals(
                11,
                tablet.getBaselineRank()
        );

        TransportOption spell =
                findOption(
                        service,
                        "Ardougne Teleport"
                );

        assertNotNull(
                spell
        );

        assertEquals(
                TransportVerificationKind.TELEPORT_SPELL,
                spell.getVerificationKind()
        );

        assertEquals(
                7,
                spell.getBaselineRank()
        );
    }

    @Test
    public void completedQuestAndCarriedTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        QuestState.FINISHED,
                        inventory,
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
                                "Plague City"
                        )
        );
    }

    @Test
    public void completedQuestAndBankedTabletIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1L
        );

        VerifiedTransportResult result =
                evaluate(
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        bank
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
                                "Ardougne teleport tablet"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "AVAILABLE FROM BANK"
                        )
        );
    }

    @Test
    public void incompletePlagueCityIsLocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        QuestState.IN_PROGRESS,
                        inventory,
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
                                "Plague City"
                        )
        );
    }

    @Test
    public void completedQuestWithoutTabletIsMissing()
    {
        VerifiedTransportResult result =
                evaluate(
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
    public void tabletDoesNotDependOnActiveSpellbook()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        QuestState.FINISHED,
                        SpellbookType.ANCIENT,
                        inventory,
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.READY_NOW,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
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
                HerbPatchDefinition.ARDOUGNE
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
            QuestState plagueCityState,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
        return evaluate(
                plagueCityState,
                SpellbookType.STANDARD,
                inventory,
                bank
        );
    }

    private VerifiedTransportResult evaluate(
            QuestState plagueCityState,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
        HerbPatchTransportService transportService =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        transportService,
                        "Ardougne teleport tablet"
                );

        assertNotNull(
                option
        );

        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.PLAGUE_CITY,
                plagueCityState
        );

        AccountState accountState =
                new AccountState(
                        true,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        questStates,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
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
                HerbPatchDefinition.ARDOUGNE,
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
                        HerbPatchDefinition.ARDOUGNE
                )
                .get(
                        0
                );
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