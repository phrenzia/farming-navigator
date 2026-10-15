package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.garden.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class GardenSceneTargetPolicyTest
{
    @Test public void everyPatchNeedsMatchingFamilyRegionChannelAndAnchor()
    {
        for (GardenPatchDefinition patch : GardenPatchDefinition.values())
        {
            int base = patch.getFamily() == GardenFamily.BUSH ? ObjectID.FARMING_BUSH_PATCH_1 : ObjectID.FARMING_CACTUS_PATCH;
            WorldPoint anchor = patch.getLocation();
            assertEquals(patch, GardenSceneTargetPolicy.gardenPatch(base, patch.getVarbit(), anchor));
            assertNull(GardenSceneTargetPolicy.gardenPatch(base, -1, anchor));
            assertNull(GardenSceneTargetPolicy.gardenPatch(base, patch.getVarbit(), new WorldPoint(anchor.getX(), anchor.getY(), 1)));
            assertNull(GardenSceneTargetPolicy.gardenPatch(base, patch.getVarbit(), new WorldPoint(anchor.getX() + 13, anchor.getY(), 0)));
        }
    }
    @Test public void nearbyGuildBushAndCactusChannelsCannotCrossMatch()
    {
        WorldPoint between = new WorldPoint(1260, 3738, 0);
        assertEquals(GardenPatchDefinition.FARMING_GUILD_BUSH, GardenSceneTargetPolicy.gardenPatch(ObjectID.FARMING_BUSH_PATCH_5, VarbitID.FARMING_TRANSMIT_B, between));
        assertEquals(GardenPatchDefinition.FARMING_GUILD_CACTUS, GardenSceneTargetPolicy.gardenPatch(ObjectID.FARMING_CACTUS_PATCH_2, VarbitID.FARMING_TRANSMIT_F, between));
        assertNull(GardenSceneTargetPolicy.gardenPatch(ObjectID.FARMING_BUSH_PATCH_5, VarbitID.FARMING_TRANSMIT_F, between));
        assertNull(GardenSceneTargetPolicy.gardenPatch(ObjectID.FARMING_CACTUS_PATCH_2, VarbitID.FARMING_TRANSMIT_B, between));
    }
    @Test public void reusedChannelsDoNotTurnHerbsTreesOrOtherFamiliesIntoGardenTargets()
    {
        WorldPoint anchor = GardenPatchDefinition.RIMMINGTON_BUSH.getLocation();
        assertNull(GardenSceneTargetPolicy.gardenPatch(ObjectID.FARMING_HERB_PATCH_1, VarbitID.FARMING_TRANSMIT_A, anchor));
        assertNull(GardenSceneTargetPolicy.gardenPatch(ObjectID.FARMING_TREE_PATCH_1, VarbitID.FARMING_TRANSMIT_A, anchor));
        assertNull(GardenSceneTargetPolicy.gardenPatch(ObjectID.FARMING_CACTUS_PATCH, VarbitID.FARMING_TRANSMIT_A, anchor));
        assertNull(GardenSceneTargetPolicy.gardenPatch(ObjectID.FARMING_BUSH_PATCH_1, VarbitID.FARMING_TRANSMIT_A, null));
    }
    @Test public void gardenerIdentityAndLocationMustBothMatchTheAssignedPatch()
    {
        Object[][] cases = {{GardenPatchDefinition.RIMMINGTON_BUSH, NpcID.FARMING_GARDENER_BUSH_2},
                {GardenPatchDefinition.ETCETERIA_BUSH, NpcID.FARMING_GARDENER_BUSH_3},
                {GardenPatchDefinition.CHAMPIONS_GUILD_BUSH, NpcID.FARMING_GARDENER_BUSH_1},
                {GardenPatchDefinition.ARDOUGNE_BUSH, NpcID.FARMING_GARDENER_BUSH_4},
                {GardenPatchDefinition.AL_KHARID_CACTUS, NpcID.FARMING_GARDENER_CACTUS},
                {GardenPatchDefinition.FARMING_GUILD_BUSH, NpcID.FARMING_GARDENER_FARMGUILD_T1},
                {GardenPatchDefinition.FARMING_GUILD_CACTUS, NpcID.FARMING_GARDENER_FARMGUILD_T1}};
        for (Object[] row : cases)
        {
            GardenPatchDefinition patch = (GardenPatchDefinition) row[0]; int id = (int) row[1]; WorldPoint anchor = patch.getLocation();
            assertTrue(GardenSceneTargetPolicy.isKnownFarmer(id)); assertTrue(GardenSceneTargetPolicy.isFarmerFor(patch, id, anchor));
            assertFalse(GardenSceneTargetPolicy.isFarmerFor(patch, NpcID.FARMING_GARDENER_FARMGUILD_T2, anchor));
            assertFalse(GardenSceneTargetPolicy.isFarmerFor(patch, id, new WorldPoint(anchor.getX() + 26, anchor.getY(), 0)));
            assertFalse(GardenSceneTargetPolicy.isFarmerFor(patch, id, new WorldPoint(anchor.getX(), anchor.getY(), 1)));
        }
        assertFalse(GardenSceneTargetPolicy.isKnownFarmer(NpcID.FARMING_TOOLS_LEPRECHAUN));
    }
}
