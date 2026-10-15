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
                "Teleport to Falador, leave through the north gate and follow the road north-west to the Taverley tree patch.")));
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
                "Teleport to Musa Point on Karamja and travel west to the fruit tree patch north of Brimhaven.", TransportVerificationKind.AMULET_OF_GLORY)));
        options.put(TreePatchDefinition.GNOME_STRONGHOLD_TREE, List.of(item(1, "Wealth to Exchange, then spirit tree",
                "Teleport to the Grand Exchange, walk to its spirit tree, travel to Gnome Stronghold, then walk south-west to the regular tree patch.", TransportVerificationKind.WEALTH_SPIRIT_TREE)));
        options.put(TreePatchDefinition.GNOME_STRONGHOLD_FRUIT, List.of(item(1, "Wealth to Exchange, then spirit tree",
                "Teleport to the Grand Exchange, walk to its spirit tree, travel to Gnome Stronghold, then walk east to the fruit tree patch.", TransportVerificationKind.WEALTH_SPIRIT_TREE)));
        options.put(TreePatchDefinition.TREE_GNOME_VILLAGE_FRUIT, List.of(item(1, "Wealth to Exchange, then spirit tree",
                "Teleport to the Grand Exchange, use its spirit tree to Tree Gnome Village, follow Elkoy out of the maze, then walk south-west to the fruit tree patch.", TransportVerificationKind.WEALTH_SPIRIT_TREE)));
        options.put(TreePatchDefinition.LLETYA_FRUIT, List.of(item(1, "Teleport crystal to Lletya",
                "Choose Lletya on a charged or eternal teleport crystal, then walk south to the fruit tree patch.", TransportVerificationKind.LLETYA_CRYSTAL)));
        List<TransportOption> fossil = List.of(item(1, "Digsite pendant, then barge",
                "Choose the base Digsite destination, quick-travel with the barge guard to Fossil Island, then walk north to the hardwood patches. This route does not require the separate pendant machine destination or mushtrees.", TransportVerificationKind.DIGSITE_BARGE));
        options.put(TreePatchDefinition.FOSSIL_ISLAND_EAST, fossil);
        options.put(TreePatchDefinition.FOSSIL_ISLAND_MIDDLE, fossil);
        options.put(TreePatchDefinition.FOSSIL_ISLAND_WEST, fossil);
        options.put(TreePatchDefinition.AVIUM_SAVANNAH_HARDWOOD, List.of(new TransportOption(1,
                "Fairy ring AJP to Avium Savannah", "Travel to an accessible fairy ring, use AJP, then run south-east to the hardwood patch at Locus Oasis.",
                TransportCategory.FAIRY_RING, TransportVerificationKind.FAIRY_RING_AJP)));
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
}
