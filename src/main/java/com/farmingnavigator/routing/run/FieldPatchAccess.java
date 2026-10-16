package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchRegion;
import net.runelite.api.Quest;

public final class FieldPatchAccess
{
    public String missingRequirement(FieldPatchDefinition patch, AccountState account)
    {
        if (account == null || !account.isLoggedIn()) { return "Log in to check patch access."; }
        FieldPatchRegion region = FieldPatchRegion.forLocation(patch.getLocation());
        switch (region)
        {
            case FARMING_GUILD:
                return account.getBoostedFarmingLevel() >= 45 ? "" : "45 Farming required for the beginner guild area (boostable).";
            case PRIFDDINAS:
                return account.hasCompletedQuest(Quest.SONG_OF_THE_ELVES) ? "" : "Complete Song of the Elves for Prifddinas access.";
            case MORYTANIA_MUSHROOM:
            case MORYTANIA:
                return account.hasCompletedQuest(Quest.PRIEST_IN_PERIL) ? "" : "Complete Priest in Peril for Morytania access.";
            case ORTUS_FARM:
                return account.hasCompletedQuest(Quest.CHILDREN_OF_THE_SUN) ? "" : "Complete Children of the Sun for Varlamore access.";
            case HARMONY_ISLAND:
                return account.hasCompletedQuest(Quest.THE_GREAT_BRAIN_ROBBERY) ? ""
                        : "Data required: partial Great Brain Robbery access has not been verified; completed quest access is supported.";
            case SEAWEED:
                return account.hasCompletedQuest(Quest.BONE_VOYAGE) ? ""
                        : "Complete Bone Voyage for Fossil Island underwater access. Diving equipment and air supply must be checked manually.";
            case AUBURNVALE:
                return "Data required: Auburnvale entry requirements have not been verified.";
            case ENTRANA:
                return "Data required: Entrana equipment and item restrictions are not checked for this plan.";
            case ALDARIN:
                return "Data required: Aldarin entry requirements have not been verified.";
            case KASTORI:
                return "Data required: Kastori entry requirements have not been verified.";
            default: return "";
        }
    }
}
