package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.tree.TreePatchDefinition;
import net.runelite.api.Quest;

/** Empty reason means verified patch entry requirements pass, not that a teleport is available. */
public final class TreePatchAccess
{
    public String missingRequirement(TreePatchDefinition patch, AccountState account)
    {
        if (account == null || !account.isLoggedIn()) { return "Log in to check patch access."; }
        switch (patch)
        {
            case FARMING_GUILD_TREE:
                return account.getBoostedFarmingLevel() >= 65 ? "" : "65 Farming required for the intermediate guild area (boostable).";
            case FARMING_GUILD_FRUIT:
                return account.getBoostedFarmingLevel() >= 85 ? "" : "85 Farming required for the advanced guild area (boostable).";
            case LLETYA_FRUIT:
                return account.hasStartedQuest(Quest.MOURNINGS_END_PART_I) ? "" : "Start Mourning's End Part I for Lletya access.";
            case FOSSIL_ISLAND_EAST:
            case FOSSIL_ISLAND_MIDDLE:
            case FOSSIL_ISLAND_WEST:
                return account.hasCompletedQuest(Quest.BONE_VOYAGE) ? "" : "Complete Bone Voyage for Fossil Island access.";
            case AVIUM_SAVANNAH_HARDWOOD:
                return account.hasCompletedQuest(Quest.THE_RIBBITING_TALE_OF_A_LILY_PAD_LABOUR_DISPUTE)
                        ? "" : "Complete The Ribbiting Tale of a Lily Pad Labour Dispute for this patch.";
            case AUBURNVALE_TREE:
            case KASTORI_FRUIT:
            case ANGLERS_RETREAT_HARDWOOD:
                return "Data required: this patch's entry requirements have not been verified.";
            default: return "";
        }
    }
}
