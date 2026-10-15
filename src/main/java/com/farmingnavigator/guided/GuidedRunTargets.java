package com.farmingnavigator.guided;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.requirement.RuneSupply;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbHouseEntryMode;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.transport.TransportCategory;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/** Cached semantic targets; renderers only test membership and draw. */
public final class GuidedRunTargets
{
    private final Set<Integer> inventoryItems;
    private final Set<Integer> bankItems;
    private final WorldPoint destination;
    private final String destinationName;
    private final HerbPatchDefinition scenePatch;
    private final boolean leprechaun;

    private GuidedRunTargets(Set<Integer> inventoryItems, Set<Integer> bankItems, WorldPoint destination,
            String destinationName, HerbPatchDefinition scenePatch, boolean leprechaun)
    {
        this.inventoryItems = Collections.unmodifiableSet(new HashSet<>(inventoryItems));
        this.bankItems = Collections.unmodifiableSet(new HashSet<>(bankItems));
        this.destination = destination;
        this.destinationName = destinationName;
        this.scenePatch = scenePatch;
        this.leprechaun = leprechaun;
    }

    public Set<Integer> getInventoryItems() { return inventoryItems; }
    public Set<Integer> getBankItems() { return bankItems; }
    public WorldPoint getDestination() { return destination; }
    public String getDestinationName() { return destinationName; }
    public HerbPatchDefinition getScenePatch() { return scenePatch; }
    public boolean isLeprechaun() { return leprechaun; }

    public static GuidedRunTargets empty()
    {
        return new GuidedRunTargets(Collections.emptySet(), Collections.emptySet(), null, "", null, false);
    }

    public static GuidedRunTargets from(GuidedRunSnapshot session, HerbRunPlan plan, TravelCapabilitySnapshot travel)
    {
        if (session == null || !session.isActive())
        {
            return empty();
        }
        Set<Integer> inventory = new HashSet<>();
        Set<Integer> bank = new HashSet<>();
        WorldPoint destination = null;
        String name = "";
        HerbHouseEntryMode houseEntryMode = plan == null || plan.getTransportResources() == null
                ? null : plan.getTransportResources().getHouseEntryMode();
        switch (session.getStep())
        {
            case PREPARE:
                if (plan != null && plan.getCrop() == session.getCrop())
                {
                    plan.getSupplies().forEach(check -> {
                        inventory.add(check.getRequirement().getItemId());
                        if (check.getWithdrawQuantity() > 0)
                        {
                            bank.add(check.getRequirement().getItemId());
                        }
                    });
                    plan.getStops().forEach(stop -> addTransport(inventory, bank, stop.getRecommended(), travel, houseEntryMode));
                    if (plan.getTransportResources() != null)
                    {
                        plan.getTransportResources().getConsumables().forEach(check -> {
                            inventory.add(check.getRequirement().getItemId());
                            if (check.getWithdrawQuantity() > 0)
                            {
                                bank.add(check.getRequirement().getItemId());
                            }
                        });
                    }
                }
                break;
            case TRAVEL:
                addTransport(inventory, bank, session.getStop().getRecommended(), travel, houseEntryMode);
                destination = HerbPatchDestination.valueOf(session.getStop().getPatch().name()).getLocation();
                name = session.getStop().getPatch().getDisplayName() + " farming area";
                break;
            case RAKE: inventory.add(ItemID.RAKE); break;
            case HARVEST:
            case CLEAR: inventory.add(ItemID.SPADE); break;
            case PLANT:
                inventory.add(session.getCrop().getSeedId());
                inventory.add(ItemID.DIBBER);
                break;
            case COMPOST:
                if (session.getCompost().getItemId() > 0) { inventory.add(session.getCompost().getItemId()); }
                break;
            case CURE: inventory.add(ItemID.PLANT_CURE); break;
            case NOTE_PRODUCE:
                Collections.addAll(inventory, ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_MARENTILL,
                        ItemID.UNIDENTIFIED_TARROMIN, ItemID.UNIDENTIFIED_HARRALANDER, ItemID.UNIDENTIFIED_RANARR,
                        ItemID.UNIDENTIFIED_TOADFLAX, ItemID.UNIDENTIFIED_IRIT, ItemID.UNIDENTIFIED_AVANTOE,
                        ItemID.UNIDENTIFIED_KWUARM, ItemID.UNIDENTIFIED_SNAPDRAGON, ItemID.UNIDENTIFIED_HUASCA,
                        ItemID.UNIDENTIFIED_CADANTINE, ItemID.UNIDENTIFIED_LANTADYME,
                        ItemID.UNIDENTIFIED_DWARF_WEED, ItemID.UNIDENTIFIED_TORSTOL,
                        ItemID.GUAM_LEAF, ItemID.MARENTILL, ItemID.TARROMIN, ItemID.HARRALANDER,
                        ItemID.RANARR_WEED, ItemID.TOADFLAX, ItemID.IRIT_LEAF, ItemID.AVANTOE,
                        ItemID.KWUARM, ItemID.SNAPDRAGON, ItemID.HUASCA, ItemID.CADANTINE,
                        ItemID.LANTADYME, ItemID.DWARF_WEED, ItemID.TORSTOL);
                break;
            default: break;
        }
        HerbPatchDefinition scenePatch = session.getStep() == GuidedRunStep.PREPARE || session.getStep() == GuidedRunStep.WAIT
                ? null : session.getStop().getPatch();
        return new GuidedRunTargets(inventory, bank, destination, name, scenePatch,
                session.getStep() == GuidedRunStep.NOTE_PRODUCE);
    }

