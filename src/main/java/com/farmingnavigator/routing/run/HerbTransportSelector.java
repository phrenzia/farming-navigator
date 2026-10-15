package com.farmingnavigator.routing.run;

import com.farmingnavigator.transport.TransportVerificationKind;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** A resource preference, not a travel-time or GP optimizer. Requirements are evaluated upstream. */
public final class HerbTransportSelector
{
    private HerbTransportSelector() { }

    public static List<VerifiedTransportResult> select(List<VerifiedTransportResult> options,
            HerbTransportPreference preference)
    {
        Comparator<VerifiedTransportResult> order = Comparator.comparingInt(result ->
                preference == HerbTransportPreference.REUSABLE_FIRST
                        && isReusable(result.getOption().getVerificationKind()) ? 0 : 1);
        order = order.thenComparingInt(result -> result.getState() == VerifiedTransportState.READY_NOW ? 0 : 1)
                .thenComparingInt(result -> result.getOption().getBaselineRank());
        return options.stream().filter(VerifiedTransportResult::isUsableForPlannedRun)
                .sorted(order).collect(Collectors.toList());
    }

    private static boolean isReusable(TransportVerificationKind kind)
    {
        switch (kind)
        {
            case EXPLORER_RING_UNLIMITED:
            case ARDOUGNE_CLOAK_UNLIMITED:
            case KANDARIN_HEADGEAR_UNLIMITED:
            case ECTOPHIAL:
            case ECTOPHIAL_HARMONY:
            case FARMING_CAPE:
            case FAIRY_RING_ACCESS:
            case FAIRY_RING_AJP:
            case QUETZAL_WHISTLE: // The current verifier accepts only the perfected infinite whistle.
                return true;
            default:
                // Charged/daily items and POH entry are intentionally outside this guaranteed subset.
                return false;
        }
    }
}
