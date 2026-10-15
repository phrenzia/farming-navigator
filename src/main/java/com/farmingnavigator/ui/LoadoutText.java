package com.farmingnavigator.ui;

import com.farmingnavigator.loadout.SupplyCheck;

public final class LoadoutText
{
    private LoadoutText()
    {
    }

    public static String describe(SupplyCheck check)
    {
        String prefix = check.getRequirement().getQuantity() + " x " + check.getRequirement().getName()
                + (check.getRequirement().isOptional() ? " (optional)" : "") + ": ";
        switch (check.getState())
        {
            case ALREADY_CARRIED: return prefix + "already carried";
            case ALREADY_EQUIPPED: return prefix + "already carried/equipped";
            case AVAILABLE_FROM_BANK: return prefix + "withdraw " + check.getWithdrawQuantity() + " from bank";
            case MISSING:
                return prefix + (check.getWithdrawQuantity() > 0 ? "withdraw " + check.getWithdrawQuantity() + "; " : "")
                        + check.getMissingQuantity() + " missing from carried items and verified bank";
            default: return prefix + "DATA REQUIRED - open a bank to check the shortfall";
        }
    }
}
