package com.farmingnavigator.account;

public enum DiaryTier
{
    NONE("None", 0),
    EASY("Easy", 1),
    MEDIUM("Medium", 2),
    HARD("Hard", 3),
    ELITE("Elite", 4);

    private final String displayName;

    private final int rank;

    DiaryTier(
            String displayName,
            int rank
    )
    {
        this.displayName = displayName;
        this.rank = rank;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public int getRank()
    {
        return rank;
    }

    public boolean isAtLeast(
            DiaryTier requiredTier
    )
    {
        return requiredTier != null
                && rank >= requiredTier.rank;
    }
}