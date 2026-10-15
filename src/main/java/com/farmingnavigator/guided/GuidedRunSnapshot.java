package com.farmingnavigator.guided;

import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.HerbRunStop;
import com.farmingnavigator.routing.run.HerbRunCompost;
import java.time.Duration;

public final class GuidedRunSnapshot
{
    private final GuidedRunStep step;
    private final HerbRunCrop crop;
    private final HerbRunStop stop;
    private final int total;
    private final int completed;
    private final int skipped;
    private final int manuallyCompleted;
    private final HerbRunCompost compost;
    private final Duration elapsed;

    GuidedRunSnapshot(GuidedRunStep step, HerbRunCrop crop, HerbRunStop stop, int total,
            int completed, int skipped, int manuallyCompleted, HerbRunCompost compost)
    {
        this(step, crop, stop, total, completed, skipped, manuallyCompleted, compost, Duration.ZERO);
    }

    GuidedRunSnapshot(GuidedRunStep step, HerbRunCrop crop, HerbRunStop stop, int total,
            int completed, int skipped, int manuallyCompleted, HerbRunCompost compost, Duration elapsed)
    {
        this.step = step;
        this.crop = crop;
        this.stop = stop;
        this.total = total;
        this.completed = completed;
        this.skipped = skipped;
        this.manuallyCompleted = manuallyCompleted;
        this.compost = compost;
        this.elapsed = elapsed;
    }

    public GuidedRunStep getStep() { return step; }
    public HerbRunCrop getCrop() { return crop; }
    public HerbRunStop getStop() { return stop; }
    public int getTotal() { return total; }
    public int getCompleted() { return completed; }
    public int getSkipped() { return skipped; }
    public int getManuallyCompleted() { return manuallyCompleted; }
    public HerbRunCompost getCompost() { return compost; }
    public Duration getElapsed() { return elapsed; }
    public boolean isActive() { return step != GuidedRunStep.INACTIVE && step != GuidedRunStep.COMPLETE; }

    public String getInstruction()
    {
        switch (step)
        {
            case PREPARE: return "Review the route and supply list, then confirm you are ready to travel.";
            case TRAVEL: return "Travel to " + stop.getPatch().getDisplayName() + " and approach the farming area.";
            case INSPECT: return "Inspect the herb patch. Fresh patch data is needed before choosing an action.";
            case RAKE: return "Rake the weeds from the herb patch.";
            case HARVEST: return "Harvest the herbs until the patch is empty.";
            case NOTE_PRODUCE: return "Inventory full. Use harvested herbs on a tool leprechaun to note them, or free a slot.";
            case CLEAR: return "Clear the dead herbs from the patch.";
            case COMPOST: return "Apply your chosen compost, then confirm. Compost application is not automatically verified.";
            case PLANT: return "Plant a " + crop.getDisplayName() + " seed in the empty herb patch.";
            case CURE: return "Cure the diseased herbs, or skip this stop if you cannot cure them.";
            case WAIT: return "The patch is still growing. Wait for it to become harvestable or skip this stop.";
            case COMPLETE: return "Run complete: " + completed + " stops completed, " + skipped + " skipped. "
                    + manuallyCompleted + " completions were confirmed manually.";
            default: return "Start a suggested herb run for step-by-step guidance.";
        }
    }
}
