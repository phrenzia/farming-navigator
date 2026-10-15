package com.farmingnavigator;

import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FarmingNavigatorStateRefreshTest
{
    @Test public void treeAlertAndDisplaySettingsDoNotCancelTreeRun()
    {
        assertTrue(FarmingNavigatorPlugin.isTreeRunSetupKey("treeRunCrop"));
        assertTrue(FarmingNavigatorPlugin.isTreeRunSetupKey("treeRunPatches"));
        assertTrue(FarmingNavigatorPlugin.isTreeRunSetupKey("treeRunProtection"));
        assertTrue(FarmingNavigatorPlugin.isTreeRunSetupKey("treeTransportPreference"));
        assertFalse(FarmingNavigatorPlugin.isTreeRunSetupKey("treeObservedAlerts"));
        assertFalse(FarmingNavigatorPlugin.isTreeRunSetupKey("treeEstimatedAlerts"));
        assertFalse(FarmingNavigatorPlugin.isTreeRunSetupKey("showTreeObservations"));
        assertFalse(FarmingNavigatorPlugin.isTreeRunSetupKey(null));
    }
    @Test
    public void onlyRunSetupChangesInvalidateCurrentChecklist()
    {
        assertTrue(FarmingNavigatorPlugin.isRunSetupKey("herbRunCrop"));
        assertTrue(FarmingNavigatorPlugin.isRunSetupKey("herbRunPatches"));
        assertTrue(FarmingNavigatorPlugin.isRunSetupKey("herbRunCompost"));
        assertTrue(FarmingNavigatorPlugin.isRunSetupKey("herbTransportPreference"));
        assertTrue(FarmingNavigatorPlugin.isRunSetupKey("herbHouseEntryMode"));
        assertTrue(FarmingNavigatorPlugin.isRunSetupKey("herbRunCure"));
        assertTrue(FarmingNavigatorPlugin.isRunSetupKey("herbAshTomeRead"));
        assertFalse(FarmingNavigatorPlugin.isRunSetupKey("guidedRunOverlays"));
        assertFalse(FarmingNavigatorPlugin.isRunSetupKey("pohHouseLocation"));
        assertFalse(FarmingNavigatorPlugin.isRunSetupKey(null));
    }

    @Test
    public void transportStateChangesTriggerRefresh()
    {
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.SEERS_SHERLOCK_TELEPORT));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.LUMBRIDGE_CABBAGE_TELEPORT));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.LUMBRIDGE_MEDIUM_REWARD));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.VARLAMORE_VISITED));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.FROG_QUEST));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.FROG_QUEST_PATCH_UNLOCKED));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.FAIRY2_QUEENCURE_QUEST));
    }
    @Test public void contractAssignmentsAndCompletionTriggerRefresh()
    {
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.FARMGUILD_CONTRACT_TYPE));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.FARMGUILD_CONTRACT_COMPLETE));
        assertFalse(FarmingNavigatorPlugin.isTreeRunSetupKey("showFarmingContract"));
    }

    @Test
    public void spellbookAndRunePouchChangesStillTriggerRefresh()
    {
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.SPELLBOOK));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.RUNE_POUCH_QUANTITY_1));
        assertTrue(FarmingNavigatorPlugin.isRelevantStateVarbit(VarbitID.RUNE_POUCH_TYPE_6));
    }

    @Test
    public void unrelatedAndAbsentVarbitsDoNotTriggerRefresh()
    {
        assertFalse(FarmingNavigatorPlugin.isRelevantStateVarbit(-1));
        assertFalse(FarmingNavigatorPlugin.isRelevantStateVarbit(Integer.MAX_VALUE));
    }
}
