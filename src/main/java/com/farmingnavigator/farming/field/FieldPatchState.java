package com.farmingnavigator.farming.field;

public final class FieldPatchState
{
    private final int rawValue;
    private final FieldCrop crop;
    private final FieldPatchCondition condition;
    private final int stage;
    FieldPatchState(int raw, FieldCrop crop, FieldPatchCondition condition, int stage)
    {
        this.rawValue = raw; this.crop = crop; this.condition = condition; this.stage = stage;
    }
    public int getRawValue() { return rawValue; }
    public FieldCrop getCrop() { return crop; }
    public FieldPatchCondition getCondition() { return condition; }
    public int getStage() { return stage; }
    public boolean isKnown() { return condition != FieldPatchCondition.UNKNOWN; }
}
