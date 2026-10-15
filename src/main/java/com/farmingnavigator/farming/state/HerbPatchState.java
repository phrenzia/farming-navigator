package com.farmingnavigator.farming.state;

/** An immutable decoding of an observed herb-patch value, without timing predictions. */
public final class HerbPatchState
{
    private final int rawValue;
    private final PatchCondition condition;
    private final HerbCrop crop;
    private final int stage;

    HerbPatchState(int rawValue, PatchCondition condition, HerbCrop crop, int stage)
    {
        this.rawValue = rawValue;
        this.condition = condition;
        this.crop = crop;
        this.stage = stage;
    }

    public int getRawValue()
    {
        return rawValue;
    }

    public PatchCondition getCondition()
    {
        return condition;
    }

    public HerbCrop getCrop()
    {
        return crop;
    }

    /** RuneLite's stage within the decoded condition; -1 when unknown. */
    public int getStage()
    {
        return stage;
    }

    public boolean isKnown()
    {
        return condition != PatchCondition.UNKNOWN;
    }
}
