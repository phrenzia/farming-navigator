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
    private FieldSceneTargetPolicy() { }
    public static FieldPatchDefinition fieldPatch(int baseId, int varbit, WorldPoint location)
    {
        FieldPatchRegion region = FieldPatchRegion.forLocation(location);
        if (region == null) { return null; }
        for (FieldPatchDefinition patch : region.getPatches())
        {
            if (patch.getVarbit() != varbit || !patch.isNear(location)) { continue; }
            if (patch.getFamily() == FieldFamily.ALLOTMENT && ALLOTMENT.contains(baseId)
                    || patch.getFamily() == FieldFamily.FLOWER && FLOWER.contains(baseId)) { return patch; }
        }
        return null;
    }
    public static boolean isKnownFarmer(int id)
    {
        return id == NpcID.KRAGEN || id == NpcID.DANTAERA || id == NpcID.FORTIS_GARDENER || id == NpcID.ELSTAN
                || id == NpcID.HOSIDIUS_ALLOTMENT_GARDENER || id == NpcID.LYRA
                || id == NpcID.FARMING_GARDENER_FARMGUILD_T1 || id == NpcID.PRIF_GARDENER;
    }
    public static boolean isFarmerFor(FieldPatchDefinition patch, int id, WorldPoint location)
    {
        if (patch == null || patch.getFamily() != FieldFamily.ALLOTMENT) { return false; }
        FieldPatchRegion region = FieldPatchRegion.forLocation(location);
        if (region == null || !region.getPatches().contains(patch) || patch.getLocation().distanceTo2D(location) > 25) { return false; }
        switch (region)
        {
            case ARDOUGNE: return id == NpcID.KRAGEN;
            case CATHERBY: return id == NpcID.DANTAERA;
            case ORTUS_FARM: return id == NpcID.FORTIS_GARDENER;
            case FALADOR: return id == NpcID.ELSTAN;
            case HOSIDIUS: return id == NpcID.HOSIDIUS_ALLOTMENT_GARDENER;
            case MORYTANIA: return id == NpcID.LYRA;
            case FARMING_GUILD: return id == NpcID.FARMING_GARDENER_FARMGUILD_T1;
            case PRIFDDINAS: return id == NpcID.PRIF_GARDENER;
            default: return false;
        }
    }
}
