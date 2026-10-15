package com.farmingnavigator.routing.run;

import com.farmingnavigator.transport.VerifiedTransportResult;

/** Common travel budget input for herb, tree and future run families. */
public interface RunTravelStop
{
    VerifiedTransportResult getRecommended();
    String getTravelLabel();
    default boolean isLocalWalk() { return false; }
}
