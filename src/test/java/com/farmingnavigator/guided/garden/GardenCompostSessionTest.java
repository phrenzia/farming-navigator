package com.farmingnavigator.guided.garden;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.guided.GuidedRunTargets;
import com.farmingnavigator.routing.run.*;
import java.time.Instant;
import java.util.*;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenCompostSessionTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final GardenRunSession session=new GardenRunSession();
    private final GardenPatchDefinition patch=GardenPatchDefinition.RIMMINGTON_BUSH;
    private GardenRunPlan start(GardenRunCompost compost)
    {
        GardenRunPlan plan=new GardenRunPlanner().plan(new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false),
                EnumSet.of(patch,GardenPatchDefinition.CHAMPIONS_GUILD_BUSH),GardenRunCrop.REDBERRIES,GardenRunMode.REPLACE_MATURE,true,compost,Map.of(),null,now);
        assertTrue(session.start(plan,true,now)); session.command(GardenRunCommand.CONFIRM,now.plusSeconds(1)); return plan;
    }
    private boolean observe(GardenPatchDefinition target,int raw,int observed,int at)
    {
        return session.observe(target.getLocation(),Map.of(target,new GardenPatchObservation(new GardenPatchStateDecoder().decode(GardenFamily.BUSH,raw),now.plusSeconds(observed),FarmingGrowthRate.NORMAL)),now.plusSeconds(at));
    }
    private GardenRunStep step() { return session.snapshot(now.plusSeconds(100)).getStep(); }
    @Test public void emptyPatchRequiresManualApplicationBeforePlantingAndHighlightsOnlySelectedCompost()
    {
        for(GardenRunCompost compost:List.of(GardenRunCompost.COMPOST,GardenRunCompost.BOTTOMLESS_ULTRACOMPOST))
        {
            GardenRunPlan plan=start(compost); observe(patch,0,2,2); assertEquals(GardenRunStep.COMPOST,step());
            assertEquals(Set.of(compost.getItemId()),GuidedRunTargets.fromGarden(session.snapshot(now),plan,null).getInventoryItems());
            assertTrue(session.snapshot(now).getInstruction().contains("not observed"));
            observe(patch,5,3,3); assertEquals(GardenRunStep.COMPOST,step());
            session.command(GardenRunCommand.CONFIRM,now.plusSeconds(4)); assertEquals(GardenRunStep.PLANT,step());
            observe(patch,5,5,5); assertEquals(GardenRunStep.PROTECT,step());
            session.command(GardenRunCommand.CONFIRM,now.plusSeconds(6));
            observe(GardenPatchDefinition.CHAMPIONS_GUILD_BUSH,0,7,7); assertEquals(GardenRunStep.COMPOST,step());
        }
    }
    @Test public void confirmationSurvivesUnknownEvidenceButResetsOnRestart()
    {
        start(GardenRunCompost.ULTRACOMPOST); observe(patch,0,2,2);
        session.command(GardenRunCommand.CONFIRM,now.plusSeconds(3)); observe(patch,-1,4,4); assertEquals(GardenRunStep.INSPECT,step());
        observe(patch,0,5,5); assertEquals(GardenRunStep.PLANT,step());
        start(GardenRunCompost.ULTRACOMPOST); observe(patch,0,2,2); assertEquals(GardenRunStep.COMPOST,step());
    }
    @Test public void weedsClearBeforeApplicationAndStaleEvidenceCannotChooseAnAction()
    {
        start(GardenRunCompost.SUPERCOMPOST);
        assertFalse(observe(patch,0,2,33)); assertEquals(GardenRunStep.TRAVEL,step());
        observe(patch,1,34,34); assertEquals(GardenRunStep.RAKE,step());
        observe(patch,0,35,35); assertEquals(GardenRunStep.COMPOST,step());
    }
    @Test public void wrongCropCannotProveCompostApplicationOrPlanting()
    {
        start(GardenRunCompost.ULTRACOMPOST); observe(patch,0,2,2); observe(patch,15,3,3);
        assertEquals(GardenRunStep.WAIT,step()); assertEquals(0,session.snapshot(now).getCompleted());
        observe(patch,0,4,4); assertEquals(GardenRunStep.COMPOST,step());
    }
}
