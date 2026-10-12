package com.farmingnavigator.transport;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class HerbPatchTransportService
{
    private final Map<
            HerbPatchDefinition,
            List<TransportOption>
            > optionsByPatch;

    @Inject
    public HerbPatchTransportService()
    {
        EnumMap<
                HerbPatchDefinition,
                List<TransportOption>
                > options =
                new EnumMap<>(
                        HerbPatchDefinition.class
                );

        options.put(
                HerbPatchDefinition.FALADOR,
                transportOptions(
                        verifiedTransport(
                                1,
                                "Explorer's ring 3/4",
                                "Use the unlimited Cabbage-port teleport to the farming patch.",
                                TransportCategory.DIARY_ITEM,
                                TransportVerificationKind.EXPLORER_RING_UNLIMITED
                        ),
                        futureTransport(
                                2,
                                "Explorer's ring 2",
                                "Use one of the three daily Cabbage-port teleports.",
                                TransportCategory.DIARY_ITEM
                        ),
                        futureTransport(
                                3,
                                "Ring of the elements",
                                "Teleport to the Air Altar and travel to the farming patch.",
                                TransportCategory.ITEM
                        ),
                        verifiedSpellTransport(
                                4,
                                TeleportSpellDefinition.FALADOR_TELEPORT,
                                "Teleport to Falador and travel south to the farming patch."
                        )
                )
        );

        options.put(
                HerbPatchDefinition.CATHERBY,
                transportOptions(
                        verifiedSpellTransport(
                                1,
                                TeleportSpellDefinition.CATHERBY_TELEPORT,
                                "Teleport directly to Catherby and travel a short distance to the patch."
                        ),
                        verifiedSpellTransport(
                                2,
                                TeleportSpellDefinition.CAMELOT_TELEPORT,
                                "Teleport to Camelot and run east to Catherby."
                        ),
                        futureTransport(
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
                        futureTransport(
                                1,
                                "Ardougne cloak 2+",
                                "Use the cloak's Ardougne farming patch teleport.",
                                TransportCategory.DIARY_ITEM
                        ),
                        verifiedTransport(
                                2,
                                "Skills necklace to Fishing Guild",
                                "Teleport to the Fishing Guild and travel south-east to the patch.",
                                TransportCategory.JEWELLERY,
                                TransportVerificationKind.SKILLS_NECKLACE
                        ),
                        verifiedSpellTransport(
                                3,
                                TeleportSpellDefinition.FISHING_GUILD_TELEPORT,
                                "Teleport to the Fishing Guild and travel south-east to the patch."
                        ),
                        futureTransport(
                                4,
                                "Ardougne Teleport",
                                "Teleport to Ardougne and travel north to the farming patch.",
                                TransportCategory.SPELL
                        )
                )
        );

        options.put(
                HerbPatchDefinition.MORYTANIA,
                transportOptions(
                        verifiedTransport(
                                1,
                                "Ectophial",
                                "Empty the Ectophial to teleport to the Ectofuntus, then travel west to the patch.",
                                TransportCategory.ITEM,
                                TransportVerificationKind.ECTOPHIAL
                        ),
                        verifiedTransport(
                                2,
                                "POH fairy ring to ALQ",
                                "Enter your POH with Teleport to House or a house tablet, use your configured POH fairy ring, enter code ALQ, then travel north to the farming patch.",
                                TransportCategory.PLAYER_OWNED_HOUSE,
                                TransportVerificationKind.POH_FAIRY_RING
                        ),
                        verifiedTransport(
                                3,
                                "Fairy ring ALQ",
                                "Use fairy ring code ALQ and travel north to the farming patch.",
                                TransportCategory.FAIRY_RING,
                                TransportVerificationKind.FAIRY_RING_ACCESS
                        ),
                        futureTransport(
                                4,
                                "Fenkenstrain's Castle Teleport",
                                "Teleport near Fenkenstrain's Castle and travel east toward the patch.",
                                TransportCategory.SPELL
                        )
                )
        );

        options.put(
                HerbPatchDefinition.HOSIDIUS,
                transportOptions(
                        futureTransport(
                                1,
                                "Xeric's talisman - Glade",
                                "Teleport to Xeric's Glade and travel to the Hosidius farming patch.",
                                TransportCategory.ITEM
                        ),
                        futureTransport(
                                2,
                                "Hosidius house portal",
                                "Use a Hosidius player-owned house portal and travel to the patch.",
                                TransportCategory.PLAYER_OWNED_HOUSE
                        ),
                        futureTransport(
                                3,
                                "Kharedst's memoirs",
                                "Teleport to Lunch by the lancalliums and travel to the patch.",
                                TransportCategory.ITEM
                        ),
                        verifiedTransport(
                                4,
                                "POH fairy ring to AKR",
                                "Enter your POH with Teleport to House or a house tablet, use your configured POH fairy ring, enter code AKR, then run west to the farming patch.",
                                TransportCategory.PLAYER_OWNED_HOUSE,
                                TransportVerificationKind.POH_FAIRY_RING
                        ),
                        verifiedTransport(
                                5,
                                "Fairy ring AKR",
                                "Use fairy ring code AKR to Hosidius Vinery, then run west to the farming patch.",
                                TransportCategory.FAIRY_RING,
                                TransportVerificationKind.FAIRY_RING_ACCESS
                        )
                )
        );

        options.put(
                HerbPatchDefinition.TROLL_STRONGHOLD,
                transportOptions(
                        verifiedTransport(
                                1,
                                "Stony basalt",
                                "Teleport near the Troll Stronghold herb patch.",
                                TransportCategory.ITEM,
                                TransportVerificationKind.STONY_BASALT
                        ),
                        verifiedSpellTransport(
                                2,
                                TeleportSpellDefinition.TROLLHEIM_TELEPORT,
                                "Teleport to Trollheim and travel to the Troll Stronghold patch."
                        )
                )
        );

        options.put(
                HerbPatchDefinition.WEISS,
                transportOptions(
                        verifiedTransport(
                                1,
                                "Icy basalt",
                                "Teleport directly to Weiss near the herb patch.",
                                TransportCategory.ITEM,
                                TransportVerificationKind.ICY_BASALT
                        ),
                        futureTransport(
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
                        verifiedTransport(
                                1,
                                "Farming cape",
                                "Use the Farming cape teleport to the Farming Guild.",
                                TransportCategory.SKILLCAPE,
                                TransportVerificationKind.FARMING_CAPE
                        ),
                        verifiedTransport(
                                2,
                                "Skills necklace",
                                "Teleport directly to the Farming Guild.",
                                TransportCategory.JEWELLERY,
                                TransportVerificationKind.SKILLS_NECKLACE
                        ),
                        futureTransport(
                                3,
                                "Spirit tree",
                                "Use a spirit tree planted in the Farming Guild.",
                                TransportCategory.TRANSPORT_NETWORK
                        ),
                        verifiedSpellTransport(
                                4,
                                TeleportSpellDefinition.BATTLEFRONT_TELEPORT,
                                "Teleport to the Battlefront and travel west to the Farming Guild."
                        ),
                        verifiedTransport(
                                5,
                                "POH fairy ring to CIR",
                                "Enter your POH with Teleport to House or a house tablet, use your configured POH fairy ring, enter code CIR, then run south-west to the Farming Guild.",
                                TransportCategory.PLAYER_OWNED_HOUSE,
                                TransportVerificationKind.POH_FAIRY_RING
                        ),
                        verifiedTransport(
                                6,
                                "Fairy ring CIR",
                                "Use fairy ring code CIR at Mount Karuulm, then run south-west to the Farming Guild.",
                                TransportCategory.FAIRY_RING,
                                TransportVerificationKind.FAIRY_RING_ACCESS
                        )
                )
        );

        options.put(
                HerbPatchDefinition.HARMONY_ISLAND,
                transportOptions(
                        verifiedSpellTransport(
                                1,
                                TeleportSpellDefinition.HARMONY_ISLAND_TELEPORT,
                                "Teleport directly to Harmony Island."
                        ),
                        futureTransport(
                                2,
                                "Mos Le'Harmless teleport scroll",
                                "Teleport to Mos Le'Harmless and speak to Brother Tranquility.",
                                TransportCategory.ITEM
                        ),
                        futureTransport(
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
                        futureTransport(
                                1,
                                "Quetzal whistle",
                                "Teleport to the Hunter Guild and run north to Ortus Farm.",
                                TransportCategory.ITEM
                        ),
                        futureTransport(
                                2,
                                "Hunter cape",
                                "Teleport to the Hunter Guild and run north to Ortus Farm.",
                                TransportCategory.SKILLCAPE
                        ),
                        verifiedSpellTransport(
                                3,
                                TeleportSpellDefinition.CIVITAS_ILLA_FORTIS_TELEPORT,
                                "Teleport to Civitas illa Fortis, use the Quetzal Transport System to the Hunter Guild, then run north."
                        ),
                        futureTransport(
                                4,
                                "Fairy ring AJP",
                                "Use fairy ring code AJP and travel north-west to Ortus Farm.",
                                TransportCategory.FAIRY_RING
                        )
                )
        );

        optionsByPatch =
                Collections.unmodifiableMap(
                        options
                );
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

    public Map<
            HerbPatchDefinition,
            List<TransportOption>
            > getAllOptions()
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

    private TransportOption verifiedTransport(
            int baselineRank,
            String displayName,
            String instructions,
            TransportCategory category,
            TransportVerificationKind verificationKind
    )
    {
        return new TransportOption(
                baselineRank,
                displayName,
                instructions,
                category,
                verificationKind
        );
    }

    private TransportOption verifiedSpellTransport(
            int baselineRank,
            TeleportSpellDefinition spellDefinition,
            String instructions
    )
    {
        return new TransportOption(
                baselineRank,
                spellDefinition.getDisplayName(),
                instructions,
                TransportCategory.SPELL,
                TransportVerificationKind.TELEPORT_SPELL,
                spellDefinition
        );
    }

    private TransportOption futureTransport(
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
                category,
                TransportVerificationKind.NOT_IMPLEMENTED
        );
    }
}