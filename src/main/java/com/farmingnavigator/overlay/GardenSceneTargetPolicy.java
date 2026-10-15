package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.garden.*;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/** Exact family bases, regional transmit and area anchor must agree. Farmer assignments follow FarmingWorld. */
public final class GardenSceneTargetPolicy
{
    private static final Set<Integer> BUSH = Set.of(ObjectID.FARMING_BUSH_PATCH_1, ObjectID.FARMING_BUSH_PATCH_2,
            ObjectID.FARMING_BUSH_PATCH_3, ObjectID.FARMING_BUSH_PATCH_4, ObjectID.FARMING_BUSH_PATCH_5);
    private static final Set<Integer> CACTUS = Set.of(ObjectID.FARMING_CACTUS_PATCH, ObjectID.FARMING_CACTUS_PATCH_2);
    private GardenSceneTargetPolicy() { }
    public static GardenPatchDefinition gardenPatch(int baseId, int varbit, WorldPoint location)
    {
        GardenPatchRegion region = GardenPatchRegion.forLocation(location);
        if (region == null) { return null; }
        for (GardenPatchDefinition patch : region.getPatches())
        {
            if (patch.getVarbit() != varbit || !patch.isNear(location)) { continue; }
            if (patch.getFamily() == GardenFamily.BUSH && BUSH.contains(baseId)
                    || patch.getFamily() == GardenFamily.CACTUS && CACTUS.contains(baseId)) { return patch; }
        }
        return null;
    }
    public static boolean isKnownFarmer(int id)
    {
        for (GardenPatchDefinition patch : GardenPatchDefinition.values()) { if (matchesFarmer(patch, id)) { return true; } }
        return false;
    }
    public static boolean isFarmerFor(GardenPatchDefinition patch, int id, WorldPoint location)
    {
        GardenPatchRegion region = GardenPatchRegion.forLocation(location);
        return region != null && region.getPatches().contains(patch) && matchesFarmer(patch, id)
                && patch.getLocation().distanceTo2D(location) <= 25;
    }
    private static boolean matchesFarmer(GardenPatchDefinition patch, int id)
    {
        switch (patch)
        {
            case RIMMINGTON_BUSH: return id == NpcID.FARMING_GARDENER_BUSH_2;
            case ETCETERIA_BUSH: return id == NpcID.FARMING_GARDENER_BUSH_3;
            case CHAMPIONS_GUILD_BUSH: return id == NpcID.FARMING_GARDENER_BUSH_1;
            case ARDOUGNE_BUSH: return id == NpcID.FARMING_GARDENER_BUSH_4;
            case AL_KHARID_CACTUS: return id == NpcID.FARMING_GARDENER_CACTUS;
            case FARMING_GUILD_BUSH:
            case FARMING_GUILD_CACTUS: return id == NpcID.FARMING_GARDENER_FARMGUILD_T1;
            default: return false;
        }
    }
}
