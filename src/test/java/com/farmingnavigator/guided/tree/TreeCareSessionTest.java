package com.farmingnavigator.guided.tree;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeCareSessionTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final TreeRunSession session=new TreeRunSession();
    private final TreePatchDefinition patch=TreePatchDefinition.VARROCK_TREE;
    private TreePatchObservation observation(int raw,int at) { return new TreePatchObservation(new TreePatchStateDecoder().decode(TreeFamily.TREE,raw),now.plusSeconds(at),FarmingGrowthRate.NORMAL); }
    private void observe(int raw,int second) { session.observe(TreePatchDestination.VARROCK_TREE.getLocation(),Map.of(patch,observation(raw,second)),now.plusSeconds(second)); }
    private TreeRunPlan start(TreeRunCompost compost,TreeRunCure cure,boolean diseased)
    {
        TreeRunPlan plan=new TreeRunPlanner().plan(new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false),Set.of(patch),TreeRunCrop.OAK,true,compost,cure,
                diseased?Map.of(patch,observation(73,0)):Map.of(),null,now);
        session.start(plan,true,now); session.command(TreeRunCommand.CONFIRM,now.plusSeconds(1)); return plan;
    }
    @Test public void bucketAndFertileSoilApplicationWaitForManualConfirmationBeforePlanting()
    {
        for(TreeRunCompost compost:List.of(TreeRunCompost.ULTRACOMPOST,TreeRunCompost.FERTILE_SOIL_ULTRA))
        {
            TreeRunPlan plan=start(compost,TreeRunCure.PRUNE,false); observe(0,2); assertEquals(TreeRunStep.COMPOST,session.snapshot(now).getStep());
            GuidedRunTargets targets=GuidedRunTargets.fromTree(session.snapshot(now),plan,null);
            if(compost.isSpell()) { assertTrue(targets.getInventoryItems().containsAll(Set.of(ItemID.ASTRALRUNE,ItemID.NATURERUNE,ItemID.FOSSIL_VOLCANIC_ASH))); }
            else { assertEquals(Set.of(ItemID.BUCKET_ULTRACOMPOST),targets.getInventoryItems()); }
            observe(8,3); assertEquals(TreeRunStep.COMPOST,session.snapshot(now).getStep());
            session.command(TreeRunCommand.CONFIRM,now.plusSeconds(4)); observe(8,5); assertEquals(TreeRunStep.PROTECT,session.snapshot(now).getStep());
        }
    }
    @Test public void treeSpellCureUsesFreshMatchingHealthyCropRatherThanPotionOrManualConfirmation()
    {
        TreeRunPlan plan=start(TreeRunCompost.NONE,TreeRunCure.CURE_PLANT,true); observe(73,2); assertEquals(TreeRunStep.CURE,session.snapshot(now).getStep());
        assertTrue(session.snapshot(now).getInstruction().contains("Lunar Cure Plant"));
        assertTrue(GuidedRunTargets.fromTree(session.snapshot(now),plan,null).getInventoryItems().contains(ItemID.ASTRALRUNE));
        assertFalse(session.command(TreeRunCommand.CONFIRM,now.plusSeconds(3))); observe(8,4); assertEquals(TreeRunStep.COMPLETE,session.snapshot(now).getStep());
    }
    @Test public void clearedDiseaseOnlyStopCannotRequestUnbudgetedSaplingOrCompost()
    {
        start(TreeRunCompost.ULTRACOMPOST,TreeRunCure.PRUNE,true); observe(0,2); assertEquals(TreeRunStep.COMPLETE,session.snapshot(now).getStep());
    }
    @Test public void staleTreeEvidenceCannotChooseActionsAndUnknownDoesNotEraseCompostConfirmation()
    {
        start(TreeRunCompost.COMPOST,TreeRunCure.PRUNE,false);
        assertFalse(session.observe(TreePatchDestination.VARROCK_TREE.getLocation(),Map.of(patch,observation(0,2)),now.plusSeconds(33)));
        observe(0,34); session.command(TreeRunCommand.CONFIRM,now.plusSeconds(35)); observe(-1,36); observe(0,37);
        assertEquals(TreeRunStep.PLANT,session.snapshot(now).getStep());
    }
    @Test public void compostConfirmationResetsBetweenTreeStops()
    {
        TreeRunPlan plan=new TreeRunPlanner().plan(new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false),Set.of(patch,TreePatchDefinition.FALADOR_TREE),TreeRunCrop.OAK,true,
                TreeRunCompost.COMPOST,TreeRunCure.PRUNE,Map.of(),null,now);
        session.start(plan,true,now); session.command(TreeRunCommand.CONFIRM,now.plusSeconds(1)); observe(0,2);
        session.command(TreeRunCommand.CONFIRM,now.plusSeconds(3)); observe(8,4); session.command(TreeRunCommand.CONFIRM,now.plusSeconds(5));
        session.observe(TreePatchDestination.FALADOR_TREE.getLocation(),Map.of(TreePatchDefinition.FALADOR_TREE,observation(0,6)),now.plusSeconds(6));
        assertEquals(TreeRunStep.COMPOST,session.snapshot(now).getStep());
    }

}
