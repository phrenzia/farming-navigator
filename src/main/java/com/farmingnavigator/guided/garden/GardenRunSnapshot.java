package com.farmingnavigator.guided.garden;

import com.farmingnavigator.routing.run.GardenRunCrop;
import com.farmingnavigator.routing.run.GardenRunCompost;
import com.farmingnavigator.routing.run.GardenRunMode;
import com.farmingnavigator.routing.run.GardenRunStop;
import java.time.Duration;

public final class GardenRunSnapshot
{
    private final GardenRunStep step;
    private final com.farmingnavigator.routing.run.HerbRunCure cure;
    private final GardenRunCrop crop;
    private final GardenRunMode mode;
    private final GardenRunCompost compost;
    private final GardenRunStop stop;
    private final int total, completed, manual, skipped, protectionConfirmed;
    private final Duration elapsed;
    GardenRunSnapshot(GardenRunStep step, GardenRunCrop crop, GardenRunMode mode, GardenRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed)
    {
        this(step, crop, mode, stop, total, completed, manual, skipped, protectionConfirmed, elapsed, GardenRunCompost.NONE);
    }
    GardenRunSnapshot(GardenRunStep step, GardenRunCrop crop, GardenRunMode mode, GardenRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed, GardenRunCompost compost)
    {
        this(step, crop, mode, stop, total, completed, manual, skipped, protectionConfirmed, elapsed, compost,
                com.farmingnavigator.routing.run.HerbRunCure.PLANT_CURE);
    }
    GardenRunSnapshot(GardenRunStep step, GardenRunCrop crop, GardenRunMode mode, GardenRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed,
            GardenRunCompost compost, com.farmingnavigator.routing.run.HerbRunCure cure)
    {
        this.cure = cure;
        this.compost = compost;
        this.step = step; this.crop = crop; this.mode = mode; this.stop = stop; this.total = total;
        this.completed = completed; this.manual = manual; this.skipped = skipped; this.protectionConfirmed = protectionConfirmed; this.elapsed = elapsed;
    }
    public com.farmingnavigator.routing.run.HerbRunCure getCure() { return cure; }
    public GardenRunStep getStep() { return step; }
    public GardenRunCrop getCrop() { return crop; }
    public GardenRunMode getMode() { return mode; }
    public GardenRunCompost getCompost() { return compost; }
    public GardenRunStop getStop() { return stop; }
    public int getTotal() { return total; }
    public int getCompleted() { return completed; }
    public int getManual() { return manual; }
    public int getSkipped() { return skipped; }
    public int getProtectionConfirmed() { return protectionConfirmed; }
    public Duration getElapsed() { return elapsed; }
    public boolean isActive() { return step != GardenRunStep.INACTIVE && step != GardenRunStep.COMPLETE; }
    public String getInstruction()
    {
        switch (step)
        {
            case PREPARE: return "Review seeds, tools, compost, protection and shared travel supplies, then confirm readiness.";
            case TRAVEL: return "Travel to " + stop.getPatch().getLabel() + " and approach the bush/cactus patch.";
            case INSPECT: return "Inspect the patch. A fresh nearby observation is required before choosing an action.";
            case CHECK_HEALTH: return "Check health. The checklist waits for a fresh health-checked state.";
            case HARVEST: return "Pick any available produce, or inspect and leave it if desired. Confirm when finished; produce quantity and harvest completion are not automatically verified.";
            case KEEP_OR_REPLACE: return stop.isReplant() ? "Keep the mature plant, or explicitly choose to clear it and plant the reserved seed."
                    : "No replacement was reserved for this plant. Keep it and prepare a new plan if replacement is needed.";
            case CLEAR: return "Clear the dead plant or explicitly selected replacement with a spade. Wait for a fresh empty-patch observation.";
            case RAKE: return "Rake until the patch is empty.";
            case COMPOST:
                if (compost.isSpell()) { return "Cast Fertile Soil on this patch, then confirm manually. Compost application is not observed."
                        + (compost == GardenRunCompost.FERTILE_SOIL_ULTRA ? " The ash-covered tome and two volcanic ash per cast are required."
                                : " Without ash the spell applies supercompost; carried ash may upgrade it if the tome is read."); }
                return "Apply " + compost + " to this patch, then confirm manually. Current compost state and application are not observed."
                    + (compost.isBottomless() ? " Check the bucket type and remaining uses yourself." : "");
            case PLANT: return "Use a seed dibber to plant one " + crop + " seed. Barbarian Training is not checked.";
            case PROTECT: return "Pay the gardener for protection, then confirm manually. The checklist does not verify the payment.";
            case PRUNE: return "Use secateurs to prune the diseased bush. Keep the living plant; wait for healthy growth of the same crop.";
            case CURE: return (cure == com.farmingnavigator.routing.run.HerbRunCure.CURE_PLANT ? "Cast Lunar Cure Plant on this diseased plant." : "Use plant cure on the diseased cactus.")
                    + " Keep the living plant; wait for healthy growth of the same crop.";
            case WAIT: return "The plant is growing. Wait or skip; time alone does not prove health-check readiness.";
            case COMPLETE: return "Bush/cactus run complete: " + completed + " completed, " + manual + " completed manually, " + skipped
                    + " skipped. Protection payments confirmed manually: " + protectionConfirmed + ".";
            default: return "Start guidance for the prepared bush/cactus run. It never performs a game action.";
        }
    }
}
