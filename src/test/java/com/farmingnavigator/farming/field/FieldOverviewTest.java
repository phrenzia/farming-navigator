package com.farmingnavigator.farming.field;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldOverviewTest
{
    private final Instant now=Instant.parse("2026-10-15T12:00:00Z");
    private final AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
    private FieldPatchObservation observation(FieldFamily family,int raw,Instant at,FarmingGrowthRate rate)
    {
        return new FieldPatchObservation(new FieldPatchStateDecoder().decode(family,raw),at,rate);
    }
    @Test public void knownProblemsSortBeforeHistoricalHarvestabilityAndOtherFamiliesAreFiltered()
    {
        FieldOverview result=FieldOverview.create(account,EnumSet.of(FieldPatchDefinition.FALADOR_NORTH,FieldPatchDefinition.FALADOR_SOUTH,FieldPatchDefinition.FALADOR_FLOWER),FieldFamily.ALLOTMENT,
                Map.of(FieldPatchDefinition.FALADOR_NORTH,observation(FieldFamily.ALLOTMENT,10,now,FarmingGrowthRate.NORMAL),
                        FieldPatchDefinition.FALADOR_SOUTH,observation(FieldFamily.ALLOTMENT,135,now,FarmingGrowthRate.NORMAL)),now);
        assertEquals(2,result.getEntries().size()); assertEquals(1,result.getCount(FieldOverview.Group.DISEASED)); assertEquals(1,result.getCount(FieldOverview.Group.HARVESTABLE));
        assertEquals(FieldPatchDefinition.FALADOR_SOUTH,result.getEntries().get(0).getPatch());
        try { result.getEntries().clear(); fail("Mutable overview"); } catch(UnsupportedOperationException expected) { }
    }
    @Test public void earliestConditionalWindowNeverTurnsIntoAnObservedHarvest()
    {
        Map<FieldPatchDefinition,FieldPatchObservation> history=Map.of(FieldPatchDefinition.LUMBRIDGE_HOPS,observation(FieldFamily.HOPS,4,now,FarmingGrowthRate.NORMAL));
        for(int seconds:new int[]{1799,1800,2400})
        {
            FieldOverview result=FieldOverview.create(account,EnumSet.of(FieldPatchDefinition.LUMBRIDGE_HOPS),FieldFamily.HOPS,history,now.plusSeconds(seconds));
            assertEquals(0,result.getCount(FieldOverview.Group.HARVESTABLE));
            assertEquals(1,result.getCount(seconds<1800?FieldOverview.Group.GROWING:FieldOverview.Group.POSSIBLY_READY));
        }
    }
    @Test public void scarecrowsAreNotGrowingOrHarvestableFlowers()
    {
        FieldOverview result=FieldOverview.create(account,EnumSet.of(FieldPatchDefinition.FALADOR_FLOWER),FieldFamily.FLOWER,
                Map.of(FieldPatchDefinition.FALADOR_FLOWER,observation(FieldFamily.FLOWER,36,now,FarmingGrowthRate.NORMAL)),now.plusSeconds(9999));
        assertEquals(1,result.getCount(FieldOverview.Group.SCARECROW)); assertEquals(0,result.getCount(FieldOverview.Group.HARVESTABLE)); assertEquals(0,result.getCount(FieldOverview.Group.POSSIBLY_READY));
    }
    @Test public void futureWrongFamilyUnknownAndMissingDataRemainUnknown()
    {
        for(FieldPatchObservation invalid:new FieldPatchObservation[]{observation(FieldFamily.ALLOTMENT,10,now.plusSeconds(1),FarmingGrowthRate.NORMAL),
                observation(FieldFamily.FLOWER,41,now,FarmingGrowthRate.NORMAL),observation(FieldFamily.ALLOTMENT,-1,now,FarmingGrowthRate.NORMAL)})
        {
            FieldOverview result=FieldOverview.create(account,EnumSet.of(FieldPatchDefinition.FALADOR_NORTH),FieldFamily.ALLOTMENT,Map.of(FieldPatchDefinition.FALADOR_NORTH,invalid),now);
            assertEquals(1,result.getCount(FieldOverview.Group.UNKNOWN)); assertEquals(0,result.getCount(FieldOverview.Group.HARVESTABLE));
        }
        assertEquals(1,FieldOverview.create(account,EnumSet.of(FieldPatchDefinition.FALADOR_NORTH),FieldFamily.ALLOTMENT,Map.of(),now).getCount(FieldOverview.Group.UNKNOWN));
    }
    @Test public void inaccessibleAndUnverifiedPlotsAreExcludedRatherThanReportedReady()
    {
        FieldOverview hops=FieldOverview.create(account,EnumSet.allOf(FieldPatchDefinition.class),FieldFamily.HOPS,Map.of(),now);
        assertEquals(3,hops.getEntries().size()); assertEquals(2,hops.getExcludedCount());
        FieldOverview guild=FieldOverview.create(new AccountState(true,1,44,EnumSet.noneOf(Quest.class),false),EnumSet.of(FieldPatchDefinition.FARMING_GUILD_NORTH),FieldFamily.ALLOTMENT,
                Map.of(FieldPatchDefinition.FARMING_GUILD_NORTH,observation(FieldFamily.ALLOTMENT,10,now,FarmingGrowthRate.NORMAL)),now);
        assertTrue(guild.getEntries().isEmpty()); assertEquals(1,guild.getExcludedCount());
    }
    @Test public void unknownGrowthRateCannotProduceAnEstimatedReadyCount()
    {
        FieldOverview result=FieldOverview.create(account,EnumSet.of(FieldPatchDefinition.FALADOR_NORTH),FieldFamily.ALLOTMENT,
                Map.of(FieldPatchDefinition.FALADOR_NORTH,observation(FieldFamily.ALLOTMENT,6,now,FarmingGrowthRate.UNKNOWN)),now.plusSeconds(9999));
        assertEquals(1,result.getCount(FieldOverview.Group.GROWING)); assertEquals(0,result.getCount(FieldOverview.Group.POSSIBLY_READY));
    }
}
