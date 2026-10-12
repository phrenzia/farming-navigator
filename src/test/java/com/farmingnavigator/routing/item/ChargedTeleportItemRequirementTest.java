package com.farmingnavigator.routing.item;

import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementReadiness;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RequirementState;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ChargedTeleportItemRequirementTest
{
    @Test
    public void catalogUsesExactSkillsNecklaceChargeVariants()
    {
        ChargedTeleportItemDefinition.Variant oneCharge =
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .findVariant(
                                ItemID.JEWL_NECKLACE_OF_SKILLS_1
                        );

        ChargedTeleportItemDefinition.Variant sixCharges =
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .findVariant(
                                ItemID.JEWL_NECKLACE_OF_SKILLS_6
                        );

        assertNotNull(
                oneCharge
        );

        assertNotNull(
                sixCharges
        );

        assertEquals(
                1,
                oneCharge
                        .getCharges()
        );

        assertEquals(
                6,
                sixCharges
                        .getCharges()
        );

        assertNull(
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .findVariant(
                                ItemID.JEWL_NECKLACE_OF_SKILLS
                        )
        );
    }

    @Test
    public void catalogIncludesVerifiedSpecialVariants()
    {
        ChargedTeleportItemDefinition.Variant trimmedGlory =
                ChargedTeleportItemDefinition.AMULET_OF_GLORY
                        .findVariant(
                                ItemID.TRAIL_AMULET_OF_GLORY_6
                        );

        ChargedTeleportItemDefinition.Variant eternalGlory =
                ChargedTeleportItemDefinition.AMULET_OF_GLORY
                        .findVariant(
                                ItemID.AMULET_OF_GLORY_INF
                        );

        ChargedTeleportItemDefinition.Variant imbuedWealth =
                ChargedTeleportItemDefinition.RING_OF_WEALTH
                        .findVariant(
                                ItemID.RING_OF_WEALTH_I5
                        );

        ChargedTeleportItemDefinition.Variant eternalSlayerRing =
                ChargedTeleportItemDefinition.SLAYER_RING
                        .findVariant(
                                ItemID.SLAYER_RING_ETERNAL
                        );

        assertNotNull(
                trimmedGlory
        );

        assertEquals(
                6,
                trimmedGlory
                        .getCharges()
        );

        assertNotNull(
                eternalGlory
        );

        assertTrue(
                eternalGlory
                        .isUnlimited()
        );

        assertNotNull(
                imbuedWealth
        );

        assertEquals(
                5,
                imbuedWealth
                        .getCharges()
        );

        assertNotNull(
                eternalSlayerRing
        );

        assertTrue(
                eternalSlayerRing
                        .isUnlimited()
        );
    }

    @Test
    public void carriedVariantReportsReadyNowWithExactCharges()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.JEWL_NECKLACE_OF_SKILLS_3,
                1
        );

        RequirementContext context =
                createContext(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        RequirementResult result =
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .createRequirement(
                                2L
                        )
                        .evaluate(
                                context
                        );

        assertTrue(
                result
                        .isSatisfied()
        );

        assertTrue(
                result
                        .isReadyNow()
        );

        assertEquals(
                RequirementReadiness.READY_NOW,
                result
                        .getReadiness()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Carried usable charges: 3"
                        )
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Skills necklace(3) x1"
                        )
        );
    }

    @Test
    public void equippedVariantCountsAsReadyNow()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.RING_OF_DUELING_1,
                1
        );

        RequirementContext context =
                createContext(
                        Collections.emptyMap(),
                        equipment,
                        Collections.emptyMap(),
                        false
                );

        RequirementResult result =
                ChargedTeleportItemDefinition.RING_OF_DUELING
                        .createRequirement()
                        .evaluate(
                                context
                        );

        assertTrue(
                result
                        .isReadyNow()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Ring of dueling(1) x1"
                        )
        );
    }

    @Test
    public void bankedHighChargeVariantReportsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.JEWL_NECKLACE_OF_SKILLS_6,
                1L
        );

        RequirementContext context =
                createContext(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true
                );

        RequirementResult result =
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .createRequirement(
                                5L
                        )
                        .evaluate(
                                context
                        );

        assertTrue(
                result
                        .isSatisfied()
        );

        assertFalse(
                result
                        .isReadyNow()
        );

        assertTrue(
                result
                        .requiresBankWithdrawal()
        );

        assertEquals(
                RequirementReadiness.AVAILABLE_FROM_BANK,
                result
                        .getReadiness()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Verified bank usable charges: 6"
                        )
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Withdraw: Skills necklace(6) x1"
                        )
        );
    }

    @Test
    public void carriedAndBankedChargesCanCombine()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.JEWL_NECKLACE_OF_SKILLS_2,
                1
        );

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.JEWL_NECKLACE_OF_SKILLS_3,
                1L
        );

        RequirementContext context =
                createContext(
                        inventory,
                        Collections.emptyMap(),
                        bank,
                        true
                );

        RequirementResult result =
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .createRequirement(
                                5L
                        )
                        .evaluate(
                                context
                        );

        assertTrue(
                result
                        .isSatisfied()
        );

        assertTrue(
                result
                        .requiresBankWithdrawal()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Carried usable charges: 2"
                        )
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Withdraw: Skills necklace(3) x1"
                        )
        );
    }

    @Test
    public void unverifiedBankReturnsDataRequired()
    {
        RequirementContext context =
                createContext(
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        RequirementResult result =
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .createRequirement()
                        .evaluate(
                                context
                        );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result
                        .getState()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Open any normal bank once"
                        )
        );
    }

    @Test
    public void unchargedSkillsNecklaceIsNotCountedAsUsable()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.JEWL_NECKLACE_OF_SKILLS,
                1
        );

        RequirementContext context =
                createContext(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        RequirementResult result =
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .createRequirement()
                        .evaluate(
                                context
                        );

        assertFalse(
                result
                        .isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result
                        .getState()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void unlimitedGlorySatisfiesAnyFiniteChargeRequirement()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.AMULET_OF_GLORY_INF,
                1
        );

        RequirementContext context =
                createContext(
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        RequirementResult result =
                ChargedTeleportItemDefinition.AMULET_OF_GLORY
                        .createRequirement(
                                1000L
                        )
                        .evaluate(
                                context
                        );

        assertTrue(
                result
                        .isReadyNow()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Carried usable charges: unlimited"
                        )
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Amulet of eternal glory x1 (unlimited)"
                        )
        );
    }

    @Test
    public void verifiedInsufficientChargesAreBlockedExactly()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.JEWL_NECKLACE_OF_SKILLS_1,
                1
        );

        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.JEWL_NECKLACE_OF_SKILLS_2,
                1L
        );

        RequirementContext context =
                createContext(
                        inventory,
                        Collections.emptyMap(),
                        bank,
                        true
                );

        RequirementResult result =
                ChargedTeleportItemDefinition.SKILLS_NECKLACE
                        .createRequirement(
                                5L
                        )
                        .evaluate(
                                context
                        );

        assertFalse(
                result
                        .isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result
                        .getState()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Still missing: 2"
                        )
        );
    }

    private RequirementContext createContext(
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.STANDARD,
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
                        bankVerified,
                        bankVerified
                                ? Instant.now()
                                : null
                );

        return new RequirementContext(
                null,
                null,
                travelSnapshot,
                storageSnapshot
        );
    }

    private Map<Integer, Long> convertToLongMap(
            Map<Integer, Integer> source
    )
    {
        Map<Integer, Long> result =
                new LinkedHashMap<>();

        for (
                Map.Entry<Integer, Integer> entry
                : source
                .entrySet()
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