package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.field.*;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/** Family base, regional channel and farming-area anchor must all agree. */
public final class FieldSceneTargetPolicy
{
    private static final Set<Integer> ALLOTMENT = Set.of(
            ObjectID.FARMING_VEG_PATCH_1, ObjectID.FARMING_VEG_PATCH_2, ObjectID.FARMING_VEG_PATCH_3,
            ObjectID.FARMING_VEG_PATCH_4, ObjectID.FARMING_VEG_PATCH_5, ObjectID.FARMING_VEG_PATCH_6,
            ObjectID.FARMING_VEG_PATCH_7, ObjectID.FARMING_VEG_PATCH_8, ObjectID.FARMING_VEG_PATCH_9,
            ObjectID.FARMING_VEG_PATCH_10, ObjectID.FARMING_VEG_PATCH_11, ObjectID.FARMING_VEG_PATCH_12,
            ObjectID.FARMING_VEG_PATCH_13, ObjectID.FARMING_VEG_PATCH_14, ObjectID.FARMING_VEG_PATCH_15,
            ObjectID.FARMING_VEG_PATCH_16, ObjectID.FARMING_VEG_PATCH_17);
    private static final Set<Integer> FLOWER = Set.of(ObjectID.FARMING_FLOWER_PATCH_1, ObjectID.FARMING_FLOWER_PATCH_2,
            ObjectID.FARMING_FLOWER_PATCH_3, ObjectID.FARMING_FLOWER_PATCH_4, ObjectID.FARMING_FLOWER_PATCH_5,
            ObjectID.FARMING_FLOWER_PATCH_6, ObjectID.FARMING_FLOWER_PATCH_7, ObjectID.FARMING_FLOWER_PATCH_8,
            ObjectID.FARMING_FLOWER_PATCH_9);
    private static final Set<Integer> HOPS = Set.of(ObjectID.FARMING_HOPS_PATCH_1, ObjectID.FARMING_HOPS_PATCH_2,
            ObjectID.FARMING_HOPS_PATCH_3, ObjectID.FARMING_HOPS_PATCH_4, ObjectID.FARMING_HOPS_PATCH_5);
    private FieldSceneTargetPolicy() { }
    public static FieldPatchDefinition fieldPatch(int baseId, int varbit, WorldPoint location)
    {
        FieldPatchRegion region = FieldPatchRegion.forLocation(location);
        if (region == null) { return null; }
        for (FieldPatchDefinition patch : region.getPatches())
        {
            if (patch.getVarbit() != varbit || !patch.isNear(location)) { continue; }
            if (patch.getFamily() == FieldFamily.ALLOTMENT && ALLOTMENT.contains(baseId)
                    || patch.getFamily() == FieldFamily.FLOWER && FLOWER.contains(baseId)
                    || patch.getFamily() == FieldFamily.HOPS && HOPS.contains(baseId)
                    || patch.getFamily() == FieldFamily.SEAWEED && (baseId == ObjectID.FARMING_SEAWEED_PATCH_1 || baseId == ObjectID.FARMING_SEAWEED_PATCH_2)
                    || patch.getFamily() == FieldFamily.MUSHROOM && baseId == ObjectID.FARMING_MUSHROOM_PATCH
                    || patch.getFamily() == FieldFamily.BELLADONNA && (baseId == ObjectID.FARMING_BELLADONNA_PATCH || baseId == ObjectID.FARMING_BELLADONNA_PATCH_2)) { return patch; }
        }
        return null;
    }
    public static boolean isKnownFarmer(int id)
    {
        return id == NpcID.KRAGEN || id == NpcID.DANTAERA || id == NpcID.FORTIS_GARDENER || id == NpcID.ELSTAN
                || id == NpcID.HOSIDIUS_ALLOTMENT_GARDENER || id == NpcID.LYRA
                || id == NpcID.FARMING_GARDENER_FARMGUILD_T1 || id == NpcID.PRIF_GARDENER
                || id == NpcID.FARMING_GARDENER_HOPS_1 || id == NpcID.FARMING_GARDENER_HOPS_3
                || id == NpcID.FOSSIL_GARDENER_UNDERWATER || id == NpcID.FARMING_GARDENER_HOPS_4 || id == NpcID.FARMING_GARDENER_HOPS_5 || id == NpcID.FRANCIS;
    }
    public static boolean isFarmerFor(FieldPatchDefinition patch, int id, WorldPoint location)
    {
        if (patch == null || patch.getFamily() == FieldFamily.FLOWER || patch.getFamily() == FieldFamily.GRAPES
                || patch.getFamily() == FieldFamily.CORAL || patch.getFamily() == FieldFamily.ANIMA) { return false; }
        FieldPatchRegion region = FieldPatchRegion.forLocation(location);
        if (region == null || !region.getPatches().contains(patch) || patch.getLocation().distanceTo2D(location) > 25) { return false; }
        switch (region)
        {
            case SEAWEED: return id == NpcID.FOSSIL_GARDENER_UNDERWATER;
            case ARDOUGNE: return id == NpcID.KRAGEN;
            case CATHERBY: return id == NpcID.DANTAERA;
            case ORTUS_FARM: return id == NpcID.FORTIS_GARDENER;
            case FALADOR: return id == NpcID.ELSTAN;
            case HOSIDIUS: return id == NpcID.HOSIDIUS_ALLOTMENT_GARDENER;
            case MORYTANIA: return id == NpcID.LYRA;
            case FARMING_GUILD: return id == NpcID.FARMING_GARDENER_FARMGUILD_T1;
            case PRIFDDINAS: return id == NpcID.PRIF_GARDENER;
            case YANILLE: return id == NpcID.FARMING_GARDENER_HOPS_1;
            case LUMBRIDGE: return id == NpcID.FARMING_GARDENER_HOPS_3;
            case SEERS_VILLAGE: return id == NpcID.FARMING_GARDENER_HOPS_4;
            case ENTRANA: return id == NpcID.FRANCIS;
            case ALDARIN: return id == NpcID.FARMING_GARDENER_HOPS_5;
            default: return false;
        }
    }
}
