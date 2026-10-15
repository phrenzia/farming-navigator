package com.farmingnavigator.routing.requirement;

/** The source selected by the existing single-weapon rune planner, without parsing display text. */
public final class RunePreparation
{
    private final RequirementResult result;
    private final InfiniteRuneSource source;
    private final boolean bankedSource;
    private final boolean withdrawPouch;

    RunePreparation(RequirementResult result)
    {
        this(result, null, false, false);
    }

    RunePreparation(RequirementResult result, InfiniteRuneSource source, boolean bankedSource, boolean withdrawPouch)
    {
        this.result = result;
        this.source = source;
        this.bankedSource = bankedSource;
        this.withdrawPouch = withdrawPouch;
    }

    public RequirementResult getResult() { return result; }
    public InfiniteRuneSource getSource() { return source; }
    public boolean isBankedSource() { return bankedSource; }
    public boolean isWithdrawPouch() { return withdrawPouch; }
}
