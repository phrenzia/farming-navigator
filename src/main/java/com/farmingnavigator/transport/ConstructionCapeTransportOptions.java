package com.farmingnavigator.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Direct portal destinations, independent of the configured house and its entry supplies. */
final class ConstructionCapeTransportOptions
{
    private ConstructionCapeTransportOptions() { }
    static <P> void append(Map<P,List<TransportOption>> options, P patch, TransportVerificationKind kind, String portal, String approach)
    {
        List<TransportOption> updated=new ArrayList<>(options.getOrDefault(patch,List.of()));
        updated.add(new TransportOption(96,"Construction cape to " + portal + " portal",
                "Choose " + portal + " under POH Portals on a Construction cape or ordinary max cape, then " + approach,
                TransportCategory.ITEM,kind));
        options.put(patch,List.copyOf(updated));
    }
}
