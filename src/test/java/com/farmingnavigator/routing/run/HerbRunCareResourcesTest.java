package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.loadout.LoadoutService;
import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.*;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class HerbRunCareResourcesTest
{
    private HerbRunPlan plan(boolean diseased)
    {
        List<HerbRunStop> stops = Arrays.asList(
                new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", null, null, !diseased),
                new HerbRunStop(HerbPatchDefinition.CATHERBY, "Inspect", null, null, !diseased));
        return new HerbRunPlan(HerbRunCrop.GUAM, stops, new LoadoutService().evaluate(Arrays.asList(
                new SupplyRequirement(ItemID.PLANT_CURE, "Plant cure", 2, true, "Cure disease"),
                new SupplyRequirement(ItemID.SPADE, "Spade", 1, false, "Harvest")), null), Collections.emptyMap(), "");
    }

    private RequirementContext context(int realMagic, int boostedMagic, QuestState quest, SpellbookType book,
            Map<Integer, Integer> inventory, Map<Integer, Integer> equipment, Map<Integer, Long> bank)
    {
        AccountState account = new AccountState(true, 99, 99, realMagic, boostedMagic, 99, 99, 99, 99,
                Collections.singletonMap(Quest.LUNAR_DIPLOMACY, quest), Collections.emptyMap(), Collections.emptyMap(), false);
        Map<Integer, Long> inv = new HashMap<>();
        Map<Integer, Long> eq = new HashMap<>();
        inventory.forEach((id, qty) -> inv.put(id, qty.longValue()));
        equipment.forEach((id, qty) -> eq.put(id, qty.longValue()));
        return new RequirementContext(account, null,
                new TravelCapabilitySnapshot(true, book, null, inventory, equipment, Collections.emptyMap()),
                new StorageSnapshot(inv, eq, bank, true, Instant.now()));
    }

    private RequirementContext eligible()
    {
        return context(66, 66, QuestState.FINISHED, SpellbookType.LUNAR,
                Map.of(ItemID.EARTHRUNE, 16, ItemID.ASTRALRUNE, 2), Collections.emptyMap(), Collections.emptyMap());
    }

    @Test public void twoDiseasedStopsReserveTwoCastsAndRemoveOnlyPotionSupply()
    {
        HerbRunCareResources care = HerbRunCareResources.evaluate(plan(true), HerbRunCure.CURE_PLANT, eligible());
        assertEquals(16, care.getRunes().getQuantity(RuneKind.EARTH));
        assertEquals(2, care.getRunes().getQuantity(RuneKind.ASTRAL));
        assertTrue(care.getChecks().get(0).isReadyNow());
        HerbRunPlan selected = plan(true).withCareResources(care);
        assertEquals(HerbRunCure.CURE_PLANT, selected.getCure());
        assertEquals(1, selected.getSupplies().size());
        assertEquals(ItemID.SPADE, selected.getSupplies().get(0).getRequirement().getItemId());
        assertEquals(2, plan(true).getSupplies().size());
        assertEquals(2, selected.withCareResources(HerbRunCareResources.evaluate(selected, HerbRunCure.PLANT_CURE, null))
                .getSupplies().size());
    }

    @Test public void potionDefaultAndCompatibilityPlansKeepTheirSupplies()
    {
        HerbRunCareResources care = HerbRunCareResources.evaluate(plan(true), null, null);
        assertEquals(HerbRunCure.PLANT_CURE, care.getCure());
        assertTrue(care.getRunes().isEmpty());
        assertTrue(care.getChecks().isEmpty());
        assertEquals(2, plan(true).withCareResources(care).getSupplies().size());
        assertEquals(HerbRunCure.PLANT_CURE, plan(true).getCure());
    }

    @Test public void healthyStopsDoNotInventCureCasts()
    {
        HerbRunCareResources care = HerbRunCareResources.evaluate(plan(false), HerbRunCure.CURE_PLANT, null);
        assertTrue(care.getRunes().isEmpty());
        assertTrue(care.getChecks().isEmpty());
    }

    @Test public void loweredMagicBlocksButObservedBoostPermitsCasting()
    {
        for (int magic : new int[]{1, 65, 66})
        {
            HerbRunCareResources care = HerbRunCareResources.evaluate(plan(true), HerbRunCure.CURE_PLANT,
                    context(65, magic, QuestState.FINISHED, SpellbookType.LUNAR, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
            assertEquals(magic == 66, care.getChecks().get(0).isSatisfied());
        }
    }

    @Test public void wrongSpellbookAndUnfinishedQuestRemainBlockers()
    {
        for (SpellbookType book : Arrays.asList(SpellbookType.STANDARD, SpellbookType.ANCIENT, SpellbookType.ARCEUUS))
        {
            assertEquals(RequirementState.BLOCKED, HerbRunCareResources.evaluate(plan(true), HerbRunCure.CURE_PLANT,
                    context(99, 99, QuestState.FINISHED, book, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()))
                    .getChecks().get(0).getState());
        }
        for (QuestState quest : Arrays.asList(QuestState.NOT_STARTED, QuestState.IN_PROGRESS))
        {
            assertEquals(RequirementState.BLOCKED, HerbRunCareResources.evaluate(plan(true), HerbRunCure.CURE_PLANT,
                    context(99, 99, quest, SpellbookType.LUNAR, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()))
                    .getChecks().get(0).getState());
        }
    }

    @Test public void missingAccountStateDoesNotAssumeAccess()
    {
        assertEquals(RequirementState.DATA_REQUIRED, HerbRunCareResources.evaluate(plan(true), HerbRunCure.CURE_PLANT, null)
                .getChecks().get(0).getState());
    }

    @Test public void farmingAndTransportCannotSpendTheSameAstralRunesTwice()
    {
        RequirementContext context = context(99, 99, QuestState.FINISHED, SpellbookType.LUNAR,
                Map.of(ItemID.ASTRALRUNE, 2, ItemID.LAWRUNE, 1, ItemID.WATERRUNE, 10, ItemID.EARTHRUNE, 16),
                Collections.emptyMap(), Collections.emptyMap());
        HerbRunCareResources care = HerbRunCareResources.evaluate(plan(true), HerbRunCure.CURE_PLANT, context);
        VerifiedTransportResult teleport = new VerifiedTransportResult(new TransportOption(1, "Catherby", "Cast manually",
                TransportCategory.SPELL, TransportVerificationKind.TELEPORT_SPELL, TeleportSpellDefinition.CATHERBY_TELEPORT),
                VerifiedTransportState.READY_NOW, "");
        List<HerbRunStop> stops = Collections.singletonList(new HerbRunStop(HerbPatchDefinition.CATHERBY, "Inspect", teleport, null, false));
        RunTransportResources total = new RunTransportResourcePlanner().evaluate(stops, context, care.getRunes());
        assertEquals(RequirementState.BLOCKED, total.getChecks().get(0).getState());
        assertTrue(total.getChecks().get(0).getDetail().contains("Astral rune x5"));
    }

    @Test public void fertileSoilReservesAllCastsAndMatchesRuneLiteMagicLevel()
    {
        assertEquals(83, net.runelite.client.plugins.skillcalculator.skills.MagicAction.FERTILE_SOIL.getLevel());
        assertEquals(66, net.runelite.client.plugins.skillcalculator.skills.MagicAction.CURE_PLANT.getLevel());
        HerbRunCareResources care = HerbRunCareResources.evaluate(plan(false), HerbRunCure.PLANT_CURE,
                HerbRunCompost.FERTILE_SOIL_SUPER, eligibleAt83());
        assertEquals(30, care.getRunes().getQuantity(RuneKind.EARTH));
        assertEquals(6, care.getRunes().getQuantity(RuneKind.ASTRAL));
        assertEquals(4, care.getRunes().getQuantity(RuneKind.NATURE));
        assertTrue(care.getChecks().get(0).isSatisfied());
        assertTrue(care.getChecks().get(1).getDetail().contains("No volcanic ash is reserved"));
    }

    private RequirementContext eligibleAt83()
    {
        return context(83, 83, QuestState.FINISHED, SpellbookType.LUNAR,
                Map.of(ItemID.ASTRALRUNE, 6, ItemID.NATURERUNE, 4), Map.of(ItemID.STAFF_OF_EARTH, 1), Collections.emptyMap());
    }

    @Test public void ultraSoilNeedsExplicitTomeDeclarationAndDoesNotInferItFromLevel()
    {
        HerbRunCareResources unknown = HerbRunCareResources.evaluate(plan(false), HerbRunCure.PLANT_CURE,
                HerbRunCompost.FERTILE_SOIL_ULTRA, eligibleAt83());
        assertEquals(RequirementState.CONFIGURATION_REQUIRED, unknown.getChecks().get(1).getState());
        FarmingNavigatorConfig declared = new FarmingNavigatorConfig()
        {
            @Override public boolean herbAshTomeRead() { return true; }
        };
        RequirementContext base = eligibleAt83();
        RequirementContext manual = new RequirementContext(base.getAccountState(), declared,
                base.getTravelCapabilitySnapshot(), base.getStorageSnapshot());
        HerbRunCareResources known = HerbRunCareResources.evaluate(plan(false), HerbRunCure.PLANT_CURE,
                HerbRunCompost.FERTILE_SOIL_ULTRA, manual);
        assertTrue(known.getChecks().get(1).isSatisfied());
        assertTrue(known.getChecks().get(1).getDetail().contains("Manually declared"));
    }

    @Test public void cureAndSoilShareFarmingRunePoolWithoutTreatingDiseaseAsReplant()
    {
        HerbRunPlan mixed = new HerbRunPlan(HerbRunCrop.GUAM, Arrays.asList(plan(false).getStops().get(0),
                plan(true).getStops().get(1)), Collections.emptyList(), Collections.emptyMap(), "");
        HerbRunCareResources care = HerbRunCareResources.evaluate(mixed, HerbRunCure.CURE_PLANT,
                HerbRunCompost.FERTILE_SOIL_SUPER, eligibleAt83());
        assertEquals(23, care.getRunes().getQuantity(RuneKind.EARTH));
        assertEquals(4, care.getRunes().getQuantity(RuneKind.ASTRAL));
        assertEquals(2, care.getRunes().getQuantity(RuneKind.NATURE));
    }

    @Test public void soilRequiresBoosted83LunarAndQuestAccess()
    {
        for (int magic : new int[]{82, 83})
        {
            RequirementContext context = context(82, magic, QuestState.FINISHED, SpellbookType.LUNAR,
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
            assertEquals(magic == 83, HerbRunCareResources.evaluate(plan(false), HerbRunCure.PLANT_CURE,
                    HerbRunCompost.FERTILE_SOIL_SUPER, context).getChecks().get(0).isSatisfied());
        }
        RequirementContext standard = context(99, 99, QuestState.FINISHED, SpellbookType.STANDARD,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertEquals(RequirementState.BLOCKED, HerbRunCareResources.evaluate(plan(false), HerbRunCure.PLANT_CURE,
                HerbRunCompost.FERTILE_SOIL_SUPER, standard).getChecks().get(0).getState());
        assertEquals(RequirementState.DATA_REQUIRED, HerbRunCareResources.evaluate(plan(false), HerbRunCure.PLANT_CURE,
                HerbRunCompost.FERTILE_SOIL_SUPER, null).getChecks().get(0).getState());
    }

    @Test public void diseasedOnlyPlanDoesNotReserveSoilOrDemandTome()
    {
        HerbRunCareResources care = HerbRunCareResources.evaluate(plan(true), HerbRunCure.PLANT_CURE,
                HerbRunCompost.FERTILE_SOIL_ULTRA, null);
        assertTrue(care.getRunes().isEmpty());
        assertTrue(care.getChecks().isEmpty());
    }

    @Test public void fertileSoilPoolAcceptsInfiniteEarthAndChecksTotalAstrals()
    {
        RequirementContext base = eligibleAt83();
        HerbRunCareResources care = HerbRunCareResources.evaluate(plan(false), HerbRunCure.PLANT_CURE,
                HerbRunCompost.FERTILE_SOIL_SUPER, base);
        assertTrue(new RunTransportResourcePlanner().evaluate(Collections.emptyList(), base, care.getRunes()).getChecks().get(0).isReadyNow());
        RequirementContext shortfall = context(83, 83, QuestState.FINISHED, SpellbookType.LUNAR,
                Map.of(ItemID.ASTRALRUNE, 5, ItemID.NATURERUNE, 4), Map.of(ItemID.STAFF_OF_EARTH, 1), Collections.emptyMap());
        assertEquals(RequirementState.BLOCKED, new RunTransportResourcePlanner().evaluate(Collections.emptyList(), shortfall,
                care.getRunes()).getChecks().get(0).getState());
    }

    @Test public void infiniteEarthSourceAndBankedAstralsFundCurePool()
    {
        RequirementContext context = context(66, 66, QuestState.FINISHED, SpellbookType.LUNAR,
                Map.of(ItemID.ASTRALRUNE, 1), Map.of(ItemID.STAFF_OF_EARTH, 1), Map.of(ItemID.ASTRALRUNE, 1L));
        HerbRunCareResources care = HerbRunCareResources.evaluate(plan(true), HerbRunCure.CURE_PLANT, context);
        RunTransportResources total = new RunTransportResourcePlanner().evaluate(Collections.emptyList(), context, care.getRunes());
        assertTrue(total.getChecks().get(0).requiresBankWithdrawal());
        assertEquals(Collections.singleton(ItemID.ASTRALRUNE), total.getBankItems());
        assertEquals(HerbRunCure.CURE_PLANT, plan(true).withCareResources(care).withTransportResources(total).getCure());
    }
}
