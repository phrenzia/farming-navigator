package com.farmingnavigator.guided.tree;

import com.farmingnavigator.routing.run.TreeRunCrop;
import com.farmingnavigator.routing.run.TreeRunStop;
import java.time.Duration;

public final class TreeRunSnapshot
{
    private final TreeRunStep step;
    private final TreeRunCrop crop;
    private final TreeRunStop stop;
    private final int total;
    private final int completed;
    private final int manual;
    private final int skipped;
    private final int protectionConfirmed;
    private final Duration elapsed;
    TreeRunSnapshot(TreeRunStep step, TreeRunCrop crop, TreeRunStop stop, int total, int completed,
            int manual, int skipped, int protectionConfirmed, Duration elapsed)
    {
        this.step = step; this.crop = crop; this.stop = stop; this.total = total; this.completed = completed;
        this.manual = manual; this.skipped = skipped; this.protectionConfirmed = protectionConfirmed; this.elapsed = elapsed;
    }
    public TreeRunStep getStep() { return step; }
    public TreeRunCrop getCrop() { return crop; }
    public TreeRunStop getStop() { return stop; }
    public int getTotal() { return total; }
    public int getCompleted() { return completed; }
    public int getManual() { return manual; }
    public int getSkipped() { return skipped; }
    public int getProtectionConfirmed() { return protectionConfirmed; }
    public Duration getElapsed() { return elapsed; }
    public boolean isActive() { return step != TreeRunStep.INACTIVE && step != TreeRunStep.COMPLETE; }
    public String getInstruction()
    {
        switch (step)
        {
            case PREPARE: return "Review saplings, tools, protection and shared travel supplies, then confirm readiness.";
            case TRAVEL: return "Travel to " + stop.getPatch().getLabel() + " and approach the patch.";
            case INSPECT: return "Inspect the tree patch. A fresh nearby observation is required to choose an action.";
            case CHECK_HEALTH: return "Check the tree's health. The checklist waits for a fresh health-checked state.";
            case KEEP_OR_REMOVE: return "The tree is health-checked. Pick available fruit if desired, then choose to keep it or remove and replant.";
            case REMOVE: return "Remove the healthy tree only if you want to replace it. Use a suitable axe or pay the farmer; removal fees are not budgeted.";
            case CLEAR: return "Inspect, then clear the dead tree or dig up the stump.";
            case RAKE: return "Rake the patch until it is empty.";
            case PLANT: return "Plant an unnoted " + crop + " sapling in the empty patch.";
            case PROTECT: return "Pay the farmer to protect the newly planted tree, then confirm manually. Payment is not automatically verified.";
            case PRUNE: return "Prune the diseased tree using secateurs. Keep the living tree; the checklist waits for healthy growth of the same crop.";
            case WAIT: return "The tree is still growing. Wait or skip; this does not prove health-check readiness.";
            case COMPLETE: return "Tree run complete: " + completed + " completed, " + manual + " completed manually, " + skipped
                    + " skipped. Protection payments confirmed manually: " + protectionConfirmed + ".";
            default: return "Start tree guidance for the prepared run. It never performs a game action.";
        }
    }
}
