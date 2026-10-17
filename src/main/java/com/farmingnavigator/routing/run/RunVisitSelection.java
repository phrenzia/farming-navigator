package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.state.PatchCondition;
import com.farmingnavigator.farming.tree.TreePatchCondition;
import com.farmingnavigator.farming.garden.GardenPatchCondition;
import com.farmingnavigator.farming.field.FieldPatchCondition;

/** Filters historical observations only. Timers never promote growing patches to observed maturity. */
public enum RunVisitSelection
{
    ALL_SELECTED("All selected patches"),
    OBSERVED_MATURE("Last observed mature"),
    OBSERVED_PROBLEMS("Last observed diseased/dead"),
    OBSERVED_MATURE_OR_PROBLEMS("Last observed mature or diseased/dead");

    private final String label;
    RunVisitSelection(String label) { this.label = label; }
    @Override public String toString() { return label; }
    public static RunVisitSelection orDefault(RunVisitSelection selection) { return selection == null ? ALL_SELECTED : selection; }

    public String exclusion(PatchCondition condition)
    { return exclusion(condition == PatchCondition.HARVESTABLE, condition == PatchCondition.DISEASED || condition == PatchCondition.DEAD); }
    public String exclusion(TreePatchCondition condition)
    { return exclusion(condition == TreePatchCondition.CHECK_HEALTH || condition == TreePatchCondition.HEALTH_CHECKED
            || condition == TreePatchCondition.HARVESTABLE, condition == TreePatchCondition.DISEASED || condition == TreePatchCondition.DEAD); }
    public String exclusion(GardenPatchCondition condition)
    { return exclusion(condition == GardenPatchCondition.CHECK_HEALTH || condition == GardenPatchCondition.HEALTH_CHECKED,
            condition == GardenPatchCondition.DISEASED || condition == GardenPatchCondition.DEAD); }
    public String exclusion(FieldPatchCondition condition)
    { return exclusion(condition == FieldPatchCondition.HARVESTABLE, condition == FieldPatchCondition.DISEASED || condition == FieldPatchCondition.DEAD); }
    private String exclusion(boolean mature, boolean problem)
    {
        boolean include = this == ALL_SELECTED || this == OBSERVED_MATURE && mature || this == OBSERVED_PROBLEMS && problem
                || this == OBSERVED_MATURE_OR_PROBLEMS && (mature || problem);
        return include ? "" : "Visit filter: " + label + ". No matching historical observation; estimates do not qualify.";
    }
}
