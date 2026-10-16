package com.farmingnavigator.economics;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.HerbPatchStatus;
import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchStateDecoder;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingMaterialEstimatorTest
{
    private final Instant now = Instant.parse("2026-10-15T12:00:00Z");
    private final FarmingMaterialEstimator estimator = new FarmingMaterialEstimator();
    private AccountState account()
    {
        return new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, Map.of(Quest.BONE_VOYAGE, QuestState.FINISHED), Map.of(), Map.of());
    }
    private TreeRunPlan trees(TreeRunCrop crop, boolean protect)
    {
        TreePatchDefinition patch = crop.getCrop().getFamily() == com.farmingnavigator.farming.tree.TreeFamily.HARDWOOD_TREE
                ? TreePatchDefinition.FOSSIL_ISLAND_EAST : TreePatchDefinition.VARROCK_TREE;
        return new TreeRunPlanner().plan(account(), EnumSet.of(patch), crop, protect, Map.of(), null, now);
    }
    private HerbRunPlan herbs(HerbRunCompost compost, boolean diseased)
    {
        return new HerbRunPlanner().plan(account(), List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR, List.of())),
                EnumSet.of(HerbPatchDefinition.FALADOR), HerbRunCrop.RANARR, compost,
                diseased ? Map.of(HerbPatchDefinition.FALADOR, new HerbPatchObservation(new HerbPatchStateDecoder().decode(140), now)) : Map.of(),
                Map.of(), null, now);
    }
    @Test public void treeSaplingAndProtectionAreValuedWithoutReusableTools()
    {
        FarmingMaterialEstimate result = estimator.trees(trees(TreeRunCrop.MAGIC, true), true,
                id -> id == ItemID.PLANTPOT_MAGIC_TREE_SAPLING ? 100000 : id == ItemID.COCONUT ? 100 : 9999999);
        assertEquals(2, result.getLines().size()); assertEquals(102500, result.getKnownSubtotal().getAsLong());
        assertEquals(25, result.getLines().get(1).getQuantity());
        assertTrue(result.getNotes().stream().anyMatch(note -> note.contains("not profit")));
        assertTrue(result.getNotes().stream().anyMatch(note -> note.contains("owned materials")));
    }
    @Test public void protectionOffAndUnknownProtectionAreDifferent()
    {
        FarmingMaterialEstimate off = estimator.trees(trees(TreeRunCrop.MAGIC, false), false, id -> 100);
        assertEquals(1, off.getLines().size()); assertEquals(100, off.getKnownSubtotal().getAsLong());
        FarmingMaterialEstimate unknown = estimator.trees(trees(TreeRunCrop.CAMPHOR, true), true, id -> 100);
        assertEquals(1, unknown.getLines().size());
        assertTrue(unknown.getNotes().stream().anyMatch(note -> note.contains("does not cover protection")));
    }
    @Test public void fullBasketValueUsesBasketQuantityInsteadOfLooseProduceCount()
    {
        FarmingMaterialEstimate result = estimator.trees(trees(TreeRunCrop.OAK, true), true,
                id -> id == ItemID.BASKET_TOMATO_5 ? 500 : 100);
        assertEquals(1, result.getLines().get(1).getQuantity()); assertEquals(600, result.getKnownSubtotal().getAsLong());
    }

    @Test public void crystalHasNoInventedProtectionCostOrTradePrice()
    {
        AccountState unlocked = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Map.of(Quest.SONG_OF_THE_ELVES, QuestState.FINISHED), Map.of(), Map.of());
        TreeRunPlan plan = new TreeRunPlanner().plan(unlocked, EnumSet.of(TreePatchDefinition.PRIFDDINAS_CRYSTAL),
                TreeRunCrop.CRYSTAL, true, Map.of(), null, now);
        FarmingMaterialEstimate result = estimator.trees(plan, true, id -> {
            assertEquals(ItemID.PLANTPOT_CRYSTAL_TREE_SAPLING, id); return 0;
        });
        assertEquals(1, result.getLines().size());
        assertEquals(1, result.getUnknownPriceCount());
        assertTrue(result.getNotes().stream().noneMatch(note -> note.contains("protection")));
    }
    @Test public void unknownAndNegativeQuotesAreExcludedInsteadOfFreeMaterials()
    {
        FarmingMaterialEstimate result = estimator.trees(trees(TreeRunCrop.MAGIC, true), true,
                id -> id == ItemID.COCONUT ? 0 : 100);
        assertEquals(1, result.getUnknownPriceCount()); assertEquals(100, result.getKnownSubtotal().getAsLong());
        assertFalse(result.getLines().get(1).getValue().isPresent());
        FarmingMaterialEstimate negative = estimator.trees(trees(TreeRunCrop.MAGIC, true), true, id -> -1);
        assertEquals(2, negative.getUnknownPriceCount()); assertEquals(0, negative.getKnownSubtotal().getAsLong());
    }
    @Test public void normalHerbCompostIsValuedButWholeBottomlessBucketIsNotConsumed()
    {
        FarmingMaterialEstimate bucket = estimator.herbs(herbs(HerbRunCompost.ULTRACOMPOST, false), HerbRunCompost.ULTRACOMPOST, id -> 100);
        assertEquals(2, bucket.getLines().size()); assertEquals(200, bucket.getKnownSubtotal().getAsLong());
        FarmingMaterialEstimate bottomless = estimator.herbs(herbs(HerbRunCompost.BOTTOMLESS_ULTRACOMPOST, false),
                HerbRunCompost.BOTTOMLESS_ULTRACOMPOST, id -> 100);
        assertEquals(1, bottomless.getLines().size());
        assertTrue(bottomless.getNotes().stream().anyMatch(note -> note.contains("Bottomless compost use value is unknown")));
    }
    @Test public void observedDiseasePotionAndCureSpellDoNotBothGetCharged()
    {
        HerbRunPlan diseased = herbs(HerbRunCompost.NONE, true);
        FarmingMaterialEstimate potion = estimator.herbs(diseased, HerbRunCompost.NONE, id -> 100);
        assertEquals(1, potion.getLines().size()); assertEquals(ItemID.PLANT_CURE, potion.getLines().get(0).getItemId());
        HerbRunPlan spell = diseased.withCareResources(HerbRunCareResources.evaluate(diseased, HerbRunCure.CURE_PLANT, null));
        assertTrue(estimator.herbs(spell, HerbRunCompost.NONE, id -> 100).getLines().isEmpty());
    }
    @Test public void fertileSoilUltraCountsAshButNotImaginaryBucketOrFreeSpellRunes()
    {
        FarmingMaterialEstimate result = estimator.herbs(herbs(HerbRunCompost.FERTILE_SOIL_ULTRA, false),
                HerbRunCompost.FERTILE_SOIL_ULTRA, id -> 100);
        assertEquals(2, result.getLines().size());
        assertEquals(300, result.getKnownSubtotal().getAsLong());
        assertTrue(result.getLines().stream().anyMatch(line -> line.getItemId() == ItemID.FOSSIL_VOLCANIC_ASH && line.getQuantity() == 2));
        assertTrue(result.getNotes().stream().anyMatch(note -> note.contains("spell runes")));
    }
    @Test public void multiplyingOrSummingLargeQuotesCannotProduceNegativeValues()
    {
        FarmingMaterialEstimate multiplied = estimator.trees(trees(TreeRunCrop.MAGIC, true), true, id -> Long.MAX_VALUE);
        assertEquals(1, multiplied.getUnknownPriceCount()); assertEquals(Long.MAX_VALUE, multiplied.getKnownSubtotal().getAsLong());
        FarmingMaterialEstimate summed = estimator.trees(trees(TreeRunCrop.OAK, true), true, id -> Long.MAX_VALUE);
        assertFalse(summed.getKnownSubtotal().isPresent());
    }
    @Test public void noStopsDoesNotReadPricesOrInventMaterials()
    {
        TreeRunPlan empty = new TreeRunPlanner().plan(account(), EnumSet.noneOf(TreePatchDefinition.class), TreeRunCrop.OAK,
                true, Map.of(), null, now);
        FarmingMaterialEstimate result = estimator.trees(empty, true, id -> { fail("Unexpected price lookup"); return 0; });
        assertTrue(result.getLines().isEmpty()); assertEquals(0, result.getKnownSubtotal().getAsLong());
    }
    @Test public void gardenReplacementValuesSeedsAndFullSackPaymentsWithoutPricingTools()
    {
        com.farmingnavigator.farming.garden.GardenPatchDefinition patch = com.farmingnavigator.farming.garden.GardenPatchDefinition.RIMMINGTON_BUSH;
        GardenRunPlan plan = new GardenRunPlanner().plan(account(), EnumSet.of(patch), GardenRunCrop.REDBERRIES,
                GardenRunMode.REPLACE_MATURE, true, Map.of(), null, now);
        FarmingMaterialEstimate result = estimator.gardens(plan, id -> {
            if (id == ItemID.REDBERRY_BUSH_SEED) { return 100; }
            if (id == ItemID.SACK_CABBAGE_10) { return 50; }
            fail("Unexpected tool or travel price lookup"); return 0;
        });
        assertEquals(2, result.getLines().size()); assertEquals(300, result.getKnownSubtotal().getAsLong());
    }
    @Test public void keptMatureGardenPlantsAndPoisonIvyDoNotInventSeedOrPaymentCosts()
    {
        com.farmingnavigator.farming.garden.GardenPatchDefinition patch = com.farmingnavigator.farming.garden.GardenPatchDefinition.RIMMINGTON_BUSH;
        GardenRunPlan keep = new GardenRunPlanner().plan(account(), EnumSet.of(patch), GardenRunCrop.REDBERRIES,
                GardenRunMode.KEEP_MATURE, true, Map.of(patch, new com.farmingnavigator.farming.garden.GardenPatchObservation(
                        new com.farmingnavigator.farming.garden.GardenPatchStateDecoder().decode(com.farmingnavigator.farming.garden.GardenFamily.BUSH, 250),
                        now, com.farmingnavigator.farming.state.FarmingGrowthRate.NORMAL)), null, now);
        assertTrue(estimator.gardens(keep, id -> { fail("No consumables reserved"); return 0; }).getLines().isEmpty());
        GardenRunPlan ivy = new GardenRunPlanner().plan(account(), EnumSet.of(patch), GardenRunCrop.POISON_IVY,
                GardenRunMode.REPLACE_MATURE, true, Map.of(), null, now);
        FarmingMaterialEstimate result = estimator.gardens(ivy, id -> { assertEquals(ItemID.POISONIVY_BUSH_SEED, id); return 0; });
        assertEquals(1, result.getUnknownPriceCount()); assertEquals(1, result.getLines().size());
    }
    @Test public void diseasedCactusValuesCureButNoReplacementOrReusableTools()
    {
        com.farmingnavigator.farming.garden.GardenPatchDefinition patch = com.farmingnavigator.farming.garden.GardenPatchDefinition.AL_KHARID_CACTUS;
        GardenRunPlan plan = new GardenRunPlanner().plan(account(), EnumSet.of(patch), GardenRunCrop.CACTUS,
                GardenRunMode.REPLACE_MATURE, true, Map.of(patch, new com.farmingnavigator.farming.garden.GardenPatchObservation(
                        new com.farmingnavigator.farming.garden.GardenPatchStateDecoder().decode(com.farmingnavigator.farming.garden.GardenFamily.CACTUS, 19),
                        now, com.farmingnavigator.farming.state.FarmingGrowthRate.NORMAL)), null, now);
        FarmingMaterialEstimate result = estimator.gardens(plan, id -> { assertEquals(ItemID.PLANT_CURE, id); return 100; });
        assertEquals(1, result.getLines().size()); assertEquals(100, result.getKnownSubtotal().getAsLong());
    }
    @Test public void fieldValuesCountSixAllotmentSeedsAndExactFullPaymentContainers()
    {
        FieldRunPlan plan = new FieldRunPlanner().plan(account(), EnumSet.of(com.farmingnavigator.farming.field.FieldPatchDefinition.FALADOR_NORTH,
                com.farmingnavigator.farming.field.FieldPatchDefinition.FALADOR_SOUTH), FieldRunCrop.TOMATO, FieldRunMode.HARVEST_ALL, true, Map.of(), null, now);
        FarmingMaterialEstimate result = estimator.fields(plan, id -> {
            assertTrue(id == ItemID.TOMATO_SEED || id == ItemID.SACK_CABBAGE_10); return id == ItemID.TOMATO_SEED ? 5 : 100;
        });
        assertEquals(2, result.getLines().size()); assertEquals(430, result.getKnownSubtotal().getAsLong());
    }
    @Test public void keptMatureFlowerHasNoInventedReplacementOrFarmerValue()
    {
        com.farmingnavigator.farming.field.FieldPatchDefinition patch = com.farmingnavigator.farming.field.FieldPatchDefinition.FALADOR_FLOWER;
        FieldRunPlan plan = new FieldRunPlanner().plan(account(), EnumSet.of(patch), FieldRunCrop.WHITE_LILY, FieldRunMode.KEEP_MATURE_FLOWERS, true,
                Map.of(patch, new com.farmingnavigator.farming.field.FieldPatchObservation(new com.farmingnavigator.farming.field.FieldPatchStateDecoder().decode(
                        com.farmingnavigator.farming.field.FieldFamily.FLOWER, 41), now, com.farmingnavigator.farming.state.FarmingGrowthRate.NORMAL)), null, now);
        assertTrue(estimator.fields(plan, id -> { fail("No consumable reserved"); return 0; }).getLines().isEmpty());
    }
    @Test public void diseasedFieldValuesCureOnlyAndUnknownQuotesStayUnknown()
    {
        com.farmingnavigator.farming.field.FieldPatchDefinition patch = com.farmingnavigator.farming.field.FieldPatchDefinition.FALADOR_NORTH;
        FieldRunPlan plan = new FieldRunPlanner().plan(account(), EnumSet.of(patch), FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, true,
                Map.of(patch, new com.farmingnavigator.farming.field.FieldPatchObservation(new com.farmingnavigator.farming.field.FieldPatchStateDecoder().decode(
                        com.farmingnavigator.farming.field.FieldFamily.ALLOTMENT, 135), now, com.farmingnavigator.farming.state.FarmingGrowthRate.NORMAL)), null, now);
        FarmingMaterialEstimate result = estimator.fields(plan, id -> { assertEquals(ItemID.PLANT_CURE, id); return 0; });
        assertEquals(1, result.getUnknownPriceCount()); assertEquals(1, result.getLines().size());
    }

    @Test public void potatoCompostValueCountsApplicationSeparatelyFromFarmerPayment()
    {
        FieldRunPlan plan = new FieldRunPlanner().plan(account(), EnumSet.of(com.farmingnavigator.farming.field.FieldPatchDefinition.FALADOR_NORTH),
                FieldRunCrop.POTATO, FieldRunMode.HARVEST_ALL, true, FieldRunCompost.COMPOST, Map.of(), null, now);
        FarmingMaterialEstimate estimate = estimator.fields(plan, id -> { assertTrue(id == ItemID.POTATO_SEED || id == ItemID.BUCKET_COMPOST); return id == ItemID.POTATO_SEED ? 5 : 100; });
        assertEquals(3, estimate.getLines().size()); assertEquals(315, estimate.getKnownSubtotal().getAsLong());
    }

    @Test public void bottomlessUseIsExcludedWhileBarleyFarmerPaymentStillHasValue()
    {
        FieldRunPlan plan=new FieldRunPlanner().plan(account(),EnumSet.of(com.farmingnavigator.farming.field.FieldPatchDefinition.LUMBRIDGE_HOPS),FieldRunCrop.BARLEY,
                FieldRunMode.HARVEST_ALL,true,FieldRunCompost.BOTTOMLESS_COMPOST,Map.of(),null,now);
        FarmingMaterialEstimate estimate=estimator.fields(plan,id -> { assertTrue(id==ItemID.BARLEY_SEED || id==ItemID.BUCKET_COMPOST); return 10; });
        assertEquals(2,estimate.getLines().size()); assertEquals(70,estimate.getKnownSubtotal().getAsLong());
        assertTrue(estimate.getNotes().stream().anyMatch(n -> n.contains("Bottomless compost use value is unknown and excluded")));
    }

}
