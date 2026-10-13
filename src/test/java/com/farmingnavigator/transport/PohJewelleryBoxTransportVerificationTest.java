package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.poh.PohJewelleryBoxTier;
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
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PohJewelleryBoxTransportVerificationTest
{
    @Test
    public void catalogueAddsPohJewelleryBoxRoutesWithoutReplacingDirectNecklaceRoutes()
    {
        HerbPatchTransportService service =
                new HerbPatchTransportService();

        assertVerificationKind(
                service,
                HerbPatchDefinition.ARDOUGNE,
                "POH jewellery box to Fishing Guild",
                TransportVerificationKind
                        .POH_JEWELLERY_BOX_SKILLS_NECKLACE
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.FARMING_GUILD,
                "POH jewellery box to Farming Guild",
                TransportVerificationKind
                        .POH_JEWELLERY_BOX_SKILLS_NECKLACE
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.ARDOUGNE,
                "Skills necklace to Fishing Guild",
                TransportVerificationKind.SKILLS_NECKLACE
        );

        assertVerificationKind(
                service,
                HerbPatchDefinition.FARMING_GUILD,
                "Skills necklace",
                TransportVerificationKind.SKILLS_NECKLACE
        );
    }

    @Test
    public void fancyBoxWithCarriedHouseTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        createConfig(
                                true,
                                PohJewelleryBoxTier.FANCY
                        ),
                        SpellbookType.LUNAR,
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

    @Test
    public void ornateBoxWithHouseTeleportSpellIsReadyNow()
    {
        VerifiedTransportResult result =
                evaluate(
                        createConfig(
                                true,
                                PohJewelleryBoxTier.ORNATE
                        ),
                        SpellbookType.STANDARD,
                        createHouseTeleportRunes(),
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

    @Test
    public void bankedHouseTabletMakesRouteAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                2L
        );

        VerifiedTransportResult result =
                evaluate(
                        createConfig(
                                true,
                                PohJewelleryBoxTier.FANCY
                        ),
                        SpellbookType.LUNAR,
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
    public void basicJewelleryBoxIsMissing()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        createConfig(
                                true,
                                PohJewelleryBoxTier.BASIC
                        ),
                        SpellbookType.LUNAR,
                        inventory,
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
                                "Fancy or Ornate jewellery box"
                        )
        );
    }

    @Test
    public void noJewelleryBoxIsMissing()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        createConfig(
                                true,
                                PohJewelleryBoxTier.NONE
                        ),
                        SpellbookType.LUNAR,
                        inventory,
                        Collections.emptyMap()
                );

        assertEquals(
                VerifiedTransportState.MISSING,
                result.getState()
        );

        assertFalse(
                result.isUsableForPlannedRun()
        );
    }

    @Test
    public void missingPohConfigurationRequiresConfiguration()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        null,
                        SpellbookType.LUNAR,
                        inventory,
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
    public void configuredJewelleryBoxWithoutHouseIsMissing()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_TELEPORTTOHOUSE,
                1
        );

        VerifiedTransportResult result =
                evaluate(
                        createConfig(
                                false,
                                PohJewelleryBoxTier.FANCY
                        ),
                        SpellbookType.LUNAR,
                        inventory,
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
                                "Player-owned house"
                        )
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

    private VerifiedTransportResult evaluate(
            FarmingNavigatorConfig config,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank
    )
    {
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
                        Collections.emptyMap(),
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

        TransportOption option =
                new TransportOption(
                        1,
                        "POH jewellery box to Fishing Guild",
                        "Test POH jewellery-box route.",
                        TransportCategory.PLAYER_OWNED_HOUSE,
                        TransportVerificationKind
                                .POH_JEWELLERY_BOX_SKILLS_NECKLACE
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

    private FarmingNavigatorConfig createConfig(
            boolean hasHouse,
            PohJewelleryBoxTier tier
    )
    {
        return new FarmingNavigatorConfig()
        {
            @Override
            public PohHouseLocation
            pohHouseLocation()
            {
                return hasHouse
                        ? PohHouseLocation.RIMMINGTON
                        : PohHouseLocation.NONE;
            }

            @Override
            public PohJewelleryBoxTier
            pohJewelleryBox()
            {
                return tier;
            }
        };
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