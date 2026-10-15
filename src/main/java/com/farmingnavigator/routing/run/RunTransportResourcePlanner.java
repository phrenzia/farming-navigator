package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.loadout.LoadoutService;
import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.routing.requirement.ChargedTeleportBudgetRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RuneCost;
import com.farmingnavigator.routing.requirement.RuneCostRequirement;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationKind;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.routing.tablet.TeleportTabletDefinition;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/** Aggregates the recommended legs only; fallbacks are alternatives, not extra travel. */
public final class RunTransportResourcePlanner
{
    public RunTransportResources evaluate(List<HerbRunStop> stops, RequirementContext context)
    {
        RuneCost.Builder runes = RuneCost.builder();
        Map<ChargedTeleportItemDefinition, Integer> jewellery = new EnumMap<>(ChargedTeleportItemDefinition.class);
        Map<Consumable, Integer> consumables = new EnumMap<>(Consumable.class);
        List<String> notes = new ArrayList<>();
        int houseVisits = 0;
        for (HerbRunStop stop : stops)
        {
            if (stop.getRecommended() == null)
            {
                notes.add("Choose transport for " + stop.getPatch().getDisplayName() + ".");
                continue;
            }
            TransportOption option = stop.getRecommended().getOption();
            Consumable consumable = consumable(option.getVerificationKind());
            if (consumable != null)
            {
                consumables.merge(consumable, 1, Integer::sum);
            }
            if (option.getVerificationKind() == TransportVerificationKind.TELEPORT_SPELL
                    && option.getTeleportSpellDefinition() != null)
            {
                option.getTeleportSpellDefinition().getRuneCost().getQuantities().forEach(runes::add);
                continue;
            }
            ChargedTeleportItemDefinition family = jewelleryFamily(option.getVerificationKind());
            if (family != null)
            {
                jewellery.merge(family, 1, Integer::sum);
            }
            else if (option.getCategory() == com.farmingnavigator.transport.TransportCategory.PLAYER_OWNED_HOUSE)
            {
                houseVisits++;
            }
        }
        List<RequirementResult> checks = new ArrayList<>();
        HerbHouseEntryMode houseEntryMode = context == null || context.getConfig() == null
                ? HerbHouseEntryMode.TABLETS : context.getConfig().herbHouseEntryMode();
        if (houseEntryMode == null) { houseEntryMode = HerbHouseEntryMode.TABLETS; }
        if (houseVisits > 0)
        {
            notes.add("POH entry budget: " + houseVisits + " x " + houseEntryMode
                    + ". Change entry supplies in plugin settings if needed.");
            if (houseEntryMode == HerbHouseEntryMode.SPELL)
            {
                int casts = houseVisits;
                TeleportSpellDefinition.TELEPORT_TO_HOUSE.getRuneCost().getQuantities()
                        .forEach((kind, amount) -> runes.add(kind, amount * casts));
            }
            else
            {
                consumables.put(Consumable.HOUSE_TABLET, houseVisits);
            }
        }
        RuneCost totalRunes = runes.build();
        if (!totalRunes.isEmpty())
        {
            checks.add(new RuneCostRequirement("All recommended spell runes (including selected POH entry)", totalRunes).evaluate(context));
        }
        if (houseVisits > 0)
        {
            checks.add(context == null ? RequirementResult.dataRequired("Selected POH entry", "Account and house data required.")
                    : houseEntryMode == HerbHouseEntryMode.SPELL
                    ? TeleportSpellDefinition.TELEPORT_TO_HOUSE.createRequirement().evaluate(context)
                    : TeleportTabletDefinition.TELEPORT_TO_HOUSE.createRequirement().evaluate(context));
        }
        jewellery.forEach((family, uses) -> checks.add(new ChargedTeleportBudgetRequirement(family, uses).evaluate(context)));
        List<SupplyRequirement> supplies = new ArrayList<>();
        consumables.forEach((item, quantity) -> supplies.add(new SupplyRequirement(item.itemId, item.name, quantity,
                false, "One consumed per recommended teleport; fallback routes are excluded.")));
        return new RunTransportResources(checks, notes,
                new LoadoutService().evaluate(supplies, context == null ? null : context.getStorageSnapshot()), houseEntryMode);
    }

    private enum Consumable
    {
        HOUSE_TABLET(ItemID.POH_TABLET_TELEPORTTOHOUSE, "Teleport to house tablet"),
        ARDOUGNE_TABLET(ItemID.POH_TABLET_ARDOUGNETELEPORT, "Ardougne teleport tablet"),
        STONY_BASALT(ItemID.STRONGHOLD_TELEPORT_BASALT, "Stony basalt"),
        ICY_BASALT(ItemID.WEISS_TELEPORT_BASALT, "Icy basalt"),
        MOS_LE_HARMLESS_SCROLL(ItemID.TELEPORTSCROLL_MOSLES, "Mos le'harmless teleport scroll");

        private final int itemId;
        private final String name;

        Consumable(int itemId, String name)
        {
            this.itemId = itemId;
            this.name = name;
        }
    }

    private Consumable consumable(TransportVerificationKind kind)
    {
        switch (kind)
        {
            case ARDOUGNE_TELEPORT_TABLET: return Consumable.ARDOUGNE_TABLET;
            case STONY_BASALT: return Consumable.STONY_BASALT;
            case ICY_BASALT: return Consumable.ICY_BASALT;
            case MOS_LE_HARMLESS_HARMONY: return Consumable.MOS_LE_HARMLESS_SCROLL;
            default: return null;
        }
    }

    private ChargedTeleportItemDefinition jewelleryFamily(TransportVerificationKind kind)
    {
        switch (kind)
        {
            case SKILLS_NECKLACE: return ChargedTeleportItemDefinition.SKILLS_NECKLACE;
            case AMULET_OF_GLORY: return ChargedTeleportItemDefinition.AMULET_OF_GLORY;
            case RING_OF_WEALTH: return ChargedTeleportItemDefinition.RING_OF_WEALTH;
            case COMBAT_BRACELET: return ChargedTeleportItemDefinition.COMBAT_BRACELET;
            default: return null;
        }
    }
}
