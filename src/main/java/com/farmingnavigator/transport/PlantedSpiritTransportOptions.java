package com.farmingnavigator.transport;

import com.farmingnavigator.routing.spirit.PlantedSpiritTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Manual-access alternatives using the already-supported wealth/Grand Exchange spirit-tree entry. */
final class PlantedSpiritTransportOptions
{
    private PlantedSpiritTransportOptions() { }
    static <P> void append(Map<P,List<TransportOption>> options,P patch,PlantedSpiritTree destination,String approach)
    {
        List<TransportOption> copy=new ArrayList<>(options.getOrDefault(patch,List.of()));
        copy.add(TransportOption.wealthToPlantedSpiritTree(90,"Wealth and planted spirit tree: " + destination,
                "Use a charged ring of wealth to the Grand Exchange, walk to its spirit tree, then choose the planted tree at " + destination
                        + ". " + approach + " Declare current planted-tree access for this login session before selecting this arrival.",destination));
        options.put(patch,List.copyOf(copy));
    }
}
