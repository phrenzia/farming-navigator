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
}
