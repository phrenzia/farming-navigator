package com.farmingnavigator.routing.run;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.*;
import com.farmingnavigator.routing.capability.*;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpecialFieldTravelTest
{
    private final Instant now=Instant.parse("2026-10-16T12:00:00Z");
    private RequirementContext context(SpellbookType book,Map<Integer,Long> inventory,Map<Integer,Long> bank,boolean rings,boolean quests)
    {
        Map<Quest,QuestState> completed=quests?Map.of(Quest.BONE_VOYAGE,QuestState.FINISHED,Quest.PRIEST_IN_PERIL,QuestState.FINISHED,Quest.DESERT_TREASURE_I,QuestState.FINISHED):Map.of();
        AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,completed,Map.of(),Map.of(),false,rings,false);
        TravelCapabilitySnapshot travel=new TravelCapabilitySnapshot(true,book,null,inventory.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue().intValue())),Map.of(),Map.of());
        return new RequirementContext(account,new FarmingNavigatorConfig(){},travel,new StorageSnapshot(inventory,Map.of(),bank,true,now));
    }
    private Map<FieldPatchDefinition,List<VerifiedTransportResult>> routes(RequirementContext context)
    {
        return new TransportVerificationService().evaluateAll(context.getAccountState(),context.getConfig(),context.getTravelCapabilitySnapshot(),context.getStorageSnapshot(),new FieldPatchTransportService().getAllOptions());
    }
    private FieldRunPlan plan(RequirementContext context,FieldRunCrop crop)
    {
        return new FieldRunPlanner().plan(context.getAccountState(),EnumSet.allOf(FieldPatchDefinition.class),crop,FieldRunMode.HARVEST_ALL,true,Map.of(),context.getStorageSnapshot(),now)
                .withTransports(routes(context),HerbTransportPreference.CATALOGUE,context);
    }
    @Test public void bothSeaweedPlotsShareOneBankedPendantChargeAndDoNotAssumeBoatShortcut()
    {
        RequirementContext context=context(SpellbookType.STANDARD,Map.of(),Map.of(ItemID.NECKLACE_OF_DIGSITE_1,1L),false,true);
        FieldRunPlan plan=plan(context,FieldRunCrop.SEAWEED);
        assertEquals(2,plan.getStops().size()); assertEquals(TransportVerificationKind.DIGSITE_BARGE,plan.getStops().get(0).getRecommended().getOption().getVerificationKind());
        assertTrue(plan.getStops().get(1).isLocalWalk()); assertNull(plan.getStops().get(1).getRecommended());
        assertEquals(1,plan.getTransportResources().getChecks().size()); assertTrue(plan.getTransportResources().getChecks().get(0).requiresBankWithdrawal());
        assertTrue(plan.getTransportResources().getBankItems().contains(ItemID.NECKLACE_OF_DIGSITE_1));
        assertTrue(plan.getStops().get(0).getRecommended().getOption().getInstructions().contains("walk to the northern rowboat"));
        assertTrue(plan.getStops().get(0).getRecommended().getOption().getInstructions().contains("unlocked shortcut yourself"));
        assertTrue(plan(context(SpellbookType.STANDARD,Map.of(ItemID.NECKLACE_OF_DIGSITE_1,1L),Map.of(),false,false),FieldRunCrop.SEAWEED).getStops().isEmpty());
    }
    @Test public void draynorManorSpellUsesArceuusAndTheSharedRuneBudget()
    {
        Map<Integer,Long> runes=Map.of(ItemID.EARTHRUNE,1L,ItemID.WATERRUNE,1L,ItemID.LAWRUNE,1L);
        FieldRunPlan ready=plan(context(SpellbookType.ARCEUUS,runes,Map.of(),false,true),FieldRunCrop.BELLADONNA);
        assertEquals(TeleportSpellDefinition.DRAYNOR_MANOR_TELEPORT,ready.getStops().get(0).getRecommended().getOption().getTeleportSpellDefinition());
        assertTrue(ready.getTransportResources().getChecks().stream().allMatch(c -> c.isReadyNow()));
        FieldRunPlan wrongBook=plan(context(SpellbookType.STANDARD,runes,Map.of(),false,true),FieldRunCrop.BELLADONNA);
        assertNull(wrongBook.getStops().get(0).getRecommended());
        FieldRunPlan banked=plan(context(SpellbookType.ARCEUUS,Map.of(),runes,false,true),FieldRunCrop.BELLADONNA);
        assertTrue(banked.getTransportResources().getChecks().stream().anyMatch(c -> c.requiresBankWithdrawal()));
    }
    @Test public void mushroomUsesVerifiedCksOrKharyrllDependingOnActualAccess()
    {
        FieldRunPlan ring=plan(context(SpellbookType.STANDARD,Map.of(ItemID.DRAMEN_STAFF,1L),Map.of(),true,true),FieldRunCrop.MUSHROOM);
        assertEquals(TransportVerificationKind.FAIRY_RING_ACCESS,ring.getStops().get(0).getRecommended().getOption().getVerificationKind());
        Map<Integer,Long> runes=Map.of(ItemID.BLOODRUNE,1L,ItemID.LAWRUNE,2L);
        FieldRunPlan ancient=plan(context(SpellbookType.ANCIENT,runes,Map.of(),false,true),FieldRunCrop.MUSHROOM);
        assertEquals(TeleportSpellDefinition.KHARYRLL_TELEPORT,ancient.getStops().get(0).getRecommended().getOption().getTeleportSpellDefinition());
        assertTrue(ancient.getTransportResources().getChecks().stream().allMatch(c -> c.isReadyNow()));
        assertNull(plan(context(SpellbookType.STANDARD,runes,Map.of(),false,true),FieldRunCrop.MUSHROOM).getStops().get(0).getRecommended());
    }
}
