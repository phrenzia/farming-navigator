package com.farmingnavigator.transport;

import com.farmingnavigator.farming.tree.TreePatchDefinition;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Small verified catalogue; rank expresses a suggestion, not measured fastest travel. */
public final class TreePatchTransportService
{
    public Map<TreePatchDefinition, List<TransportOption>> getAllOptions()
    {
        Map<TreePatchDefinition, List<TransportOption>> options = new EnumMap<>(TreePatchDefinition.class);
        options.put(TreePatchDefinition.VARROCK_TREE, List.of(
                spell(1, TeleportSpellDefinition.VARROCK_TELEPORT,
                        "Use the Varrock town-square destination and run north to the palace tree patch. If redirected to the Grand Exchange, choose Varrock explicitly or walk from the Exchange."),
                item(2, "Ring of wealth to Grand Exchange", "Teleport to the Grand Exchange and run east to the Varrock Palace tree patch.",
                        TransportVerificationKind.RING_OF_WEALTH)));
        options.put(TreePatchDefinition.LUMBRIDGE_TREE, List.of(spell(1, TeleportSpellDefinition.LUMBRIDGE_TELEPORT,
                "Teleport to Lumbridge Castle and run west outside the castle to the tree patch.")));
        options.put(TreePatchDefinition.FALADOR_TREE, List.of(spell(1, TeleportSpellDefinition.FALADOR_TELEPORT,
                "Teleport to Falador and run north-east into Falador Park to the tree patch."),
                item(2, "Ring of wealth to Falador Park", "Teleport to Falador Park and walk to the tree patch.", TransportVerificationKind.RING_OF_WEALTH)));
        options.put(TreePatchDefinition.TAVERLEY_TREE, List.of(spell(1, TeleportSpellDefinition.FALADOR_TELEPORT,
                "Teleport to Falador, leave through the north gate and follow the road north-west to the Taverley tree patch."),
                item(90, "Games necklace to Burthorpe, then Taverley",
                        "Choose Burthorpe on a charged games necklace, then follow the roads south through Taverley to the tree patch beside Alain.",
                        TransportVerificationKind.GAMES_NECKLACE),
                house(95, "Configured Taverley house, then tree patch",
                        "Teleport to your configured Taverley house, choose the outside arrival or leave through its portal, then walk south-east to the tree patch beside Alain.",
                        TransportVerificationKind.POH_TAVERLEY_HOUSE_LOCATION)));
        options.put(TreePatchDefinition.CATHERBY_FRUIT, List.of(spell(1, TeleportSpellDefinition.CATHERBY_TELEPORT,
                "Teleport to Catherby and run east past the fishing spots to the fruit tree patch."),
                spell(2, TeleportSpellDefinition.CAMELOT_TELEPORT,
                        "Teleport to Camelot and travel south-east through Catherby to the fruit tree patch.")));
        List<TransportOption> guild = List.of(item(1, "Skills necklace to Farming Guild",
                "Teleport to the Farming Guild and enter the appropriate tree-patch tier; maintain any required Farming boost.", TransportVerificationKind.SKILLS_NECKLACE));
        options.put(TreePatchDefinition.FARMING_GUILD_TREE, guild);
        options.put(TreePatchDefinition.FARMING_GUILD_FRUIT, guild);
        options.put(TreePatchDefinition.FARMING_GUILD_CELASTRUS, guild);
        options.put(TreePatchDefinition.FARMING_GUILD_REDWOOD, guild);
        options.put(TreePatchDefinition.BRIMHAVEN_FRUIT, List.of(item(1, "Amulet of glory to Karamja",
                "Teleport to Musa Point on Karamja and travel west to the fruit tree patch north of Brimhaven.", TransportVerificationKind.AMULET_OF_GLORY),
                house(95, "Configured Brimhaven house, then fruit tree",
                        "Teleport to your configured Brimhaven house, choose the outside arrival or leave through its portal, then walk north to the fruit tree patch.",
                        TransportVerificationKind.POH_BRIMHAVEN_HOUSE_LOCATION)));
        options.put(TreePatchDefinition.GNOME_STRONGHOLD_TREE, List.of(item(1, "Wealth to Exchange, then spirit tree",
                "Teleport to the Grand Exchange, walk to its spirit tree, travel to Gnome Stronghold, then walk south-west to the regular tree patch.", TransportVerificationKind.WEALTH_SPIRIT_TREE)));
        options.put(TreePatchDefinition.GNOME_STRONGHOLD_FRUIT, List.of(item(1, "Wealth to Exchange, then spirit tree",
                "Teleport to the Grand Exchange, walk to its spirit tree, travel to Gnome Stronghold, then walk east to the fruit tree patch.", TransportVerificationKind.WEALTH_SPIRIT_TREE)));
        options.put(TreePatchDefinition.TREE_GNOME_VILLAGE_FRUIT, List.of(item(1, "Wealth to Exchange, then spirit tree",
                "Teleport to the Grand Exchange, use its spirit tree to Tree Gnome Village, follow Elkoy out of the maze, then walk south-west to the fruit tree patch.", TransportVerificationKind.WEALTH_SPIRIT_TREE)));
        options.put(TreePatchDefinition.LLETYA_FRUIT, List.of(item(1, "Teleport crystal to Lletya",
                "Choose Lletya on a charged or eternal teleport crystal, then walk south to the fruit tree patch.", TransportVerificationKind.LLETYA_CRYSTAL)));
        options.put(TreePatchDefinition.PRIFDDINAS_CRYSTAL, List.of(item(1, "Teleport crystal to Prifddinas",
                "Choose Prifddinas on a charged or eternal teleport crystal, then walk north-east to the crystal tree patch.", TransportVerificationKind.PRIFDDINAS_CRYSTAL),
                house(95, "Configured Prifddinas house, then crystal tree",
                        "Teleport to your configured Prifddinas house, choose the outside arrival or leave through its portal, then walk north-east to the crystal tree patch.",
                        TransportVerificationKind.POH_PRIFDDINAS_HOUSE_LOCATION)));
        options.put(TreePatchDefinition.TAI_BWO_WANNAI_CALQUAT, List.of(
                new TransportOption(1, "Tai bwo wannai teleport scroll",
                        "Use a loose Tai bwo wannai teleport scroll, then run north to the calquat patch. Master scroll book contents are not checked.",
                        TransportCategory.ITEM, TransportVerificationKind.TAI_BWO_WANNAI_SCROLL),
                new TransportOption(2, "Fairy ring CKR to Tai Bwo Wannai",
                        "Travel to an accessible fairy ring, use CKR, then run north through Tai Bwo Wannai to the calquat patch.",
                        TransportCategory.FAIRY_RING, TransportVerificationKind.FAIRY_RING_ACCESS),
                house(95, "Configured Brimhaven house, then calquat patch",
                        "Teleport to your configured Brimhaven house, choose the outside arrival or leave through its portal, then follow the road south-east to the calquat patch north of Tai Bwo Wannai.",
                        TransportVerificationKind.POH_BRIMHAVEN_HOUSE_LOCATION)));
        List<TransportOption> fossil = List.of(item(1, "Digsite pendant, then barge",
                "Choose the base Digsite destination, quick-travel with the barge guard to Fossil Island, then walk north to the hardwood patches. This route does not require the separate pendant machine destination or mushtrees.", TransportVerificationKind.DIGSITE_BARGE));
        options.put(TreePatchDefinition.FOSSIL_ISLAND_EAST, fossil);
        options.put(TreePatchDefinition.FOSSIL_ISLAND_MIDDLE, fossil);
        options.put(TreePatchDefinition.FOSSIL_ISLAND_WEST, fossil);
        options.put(TreePatchDefinition.AVIUM_SAVANNAH_HARDWOOD, List.of(new TransportOption(1,
                "Fairy ring AJP to Avium Savannah", "Travel to an accessible fairy ring, use AJP, then run south-east to the hardwood patch at Locus Oasis.",
                TransportCategory.FAIRY_RING, TransportVerificationKind.FAIRY_RING_AJP)));
        PlantedSpiritTransportOptions.append(options,TreePatchDefinition.BRIMHAVEN_FRUIT,com.farmingnavigator.routing.spirit.PlantedSpiritTree.BRIMHAVEN,"Travel west to the Brimhaven fruit-tree patch.");
        for(TreePatchDefinition patch:List.of(TreePatchDefinition.FARMING_GUILD_TREE,TreePatchDefinition.FARMING_GUILD_FRUIT,TreePatchDefinition.FARMING_GUILD_CELASTRUS,TreePatchDefinition.FARMING_GUILD_REDWOOD))
        { PlantedSpiritTransportOptions.append(options,patch,com.farmingnavigator.routing.spirit.PlantedSpiritTree.FARMING_GUILD,"Approach the selected Guild patch; its entry tier is checked separately."); }
        ConstructionCapeTransportOptions.append(options,TreePatchDefinition.TAVERLEY_TREE,TransportVerificationKind.CONSTRUCTION_CAPE_TAVERLEY,"Taverley","walk south-east to the tree patch beside Alain.");
        ConstructionCapeTransportOptions.append(options,TreePatchDefinition.BRIMHAVEN_FRUIT,TransportVerificationKind.CONSTRUCTION_CAPE_BRIMHAVEN,"Brimhaven","walk north to the fruit-tree patch.");
        ConstructionCapeTransportOptions.append(options,TreePatchDefinition.TAI_BWO_WANNAI_CALQUAT,TransportVerificationKind.CONSTRUCTION_CAPE_BRIMHAVEN,"Brimhaven","follow the road south-east to the calquat patch north of Tai Bwo Wannai.");
        ConstructionCapeTransportOptions.append(options,TreePatchDefinition.PRIFDDINAS_CRYSTAL,TransportVerificationKind.CONSTRUCTION_CAPE_PRIFDDINAS,"Prifddinas","walk north-east to the crystal-tree patch.");
        return Collections.unmodifiableMap(options);
    }
    private TransportOption spell(int rank, TeleportSpellDefinition spell, String instructions)
    {
        return new TransportOption(rank, spell.getDisplayName(), instructions, TransportCategory.SPELL,
                TransportVerificationKind.TELEPORT_SPELL, spell);
    }
    private TransportOption item(int rank, String label, String instructions, TransportVerificationKind kind)
    {
        return new TransportOption(rank, label, instructions, TransportCategory.JEWELLERY, kind);
    }
    private TransportOption house(int rank, String label, String instructions, TransportVerificationKind kind)
    {
        return new TransportOption(rank, label, instructions, TransportCategory.PLAYER_OWNED_HOUSE, kind);
    }
}
