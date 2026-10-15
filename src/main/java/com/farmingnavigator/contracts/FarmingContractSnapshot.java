package com.farmingnavigator.contracts;

public final class FarmingContractSnapshot
{
    public enum State { LOGGED_OUT, NONE, ACTIVE, REPORTED_COMPLETE, UNKNOWN }
    private final State state;
    private final ContractCrop crop;
    private FarmingContractSnapshot(State state, ContractCrop crop) { this.state = state; this.crop = crop; }
    public State getState() { return state; }
    public ContractCrop getCrop() { return crop; }
    public static FarmingContractSnapshot decode(boolean loggedIn, int type, int complete)
    {
        if (!loggedIn) { return new FarmingContractSnapshot(State.LOGGED_OUT, null); }
        if (type < 0 || complete < 0) { return new FarmingContractSnapshot(State.UNKNOWN, null); }
        if (complete > 0) { return new FarmingContractSnapshot(State.REPORTED_COMPLETE, null); }
        if (type == 0) { return new FarmingContractSnapshot(State.NONE, null); }
        ContractCrop crop = ContractCrop.fromId(type);
        return new FarmingContractSnapshot(crop == null ? State.UNKNOWN : State.ACTIVE, crop);
    }
}
