package com.farmingnavigator.routing.run;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.transport.VerifiedTransportResult;

public final class HerbRunStop
{
    private final HerbPatchDefinition patch;
    private final String action;
    private final VerifiedTransportResult recommended;
    private final VerifiedTransportResult fallback;
    private final boolean replant;

    HerbRunStop(HerbPatchDefinition patch, String action, VerifiedTransportResult recommended,
            VerifiedTransportResult fallback, boolean replant)
    {
        this.patch = patch;
        this.action = action;
        this.recommended = recommended;
        this.fallback = fallback;
        this.replant = replant;
    }

    public HerbPatchDefinition getPatch() { return patch; }
    public String getAction() { return action; }
    public VerifiedTransportResult getRecommended() { return recommended; }
    public VerifiedTransportResult getFallback() { return fallback; }
    public boolean isReplant() { return replant; }

    public HerbRunStop withTransports(VerifiedTransportResult primary, VerifiedTransportResult alternative)
    {
        return new HerbRunStop(patch, action, primary, alternative, replant);
    }
}
