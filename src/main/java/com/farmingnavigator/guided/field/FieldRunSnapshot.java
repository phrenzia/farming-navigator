package com.farmingnavigator.guided.field;

import com.farmingnavigator.routing.run.FieldRunCrop;
import com.farmingnavigator.routing.run.FieldRunMode;
import com.farmingnavigator.routing.run.FieldRunStop;
import java.time.Duration;

public final class FieldRunSnapshot
{
    private final long revision;
    public long getRevision() { return revision; }
    private final FieldRunStep step;
    private final com.farmingnavigator.routing.run.FieldRunWatering watering;
    private final com.farmingnavigator.routing.run.HerbRunCure cure;
    private final FieldRunCrop crop;
    private final FieldRunMode mode;
    private final FieldRunStop stop;
    private final FieldRunStop nextStop;
    public FieldRunStop getNextStop() { return nextStop; }
    private final com.farmingnavigator.routing.run.FieldRunCompost compost;
    private final int total, completed, manual, skipped, protectionConfirmed;
    private final Duration elapsed;
    FieldRunSnapshot(FieldRunStep step, FieldRunCrop crop, FieldRunMode mode, FieldRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed)
    {
        this(step, crop, mode, stop, total, completed, manual, skipped, protectionConfirmed, elapsed, com.farmingnavigator.routing.run.FieldRunCompost.NONE);
    }
    FieldRunSnapshot(FieldRunStep step, FieldRunCrop crop, FieldRunMode mode, FieldRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed,
            com.farmingnavigator.routing.run.FieldRunCompost compost)
    {
        this(step, crop, mode, stop, total, completed, manual, skipped, protectionConfirmed, elapsed, compost,
                com.farmingnavigator.routing.run.HerbRunCure.PLANT_CURE);
    }
    FieldRunSnapshot(FieldRunStep step, FieldRunCrop crop, FieldRunMode mode, FieldRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed,
            com.farmingnavigator.routing.run.FieldRunCompost compost, com.farmingnavigator.routing.run.HerbRunCure cure)
    {
        this(step, crop, mode, stop, total, completed, manual, skipped, protectionConfirmed, elapsed, compost, cure,
                com.farmingnavigator.routing.run.FieldRunWatering.NONE);
    }
    FieldRunSnapshot(FieldRunStep step, FieldRunCrop crop, FieldRunMode mode, FieldRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed,
            com.farmingnavigator.routing.run.FieldRunCompost compost, com.farmingnavigator.routing.run.HerbRunCure cure,
            com.farmingnavigator.routing.run.FieldRunWatering watering)
    {
        this(step, crop, mode, stop, total, completed, manual, skipped, protectionConfirmed, elapsed, compost, cure, watering, 0L);
    }
    FieldRunSnapshot(FieldRunStep step, FieldRunCrop crop, FieldRunMode mode, FieldRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed,
            com.farmingnavigator.routing.run.FieldRunCompost compost, com.farmingnavigator.routing.run.HerbRunCure cure,
            com.farmingnavigator.routing.run.FieldRunWatering watering, long revision)
    {
        this(step,crop,mode,stop,total,completed,manual,skipped,protectionConfirmed,elapsed,compost,cure,watering,revision,null);
    }
    FieldRunSnapshot(FieldRunStep step, FieldRunCrop crop, FieldRunMode mode, FieldRunStop stop,
            int total, int completed, int manual, int skipped, int protectionConfirmed, Duration elapsed,
            com.farmingnavigator.routing.run.FieldRunCompost compost, com.farmingnavigator.routing.run.HerbRunCure cure,
            com.farmingnavigator.routing.run.FieldRunWatering watering, long revision, FieldRunStop nextStop)
    {
        this.nextStop = nextStop;
        this.revision = revision;
        this.watering = watering;
        this.cure = cure;
        this.compost = compost;
        this.step = step; this.crop = crop; this.mode = mode; this.stop = stop; this.total = total;
        this.completed = completed; this.manual = manual; this.skipped = skipped; this.protectionConfirmed = protectionConfirmed; this.elapsed = elapsed;
    }
    public com.farmingnavigator.routing.run.HerbRunCure getCure() { return cure; }
    public com.farmingnavigator.routing.run.FieldRunWatering getWatering() { return watering; }
    public FieldRunStep getStep() { return step; }
    public FieldRunCrop getCrop() { return crop; }
    public FieldRunMode getMode() { return mode; }
    public FieldRunStop getStop() { return stop; }
    public com.farmingnavigator.routing.run.FieldRunCompost getCompost() { return compost; }
    public int getTotal() { return total; }
    public int getCompleted() { return completed; }
    public int getManual() { return manual; }
    public int getSkipped() { return skipped; }
    public int getProtectionConfirmed() { return protectionConfirmed; }
    public Duration getElapsed() { return elapsed; }
    public boolean isActive() { return step != FieldRunStep.INACTIVE && step != FieldRunStep.COMPLETE; }
    public String getInstruction()
    {
        switch (step)
        {
            case PREPARE: return "Review seeds, tools, compost, watering, protection and shared travel supplies, then confirm readiness.";
            case TRAVEL: return (stop.isLocalWalk() ? "Walk within the farming area to " : "Travel to ") + stop.getPatch().getLabel() + " and approach the labelled plot."
                    + (crop == FieldRunCrop.SEAWEED ? " Check your diving equipment, air supply and underwater item restrictions manually." : "");
            case INSPECT: return "Inspect the labelled plot. A fresh nearby observation is required before choosing an action.";
            case MAKE_SPACE: return "Inventory full. Make space manually by banking or using compatible produce-noting where available. Return to this patch with a free slot to resume harvesting. Freeing space does not verify produce quantity or harvest completion.";
            case HARVEST: return (crop == FieldRunCrop.BELLADONNA ? "Equip suitable gloves before picking belladonna to avoid poison damage. " : "")
                    + "Harvest this plot manually until empty. Harvest stages do not prove produce quantity; the checklist waits for a fresh empty-patch observation.";
            case KEEP_FLOWER: return "Inspect and keep this mature flower. Harvesting it may remove nearby allotment protection. Confirm when finished.";
            case CLEAR: return "Clear the dead crop with a spade. Wait for a fresh empty-patch observation.";
            case RAKE: return "Rake until the plot is empty.";
            case COMPOST:
                if (compost.isSpell()) { return "Cast Fertile Soil on this plot, then confirm manually. Compost application is not automatically verified."
                        + (compost == com.farmingnavigator.routing.run.FieldRunCompost.FERTILE_SOIL_ULTRA
                                ? " The ash-covered tome and two volcanic ash per cast are required." : " Without ash, the spell applies supercompost; carried ash may upgrade it if the tome is read."); }
                if (compost.isBottomless()) { return "Check your selected bottomless bucket's compost type and remaining uses, apply it to this plot, then confirm manually. Contents and application are not automatically verified; farmer payments are separate."; }
                return "Apply one unnoted bucket of " + compost + " to this plot, then confirm manually. Current compost state and application are not automatically verified; farmer payments are separate.";
            case PLANT: return "Use a seed dibber to plant " + crop.getSeedsPerPatch() + " " + crop + " seed/spore(s). Seed saving and Barbarian Training are not assumed.";
            case WATER: return "Water the planted crop once using a filled standard can, then confirm manually. A fresh nearby watered state of the same crop advances this step, or confirm manually. Watering only covers this growth cycle; later cycles need watering again.";
            case REPREPARE: return "A planting was already observed at this stop, but the plot is empty again. No second seed or care application is reserved. Prepare a new plan before replanting.";
            case PROTECT: return "Pay the gardener for protection, then confirm manually. The checklist does not verify the payment.";
            case CURE: return (cure == com.farmingnavigator.routing.run.HerbRunCure.CURE_PLANT ? "Cast Lunar Cure Plant" : "Use plant cure")
                    + " on this diseased crop. Wait for healthy growth or harvestable state of the same crop.";
            case SCARECROW: return "Scarecrow present. Keep it or remove it manually and prepare a new plan; this checklist does not assume removal or flower planting.";
            case WAIT: return "The crop is growing. Wait or skip; time alone does not prove harvest readiness.";
            case COMPLETE: return "Field run complete: " + completed + " completed, " + manual + " completed manually, " + skipped
                    + " skipped. Protection payments confirmed manually: " + protectionConfirmed + ".";
            default: return "Start guidance for the prepared field run. It never performs a game action.";
        }
    }
}
