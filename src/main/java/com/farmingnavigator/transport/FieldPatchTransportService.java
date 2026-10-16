package com.farmingnavigator.transport;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
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
                case KASTORI:
                case VINERY: continue;
                case YANILLE:
                    arrivals = List.of(new TransportOption(1, "Fairy ring CIQ to Yanille hops",
                            "Travel to an accessible fairy ring, use CIQ north-west of Yanille, then travel east and south to the hops plot.",
                            TransportCategory.FAIRY_RING, TransportVerificationKind.FAIRY_RING_ACCESS)); break;
                case SEERS_VILLAGE:
                    arrivals = List.of(spell(TeleportSpellDefinition.CAMELOT_TELEPORT,
                            "Use Camelot Teleport and travel north-west to the hops plot north of McGrubor's Wood. If your diary redirect sends you to Seers' Village, travel north from there."),
                            new TransportOption(2, "Fairy ring ALS to Seers' hops",
                                    "Travel to an accessible fairy ring, use ALS in McGrubor's Wood, then leave the wood and travel north to the hops plot.",
                                    TransportCategory.FAIRY_RING, TransportVerificationKind.FAIRY_RING_ACCESS)); break;
                case LUMBRIDGE:
                    arrivals = List.of(spell(TeleportSpellDefinition.LUMBRIDGE_TELEPORT,
                            "Teleport to Lumbridge Castle, then run north to the hops plot north of the farm.")); break;
                case SEAWEED:
                    arrivals = List.of(new TransportOption(1, "Digsite pendant, barge and northern rowboat",
                            "Choose the base Digsite pendant destination, quick-travel with the barge guard to Fossil Island, then walk to the northern rowboat. Row to the small island and choose Dive. After visiting once, check the Museum Camp rowboat for the unlocked shortcut yourself. Check diving equipment, air supply and underwater item restrictions manually.",
                            TransportCategory.ITEM, TransportVerificationKind.DIGSITE_BARGE)); break;
                case MORYTANIA_MUSHROOM:
                    arrivals = List.of(new TransportOption(1, "Fairy ring CKS to mushroom patch",
                            "Travel to an accessible fairy ring, use CKS, then approach the mushroom patch beside the ring.",
                            TransportCategory.FAIRY_RING, TransportVerificationKind.FAIRY_RING_ACCESS),
                            spell(TeleportSpellDefinition.KHARYRLL_TELEPORT, "Teleport to Canifis, then travel west to the mushroom patch beside fairy ring CKS.")); break;
                case DRAYNOR_MANOR:
                    arrivals = List.of(spell(TeleportSpellDefinition.DRAYNOR_MANOR_TELEPORT,
                            "Teleport to Draynor Manor, then approach the belladonna patch in the manor grounds. Equip suitable gloves before harvesting.")); break;
                case AUBURNVALE:
                case ENTRANA:
                case ALDARIN: arrivals = List.of(); break;
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
    private TransportOption spell(TeleportSpellDefinition spell, String instructions)
    {
        return new TransportOption(1, spell.getDisplayName(), instructions, TransportCategory.SPELL,
                TransportVerificationKind.TELEPORT_SPELL, spell);
    }

}
