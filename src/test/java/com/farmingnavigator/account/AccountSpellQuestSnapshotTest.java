package com.farmingnavigator.account;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.routing.spell.TeleportSpellRequirement;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccountSpellQuestSnapshotTest
{
    @Test public void samplerCoversEveryCatalogueSpellQuestDependency()
    {
        Map<Quest,QuestState> sampled=AccountStateService.readQuestStates(quest -> QuestState.NOT_STARTED);
        for(TeleportSpellDefinition spell:TeleportSpellDefinition.values())
        {
            if(spell.getRequiredQuest()!=null)
            { assertTrue(spell.getDisplayName()+" quest is not sampled",sampled.containsKey(spell.getRequiredQuest())); }
        }
    }

    @Test public void sampledWatchtowerAndDesertTreasureCompletionUnlockTheirSpellsWithoutInventingCompletion()
    {
        for(TeleportSpellDefinition spell:new TeleportSpellDefinition[]{TeleportSpellDefinition.WATCHTOWER_TELEPORT,TeleportSpellDefinition.KHARYRLL_TELEPORT})
        {
            for(QuestState state:new QuestState[]{QuestState.NOT_STARTED,QuestState.IN_PROGRESS,QuestState.FINISHED})
            {
                Map<Quest,QuestState> sampled=AccountStateService.readQuestStates(quest -> quest==spell.getRequiredQuest()?state:QuestState.NOT_STARTED);
                AccountState account=new AccountState(true,99,99,99,99,99,99,99,99,sampled,Map.of(),Map.of());
                Map<Integer,Integer> runes=Map.of(ItemID.LAWRUNE,2,ItemID.EARTHRUNE,2,ItemID.BLOODRUNE,1);
                RequirementContext context=new RequirementContext(account,new FarmingNavigatorConfig(){},
                        new TravelCapabilitySnapshot(true,spell.getSpellbook(),null,runes,Map.of(),Map.of()),
                        new StorageSnapshot(Map.of(ItemID.LAWRUNE,2L,ItemID.EARTHRUNE,2L,ItemID.BLOODRUNE,1L),Map.of(),Map.of(),true,Instant.EPOCH));
                assertEquals(spell.getDisplayName()+" "+state,state==QuestState.FINISHED,new TeleportSpellRequirement(spell).evaluate(context).isReadyNow());
            }
        }
    }
}
