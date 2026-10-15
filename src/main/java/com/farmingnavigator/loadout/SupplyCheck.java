package com.farmingnavigator.loadout;

public final class SupplyCheck
{
    private final SupplyRequirement requirement;
    private final SupplyState state;
    private final long withdrawQuantity;
    private final long missingQuantity;

    SupplyCheck(SupplyRequirement requirement, SupplyState state, long withdrawQuantity, long missingQuantity)
    {
        this.requirement = requirement;
        this.state = state;
        this.withdrawQuantity = withdrawQuantity;
        this.missingQuantity = missingQuantity;
    }

    public SupplyRequirement getRequirement() { return requirement; }
    public SupplyState getState() { return state; }
    public long getWithdrawQuantity() { return withdrawQuantity; }
    /** Known shortfall, or zero when availability cannot be established. */
    public long getMissingQuantity() { return missingQuantity; }
}
