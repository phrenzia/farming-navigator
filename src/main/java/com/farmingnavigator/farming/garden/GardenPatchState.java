package com.farmingnavigator.farming.garden;

public final class GardenPatchState
{
    private final int rawValue;
    private final GardenCrop crop;
    private final GardenPatchCondition condition;
    private final int stage;
    GardenPatchState(int raw, GardenCrop crop, GardenPatchCondition condition, int stage)
    {
        this.rawValue = raw; this.crop = crop; this.condition = condition; this.stage = stage;
    }
    public int getRawValue() { return rawValue; }
    public GardenCrop getCrop() { return crop; }
    public GardenPatchCondition getCondition() { return condition; }
    public int getStage() { return stage; }
    public boolean isKnown() { return condition != GardenPatchCondition.UNKNOWN; }
}
