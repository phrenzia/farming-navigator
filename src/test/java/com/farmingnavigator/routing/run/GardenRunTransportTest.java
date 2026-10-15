package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementState;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenRunTransportTest
{
    private RequirementContext context(QuestState plague, boolean rings, Map<Integer, Long> inventory, Map<Integer, Long> bank)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Map.of(Quest.PLAGUE_CITY, plague), Map.of(), Map.of(), false, rings, false);
        return new RequirementContext(account, new FarmingNavigatorConfig() {}, new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue())), Map.of(), Map.of()),
                new StorageSnapshot(inventory, Map.of(), bank, true, Instant.now()));
    }
    private Map<GardenPatchDefinition, List<VerifiedTransportResult>> routes(RequirementContext context)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(), context.getConfig(), context.getTravelCapabilitySnapshot(),
                context.getStorageSnapshot(), new GardenPatchTransportService().getAllOptions());
    }
    private GardenRunPlan plan(RequirementContext context, GardenRunCrop crop, GardenPatchDefinition... selected)
    {
        return new GardenRunPlanner().plan(context.getAccountState(), EnumSet.copyOf(List.of(selected)), crop, GardenRunMode.KEEP_MATURE,
                true, Map.of(), context.getStorageSnapshot(), Instant.now()).withTransports(routes(context), HerbTransportPreference.CATALOGUE, context);
    }
    @Test public void bankedGuildNecklaceUsesSharedChargeAndWithdrawalChecks()
    {
        RequirementContext context = context(QuestState.NOT_STARTED, false, Map.of(), Map.of(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1L));
        GardenRunPlan plan = plan(context, GardenRunCrop.REDBERRIES, GardenPatchDefinition.FARMING_GUILD_BUSH);
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, plan.getStops().get(0).getRecommended().getState());
        assertTrue(plan.getTransportResources().getChecks().get(0).requiresBankWithdrawal());
        assertTrue(plan.getTransportResources().getBankItems().contains(ItemID.JEWL_NECKLACE_OF_SKILLS_1));
    }
    @Test public void independentlyReadyGardenSpellsCannotReuseOneLawRune()
    {
        RequirementContext context = context(QuestState.NOT_STARTED, false,
                Map.of(ItemID.AIRRUNE, 6L, ItemID.FIRERUNE, 1L, ItemID.WATERRUNE, 1L, ItemID.LAWRUNE, 1L), Map.of());
        GardenRunPlan plan = plan(context, GardenRunCrop.REDBERRIES, GardenPatchDefinition.RIMMINGTON_BUSH, GardenPatchDefinition.CHAMPIONS_GUILD_BUSH);
        assertTrue(plan.getStops().stream().allMatch(stop -> stop.getRecommended() != null));
        assertEquals(RequirementState.BLOCKED, plan.getTransportResources().getChecks().get(0).getState());
        assertTrue(plan.getTransportResources().getChecks().get(0).getDetail().contains("Law rune x2"));
    }
    @Test public void ardougneSpellRequiresPlagueCityButFairyRingDjpIsIndependent()
    {
        Map<Integer, Long> runes = Map.of(ItemID.WATERRUNE, 2L, ItemID.LAWRUNE, 2L);
        assertNull(plan(context(QuestState.IN_PROGRESS, false, runes, Map.of()), GardenRunCrop.REDBERRIES,
                GardenPatchDefinition.ARDOUGNE_BUSH).getStops().get(0).getRecommended());
        assertEquals(TransportVerificationKind.TELEPORT_SPELL, plan(context(QuestState.FINISHED, false, runes, Map.of()), GardenRunCrop.REDBERRIES,
                GardenPatchDefinition.ARDOUGNE_BUSH).getStops().get(0).getRecommended().getOption().getVerificationKind());
        GardenRunPlan rings = plan(context(QuestState.NOT_STARTED, true, Map.of(ItemID.DRAMEN_STAFF, 1L), Map.of()),
                GardenRunCrop.REDBERRIES, GardenPatchDefinition.ARDOUGNE_BUSH, GardenPatchDefinition.FARMING_GUILD_BUSH);
        assertTrue(rings.getStops().stream().allMatch(stop -> stop.getRecommended().getOption().getVerificationKind() == TransportVerificationKind.FAIRY_RING_ACCESS));
        assertTrue(rings.getStops().get(0).getRecommended().getOption().getDisplayName().contains("DJP"));
        assertTrue(rings.getTransportResources().getConsumables().isEmpty());
    }
    @Test public void alKharidGloryAndChampionsBraceletNeedChargedItems()
    {
        RequirementContext context = context(QuestState.NOT_STARTED, false,
                Map.of(ItemID.AMULET_OF_GLORY_1, 1L, ItemID.JEWL_BRACELET_OF_COMBAT_1, 1L), Map.of());
        assertEquals(TransportVerificationKind.AMULET_OF_GLORY, plan(context, GardenRunCrop.CACTUS,
                GardenPatchDefinition.AL_KHARID_CACTUS).getStops().get(0).getRecommended().getOption().getVerificationKind());
        GardenRunPlan bush = plan(context, GardenRunCrop.REDBERRIES, GardenPatchDefinition.CHAMPIONS_GUILD_BUSH);
        assertEquals(TransportVerificationKind.COMBAT_BRACELET, bush.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(bush.getStops().get(0).getRecommended().getOption().getInstructions().contains("Do not enter the guild"));
        assertTrue(bush.getTransportResources().getChecks().get(0).isReadyNow());
        assertFalse(new GardenPatchTransportService().getAllOptions().containsKey(GardenPatchDefinition.ETCETERIA_BUSH));
    }
}
