package com.farmingnavigator.guided.tree;

import com.farmingnavigator.routing.run.TreeRunCrop;
import com.farmingnavigator.routing.run.TreeRunStop;
import java.time.Duration;

public final class TreeRunSnapshot
{
    private final long revision;
    public long getRevision() { return revision; }
    private final TreeRunStep step;
    private final com.farmingnavigator.routing.run.TreeRunCompost compost;
    private final com.farmingnavigator.routing.run.TreeRunCure cure;
    private final TreeRunCrop crop;
    private final TreeRunStop stop;
    private final TreeRunStop nextStop;
    public TreeRunStop getNextStop() { return nextStop; }
    private final int total;
    private final int completed;
    private final int manual;
    private final int skipped;
    private final int protectionConfirmed;
    private final Duration elapsed;
    TreeRunSnapshot(TreeRunStep step, TreeRunCrop crop, TreeRunStop stop, int total, int completed,
            int manual, int skipped, int protectionConfirmed, Duration elapsed)
    {
        this(step, crop, stop, total, completed, manual, skipped, protectionConfirmed, elapsed, 0L);
    }
    TreeRunSnapshot(TreeRunStep step, TreeRunCrop crop, TreeRunStop stop, int total, int completed,
            int manual, int skipped, int protectionConfirmed, Duration elapsed, long revision)
    {
        this(step, crop, stop, total, completed, manual, skipped, protectionConfirmed, elapsed,
                com.farmingnavigator.routing.run.TreeRunCompost.NONE, com.farmingnavigator.routing.run.TreeRunCure.PRUNE, revision);
    }
    TreeRunSnapshot(TreeRunStep step, TreeRunCrop crop, TreeRunStop stop, int total, int completed,
            int manual, int skipped, int protectionConfirmed, Duration elapsed,
            com.farmingnavigator.routing.run.TreeRunCompost compost, com.farmingnavigator.routing.run.TreeRunCure cure, long revision)
    {
        this(step,crop,stop,total,completed,manual,skipped,protectionConfirmed,elapsed,compost,cure,revision,null);
    }
    TreeRunSnapshot(TreeRunStep step, TreeRunCrop crop, TreeRunStop stop, int total, int completed,
            int manual, int skipped, int protectionConfirmed, Duration elapsed,
            com.farmingnavigator.routing.run.TreeRunCompost compost, com.farmingnavigator.routing.run.TreeRunCure cure, long revision, TreeRunStop nextStop)
    {
        this.nextStop = nextStop;
        this.compost = compost; this.cure = cure;
        this.revision = revision;
        this.step = step; this.crop = crop; this.stop = stop; this.total = total; this.completed = completed;
        this.manual = manual; this.skipped = skipped; this.protectionConfirmed = protectionConfirmed; this.elapsed = elapsed;
    }
    public com.farmingnavigator.routing.run.TreeRunCompost getCompost() { return compost; }
    public com.farmingnavigator.routing.run.TreeRunCure getCure() { return cure; }
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
            case PREPARE: return "Review saplings, tools, compost, protection and shared travel supplies, then confirm readiness.";
            case TRAVEL: return "Travel to " + stop.getPatch().getLabel() + " and approach the patch.";
            case INSPECT: return "Inspect the tree patch. A fresh nearby observation is required to choose an action.";
            case CHECK_HEALTH: return "Check the tree's health. The checklist waits for a fresh health-checked state.";
            case MAKE_SPACE: return "Inventory full. Make space manually, then return to this tree with a free slot to resume bark harvesting. Freeing space does not prove bark depletion or permit tree removal; a fresh depleted-bark observation is still required.";
            case HARVEST_BARK: return "Use a suitable axe to harvest available celastrus bark. The checklist waits for an observed depleted-bark state before offering removal.";
            case KEEP_OR_REMOVE:
                if (crop == TreeRunCrop.CRYSTAL) { return "The crystal tree is health-checked. Keep it, or choose to chop it for crystal shards and replant. Shard yield is not predicted."; }
                if (crop == TreeRunCrop.CELASTRUS) { return "The celastrus bark is depleted. Keep the remaining tree or choose to cut it down, clear its stump and replant."; }
                if (crop == TreeRunCrop.REDWOOD) { return "Keep the mature redwood, or explicitly choose removal. No replacement is reserved for an observed mature tree; prepare a new plan after removal."; }
                return "The tree is health-checked. Pick available fruit if desired, then choose to keep it or remove and replant.";
            case REMOVE:
                if (crop == TreeRunCrop.CRYSTAL) { return "Chop the crystal tree with a suitable axe to collect crystal shards. Wait for a fresh empty-patch observation before replanting."; }
                if (crop == TreeRunCrop.REDWOOD) { return "If you still want to remove the redwood, pay Alexandra 2,000 coins. Coins are not budgeted. Prepare a new plan after removal for any unreserved replacement supplies."; }
                if (crop == TreeRunCrop.CALQUAT) { return "Pick available calquat fruit if desired, then use a spade to remove the tree only if you want to replace it."; }
                if (crop == TreeRunCrop.CELASTRUS) { return "Cut down the depleted celastrus tree with a suitable axe, then dig up its stump before replanting."; }
                return "Remove the healthy tree only if you want to replace it. Use a suitable axe or pay the farmer; removal fees are not budgeted.";
            case CLEAR: return "Inspect, then clear the dead tree or dig up the stump.";
            case RAKE: return "Rake the patch until it is empty.";
            case COMPOST:
                if (compost.isSpell()) { return "Cast Fertile Soil on this patch, then confirm manually. Application is not automatically observed."
                        + (compost == com.farmingnavigator.routing.run.TreeRunCompost.FERTILE_SOIL_ULTRA ? " The ash-covered tome and two volcanic ash per cast are required." : " Without ash the spell applies supercompost; carried ash may upgrade it if the tome is read."); }
                return "Apply " + compost + " to this patch, then confirm manually. Current compost state and application are not observed."
                        + (compost.isBottomless() ? " Check the selected contents and remaining uses manually." : "");
            case CURE: return "Cast Lunar Cure Plant on the diseased tree. Keep the living tree; wait for fresh healthy growth of the same crop.";
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
