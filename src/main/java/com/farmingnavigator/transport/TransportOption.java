package com.farmingnavigator.transport;

public class TransportOption
{
    private final int baselineRank;
    private final String displayName;
    private final String instructions;
    private final TransportCategory category;
    private final TransportVerificationKind verificationKind;

    public TransportOption(
            int baselineRank,
            String displayName,
            String instructions,
            TransportCategory category,
            TransportVerificationKind verificationKind
    )
    {
        this.baselineRank = baselineRank;
        this.displayName = displayName;
        this.instructions = instructions;
        this.category = category;
        this.verificationKind = verificationKind;
    }

    public int getBaselineRank()
    {
        return baselineRank;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getInstructions()
    {
        return instructions;
    }

    public TransportCategory getCategory()
    {
        return category;
    }

    public TransportVerificationKind getVerificationKind()
    {
        return verificationKind;
    }
}