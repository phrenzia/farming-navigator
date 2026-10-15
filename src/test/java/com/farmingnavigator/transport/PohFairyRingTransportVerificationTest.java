package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.poh.PohHouseLocation;
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
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PohFairyRingTransportVerificationTest
{
    @Test
    public void catalogueAddsPohOriginsWithoutReplacingGenericFairyRoutes()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        assertVerificationKind(
                service,
                HerbPatchDefinition.MORYTANIA,
                "POH fairy ring to ALQ",
                TransportVerificationKind.POH_FAIRY_RING
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.HOSIDIUS,
                "POH fairy ring to AKR",
                TransportVerificationKind.POH_FAIRY_RING
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.FARMING_GUILD,
                "POH fairy ring to CIR",
                TransportVerificationKind.POH_FAIRY_RING
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.MORYTANIA,
                "Fairy ring ALQ",
                TransportVerificationKind.FAIRY_RING_ACCESS
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.HOSIDIUS,
                "Fairy ring AKR",
                TransportVerificationKind.FAIRY_RING_ACCESS
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.FARMING_GUILD,
                "Fairy ring CIR",
                TransportVerificationKind.FAIRY_RING_ACCESS
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.ORTUS_FARM,
                "Fairy ring AJP",
                TransportVerificationKind.FAIRY_RING_AJP
        );
    }

    @Test
    public void missingPohConfigurationRequiresConfiguration()
    {
        Map<Integer, Integer> inventory =
                createHouseTeleportRunes();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluatePohFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
                        null,
                        SpellbookType.STANDARD,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.CONFIGURATION_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Configure your POH profile"
                        )
        );
    }

    @Test
    public void configuredPohFairyRingSpellIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                createHouseTeleportRunes();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluatePohFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
                        createConfig(
                                true,
                                true
                        ),
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
                                "Teleport to House"
                        )
        );
    }

    @Test
    public void carriedHouseTabletMakesRouteReadyOnLunar()
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
                evaluatePohFairyRing(
                        createAccountState(
                                true,
                                false
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
    public void bankedHouseTabletMakesRouteAvailableOnArceuus()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                2L
        );

        VerifiedTransportResult result =
                evaluatePohFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
                        createConfig(
                                true,
                                true
                        ),
                        SpellbookType.ARCEUUS,
                        inventory,
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
                                "Teleport to house tablet"
                        )
        );
    }

    @Test
    public void wrongSpellbookWithoutTabletIsMissing()
    {
        Map<Integer, Integer> inventory =
                createHouseTeleportRunes();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluatePohFairyRing(
                        createAccountState(
                                true,
                                false
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

        /*
         * The spell itself is locked on Lunar, but the POH
         * route is not globally locked because a house tablet
         * is an exact unrestricted alternative.
         *
         * No usable tablet was found, so the preparation state
         * for the complete POH-entry route is MISSING.
         */
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
                                "Standard spellbook"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void configuredHouseWithoutFairyRingIsMissing()
    {
        Map<Integer, Integer> inventory =
                createHouseTeleportRunes();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluatePohFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
                        createConfig(
                                true,
                                false
                        ),
                        SpellbookType.STANDARD,
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

    @Test
    public void bankedHouseTeleportRunesAreAvailableFromBank()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.DRAMEN_STAFF,
                1
        );

        VerifiedTransportResult result =
                evaluatePohFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
                        createConfig(
                                true,
                                true
                        ),
                        SpellbookType.STANDARD,
                        inventory,
                        Collections.emptyMap(),
                        createBankHouseTeleportRunes()
                );

        assertEquals(
                VerifiedTransportState.AVAILABLE_FROM_BANK,
                result.getState()
        );

        assertTrue(
                result.isUsableForPlannedRun()
        );
    }

    @Test
    public void bankedDramenStaffIsAvailableFromBank()
    {
        Map<Integer, Integer> inventory =
                createHouseTeleportRunes();

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.DRAMEN_STAFF,
                1L
        );

        VerifiedTransportResult result =
                evaluatePohFairyRing(
                        createAccountState(
                                true,
                                false
                        ),
                        createConfig(
                                true,
                                true
                        ),
                        SpellbookType.STANDARD,
                        inventory,
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
                                "Dramen staff"
                        )
        );
    }

    @Test
    public void fairyGodfatherUnlockIsStillRequired()
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
                evaluatePohFairyRing(
                        createAccountState(
                                false,
                                false
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
    public void claimedLumbridgeEliteNeedsNoFairyStaff()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluatePohFairyRing(
                        createAccountState(
                                true,
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
    }

    private void assertVerificationKind(
            HerbPatchTransportService service,
            HerbPatchDefinition patch,
            String displayName,
            TransportVerificationKind expectedKind
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
                expectedKind,
                option.getVerificationKind()
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

    private VerifiedTransportResult evaluatePohFairyRing(
            AccountState accountState,
            FarmingNavigatorConfig config,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank
    )
    {
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

        TransportOption option =
                new TransportOption(
                        1,
                        "POH fairy ring to ALQ",
                        "Test POH fairy-ring route.",
                        TransportCategory.PLAYER_OWNED_HOUSE,
                        TransportVerificationKind.POH_FAIRY_RING
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
                                config,
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

    private AccountState createAccountState(
            boolean fairyRingAccessUnlocked,
            boolean lumbridgeEliteClaimed
    )
    {
        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.FAIRYTALE_II__CURE_A_QUEEN,
                QuestState.IN_PROGRESS
        );

        EnumMap<DiaryArea, DiaryTier>
                completedDiaries =
                new EnumMap<>(
                        DiaryArea.class
                );

        EnumMap<DiaryArea, DiaryTier>
                claimedDiaries =
                new EnumMap<>(
                        DiaryArea.class
                );

        if (lumbridgeEliteClaimed)
        {
            completedDiaries.put(
                    DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                    DiaryTier.ELITE
            );

            claimedDiaries.put(
                    DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                    DiaryTier.ELITE
            );
        }

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
                completedDiaries,
                claimedDiaries,
                false,
                fairyRingAccessUnlocked
        );
    }

    private Map<Integer, Integer>
    createHouseTeleportRunes()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        for (
                Map.Entry<RuneKind, Integer> entry
                : TeleportSpellDefinition
                .TELEPORT_TO_HOUSE
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
    createBankHouseTeleportRunes()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        for (
                Map.Entry<RuneKind, Integer> entry
                : TeleportSpellDefinition
                .TELEPORT_TO_HOUSE
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

    private Map<Integer, Long> convertToLongMap(
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