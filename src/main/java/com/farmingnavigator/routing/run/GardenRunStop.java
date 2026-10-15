package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.transport.VerifiedTransportResult;

public final class GardenRunStop implements RunTravelStop
{
    private final GardenPatchDefinition patch;
    private final String action;
    private final boolean replant;
    private final VerifiedTransportResult recommended, fallback;
    GardenRunStop(GardenPatchDefinition patch, String action, boolean replant) { this(patch, action, replant, null, null); }
    private GardenRunStop(GardenPatchDefinition patch, String action, boolean replant, VerifiedTransportResult primary, VerifiedTransportResult fallback)
    {
        this.patch = patch; this.action = action; this.replant = replant; this.recommended = primary; this.fallback = fallback;
    }
    public GardenPatchDefinition getPatch() { return patch; }
    public String getAction() { return action; }
    public boolean isReplant() { return replant; }
    @Override public String getTravelLabel() { return patch.getLabel(); }
    @Override public VerifiedTransportResult getRecommended() { return recommended; }
    public VerifiedTransportResult getFallback() { return fallback; }
    public GardenRunStop withTransports(VerifiedTransportResult primary, VerifiedTransportResult alternative)
    {
        return new GardenRunStop(patch, action, replant, primary, alternative);
    }
}
