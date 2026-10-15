package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.tree.*;
import com.farmingnavigator.guided.tree.TreePatchDestination;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/** Region, transmit channel, farming-area anchor and object family must agree. */
public final class TreeSceneTargetPolicy
{
    private static final Set<Integer> REGULAR = Set.of(ObjectID.FARMING_TREE_PATCH_1, ObjectID.FARMING_TREE_PATCH_2,
            ObjectID.FARMING_TREE_PATCH_3, ObjectID.FARMING_TREE_PATCH_4, ObjectID.FARMING_TREE_PATCH_5);
    private static final Set<Integer> FRUIT = Set.of(ObjectID.FARMING_FRUIT_TREE_PATCH_1, ObjectID.FARMING_FRUIT_TREE_PATCH_2,
            ObjectID.FARMING_FRUIT_TREE_PATCH_3, ObjectID.FARMING_FRUIT_TREE_PATCH_4, ObjectID.FARMING_FRUIT_TREE_PATCH_5);
    private static final Set<Integer> HARDWOOD = Set.of(ObjectID.FARMING_HARDWOOD_TREE_PATCH_1,
            ObjectID.FARMING_HARDWOOD_TREE_PATCH_2, ObjectID.FARMING_HARDWOOD_TREE_PATCH_3);
    private static final Set<Integer> CALQUAT = Set.of(ObjectID.FARMING_CALQUAT_TREE_PATCH,
            ObjectID.FARMING_CALQUAT_TREE_PATCH_2, ObjectID.FARMING_CALQUAT_TREE_PATCH_3);
    private static final Set<Integer> CELASTRUS = Set.of(ObjectID.FARMING_CELASTRUS_PATCH_1);
    private static final Set<Integer> REDWOOD = Set.of(ObjectID.FARMING_REDWOOD_TREE_PATCH_0_1,
            ObjectID.FARMING_REDWOOD_TREE_PATCH_0_2, ObjectID.FARMING_REDWOOD_TREE_PATCH_0_3,
            ObjectID.FARMING_REDWOOD_TREE_PATCH_0_4, ObjectID.FARMING_REDWOOD_TREE_PATCH_0_5,
            ObjectID.FARMING_REDWOOD_TREE_PATCH_0_6, ObjectID.FARMING_REDWOOD_TREE_PATCH_0_7,
            ObjectID.FARMING_REDWOOD_TREE_PATCH_0_8, ObjectID.FARMING_REDWOOD_TREE_PATCH_0_9);
    private static final Set<Integer> CRYSTAL = Set.of(ObjectID.FARMING_CRYSTAL_TREE_PATCH_1);
    private static final Set<Integer> CANOPIES = Set.of(ObjectID.FARMING_TREE_PATCH_1_CANOPY,
            ObjectID.FARMING_TREE_PATCH_2_CANOPY, ObjectID.FARMING_TREE_PATCH_3_CANOPY, ObjectID.FARMING_TREE_PATCH_4_CANOPY,
            ObjectID.FARMING_TREE_PATCH_5_CANOPY, ObjectID.FARMING_HARDWOOD_TREE_PATCH_1_CANOPY,
            ObjectID.FARMING_HARDWOOD_TREE_PATCH_2_CANOPY, ObjectID.FARMING_HARDWOOD_TREE_PATCH_3_CANOPY);
    private static final Set<String> REGULAR_NAMES = Set.of("tree patch", "tree stump", "dead tree", "diseased tree",
            "oak", "oak tree", "willow tree", "maple tree", "yew tree", "magic tree");
    private static final Set<String> FRUIT_NAMES = Set.of("fruit tree patch", "fruit tree stump", "dead fruit tree", "diseased fruit tree",
            "apple tree", "banana tree", "orange tree", "curry tree", "pineapple plant", "papaya tree", "palm tree", "dragonfruit tree");
    private static final Set<String> HARDWOOD_NAMES = Set.of("hardwood tree patch", "hardwood tree stump", "dead hardwood tree",
            "diseased hardwood tree", "teak", "teak tree", "mahogany", "mahogany tree", "camphor", "camphor tree",
            "ironwood", "ironwood tree", "rosewood", "rosewood tree");
    private TreeSceneTargetPolicy() { }
    public static TreePatchDefinition treePatch(int baseId, int varbit, String transformedName, WorldPoint location)
    {
        TreePatchRegion region = TreePatchRegion.forLocation(location);
        if (region == null || CANOPIES.contains(baseId)) { return null; }
        String name = transformedName == null ? "" : transformedName.toLowerCase(Locale.ROOT);
        for (TreePatchDefinition patch : region.getPatches())
        {
            if (varbit != patch.getVarbit() || !TreePatchDestination.isNear(patch, location)) { continue; }
            if (matchesFamily(patch.getFamily(), baseId, name)) { return patch; }
        }
        return null;
    }
    private static boolean matchesFamily(TreeFamily family, int baseId, String name)
    {
        switch (family)
        {
            case TREE: return REGULAR.contains(baseId) || REGULAR_NAMES.contains(name);
            case FRUIT_TREE: return FRUIT.contains(baseId) || FRUIT_NAMES.contains(name);
            case HARDWOOD_TREE: return HARDWOOD.contains(baseId) || HARDWOOD_NAMES.contains(name);
            // Exact special-tree bases only; no name fallback for upper redwood sections or decorative objects.
            case CALQUAT: return CALQUAT.contains(baseId);
            case CELASTRUS: return CELASTRUS.contains(baseId);
            case REDWOOD: return REDWOOD.contains(baseId);
            case CRYSTAL_TREE: return CRYSTAL.contains(baseId);
            default: return false;
        }
    }
    public static boolean isKnownFarmer(int id)
    {
        for (TreePatchDefinition patch : TreePatchDefinition.values()) { if (farmerIdMatches(patch, id)) { return true; } }
        return false;
    }
    public static boolean isFarmerFor(TreePatchDefinition patch, int id, WorldPoint location)
    {
        if (location == null || location.getPlane() != 0 || !farmerIdMatches(patch, id)) { return false; }
        TreePatchRegion region = TreePatchRegion.forLocation(location);
        return region != null && region.getPatches().contains(patch)
                && TreePatchDestination.valueOf(patch.name()).getLocation().distanceTo2D(location) <= 25;
    }
    private static boolean farmerIdMatches(TreePatchDefinition patch, int id)
    {
        switch (patch)
        {
            case VARROCK_TREE: return id == NpcID.FARMING_GARDENER_TREE_3 || id == NpcID.FARMING_GARDENER_TREE_3_02;
            case TAVERLEY_TREE: return id == NpcID.FARMING_GARDENER_TREE_1;
            case FALADOR_TREE: return id == NpcID.FARMING_GARDENER_TREE_2;
            case LUMBRIDGE_TREE: return id == NpcID.FARMING_GARDENER_TREE_4;
            case GNOME_STRONGHOLD_TREE: return id == NpcID.FARMING_GARDENER_TREE_GNOME;
            case FARMING_GUILD_TREE: return id == NpcID.FARMING_GARDENER_FARMGUILD_T2;
            case BRIMHAVEN_FRUIT: return id == NpcID.GARTH;
            case CATHERBY_FRUIT: return id == NpcID.FARMING_GARDENER_FRUIT_4;
            case GNOME_STRONGHOLD_FRUIT: return id == NpcID.FARMING_GARDENER_FRUIT_1;
            case TREE_GNOME_VILLAGE_FRUIT: return id == NpcID.FARMING_GARDENER_FRUIT_2;
            case LLETYA_FRUIT: return id == NpcID.FARMING_GARDENER_FRUIT_TREE_5;
            case FARMING_GUILD_FRUIT: return id == NpcID.FARMING_GARDENER_FARMGUILD_T3;
            case AVIUM_SAVANNAH_HARDWOOD: return id == NpcID.FROG_QUEST_MARCELLUS_FARMER;
            case TAI_BWO_WANNAI_CALQUAT: return id == NpcID.FARMING_GARDENER_CALQUAT;
            case KASTORI_CALQUAT: return id == NpcID.FARMING_GARDENER_CALQUAT_2;
            case GREAT_CONCH_CALQUAT: return id == NpcID.FARMING_GARDENER_CALQUAT_3;
            case FARMING_GUILD_CELASTRUS: return id == NpcID.FARMING_GARDENER_FARMGUILD_CELASTRUS;
            case FARMING_GUILD_REDWOOD: return id == NpcID.FARMING_GARDENER_FARMGUILD_REDWOOD;
            default:
                // The three Fossil squirrel IDs are known, but their individual patch assignment is not yet verified.
                return false;
        }
    }
}
