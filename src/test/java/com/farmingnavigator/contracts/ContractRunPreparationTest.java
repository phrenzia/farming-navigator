package com.farmingnavigator.contracts;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.TreeRunCrop;
import java.util.EnumSet;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContractRunPreparationTest
{
    @Test public void herbSetupSelectsOnlyItsGuildPatchAndLeavesOtherPreferencesAlone()
    {
        Map<String, Object> settings = ContractRunPreparation.settings(FarmingContractSnapshot.decode(true, 24, 0), ContractCrop.RANARR, false);
        assertEquals(Map.of("herbRunCrop", HerbRunCrop.RANARR, "herbRunPatches", EnumSet.of(HerbPatchDefinition.FARMING_GUILD)), settings);
    }
    @Test public void eachSupportedTreeFamilySelectsItsOwnGuildPatch()
    {
        Map<ContractCrop, TreePatchDefinition> cases = Map.of(ContractCrop.MAGIC, TreePatchDefinition.FARMING_GUILD_TREE,
                ContractCrop.DRAGONFRUIT, TreePatchDefinition.FARMING_GUILD_FRUIT,
                ContractCrop.CELASTRUS, TreePatchDefinition.FARMING_GUILD_CELASTRUS,
                ContractCrop.REDWOOD, TreePatchDefinition.FARMING_GUILD_REDWOOD);
        cases.forEach((crop, patch) -> {
            Map<String, Object> settings = ContractRunPreparation.settings(FarmingContractSnapshot.decode(true, crop.getId(), 0), crop, false);
            assertEquals(Map.of("treeRunCrop", crop.getTreeRunCrop(), "treeRunPatches", EnumSet.of(patch)), settings);
            assertTrue(ContractRunPreparation.isGuildSelected(crop.getTreeRunCrop(), EnumSet.of(patch)));
            assertFalse(ContractRunPreparation.isGuildSelected(crop.getTreeRunCrop(), EnumSet.of(TreePatchDefinition.VARROCK_TREE)));
        });
        assertFalse(ContractRunPreparation.isGuildSelected(TreeRunCrop.REDWOOD, EnumSet.of(TreePatchDefinition.FARMING_GUILD_FRUIT)));
    }
    @Test public void staleCompleteLoggedOutUnknownAndUnsupportedRequestsNeverProduceSettings()
    {
        for (FarmingContractSnapshot snapshot : new FarmingContractSnapshot[]{null, FarmingContractSnapshot.decode(false, 24, 0),
                FarmingContractSnapshot.decode(true, 0, 0), FarmingContractSnapshot.decode(true, 24, 1),
                FarmingContractSnapshot.decode(true, 999, 0), FarmingContractSnapshot.decode(true, 25, 0)})
        {
            assertTrue(ContractRunPreparation.settings(snapshot, ContractCrop.RANARR, false).isEmpty());
        }
        assertTrue(ContractRunPreparation.settings(FarmingContractSnapshot.decode(true, 1, 0), ContractCrop.REDBERRIES, false).isEmpty());
        assertTrue(ContractRunPreparation.settings(FarmingContractSnapshot.decode(true, 24, 0), null, false).isEmpty());
    }
    @Test public void activeChecklistCannotBeReconfiguredEvenWithMatchingAssignment()
    {
        assertTrue(ContractRunPreparation.settings(FarmingContractSnapshot.decode(true, 24, 0), ContractCrop.RANARR, true).isEmpty());
    }
}
