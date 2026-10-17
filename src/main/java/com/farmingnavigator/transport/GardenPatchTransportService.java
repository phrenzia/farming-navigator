package com.farmingnavigator.transport;

import com.farmingnavigator.farming.garden.GardenPatchDefinition;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Verified arrival suggestions; catalogue ranks do not measure route speed. */
public final class GardenPatchTransportService
{
    public Map<GardenPatchDefinition, List<TransportOption>> getAllOptions()
    {
        Map<GardenPatchDefinition, List<TransportOption>> options = new EnumMap<>(GardenPatchDefinition.class);
        List<TransportOption> guild = List.of(new TransportOption(1, "Skills necklace to Farming Guild",
                "Teleport to the Farming Guild and enter the beginner wing; maintain the required Farming boost.",
                TransportCategory.JEWELLERY, TransportVerificationKind.SKILLS_NECKLACE),
                new TransportOption(2, "Fairy ring CIR to Farming Guild", "Travel to an accessible fairy ring, use CIR, then run south-west to the Farming Guild beginner wing.",
                        TransportCategory.FAIRY_RING, TransportVerificationKind.FAIRY_RING_ACCESS));
        options.put(GardenPatchDefinition.FARMING_GUILD_BUSH, guild);
        options.put(GardenPatchDefinition.FARMING_GUILD_CACTUS, guild);
        options.put(GardenPatchDefinition.AL_KHARID_CACTUS, List.of(new TransportOption(1, "Amulet of glory to Al Kharid",
                "Choose Al Kharid on a charged glory, then run north to the cactus patch.", TransportCategory.JEWELLERY, TransportVerificationKind.AMULET_OF_GLORY)));
        options.put(GardenPatchDefinition.CHAMPIONS_GUILD_BUSH, List.of(new TransportOption(1, "Combat bracelet to Champions' Guild",
                "Teleport outside the Champions' Guild, then walk south-west to the bush patch. Do not enter the guild.",
                TransportCategory.JEWELLERY, TransportVerificationKind.COMBAT_BRACELET),
                spell(2, TeleportSpellDefinition.VARROCK_TELEPORT, "Choose the Varrock town-square destination, then run south-west to the bush outside the Champions' Guild. If redirected to the Exchange, choose Varrock explicitly or walk from there.")));
        options.put(GardenPatchDefinition.RIMMINGTON_BUSH, List.of(spell(1, TeleportSpellDefinition.FALADOR_TELEPORT,
                "Teleport to Falador, leave by the south gate and follow the roads south-west to the Rimmington bush patch."),
                new TransportOption(90, "Skills necklace to Crafting Guild, then Rimmington",
                        "Choose Crafting Guild on a charged skills necklace. Stay outside the guild, then walk south-east to Rimmington and the bush beside the house portal.",
                        TransportCategory.JEWELLERY, TransportVerificationKind.SKILLS_NECKLACE),
                new TransportOption(95, "Configured Rimmington house, then bush",
                        "Teleport to your configured Rimmington house, choose the outside arrival or leave through its portal, then walk to the bush beside the house portal.",
                        TransportCategory.PLAYER_OWNED_HOUSE, TransportVerificationKind.POH_RIMMINGTON_HOUSE_LOCATION)));
        options.put(GardenPatchDefinition.ARDOUGNE_BUSH, List.of(spell(1, TeleportSpellDefinition.ARDOUGNE_TELEPORT,
                "Teleport to Ardougne, then travel south-west to the bush north of the monastery."),
                new TransportOption(2, "Fairy ring DJP to Ardougne bush", "Travel to an accessible fairy ring, use DJP, then run west to the bush near Ardougne Monastery.",
                        TransportCategory.FAIRY_RING, TransportVerificationKind.FAIRY_RING_ACCESS),
                new TransportOption(90,"Ardougne cloak to monastery","Choose Monastery on an Ardougne cloak, then walk north-east to the bush just north of the monastery.",
                        TransportCategory.ITEM,TransportVerificationKind.ARDOUGNE_CLOAK_MONASTERY)));
        for(GardenPatchDefinition patch:List.of(GardenPatchDefinition.FARMING_GUILD_BUSH,GardenPatchDefinition.FARMING_GUILD_CACTUS))
        { PlantedSpiritTransportOptions.append(options,patch,com.farmingnavigator.routing.spirit.PlantedSpiritTree.FARMING_GUILD,"Approach the selected Guild patch; its entry tier is checked separately."); }
        ConstructionCapeTransportOptions.append(options,GardenPatchDefinition.RIMMINGTON_BUSH,TransportVerificationKind.CONSTRUCTION_CAPE_RIMMINGTON,"Rimmington","walk to the bush beside the house portal.");
        List<TransportOption> rimmington=new java.util.ArrayList<>(options.get(GardenPatchDefinition.RIMMINGTON_BUSH));
        rimmington.add(new TransportOption(97,"Crafting cape to Guild, then Rimmington",
                "Teleport to the Crafting Guild using a Crafting cape or ordinary max cape, leave the guild, then walk south-east to Rimmington and the bush beside the house portal.",
                TransportCategory.ITEM,TransportVerificationKind.CRAFTING_CAPE));
        options.put(GardenPatchDefinition.RIMMINGTON_BUSH,List.copyOf(rimmington));
        return Collections.unmodifiableMap(options);
    }
    private TransportOption spell(int rank, TeleportSpellDefinition spell, String instructions)
    {
        return new TransportOption(rank, spell.getDisplayName(), instructions, TransportCategory.SPELL, TransportVerificationKind.TELEPORT_SPELL, spell);
    }
}
