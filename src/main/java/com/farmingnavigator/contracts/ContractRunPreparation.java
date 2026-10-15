package com.farmingnavigator.contracts;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.farming.tree.TreePatchRegion;
import com.farmingnavigator.routing.run.TreeRunCrop;
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
        return Map.of();
    }
    public static boolean isGuildSelected(TreeRunCrop crop, Set<TreePatchDefinition> selected)
    {
        return TreePatchRegion.FARMING_GUILD.getPatches().stream().anyMatch(patch -> selected.contains(patch)
                && patch.getFamily() == crop.getCrop().getFamily());
    }
}
