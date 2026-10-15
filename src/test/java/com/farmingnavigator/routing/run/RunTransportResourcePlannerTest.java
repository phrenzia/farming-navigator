package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementState;
import com.farmingnavigator.routing.requirement.RuneCostRequirement;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.TransportCategory;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationKind;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunTransportResourcePlannerTest
{
    @Test
    public void individuallyReadySpellsCannotReuseLastLawRune()
    {
        RequirementContext context = context(Map.of(ItemID.AIRRUNE, 6, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 1),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertTrue(new RuneCostRequirement("One spell", TeleportSpellDefinition.FALADOR_TELEPORT.getRuneCost())
                .evaluate(context).isReadyNow());
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(twoSpellStops(), context);
        assertEquals(RequirementState.BLOCKED, result.getChecks().get(0).getState());
        assertTrue(result.getChecks().get(0).getDetail().contains("Law rune x2"));
    }

    @Test
    public void combinedSpellRunesCanBePreparedFromBank()
    {
        RequirementContext context = context(Map.of(ItemID.AIRRUNE, 6, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 1),
                Collections.emptyMap(), Collections.singletonMap(ItemID.LAWRUNE, 1L), Collections.emptyMap());
        assertTrue(new RunTransportResourcePlanner().evaluate(twoSpellStops(), context).getChecks().get(0).requiresBankWithdrawal());
    }

    @Test
    public void sharedRuneBudgetUsesEquippedStaffAndCarriedPouch()
    {
        RequirementContext staff = context(Map.of(ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2),
                Collections.singletonMap(ItemID.STAFF_OF_AIR, 1), Collections.emptyMap(), Collections.emptyMap());
        assertTrue(new RunTransportResourcePlanner().evaluate(twoSpellStops(), staff).getChecks().get(0).isReadyNow());
        RequirementContext pouch = context(Collections.singletonMap(ItemID.BH_RUNE_POUCH, 1), Collections.emptyMap(),
                Collections.emptyMap(), Map.of(ItemID.AIRRUNE, 6, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2));
        assertTrue(new RunTransportResourcePlanner().evaluate(twoSpellStops(), pouch).getChecks().get(0).isReadyNow());
    }

    @Test
    public void fallbackSpellIsNotAddedToRecommendedSpellCost()
    {
        HerbRunStop stop = new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", spell(), spell(), true);
        RequirementContext context = context(Map.of(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertTrue(new RunTransportResourcePlanner().evaluate(Collections.singletonList(stop), context).getChecks().get(0).isReadyNow());
    }

    @Test
    public void jewelleryUsesAreAggregatedAcrossDestinations()
    {
        VerifiedTransportResult necklace = new VerifiedTransportResult(new TransportOption(1, "Skills necklace", "Teleport",
                TransportCategory.JEWELLERY, TransportVerificationKind.SKILLS_NECKLACE), VerifiedTransportState.READY_NOW, "");
        List<HerbRunStop> stops = Arrays.asList(new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", necklace, null, true),
                new HerbRunStop(HerbPatchDefinition.FARMING_GUILD, "Inspect", necklace, null, true));
        RequirementContext context = context(Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(stops, context);
        assertEquals(1, result.getChecks().size());
        assertEquals(RequirementState.BLOCKED, result.getChecks().get(0).getState());
        assertTrue(result.getChecks().get(0).getRequirement().contains("2 run teleports"));
    }

    @Test
    public void missingRouteRemainsAnExplicitPlanningNote()
    {
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(Collections.singletonList(
                new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", null, null, true)), null);
        assertTrue(result.getChecks().isEmpty());
        assertEquals("Choose transport for Falador.", result.getNotes().get(0));
    }

    private List<HerbRunStop> twoSpellStops()
    {
        return Arrays.asList(new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", spell(), null, true),
                new HerbRunStop(HerbPatchDefinition.CATHERBY, "Inspect", spell(), null, true));
    }

    private VerifiedTransportResult spell()
    {
        return new VerifiedTransportResult(new TransportOption(1, "Falador Teleport", "Cast manually", TransportCategory.SPELL,
                TransportVerificationKind.TELEPORT_SPELL, TeleportSpellDefinition.FALADOR_TELEPORT), VerifiedTransportState.READY_NOW, "");
    }

    private RequirementContext context(Map<Integer, Integer> inventory, Map<Integer, Integer> equipment,
            Map<Integer, Long> bank, Map<Integer, Integer> pouch)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
        Map<Integer, Long> inv = new HashMap<>();
        Map<Integer, Long> eq = new HashMap<>();
        inventory.forEach((id, quantity) -> inv.put(id, quantity.longValue()));
        equipment.forEach((id, quantity) -> eq.put(id, quantity.longValue()));
        return new RequirementContext(account, null, new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                inventory, equipment, pouch), new StorageSnapshot(inv, eq, bank, true, Instant.now()));
    }
}
