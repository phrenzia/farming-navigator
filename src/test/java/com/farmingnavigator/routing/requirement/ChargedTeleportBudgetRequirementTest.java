package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChargedTeleportBudgetRequirementTest
{
    @Test
    public void oneChargeCannotServeTwoStops()
    {
        RequirementResult result = necklace(2).evaluate(context(
                Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1), Collections.emptyMap(), Collections.emptyMap(), true));
        assertEquals(RequirementState.BLOCKED, result.getState());
        assertTrue(result.getDetail().contains("lack 1"));
    }

    @Test
    public void multipleCarriedItemsAndEquipmentContributeToSharedPool()
    {
        assertTrue(necklace(2).evaluate(context(Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 2),
                Collections.emptyMap(), Collections.emptyMap(), true)).isReadyNow());
        assertTrue(necklace(3).evaluate(context(Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1),
                Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_2, 1), Collections.emptyMap(), true)).isReadyNow());
    }

    @Test
    public void bankPreparationChoosesHighChargeVariantAndReportsExactWithdrawal()
    {
        RequirementResult result = necklace(3).evaluate(context(
                Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1), Collections.emptyMap(),
                Map.of(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 10L, ItemID.JEWL_NECKLACE_OF_SKILLS_6, 1L), true));
        assertTrue(result.requiresBankWithdrawal());
        assertTrue(result.getDetail().contains("withdraw 1 x Skills necklace(6)"));
    }

    @Test
    public void exhaustedAndInertJewelleryCannotSupplyCharges()
    {
        assertFalse(necklace(1).evaluate(context(Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS, 1),
                Collections.emptyMap(), Collections.emptyMap(), true)).isSatisfied());
    }

    @Test
    public void unknownBankRemainsUnknownWhenCarriedChargesAreInsufficient()
    {
        assertEquals(RequirementState.DATA_REQUIRED, necklace(2).evaluate(context(
                Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1), Collections.emptyMap(),
                Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_6, 1L), false)).getState());
        assertTrue(necklace(1).evaluate(context(Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1),
                Collections.emptyMap(), Collections.emptyMap(), false)).isReadyNow());
    }

    @Test
    public void eternalGloryCoversAnyRunWithoutFiniteCharges()
    {
        ChargedTeleportBudgetRequirement glory = new ChargedTeleportBudgetRequirement(ChargedTeleportItemDefinition.AMULET_OF_GLORY, 100);
        assertTrue(glory.evaluate(context(Collections.emptyMap(), Collections.singletonMap(ItemID.AMULET_OF_GLORY_INF, 1),
                Collections.emptyMap(), false)).isReadyNow());
        assertTrue(glory.evaluate(context(Collections.emptyMap(), Collections.emptyMap(),
                Collections.singletonMap(ItemID.AMULET_OF_GLORY_INF, 1L), true)).requiresBankWithdrawal());
    }

    @Test
    public void extremeBankQuantitiesCannotOverflowCapacityCalculation()
    {
        assertTrue(necklace(10).evaluate(context(Collections.emptyMap(), Collections.emptyMap(),
                Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_6, Long.MAX_VALUE), true)).requiresBankWithdrawal());
    }

    @Test
    public void missingContextRequiresData()
    {
        assertEquals(RequirementState.DATA_REQUIRED, necklace(2).evaluate(null).getState());
    }

    private ChargedTeleportBudgetRequirement necklace(int uses)
    {
        return new ChargedTeleportBudgetRequirement(ChargedTeleportItemDefinition.SKILLS_NECKLACE, uses);
    }

    private RequirementContext context(Map<Integer, Integer> inventory, Map<Integer, Integer> equipment,
            Map<Integer, Long> bank, boolean verified)
    {
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                inventory, equipment, Collections.emptyMap());
        return new RequirementContext(null, null, travel, new StorageSnapshot(Collections.emptyMap(), Collections.emptyMap(),
                bank, verified, verified ? Instant.now() : null));
    }
}
