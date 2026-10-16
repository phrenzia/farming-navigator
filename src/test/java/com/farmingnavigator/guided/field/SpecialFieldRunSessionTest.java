package com.farmingnavigator.guided.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.routing.run.*;
import com.farmingnavigator.overlay.FieldSceneTargetPolicy;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpecialFieldRunSessionTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final FieldRunSession session=new FieldRunSession();
    private void observe(FieldPatchDefinition patch,int raw,int second)
    {
        session.observe(patch.getLocation(),Map.of(patch,new FieldPatchObservation(new FieldPatchStateDecoder().decode(patch.getFamily(),raw),now.plusSeconds(second),FarmingGrowthRate.NORMAL)),now.plusSeconds(second));
    }
    private FieldRunPlan start(FieldRunCrop crop,FieldPatchDefinition patch)
    {
        FieldRunPlan plan=new FieldRunPlanner().plan(new AccountState(true,99,99,EnumSet.of(Quest.BONE_VOYAGE,Quest.PRIEST_IN_PERIL),false),
                Set.of(patch),crop,FieldRunMode.KEEP_MATURE_FLOWERS,true,FieldRunCompost.ULTRACOMPOST,Map.of(),null,now);
        assertTrue(session.start(plan,true,now)); session.command(FieldRunCommand.CONFIRM,now.plusSeconds(1)); return plan;
    }
    @Test public void allThreeSpecialCropsWaitForEmptyManualCompostAndMatchingPlanting()
    {
        FieldRunCrop[] crops={FieldRunCrop.SEAWEED,FieldRunCrop.MUSHROOM,FieldRunCrop.BELLADONNA};
        FieldPatchDefinition[] patches={FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH,FieldPatchDefinition.MORYTANIA_MUSHROOM,FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA};
        int[] harvest={8,10,8};
        for(int i=0;i<crops.length;i++)
        {
            FieldRunPlan plan=start(crops[i],patches[i]); observe(patches[i],harvest[i],2);
            FieldRunSnapshot snapshot=session.snapshot(now); assertEquals(FieldRunStep.HARVEST,snapshot.getStep());
            if(crops[i]==FieldRunCrop.BELLADONNA)
            {
                assertTrue(snapshot.getInstruction().contains("gloves"));
                assertTrue(GuidedRunTargets.fromField(snapshot,plan,null).getInventoryItems().contains(ItemID.LEATHER_GLOVES));
            }
            observe(patches[i],0,3); assertEquals(FieldRunStep.COMPOST,session.snapshot(now).getStep());
            session.command(FieldRunCommand.CONFIRM,now.plusSeconds(4)); observe(patches[i],4,5);
            assertEquals(crops[i]==FieldRunCrop.SEAWEED?FieldRunStep.PROTECT:FieldRunStep.COMPLETE,session.snapshot(now).getStep());
            if(crops[i]==FieldRunCrop.SEAWEED)
            {
                assertEquals(Set.of(ItemID.FOSSIL_NUMULITE),GuidedRunTargets.fromField(session.snapshot(now),plan,null).getInventoryItems());
                session.command(FieldRunCommand.CONFIRM,now.plusSeconds(6)); assertEquals(1,session.snapshot(now).getProtectionConfirmed());
            }
        }
    }
    @Test public void underwaterGardenerMatchesBothChannelsAndNeverBelladonnaOrMushroom()
    {
        assertTrue(FieldSceneTargetPolicy.isKnownFarmer(NpcID.FOSSIL_GARDENER_UNDERWATER));
        for(FieldPatchDefinition patch:List.of(FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_NORTH,FieldPatchDefinition.FOSSIL_ISLAND_SEAWEED_SOUTH))
        { assertTrue(FieldSceneTargetPolicy.isFarmerFor(patch,NpcID.FOSSIL_GARDENER_UNDERWATER,patch.getLocation())); }
        for(FieldPatchDefinition patch:List.of(FieldPatchDefinition.MORYTANIA_MUSHROOM,FieldPatchDefinition.DRAYNOR_MANOR_BELLADONNA))
        { assertFalse(FieldSceneTargetPolicy.isFarmerFor(patch,NpcID.FOSSIL_GARDENER_UNDERWATER,patch.getLocation())); }
    }
}
