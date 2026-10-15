package com.farmingnavigator.farming.tree;

public final class TreePatchState
{
    private final int rawValue;
    private final TreeCrop crop;
    private final TreePatchCondition condition;
    private final int stage;

    TreePatchState(int rawValue, TreeCrop crop, TreePatchCondition condition, int stage)
    {
        this.rawValue = rawValue;
        this.crop = crop;
        this.condition = condition;
        this.stage = stage;
    }

    public int getRawValue() { return rawValue; }
    public TreeCrop getCrop() { return crop; }
    public TreePatchCondition getCondition() { return condition; }
    public int getStage() { return stage; }
    public boolean isKnown() { return condition != TreePatchCondition.UNKNOWN; }
}
