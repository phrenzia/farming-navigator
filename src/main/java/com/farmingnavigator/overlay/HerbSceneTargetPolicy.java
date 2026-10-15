package com.farmingnavigator.overlay;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.state.HerbPatchRegion;
import com.farmingnavigator.guided.HerbPatchDestination;
import java.util.Locale;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/** Combines known herb object IDs with region-specific transmit metadata. */
public final class HerbSceneTargetPolicy
{
    private static final Set<Integer> HERB_BASE_IDS = Set.of(ObjectID.FARMING_HERB_PATCH_1,
            ObjectID.FARMING_HERB_PATCH_2, ObjectID.FARMING_HERB_PATCH_3, ObjectID.FARMING_HERB_PATCH_4,
            ObjectID.FARMING_HERB_PATCH_5, ObjectID.FARMING_HERB_PATCH_6, ObjectID.MYARM_HERBPATCH);
    private static final Set<String> HERB_NAMES = Set.of("herb patch", "herbs", "diseased herbs", "dead herbs",
            "goutweed", "diseased goutweed", "dead goutweed");

    private HerbSceneTargetPolicy()
    {
    }

    public static HerbPatchDefinition herbPatch(int baseId, int varbit, String transformedName, WorldPoint location)
    {
        HerbPatchRegion region = HerbPatchRegion.forLocation(location);
        if (region == null || !HerbPatchDestination.isNear(region.getPatch(), location))
        {
            return null;
        }
        boolean transmittedHerb = varbit == region.getVarbit() && transformedName != null
                && HERB_NAMES.contains(transformedName.toLowerCase(Locale.ROOT));
        return HERB_BASE_IDS.contains(baseId) || transmittedHerb ? region.getPatch() : null;
    }

    public static boolean isToolLeprechaun(int npcId)
    {
        return npcId == NpcID.FARMING_TOOLS_LEPRECHAUN || npcId == NpcID.MYARM_LEPRECHAUN
                || npcId == NpcID.MY2ARM_LEPRECHAUN || npcId == NpcID.FARMING_TOOLS_LEPRECHAUN_VARLAMORE
                || npcId == NpcID.FARMING_TOOLS_LEPRECHAUN_DRAYNOR || npcId == NpcID.FOSSIL_LEPRECHAUN_UNDERWATER;
    }
}
