package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.poh.PohHouseLocation;
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

public class ArdougneFairyRingTransportVerificationTest
{
    @Test
    public void catalogueAddsPohAndDirectBlrRoutes()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        TransportOption poh =
                findOption(
                        service,
                        "POH fairy ring to BLR"
                );

        assertNotNull(
                poh
        );

        assertEquals(
                TransportVerificationKind.POH_FAIRY_RING,
                poh.getVerificationKind()
        );

        assertEquals(
                TransportCategory.PLAYER_OWNED_HOUSE,
                poh.getCategory()
        );

        assertEquals(
                8,
                poh.getBaselineRank()
        );

        TransportOption direct =
                findOption(
                        service,
                        "Fairy ring BLR"
                );

        assertNotNull(
                direct
        );

        assertEquals(
                TransportVerificationKind.FAIRY_RING_ACCESS,
                direct.getVerificationKind()
        );

        assertEquals(
                TransportCategory.FAIRY_RING,
                direct.getCategory()
        );

        assertEquals(
                9,
                direct.getBaselineRank()
        );
    }

    @Test
    public void directBlrWithCarriedDramenStaffIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        "Fairy ring BLR",
                        createAccountState(
                                true
                        ),
                        null,
                        SpellbookType.STANDARD,
                        inventory,
                        Collections.emptyMap(),
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
                                "Dramen staff"
                        )
        );
    }

    @Test
    public void directBlrWithBankedLunarStaffIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF,
                1L
        );

        VerifiedTransportResult result =
                evaluate(
                        "Fairy ring BLR",
                        createAccountState(
                                true
                        ),
                        null,
                        SpellbookType.STANDARD,
                        Collections.emptyMap(),
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
                                "Lunar staff"
                        )
        );
    }

    @Test
    public void directBlrBeforeFairyRingUnlockIsLocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        "Fairy ring BLR",
                        createAccountState(
                                false
                        ),
                        null,
                        SpellbookType.STANDARD,
                        inventory,
                        Collections.emptyMap(),
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
                                "Fairy Godfather"
                        )
        );
    }

    @Test
    public void pohBlrWithHouseTabletAndDramenStaffIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        "POH fairy ring to BLR",
                        createAccountState(
                                true
                        ),
                        createConfig(
                                true,
                                true
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap(),
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
                                "Teleport to house tablet"
                        )
        );
    }

    @Test
    public void pohBlrWithoutConfiguredFairyRingIsMissing()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        "POH fairy ring to BLR",
                        createAccountState(
                                true
                        ),
                        createConfig(
                                true,
                                false
                        ),
                        SpellbookType.LUNAR,
                        inventory,
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
                                "POH fairy ring is not enabled"
                        )
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
            String displayName,
            AccountState accountState,
            FarmingNavigatorConfig config,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank
    )
    {
        HerbPatchTransportService transportService =
                new HerbPatchTransportService();

        TransportOption option =
                findOption(
                        transportService,
                        displayName
                );

        assertNotNull(
                option
        );

        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        spellbook,
                        null,
                        inventory,
                        equipment,
                        Collections.emptyMap()
                );

        StorageSnapshot storageSnapshot =
                new StorageSnapshot(
                        convertToLongMap(
                                inventory
                        ),
                        convertToLongMap(
                                equipment
                        ),
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
                                config,
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

    private AccountState createAccountState(
            boolean fairyRingAccessUnlocked
    )
    {
        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        /*
         * The exact fairy-ring progress state is represented
         * by fairyRingAccessUnlocked.
         *
         * Fairytale II itself does not need to be completed.
         */
        questStates.put(
                Quest.FAIRYTALE_II__CURE_A_QUEEN,
                QuestState.IN_PROGRESS
        );

        return new AccountState(
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
                fairyRingAccessUnlocked
        );
    }

    private FarmingNavigatorConfig createConfig(
            boolean hasHouse,
            boolean hasFairyRing
    )
    {
        return new FarmingNavigatorConfig()
        {
            @Override
            public PohHouseLocation pohHouseLocation()
            {
                return hasHouse
                        ? PohHouseLocation.RIMMINGTON
                        : PohHouseLocation.NONE;
            }

            @Override
            public boolean pohFairyRing()
            {
                return hasFairyRing;
            }
        };
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