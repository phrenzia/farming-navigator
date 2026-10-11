package com.farmingnavigator.transport;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class HerbPatchTransportService
{
    private final Map<HerbPatchDefinition, List<TransportOption>>
            optionsByPatch;

    @Inject
    public HerbPatchTransportService()
    {
        EnumMap<HerbPatchDefinition, List<TransportOption>> options =
                new EnumMap<>(HerbPatchDefinition.class);

        options.put(
                HerbPatchDefinition.FALADOR,
                transportOptions(
                        transport(
                                1,
                                "Explorer's ring 2+",
                                "Use Cabbage-port near the Falador farming patch.",
                                TransportCategory.DIARY_ITEM
                        ),
                        transport(
                                2,
                                "Ring of the elements",
                                "Teleport to the Air Altar and travel to the farming patch.",
                                TransportCategory.ITEM
                        ),
                        transport(
                                3,
                                "Falador Teleport",
                                "Teleport to Falador and travel south to the farming patch.",
                                TransportCategory.SPELL
                        )
                )
        );

        options.put(
                HerbPatchDefinition.CATHERBY,
                transportOptions(
                        transport(
                                1,
                                "Catherby Teleport",
                                "Teleport directly to Catherby and travel a short distance to the patch.",
                                TransportCategory.SPELL
                        ),
                        transport(
                                2,
                                "Camelot Teleport",
                                "Teleport to Camelot and run east to Catherby.",
                                TransportCategory.SPELL
                        ),
                        transport(
                                3,
                                "Charter ship to Catherby",
                                "Use the charter ship network to reach Catherby.",
                                TransportCategory.TRANSPORT_NETWORK
                        )
                )
        );

        options.put(
                HerbPatchDefinition.ARDOUGNE,
                transportOptions(
                        transport(
                                1,
                                "Ardougne cloak 2+",
                                "Use the cloak's farming patch teleport.",
                                TransportCategory.DIARY_ITEM
                        ),
                        transport(
                                2,
                                "Skills necklace to Fishing Guild",
                                "Teleport to the Fishing Guild and travel south-east to the patch.",
                                TransportCategory.JEWELLERY
                        ),
                        transport(
                                3,
                                "Ardougne Teleport",
                                "Teleport to Ardougne and travel north to the farming patch.",
                                TransportCategory.SPELL
                        )
                )
        );

        options.put(
                HerbPatchDefinition.MORYTANIA,
                transportOptions(
                        transport(
                                1,
                                "Ectophial",
                                "Teleport to the Ectofuntus and travel west to the patch.",
                                TransportCategory.ITEM
                        ),
                        transport(
                                2,
                                "Fairy ring ALQ",
                                "Use fairy ring code ALQ and travel to the farming patch.",
                                TransportCategory.FAIRY_RING
                        ),
                        transport(
                                3,
                                "Fenkenstrain's Castle Teleport",
                                "Teleport near Fenkenstrain's Castle and travel east toward the patch.",
                                TransportCategory.SPELL
                        )
                )
        );

        options.put(
                HerbPatchDefinition.HOSIDIUS,
                transportOptions(
                        transport(
                                1,
                                "Xeric's talisman - Glade",
                                "Teleport to Xeric's Glade and travel to the Hosidius farming patch.",
                                TransportCategory.ITEM
                        ),
                        transport(
                                2,
                                "Hosidius house portal",
                                "Use a Hosidius player-owned house portal and travel to the patch.",
                                TransportCategory.PLAYER_OWNED_HOUSE
                        ),
                        transport(
                                3,
                                "Kharedst's memoirs",
                                "Teleport to Lunch by the lancalliums and travel to the patch.",
                                TransportCategory.ITEM
                        )
                )
        );

        options.put(
                HerbPatchDefinition.TROLL_STRONGHOLD,
                transportOptions(
                        transport(
                                1,
                                "Stony basalt",
                                "Teleport near the Troll Stronghold herb patch.",
                                TransportCategory.ITEM
                        ),
                        transport(
                                2,
                                "Trollheim Teleport",
                                "Teleport to Trollheim and travel west to the Troll Stronghold.",
                                TransportCategory.SPELL
                        )
                )
        );

        options.put(
                HerbPatchDefinition.WEISS,
                transportOptions(
                        transport(
                                1,
                                "Icy basalt",
                                "Teleport directly to Weiss near the herb patch.",
                                TransportCategory.ITEM
                        ),
                        transport(
                                2,
                                "Fairy ring DKS and Larry's boat",
                                "Use fairy ring DKS, travel to Larry's boat, then sail to Weiss.",
                                TransportCategory.FAIRY_RING
                        )
                )
        );

        options.put(
                HerbPatchDefinition.FARMING_GUILD,
                transportOptions(
                        transport(
                                1,
                                "Farming cape",
                                "Use the Farming cape teleport to the Farming Guild.",
                                TransportCategory.SKILLCAPE
                        ),
                        transport(
                                2,
                                "Skills necklace",
                                "Teleport directly to the Farming Guild.",
                                TransportCategory.JEWELLERY
                        ),
                        transport(
                                3,
                                "Spirit tree",
                                "Use a spirit tree planted in the Farming Guild.",
                                TransportCategory.TRANSPORT_NETWORK
                        )
                )
        );

        options.put(
                HerbPatchDefinition.HARMONY_ISLAND,
                transportOptions(
                        transport(
                                1,
                                "Harmony Island Teleport",
                                "Teleport directly to Harmony Island.",
                                TransportCategory.SPELL
                        ),
                        transport(
                                2,
                                "Mos Le'Harmless teleport scroll",
                                "Teleport to Mos Le'Harmless and speak to Brother Tranquility.",
                                TransportCategory.ITEM
                        ),
                        transport(
                                3,
                                "Ectophial and boat",
                                "Travel through Port Phasmatys and Mos Le'Harmless, then speak to Brother Tranquility.",
                                TransportCategory.TRANSPORT_NETWORK
                        )
                )
        );

        options.put(
                HerbPatchDefinition.ORTUS_FARM,
                transportOptions(
                        transport(
                                1,
                                "Quetzal whistle",
                                "Teleport to the Hunter Guild and run north to Ortus Farm.",
                                TransportCategory.ITEM
                        ),
                        transport(
                                2,
                                "Hunter cape",
                                "Teleport to the Hunter Guild and run north to Ortus Farm.",
                                TransportCategory.SKILLCAPE
                        ),
                        transport(
                                3,
                                "Civitas illa Fortis Teleport",
                                "Teleport to Civitas illa Fortis, use the Quetzal Transport System to the Hunter Guild, then run north.",
                                TransportCategory.SPELL
                        ),
                        transport(
                                4,
                                "Fairy ring AJP",
                                "Use fairy ring code AJP and travel north-west to Ortus Farm.",
                                TransportCategory.FAIRY_RING
                        )
                )
        );

        optionsByPatch =
                Collections.unmodifiableMap(options);
    }

    public List<TransportOption> getOptions(
            HerbPatchDefinition patch
    )
    {
        List<TransportOption> options =
                optionsByPatch.get(patch);

        if (options == null)
        {
            return Collections.emptyList();
        }

        return options;
    }

    public Map<HerbPatchDefinition, List<TransportOption>>
    getAllOptions()
    {
        return optionsByPatch;
    }

    private List<TransportOption> transportOptions(
            TransportOption... options
    )
    {
        return Collections.unmodifiableList(
                Arrays.asList(options)
        );
    }

    private TransportOption transport(
            int baselineRank,
            String displayName,
            String instructions,
            TransportCategory category
    )
    {
        return new TransportOption(
                baselineRank,
                displayName,
                instructions,
                category
        );
    }
}