    private static void addTransport(Set<Integer> inventory, Set<Integer> bank, VerifiedTransportResult route,
            TravelCapabilitySnapshot travel, HerbHouseEntryMode houseEntryMode)
    {
        if (route == null || !route.isUsableForPlannedRun())
        {
            return;
        }
        Set<Integer> items = new HashSet<>();
        TransportOption option = route.getOption();
        if (option.getCategory() == TransportCategory.PLAYER_OWNED_HOUSE)
        {
            if (houseEntryMode != HerbHouseEntryMode.SPELL)
            {
                items.add(ItemID.POH_TABLET_TELEPORTTOHOUSE);
            }
            if (houseEntryMode != HerbHouseEntryMode.TABLETS)
            {
                addSpell(items, TeleportSpellDefinition.TELEPORT_TO_HOUSE, travel);
            }
        }
        switch (option.getVerificationKind())
        {
            case EXPLORER_RING_UNLIMITED:
                Collections.addAll(items, ItemID.LUMBRIDGE_RING_HARD, ItemID.LUMBRIDGE_RING_ELITE); break;
            case EXPLORER_RING_2_DAILY: items.add(ItemID.LUMBRIDGE_RING_MEDIUM); break;
            case ARDOUGNE_CLOAK_UNLIMITED:
                Collections.addAll(items, ItemID.ARDY_CAPE_ELITE, ItemID.SKILLCAPE_MAX_ARDY); break;
            case KANDARIN_HEADGEAR_UNLIMITED: items.add(ItemID.SEERS_HEADBAND_ELITE); break;
            case KANDARIN_HEADGEAR_3_DAILY: items.add(ItemID.SEERS_HEADBAND_HARD); break;
            case SKILLS_NECKLACE: addJewellery(items, ChargedTeleportItemDefinition.SKILLS_NECKLACE); break;
            case AMULET_OF_GLORY: addJewellery(items, ChargedTeleportItemDefinition.AMULET_OF_GLORY); break;
            case RING_OF_WEALTH: addJewellery(items, ChargedTeleportItemDefinition.RING_OF_WEALTH); break;
            case COMBAT_BRACELET: addJewellery(items, ChargedTeleportItemDefinition.COMBAT_BRACELET); break;
            case ARDOUGNE_TELEPORT_TABLET: items.add(ItemID.POH_TABLET_ARDOUGNETELEPORT); break;
            case STONY_BASALT: items.add(ItemID.STRONGHOLD_TELEPORT_BASALT); break;
            case ICY_BASALT: items.add(ItemID.WEISS_TELEPORT_BASALT); break;
            case ECTOPHIAL:
            case ECTOPHIAL_HARMONY: items.add(ItemID.ECTOPHIAL); break;
            case XERICS_TALISMAN_GLADE: items.add(ItemID.XERIC_TALISMAN); break;
            case RING_OF_ELEMENTS: items.add(ItemID.RING_OF_ELEMENTS_CHARGED); break;
            case QUETZAL_WHISTLE: items.add(ItemID.HG_QUETZALWHISTLE_PERFECTED_INFINITE); break;
            case FARMING_CAPE:
                Collections.addAll(items, ItemID.SKILLCAPE_FARMING, ItemID.SKILLCAPE_FARMING_TRIMMED, ItemID.SKILLCAPE_MAX_WORN); break;
            case FAIRY_RING_ACCESS:
            case FAIRY_RING_AJP:
            case POH_FAIRY_RING:
            case WEISS_LARRY_BOAT:
                Collections.addAll(items, ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF); break;
            case MOS_LE_HARMLESS_HARMONY: items.add(ItemID.TELEPORTSCROLL_MOSLES); break;
            case TELEPORT_SPELL: addSpell(items, option.getTeleportSpellDefinition(), travel); break;
            default: break;
        }
        inventory.addAll(items);
        if (route.getState() == VerifiedTransportState.AVAILABLE_FROM_BANK)
        {
            bank.addAll(items);
        }
    }

    private static void addJewellery(Set<Integer> items, ChargedTeleportItemDefinition family)
    {
        family.getVariants().forEach(variant -> items.add(variant.getItemId()));
    }

    private static void addSpell(Set<Integer> items, TeleportSpellDefinition spell, TravelCapabilitySnapshot travel)
    {
        if (spell == null) { return; }
        boolean pouchUseful = false;
        for (RuneSupply supply : RuneSupply.values())
        {
            if (supply.getSuppliedKinds().stream().anyMatch(kind -> spell.getRuneCost().getQuantity(kind) > 0))
            {
                items.add(supply.getItemId());
                pouchUseful |= travel != null && travel.getStoredRunePouchQuantities().getOrDefault(supply.getItemId(), 0) > 0;
            }
        }
        if (pouchUseful)
        {
            Collections.addAll(items, ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER,
                    ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER);
        }
    }
}
