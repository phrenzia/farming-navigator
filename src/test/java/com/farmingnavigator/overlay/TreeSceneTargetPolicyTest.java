package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.guided.tree.TreePatchDestination;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeSceneTargetPolicyTest
{
    @Test public void everyPatchNeedsMatchingRegionAnchorFamilyAndTransmit()
    {
        for (TreePatchDefinition patch : TreePatchDefinition.values())
        {
            int id = patch.getFamily() == TreeFamily.TREE ? ObjectID.FARMING_TREE_PATCH_1
                    : patch.getFamily() == TreeFamily.FRUIT_TREE ? ObjectID.FARMING_FRUIT_TREE_PATCH_1 : ObjectID.FARMING_HARDWOOD_TREE_PATCH_1;
            WorldPoint location = TreePatchDestination.valueOf(patch.name()).getLocation();
            assertEquals(patch.name(), patch, TreeSceneTargetPolicy.treePatch(id, patch.getVarbit(), null, location));
            assertNull(TreeSceneTargetPolicy.treePatch(id, -1, null, location));
            assertNull(TreeSceneTargetPolicy.treePatch(id, patch.getVarbit(), null, new WorldPoint(location.getX(), location.getY(), 1)));
        }
    }
    @Test public void adjacentFossilPatchesAreSeparatedByTransmitChannel()
    {
        WorldPoint area = TreePatchDestination.FOSSIL_ISLAND_EAST.getLocation();
        assertEquals(TreePatchDefinition.FOSSIL_ISLAND_EAST, TreeSceneTargetPolicy.treePatch(
                ObjectID.FARMING_HARDWOOD_TREE_PATCH_1, VarbitID.FARMING_TRANSMIT_A, "Teak", area));
        assertEquals(TreePatchDefinition.FOSSIL_ISLAND_MIDDLE, TreeSceneTargetPolicy.treePatch(
                ObjectID.FARMING_HARDWOOD_TREE_PATCH_2, VarbitID.FARMING_TRANSMIT_B, "Mahogany", area));
        assertEquals(TreePatchDefinition.FOSSIL_ISLAND_WEST, TreeSceneTargetPolicy.treePatch(
                ObjectID.FARMING_HARDWOOD_TREE_PATCH_3, VarbitID.FARMING_TRANSMIT_C, "Camphor", area));
    }
    @Test public void matchingTransmitDoesNotTurnHerbsOrOtherTreeFamiliesIntoTrees()
    {
        WorldPoint varrock = TreePatchDestination.VARROCK_TREE.getLocation();
        assertNull(TreeSceneTargetPolicy.treePatch(ObjectID.FARMING_HERB_PATCH_1, VarbitID.FARMING_TRANSMIT_A, "Herb patch", varrock));
        assertNull(TreeSceneTargetPolicy.treePatch(ObjectID.FARMING_FRUIT_TREE_PATCH_1, VarbitID.FARMING_TRANSMIT_A, "Apple tree", varrock));
        assertNull(TreeSceneTargetPolicy.treePatch(ObjectID.FARMING_TREE_PATCH_1, VarbitID.FARMING_TRANSMIT_A, "Oak",
                new WorldPoint(varrock.getX() + 13, varrock.getY(), 0)));
    }
    @Test public void canopiesAreExcludedAndExactTransformedNamesCanSupportAdditionalBases()
    {
        WorldPoint varrock = TreePatchDestination.VARROCK_TREE.getLocation();
        assertNull(TreeSceneTargetPolicy.treePatch(ObjectID.FARMING_TREE_PATCH_1_CANOPY, VarbitID.FARMING_TRANSMIT_A, "Oak", varrock));
        assertEquals(TreePatchDefinition.VARROCK_TREE, TreeSceneTargetPolicy.treePatch(ObjectID.TREE_PATCH_WEEDED,
                VarbitID.FARMING_TRANSMIT_A, "Tree patch", varrock));
        assertNull(TreeSceneTargetPolicy.treePatch(ObjectID.TREE_PATCH_WEEDED, VarbitID.FARMING_TRANSMIT_A, "Decorative tree", varrock));
    }
    @Test public void farmerIdentityAndLocationBothMatter()
    {
        WorldPoint varrock = TreePatchDestination.VARROCK_TREE.getLocation();
        assertTrue(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.VARROCK_TREE, NpcID.FARMING_GARDENER_TREE_3, varrock));
        assertFalse(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.VARROCK_TREE, NpcID.FARMING_GARDENER_TREE_4, varrock));
        assertFalse(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.VARROCK_TREE, NpcID.FARMING_GARDENER_TREE_3,
                new WorldPoint(varrock.getX() + 26, varrock.getY(), 0)));
        assertFalse(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.VARROCK_TREE, NpcID.FARMING_GARDENER_TREE_3,
                new WorldPoint(varrock.getX(), varrock.getY(), 1)));
        assertFalse(TreeSceneTargetPolicy.isKnownFarmer(NpcID.FARMING_TOOLS_LEPRECHAUN));
    }
    @Test public void guildTreeAndFruitFarmersAreNotInterchangeable()
    {
        assertTrue(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.FARMING_GUILD_TREE,
                NpcID.FARMING_GARDENER_FARMGUILD_T2, TreePatchDestination.FARMING_GUILD_TREE.getLocation()));
        assertTrue(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.FARMING_GUILD_FRUIT,
                NpcID.FARMING_GARDENER_FARMGUILD_T3, TreePatchDestination.FARMING_GUILD_FRUIT.getLocation()));
        assertFalse(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.FARMING_GUILD_TREE,
                NpcID.FARMING_GARDENER_FARMGUILD_T3, TreePatchDestination.FARMING_GUILD_TREE.getLocation()));
    }
    @Test public void unverifiedFossilSquirrelAssignmentsStaySuppressed()
    {
        assertFalse(TreeSceneTargetPolicy.isKnownFarmer(NpcID.FOSSIL_SQUIRREL_GARDENER1));
        assertFalse(TreeSceneTargetPolicy.isFarmerFor(TreePatchDefinition.FOSSIL_ISLAND_EAST,
                NpcID.FOSSIL_SQUIRREL_GARDENER1, TreePatchDestination.FOSSIL_ISLAND_EAST.getLocation()));
    }
}
