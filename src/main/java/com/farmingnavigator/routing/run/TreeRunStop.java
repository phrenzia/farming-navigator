package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.transport.VerifiedTransportResult;

public final class TreeRunStop implements RunTravelStop
{
    private final TreePatchDefinition patch;
    private final String action;
    private final boolean replant;
    private final VerifiedTransportResult recommended;
    private final VerifiedTransportResult fallback;
    private final boolean localWalk;
    TreeRunStop(TreePatchDefinition patch, String action, boolean replant)
    {
        this(patch, action, replant, null, null, false);
    }
    private TreeRunStop(TreePatchDefinition patch, String action, boolean replant,
            VerifiedTransportResult recommended, VerifiedTransportResult fallback, boolean localWalk)
    {
        this.patch = patch;
        this.action = action;
        this.replant = replant;
        this.recommended = recommended;
        this.fallback = fallback;
        this.localWalk = localWalk;
    }
    public TreePatchDefinition getPatch() { return patch; }
    public String getAction() { return action; }
    public boolean isReplant() { return replant; }
    @Override public String getTravelLabel() { return patch.getLabel(); }
    @Override public VerifiedTransportResult getRecommended() { return recommended; }
    public VerifiedTransportResult getFallback() { return fallback; }
    @Override public boolean isLocalWalk() { return localWalk; }
    public TreeRunStop withTransports(VerifiedTransportResult primary, VerifiedTransportResult alternative)
    {
        return new TreeRunStop(patch, action, replant, primary, alternative, false);
    }
    public TreeRunStop withLocalWalk() { return new TreeRunStop(patch, action, replant, null, null, true); }
}
