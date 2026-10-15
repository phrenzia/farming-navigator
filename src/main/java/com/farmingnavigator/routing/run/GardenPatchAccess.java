package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;

public final class GardenPatchAccess
{
    public String missingRequirement(GardenPatchDefinition patch, AccountState account)
    {
        if (account == null || !account.isLoggedIn()) { return "Log in to check patch access."; }
        switch (patch)
        {
            case FARMING_GUILD_BUSH:
            case FARMING_GUILD_CACTUS:
                return account.getBoostedFarmingLevel() >= 45 ? "" : "45 Farming required for the beginner guild area (boostable).";
            case ETCETERIA_BUSH:
                return "Data required: Etceteria entry requirements have not been verified.";
            default: return "";
        }
    }
}
