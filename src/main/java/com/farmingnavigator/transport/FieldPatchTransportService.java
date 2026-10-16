package com.farmingnavigator.transport;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchDefinition;
import com.farmingnavigator.farming.field.FieldPatchRegion;
import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Reuse verified arrivals to shared farming areas; access to each plot is checked separately. */
public final class FieldPatchTransportService
{
    public Map<FieldPatchDefinition, List<TransportOption>> getAllOptions()
    {
        Map<FieldPatchDefinition, List<TransportOption>> options = new EnumMap<>(FieldPatchDefinition.class);
        Map<HerbPatchDefinition, List<TransportOption>> herbs = new HerbPatchTransportService().getAllOptions();
        List<TransportOption> guild = new GardenPatchTransportService().getAllOptions().get(GardenPatchDefinition.FARMING_GUILD_BUSH);
        for (FieldPatchDefinition patch : FieldPatchDefinition.values())
        {
            FieldPatchRegion region = FieldPatchRegion.forLocation(patch.getLocation());
            List<TransportOption> arrivals;
            switch (region)
            {
                case KASTORI: continue;
                case FARMING_GUILD: arrivals = guild; break;
                case PRIFDDINAS:
                    arrivals = List.of(new TransportOption(1, "Teleport crystal to Prifddinas",
                            "Choose Prifddinas on a charged or eternal teleport crystal, then walk north-east to the allotment/flower area.",
                            TransportCategory.ITEM, TransportVerificationKind.PRIFDDINAS_CRYSTAL)); break;
                default: arrivals = herbs.getOrDefault(HerbPatchDefinition.valueOf(region.name()), List.of()); break;
            }
            List<TransportOption> rewritten = new ArrayList<>();
            for (TransportOption arrival : arrivals)
            {
                String instructions = arrival.getInstructions().replace("herb patch", "allotment/flower area")
                        + " Use the labelled plot: " + patch.getLabel() + ".";
                rewritten.add(arrival.getPohPortalDestination() == null
                        ? new TransportOption(arrival.getBaselineRank(), arrival.getDisplayName(), instructions,
                                arrival.getCategory(), arrival.getVerificationKind(), arrival.getTeleportSpellDefinition())
                        : TransportOption.pohPortal(arrival.getBaselineRank(), arrival.getDisplayName(), instructions,
                                arrival.getPohPortalDestination()));
            }
            options.put(patch, List.copyOf(rewritten));
        }
        return Collections.unmodifiableMap(options);
    }
}
