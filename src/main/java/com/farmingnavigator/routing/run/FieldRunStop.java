package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.transport.VerifiedTransportResult;

public final class FieldRunStop implements RunTravelStop
{
    private final FieldPatchDefinition patch;
    private final String action;
    private final boolean replant;
    private final VerifiedTransportResult recommended, fallback;
    FieldRunStop(FieldPatchDefinition patch, String action, boolean replant) { this(patch, action, replant, null, null); }
    private FieldRunStop(FieldPatchDefinition patch, String action, boolean replant, VerifiedTransportResult primary, VerifiedTransportResult fallback)
    {
        this.patch = patch; this.action = action; this.replant = replant; this.recommended = primary; this.fallback = fallback;
    }
    public FieldPatchDefinition getPatch() { return patch; }
    public String getAction() { return action; }
    public boolean isReplant() { return replant; }
    @Override public String getTravelLabel() { return patch.getLabel(); }
    @Override public VerifiedTransportResult getRecommended() { return recommended; }
    public VerifiedTransportResult getFallback() { return fallback; }
    public FieldRunStop withTransports(VerifiedTransportResult primary, VerifiedTransportResult alternative)
    {
        return new FieldRunStop(patch, action, replant, primary, alternative);
    }
}
