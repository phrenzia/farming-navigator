package com.farmingnavigator.farming.state;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.*;
import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.farming.garden.*;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.routing.run.CombinedRunKind;
import java.time.*;
import java.util.*;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReadinessDashboardTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private final AccountState account=new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);
    private HerbPatchObservation herb(int raw,Instant at,FarmingGrowthRate rate) { return new HerbPatchObservation(new HerbPatchStateDecoder().decode(raw),at,rate); }
    private TreePatchObservation tree(TreeFamily family,int raw,Instant at) { return new TreePatchObservation(new TreePatchStateDecoder().decode(family,raw),at,FarmingGrowthRate.NORMAL); }
    private GardenPatchObservation garden(GardenFamily family,int raw,Instant at) { return new GardenPatchObservation(new GardenPatchStateDecoder().decode(family,raw),at,FarmingGrowthRate.NORMAL); }
    private FieldPatchObservation field(FieldFamily family,int raw,Instant at) { return new FieldPatchObservation(new FieldPatchStateDecoder().decode(family,raw),at,FarmingGrowthRate.NORMAL); }
    private ReadinessDashboard.Builder herbs(int raw,Instant at,FarmingGrowthRate rate)
    {
        return ReadinessDashboard.at(now).herbs(List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of())),Set.of(HerbPatchDefinition.FALADOR),Map.of(HerbPatchDefinition.FALADOR,herb(raw,at,rate)));
    }
    @Test public void crossFamilyIdentityPreservesCoLocatedPlotsAndPriority()
    {
        ReadinessDashboard result=herbs(8,now,FarmingGrowthRate.NORMAL)
                .trees(account,Set.of(TreePatchDefinition.FALADOR_TREE),Map.of(TreePatchDefinition.FALADOR_TREE,tree(TreeFamily.TREE,12,now)))
                .gardens(account,Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,garden(GardenFamily.BUSH,70,now)))
                .fields(account,Set.of(FieldPatchDefinition.FALADOR_NORTH,FieldPatchDefinition.FALADOR_SOUTH),Map.of(FieldPatchDefinition.FALADOR_NORTH,field(FieldFamily.ALLOTMENT,10,now))).build();
        assertEquals(5,result.getEntries().size()); assertEquals(5,result.getEntries().stream().map(ReadinessDashboard.Entry::getIdentity).distinct().count());
        assertEquals(ReadinessDashboard.Group.DISEASED,result.getEntries().get(0).getGroup());
        assertEquals(2,result.getCount(ReadinessDashboard.Group.HARVESTABLE)); assertEquals(1,result.getCount(ReadinessDashboard.Group.CHECK_HEALTH));
        assertEquals(1,result.getCount(ReadinessDashboard.Group.UNKNOWN));
        try { result.getEntries().clear(); fail("Mutable entries"); } catch(UnsupportedOperationException expected) { }
    }
    @Test public void horizonBoundariesAreCumulativeAndExcludeAlreadyDueEstimates()
    {
        for(int seconds:new int[]{299,300,301,600,1200,1800,3600,3601})
        {
            ReadinessDashboard result=herbs(4,now.minusSeconds(3600-seconds),FarmingGrowthRate.NORMAL).build();
            // A future observation is deliberately unknown, including the 3601-second case.
            for(int minutes:new int[]{5,10,20,30,60})
            {
                assertEquals(seconds<=3600 && seconds<=minutes*60 ? 1:0,result.getUpcomingCount(Duration.ofMinutes(minutes)));
            }
            assertEquals(0,result.getCount(ReadinessDashboard.Group.HARVESTABLE));
        }
        ReadinessDashboard due=herbs(4,now.minusSeconds(3600),FarmingGrowthRate.NORMAL).build();
        assertEquals(1,due.getCount(ReadinessDashboard.Group.POSSIBLY_READY)); assertEquals(0,due.getUpcomingCount(Duration.ofHours(1))); assertFalse(due.getNext().isPresent());
        assertEquals(0,due.getUpcomingCount(Duration.ZERO)); assertEquals(0,due.getUpcomingCount(Duration.ofMinutes(-1)));
    }
    @Test public void nextWindowIsEarliestAcrossFamiliesRatherThanCatalogueOrder()
    {
        ReadinessDashboard result=herbs(4,now.minusSeconds(3000),FarmingGrowthRate.NORMAL)
                .trees(account,Set.of(TreePatchDefinition.VARROCK_TREE),Map.of(TreePatchDefinition.VARROCK_TREE,tree(TreeFamily.TREE,8,now)))
                .fields(account,Set.of(FieldPatchDefinition.LUMBRIDGE_HOPS),Map.of(FieldPatchDefinition.LUMBRIDGE_HOPS,field(FieldFamily.HOPS,4,now.minusSeconds(1500)))).build();
        assertEquals("FIELD:LUMBRIDGE_HOPS",result.getNext().get().getIdentity()); assertEquals(now.plusSeconds(300),result.getNext().get().getEarliest());
        assertEquals(1,result.getUpcomingCount(Duration.ofMinutes(5))); assertEquals(2,result.getUpcomingCount(Duration.ofMinutes(10)));
    }
    @Test public void invalidFamilyFutureUnknownAndUnknownRateCannotCreateReadiness()
    {
        ReadinessDashboard result=herbs(4,now.minusSeconds(99999),FarmingGrowthRate.UNKNOWN)
                .trees(account,Set.of(TreePatchDefinition.VARROCK_TREE),Map.of(TreePatchDefinition.VARROCK_TREE,tree(TreeFamily.FRUIT_TREE,8,now)))
                .gardens(account,Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,garden(GardenFamily.CACTUS,31,now)))
                .fields(account,Set.of(FieldPatchDefinition.FALADOR_NORTH),Map.of(FieldPatchDefinition.FALADOR_NORTH,field(FieldFamily.ALLOTMENT,10,now.plusSeconds(1)))).build();
        assertEquals(3,result.getCount(ReadinessDashboard.Group.UNKNOWN)); assertEquals(1,result.getCount(ReadinessDashboard.Group.GROWING));
        assertEquals(0,result.getUpcomingCount(Duration.ofDays(1))); assertFalse(result.getNext().isPresent());
    }
    @Test public void accessExclusionsAndDuplicateStatusesNeverInflateReadiness()
    {
        HerbPatchStatus locked=new HerbPatchStatus(HerbPatchDefinition.WEISS,List.of("Fire required"));
        ReadinessDashboard result=ReadinessDashboard.at(now).herbs(List.of(locked,locked),Set.of(HerbPatchDefinition.WEISS),Map.of(HerbPatchDefinition.WEISS,herb(8,now,FarmingGrowthRate.NORMAL)))
                .trees(account,Set.of(TreePatchDefinition.AUBURNVALE_TREE),Map.of())
                .gardens(account,Set.of(GardenPatchDefinition.ETCETERIA_BUSH),Map.of())
                .fields(account,Set.of(FieldPatchDefinition.ENTRANA_HOPS,FieldPatchDefinition.ALDARIN_HOPS),Map.of()).build();
        assertEquals(5,result.getExcludedCount()); assertTrue(result.getEntries().isEmpty());
        ReadinessDashboard duplicate=herbs(8,now,FarmingGrowthRate.NORMAL).herbs(List.of(new HerbPatchStatus(HerbPatchDefinition.FALADOR,List.of())),Set.of(HerbPatchDefinition.FALADOR),Map.of(HerbPatchDefinition.FALADOR,herb(8,now,FarmingGrowthRate.NORMAL))).build();
        assertEquals(1,duplicate.getCount(ReadinessDashboard.Group.HARVESTABLE));
    }
    @Test public void matureTreeAndBushNeverPromiseProduceRegrowthAndScarecrowIsSeparate()
    {
        ReadinessDashboard result=ReadinessDashboard.at(now)
                .trees(account,Set.of(TreePatchDefinition.VARROCK_TREE),Map.of(TreePatchDefinition.VARROCK_TREE,tree(TreeFamily.TREE,13,now)))
                .gardens(account,Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,garden(GardenFamily.BUSH,14,now)))
                .fields(account,Set.of(FieldPatchDefinition.FALADOR_FLOWER),Map.of(FieldPatchDefinition.FALADOR_FLOWER,field(FieldFamily.FLOWER,36,now))).build();
        assertEquals(2,result.getCount(ReadinessDashboard.Group.MATURE)); assertEquals(1,result.getCount(ReadinessDashboard.Group.SCARECROW));
        assertFalse(result.getNext().isPresent()); assertEquals(0,result.getCount(ReadinessDashboard.Group.HARVESTABLE));
    }
    @Test public void celastrusBarkStatesStayDistinctFromHealthChecksAndStumps()
    {
        int[] values={13,14,17,28};
        ReadinessDashboard.Group[] expected={ReadinessDashboard.Group.CHECK_HEALTH,ReadinessDashboard.Group.HARVESTABLE,ReadinessDashboard.Group.MATURE,ReadinessDashboard.Group.CLEARING};
        for(int i=0;i<values.length;i++)
        {
            ReadinessDashboard result=ReadinessDashboard.at(now).trees(account,Set.of(TreePatchDefinition.FARMING_GUILD_CELASTRUS),
                    Map.of(TreePatchDefinition.FARMING_GUILD_CELASTRUS,tree(TreeFamily.CELASTRUS,values[i],now))).build();
            assertEquals(expected[i],result.getEntries().get(0).getGroup()); assertFalse(result.getNext().isPresent());
        }
    }
    @Test public void builtSnapshotsDoNotChangeWhenBuilderIsReused()
    {
        ReadinessDashboard.Builder builder=herbs(8,now,FarmingGrowthRate.NORMAL); ReadinessDashboard first=builder.build();
        builder.fields(account,Set.of(FieldPatchDefinition.FALADOR_NORTH),Map.of()); assertEquals(1,first.getEntries().size()); assertEquals(2,builder.build().getEntries().size());
    }
    @Test public void runTypesKeepCoLocatedWorkAndUnknownEvidenceSeparate()
    {
        ReadinessDashboard result=herbs(8,now,FarmingGrowthRate.NORMAL)
                .trees(account,Set.of(TreePatchDefinition.FALADOR_TREE),Map.of(TreePatchDefinition.FALADOR_TREE,tree(TreeFamily.TREE,12,now)))
                .gardens(account,Set.of(GardenPatchDefinition.RIMMINGTON_BUSH),Map.of(GardenPatchDefinition.RIMMINGTON_BUSH,garden(GardenFamily.BUSH,70,now)))
                .fields(account,Set.of(FieldPatchDefinition.FALADOR_NORTH,FieldPatchDefinition.FALADOR_SOUTH),Map.of(FieldPatchDefinition.FALADOR_NORTH,field(FieldFamily.ALLOTMENT,10,now))).build();
        assertEquals(1,result.getCount(CombinedRunKind.HERB,ReadinessDashboard.Group.HARVESTABLE));
        assertEquals(1,result.getCount(CombinedRunKind.TREE,ReadinessDashboard.Group.CHECK_HEALTH));
        assertEquals(1,result.getCount(CombinedRunKind.GARDEN,ReadinessDashboard.Group.DISEASED));
        assertEquals(1,result.getCount(CombinedRunKind.FIELD,ReadinessDashboard.Group.HARVESTABLE));
        assertEquals(1,result.getCount(CombinedRunKind.FIELD,ReadinessDashboard.Group.UNKNOWN));
        for (ReadinessDashboard.Group group:ReadinessDashboard.Group.values())
        {
            long total=0;
            for (CombinedRunKind kind:CombinedRunKind.values()) { total+=result.getCount(kind,group); }
            assertEquals(result.getCount(group),total);
        }
        for (CombinedRunKind kind:CombinedRunKind.values())
        { for (ReadinessDashboard.Entry entry:result.getEntries(kind)) { assertEquals(kind,entry.getRunKind()); } }
        try { result.getEntries(CombinedRunKind.FIELD).clear(); fail("Mutable run view"); } catch(UnsupportedOperationException expected) { }
    }
    @Test public void perRunFutureWindowsRespectInclusiveHorizonsAndExcludeAlreadyDueEstimates()
    {
        ReadinessDashboard result=herbs(4,now.minusSeconds(3000),FarmingGrowthRate.NORMAL)
                .fields(account,Set.of(FieldPatchDefinition.LUMBRIDGE_HOPS,FieldPatchDefinition.FALADOR_NORTH),Map.of(
                        FieldPatchDefinition.LUMBRIDGE_HOPS,field(FieldFamily.HOPS,4,now.minusSeconds(1500)),
                        FieldPatchDefinition.FALADOR_NORTH,field(FieldFamily.ALLOTMENT,6,now.minusSeconds(2400)))).build();
        assertEquals(0,result.getUpcomingCount(CombinedRunKind.HERB,Duration.ofSeconds(599)));
        assertEquals(1,result.getUpcomingCount(CombinedRunKind.HERB,Duration.ofSeconds(600)));
        assertEquals(1,result.getUpcomingCount(CombinedRunKind.FIELD,Duration.ofSeconds(300)));
        assertEquals(1,result.getCount(CombinedRunKind.FIELD,ReadinessDashboard.Group.POSSIBLY_READY));
        assertEquals(0,result.getUpcomingCount(CombinedRunKind.FIELD,Duration.ZERO));
        assertEquals(0,result.getUpcomingCount(CombinedRunKind.FIELD,Duration.ofSeconds(-1)));
        assertEquals("HERB:FALADOR",result.getNext(CombinedRunKind.HERB).get().getIdentity());
        assertEquals("FIELD:LUMBRIDGE_HOPS",result.getNext(CombinedRunKind.FIELD).get().getIdentity());
        assertTrue(result.getEntries(CombinedRunKind.TREE).isEmpty()); assertFalse(result.getNext(CombinedRunKind.TREE).isPresent());
    }
}
