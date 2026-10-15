package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.guided.HerbPatchDestination;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HerbSceneTargetPolicyTest
{
    private final WorldPoint falador = HerbPatchDestination.FALADOR.getLocation();

    @Test
    public void knownHerbBaseAndVerifiedTransmitMetadataIdentifyPatch()
    {
        assertEquals(HerbPatchDefinition.FALADOR, HerbSceneTargetPolicy.herbPatch(
                ObjectID.FARMING_HERB_PATCH_1, -1, null, falador));
        assertEquals(HerbPatchDefinition.FALADOR, HerbSceneTargetPolicy.herbPatch(
                ObjectID.HERB_PATCH_WEEDS_3, VarbitID.FARMING_TRANSMIT_D, "Herb patch", falador));
        assertEquals(HerbPatchDefinition.WEISS, HerbSceneTargetPolicy.herbPatch(
                ObjectID.HERB_PATCH_WEEDS_3, VarbitID.FARMING_TRANSMIT_A, "Herbs", HerbPatchDestination.WEISS.getLocation()));
    }

    @Test
    public void wrongTransmitChannelFlowerAndQuestFakePatchAreExcluded()
    {
        assertNull(HerbSceneTargetPolicy.herbPatch(ObjectID.HERB_PATCH_WEEDS_3,
                VarbitID.FARMING_TRANSMIT_A, "Herb patch", falador));
        assertNull(HerbSceneTargetPolicy.herbPatch(ObjectID.HERB_PATCH_WEEDS_3,
                VarbitID.FARMING_TRANSMIT_D, "Flower patch", falador));
        assertNull(HerbSceneTargetPolicy.herbPatch(ObjectID.MYARM_FAKEFARMINGPATCH,
                VarbitID.MYARM, "Herb patch", HerbPatchDestination.TROLL_STRONGHOLD.getLocation()));
    }

    @Test
    public void wrongAreaAndUpperFloorsAreExcluded()
    {
        assertNull(HerbSceneTargetPolicy.herbPatch(ObjectID.FARMING_HERB_PATCH_1, -1, null,
                new WorldPoint(falador.getX(), falador.getY(), 1)));
        assertNull(HerbSceneTargetPolicy.herbPatch(ObjectID.FARMING_HERB_PATCH_1, -1, null,
                new WorldPoint(falador.getX() + 20, falador.getY(), 0)));
    }

    @Test
    public void toolLeprechaunsAreRecognizedWithoutHighlightingForestryLeprechauns()
    {
        assertTrue(HerbSceneTargetPolicy.isToolLeprechaun(NpcID.FARMING_TOOLS_LEPRECHAUN));
        assertTrue(HerbSceneTargetPolicy.isToolLeprechaun(NpcID.MYARM_LEPRECHAUN));
        assertTrue(HerbSceneTargetPolicy.isToolLeprechaun(NpcID.MY2ARM_LEPRECHAUN));
        assertTrue(HerbSceneTargetPolicy.isToolLeprechaun(NpcID.FARMING_TOOLS_LEPRECHAUN_VARLAMORE));
        assertFalse(HerbSceneTargetPolicy.isToolLeprechaun(NpcID.GATHERING_EVENT_WOODCUTTING_LEPRECHAUN));
        assertFalse(HerbSceneTargetPolicy.isToolLeprechaun(-1));
    }
}
