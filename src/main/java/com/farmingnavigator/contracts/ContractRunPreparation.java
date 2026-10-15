package com.farmingnavigator.contracts;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchRegion;
import com.farmingnavigator.routing.run.TreeRunCrop;
import com.farmingnavigator.routing.run.GardenRunCrop;
import com.farmingnavigator.routing.run.GardenRunMode;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.farming.garden.GardenPatchRegion;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Explicit sidebar setup only. Recheck the live assignment before applying any configuration changes. */
public final class ContractRunPreparation
{
    private ContractRunPreparation() { }
    public static Map<String, Object> settings(FarmingContractSnapshot current, ContractCrop requested, boolean runActive)
    {
        if (runActive || current == null || current.getState() != FarmingContractSnapshot.State.ACTIVE
                || requested == null || current.getCrop() != requested) { return Map.of(); }
        if (requested.getHerbRunCrop() != null)
        {
            return Map.of("herbRunCrop", requested.getHerbRunCrop(), "herbRunPatches", EnumSet.of(HerbPatchDefinition.FARMING_GUILD));
        }
        TreeRunCrop tree = requested.getTreeRunCrop();
        if (tree != null)
        {
            for (TreePatchDefinition patch : TreePatchRegion.FARMING_GUILD.getPatches())
            {
                if (patch.getFamily() == tree.getCrop().getFamily())
                {
                    return Map.of("treeRunCrop", tree, "treeRunPatches", EnumSet.of(patch));
                }
            }
        }
        GardenRunCrop garden = requested.getGardenRunCrop();
        if (garden != null)
        {
            for (GardenPatchDefinition patch : GardenPatchRegion.FARMING_GUILD.getPatches())
            {
                if (patch.getFamily() == garden.getCrop().getFamily())
                {
                    return Map.of("gardenRunCrop", garden, "gardenRunPatches", EnumSet.of(patch), "gardenRunMode", GardenRunMode.REPLACE_MATURE);
                }
            }
        }
        return Map.of();
    }
    public static boolean isGuildSelected(TreeRunCrop crop, Set<TreePatchDefinition> selected)
    {
        return TreePatchRegion.FARMING_GUILD.getPatches().stream().anyMatch(patch -> selected.contains(patch)
                && patch.getFamily() == crop.getCrop().getFamily());
    }
    public static boolean isGuildSelected(GardenRunCrop crop, Set<GardenPatchDefinition> selected)
    {
        return GardenPatchRegion.FARMING_GUILD.getPatches().stream().anyMatch(patch -> selected.contains(patch)
                && patch.getFamily() == crop.getCrop().getFamily());
    }
}
