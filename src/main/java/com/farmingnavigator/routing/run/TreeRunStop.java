package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.tree.TreePatchDefinition;

public final class TreeRunStop
{
    private final TreePatchDefinition patch;
    private final String action;
    private final boolean replant;
    TreeRunStop(TreePatchDefinition patch, String action, boolean replant)
    {
        this.patch = patch;
        this.action = action;
        this.replant = replant;
    }
    public TreePatchDefinition getPatch() { return patch; }
    public String getAction() { return action; }
    public boolean isReplant() { return replant; }
}
