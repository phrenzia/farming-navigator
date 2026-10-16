package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.field.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FieldSceneTargetPolicyTest
{
    @Test public void allGuidedPlotsRequireMatchingFamilyChannelAndArea()
    {
        for (FieldPatchDefinition patch : FieldPatchDefinition.values())
        {
            if (patch.getFamily() == FieldFamily.GRAPES)
            {
                assertNull(FieldSceneTargetPolicy.fieldPatch(ObjectID.FARMING_BELLADONNA_PATCH,patch.getVarbit(),patch.getLocation()));
                continue; // Vineyard guidance/object bases are not implemented.
            }
            int base = patch.getFamily() == FieldFamily.ALLOTMENT ? ObjectID.FARMING_VEG_PATCH_1 : patch.getFamily() == FieldFamily.FLOWER ? ObjectID.FARMING_FLOWER_PATCH_1 : patch.getFamily() == FieldFamily.HOPS ? ObjectID.FARMING_HOPS_PATCH_1
                    : patch.getFamily() == FieldFamily.SEAWEED ? ObjectID.FARMING_SEAWEED_PATCH_1
                    : patch.getFamily() == FieldFamily.MUSHROOM ? ObjectID.FARMING_MUSHROOM_PATCH : ObjectID.FARMING_BELLADONNA_PATCH;
            assertEquals(patch, FieldSceneTargetPolicy.fieldPatch(base, patch.getVarbit(), patch.getLocation()));
            assertNull(FieldSceneTargetPolicy.fieldPatch(base, VarbitID.FARMING_TRANSMIT_N, patch.getLocation()));
            assertNull(FieldSceneTargetPolicy.fieldPatch(ObjectID.VEG_PATCH_WEEDED, patch.getVarbit(), patch.getLocation()));
            assertNull(FieldSceneTargetPolicy.fieldPatch(ObjectID.FLOWER_PATCH_WEEDED, patch.getVarbit(), patch.getLocation()));
            WorldPoint point = patch.getLocation();
            assertNull(FieldSceneTargetPolicy.fieldPatch(base, patch.getVarbit(), new WorldPoint(point.getX(), point.getY(), 1)));
            assertNull(FieldSceneTargetPolicy.fieldPatch(base, patch.getVarbit(), new WorldPoint(0,0,0)));
        }
    }
    @Test public void guildNorthSouthAndFlowerCannotCrossMatch()
    {
        WorldPoint north = FieldPatchDefinition.FARMING_GUILD_NORTH.getLocation();
        assertEquals(FieldPatchDefinition.FARMING_GUILD_NORTH, FieldSceneTargetPolicy.fieldPatch(ObjectID.FARMING_VEG_PATCH_12, VarbitID.FARMING_TRANSMIT_C, north));
        assertEquals(FieldPatchDefinition.FARMING_GUILD_SOUTH, FieldSceneTargetPolicy.fieldPatch(ObjectID.FARMING_VEG_PATCH_13, VarbitID.FARMING_TRANSMIT_D, north));
        assertNull(FieldSceneTargetPolicy.fieldPatch(ObjectID.FARMING_FLOWER_PATCH_6, VarbitID.FARMING_TRANSMIT_C, north));
        assertNull(FieldSceneTargetPolicy.fieldPatch(ObjectID.FARMING_VEG_PATCH_12, VarbitID.FARMING_TRANSMIT_H, north));
        assertEquals(FieldPatchDefinition.FARMING_GUILD_FLOWER, FieldSceneTargetPolicy.fieldPatch(ObjectID.FARMING_FLOWER_PATCH_6,
                VarbitID.FARMING_TRANSMIT_H, FieldPatchDefinition.FARMING_GUILD_FLOWER.getLocation()));
    }
    @Test public void regionalFarmersServeBothAllotmentsButNeverFlowerPlots()
    {
        FieldPatchRegion[] regions = {FieldPatchRegion.ARDOUGNE, FieldPatchRegion.CATHERBY, FieldPatchRegion.ORTUS_FARM, FieldPatchRegion.FALADOR,
                FieldPatchRegion.HOSIDIUS, FieldPatchRegion.MORYTANIA, FieldPatchRegion.FARMING_GUILD, FieldPatchRegion.PRIFDDINAS};
        int[] farmers = {NpcID.KRAGEN, NpcID.DANTAERA, NpcID.FORTIS_GARDENER, NpcID.ELSTAN, NpcID.HOSIDIUS_ALLOTMENT_GARDENER,
                NpcID.LYRA, NpcID.FARMING_GARDENER_FARMGUILD_T1, NpcID.PRIF_GARDENER};
        for (int i=0; i<regions.length; i++)
        {
            assertTrue(FieldSceneTargetPolicy.isKnownFarmer(farmers[i]));
            for (FieldPatchDefinition patch : regions[i].getPatches())
            {
                assertEquals(patch.getFamily() == FieldFamily.ALLOTMENT, FieldSceneTargetPolicy.isFarmerFor(patch, farmers[i], patch.getLocation()));
                assertFalse(FieldSceneTargetPolicy.isFarmerFor(patch, farmers[(i+1)%farmers.length], patch.getLocation()));
            }
        }
    }
    @Test public void missingFarmersWrongGuildTiersAndDistantNpcsAreRejected()
    {
        FieldPatchDefinition guild = FieldPatchDefinition.FARMING_GUILD_NORTH;
        assertFalse(FieldSceneTargetPolicy.isFarmerFor(guild, NpcID.FARMING_GARDENER_FARMGUILD_T2, guild.getLocation()));
        assertFalse(FieldSceneTargetPolicy.isFarmerFor(guild, NpcID.FARMING_GARDENER_FARMGUILD_T3, guild.getLocation()));
        FieldPatchDefinition harmony = FieldPatchDefinition.HARMONY_ISLAND_ALLOTMENT;
        assertFalse(FieldSceneTargetPolicy.isFarmerFor(harmony, NpcID.ELSTAN, harmony.getLocation()));
        assertFalse(FieldSceneTargetPolicy.isFarmerFor(null, NpcID.ELSTAN, null));
        assertFalse(FieldSceneTargetPolicy.isKnownFarmer(-1));
        WorldPoint point = FieldPatchDefinition.FALADOR_NORTH.getLocation();
        assertFalse(FieldSceneTargetPolicy.isFarmerFor(FieldPatchDefinition.FALADOR_NORTH, NpcID.ELSTAN, new WorldPoint(point.getX()+26, point.getY(), 0)));
        assertFalse(FieldSceneTargetPolicy.isFarmerFor(FieldPatchDefinition.FALADOR_NORTH, NpcID.ELSTAN, new WorldPoint(point.getX(), point.getY(), 1)));
    }
    @Test public void fiveHopsGardenersAreBoundToTheirActualRegion()
    {
        FieldPatchDefinition[] patches={FieldPatchDefinition.YANILLE_HOPS,FieldPatchDefinition.LUMBRIDGE_HOPS,FieldPatchDefinition.SEERS_VILLAGE_HOPS,FieldPatchDefinition.ENTRANA_HOPS,FieldPatchDefinition.ALDARIN_HOPS};
        int[] ids={NpcID.FARMING_GARDENER_HOPS_1,NpcID.FARMING_GARDENER_HOPS_3,NpcID.FARMING_GARDENER_HOPS_4,NpcID.FRANCIS,NpcID.FARMING_GARDENER_HOPS_5};
        for(int i=0;i<patches.length;i++)
        {
            assertTrue(FieldSceneTargetPolicy.isKnownFarmer(ids[i]));
            assertTrue(FieldSceneTargetPolicy.isFarmerFor(patches[i],ids[i],patches[i].getLocation()));
            assertFalse(FieldSceneTargetPolicy.isFarmerFor(patches[i],ids[(i+1)%ids.length],patches[i].getLocation()));
        }
        assertNull(FieldSceneTargetPolicy.fieldPatch(ObjectID.FARMING_HOPS_PATCH_KELDAGRIM,VarbitID.FARMING_TRANSMIT_A,patches[0].getLocation()));
    }

}
