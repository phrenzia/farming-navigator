package com.farmingnavigator.farming.field;

public final class FieldPatchState
{
    private final int rawValue;
    private final FieldCrop crop;
    private final FieldPatchCondition condition;
    private final int stage;
    private final boolean watered;
    FieldPatchState(int raw, FieldCrop crop, FieldPatchCondition condition, int stage)
    {
        this(raw, crop, condition, stage, false);
    }
    FieldPatchState(int raw, FieldCrop crop, FieldPatchCondition condition, int stage, boolean watered)
    {
        this.watered = watered && condition == FieldPatchCondition.GROWING;
        this.rawValue = raw; this.crop = crop; this.condition = condition; this.stage = stage;
    }
    public int getRawValue() { return rawValue; }
    public FieldCrop getCrop() { return crop; }
    public FieldPatchCondition getCondition() { return condition; }
    public int getStage() { return stage; }
    /** Watered during the observed growing cycle only; not a promise for later cycles. */
    public boolean isWatered() { return watered; }
    public boolean isKnown() { return condition != FieldPatchCondition.UNKNOWN; }
}
