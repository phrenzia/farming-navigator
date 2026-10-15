package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.loadout.LoadoutService;
import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.routing.requirement.ChargedTeleportBudgetRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RuneCost;
import com.farmingnavigator.routing.requirement.RuneCostRequirement;
import com.farmingnavigator.routing.requirement.RuneSupply;
import com.farmingnavigator.routing.requirement.RunePreparation;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationKind;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.routing.tablet.TeleportTabletDefinition;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.runelite.api.gameval.ItemID;

/** Aggregates the recommended legs only; fallbacks are alternatives, not extra travel. */
public final class RunTransportResourcePlanner
{
    public RunTransportResources evaluate(List<? extends RunTravelStop> stops, RequirementContext context)
    {
        return evaluate(stops, context, RuneCost.builder().build());
    }

    public RunTransportResources evaluate(List<? extends RunTravelStop> stops, RequirementContext context, RuneCost farmingRunes)
    {
        RuneCost.Builder runes = RuneCost.builder();
        farmingRunes.getQuantities().forEach(runes::add);
        Map<ChargedTeleportItemDefinition, Integer> jewellery = new EnumMap<>(ChargedTeleportItemDefinition.class);
        Map<Consumable, Integer> consumables = new EnumMap<>(Consumable.class);
        List<String> notes = new ArrayList<>();
        int houseVisits = 0;
        for (RunTravelStop stop : stops)
        {
            if (stop.isLocalWalk()) { continue; }
            if (stop.getRecommended() == null)
            {
                notes.add("Choose transport for " + stop.getTravelLabel() + ".");
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
        Set<Integer> bankItems = new HashSet<>();
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
        RunePreparation runePreparation = null;
        if (!totalRunes.isEmpty())
        {
            runePreparation = new RuneCostRequirement(
                    "All recommended spell runes (travel, house entry and farming)", totalRunes).evaluatePreparation(context);
            RequirementResult runeCheck = runePreparation.getResult();
            checks.add(runeCheck);
            if (runeCheck.requiresBankWithdrawal() && context != null && context.getStorageSnapshot() != null)
            {
                if (runePreparation.isBankedSource())
                {
                    bankItems.add(runePreparation.getSource().getItemId());
                }
                if (runePreparation.isWithdrawPouch())
                {
                    for (int pouchId : new int[]{ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER,
                            ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER})
                    {
                        if (context.getStorageSnapshot().getVerifiedBankQuantity(pouchId) > 0) { bankItems.add(pouchId); }
                    }
                }
                for (RuneSupply supply : RuneSupply.values())
                {
                    if (supply.getSuppliedKinds().stream().anyMatch(kind -> totalRunes.getQuantity(kind) > 0)
                            && context.getStorageSnapshot().getVerifiedBankQuantity(supply.getItemId()) > 0)
                    {
                        bankItems.add(supply.getItemId());
                    }
                }
            }
        }
        if (houseVisits > 0)
        {
            checks.add(context == null ? RequirementResult.dataRequired("Selected POH entry", "Account and house data required.")
                    : houseEntryMode == HerbHouseEntryMode.SPELL
                    ? TeleportSpellDefinition.TELEPORT_TO_HOUSE.createRequirement().evaluate(context)
                    : TeleportTabletDefinition.TELEPORT_TO_HOUSE.createRequirement().evaluate(context));
        }
        jewellery.forEach((family, uses) -> {
            RequirementResult check = new ChargedTeleportBudgetRequirement(family, uses).evaluate(context);
            checks.add(check);
            if (check.requiresBankWithdrawal() && context != null && context.getStorageSnapshot() != null)
            {
                family.getVariants().forEach(variant -> {
                    if (context.getStorageSnapshot().getVerifiedBankQuantity(variant.getItemId()) > 0)
                    {
                        bankItems.add(variant.getItemId());
                    }
                });
            }
        });
        List<SupplyRequirement> supplies = new ArrayList<>();
        consumables.forEach((item, quantity) -> supplies.add(new SupplyRequirement(item.itemId, item.name, quantity,
                false, "One consumed per recommended teleport; fallback routes are excluded.")));
        return new RunTransportResources(checks, notes,
                new LoadoutService().evaluate(supplies, context == null ? null : context.getStorageSnapshot()), houseEntryMode, bankItems,
                runePreparation);
    }

    private enum Consumable
    {
        HOUSE_TABLET(ItemID.POH_TABLET_TELEPORTTOHOUSE, "Teleport to house tablet"),
        ARDOUGNE_TABLET(ItemID.POH_TABLET_ARDOUGNETELEPORT, "Ardougne teleport tablet"),
        STONY_BASALT(ItemID.STRONGHOLD_TELEPORT_BASALT, "Stony basalt"),
        ICY_BASALT(ItemID.WEISS_TELEPORT_BASALT, "Icy basalt"),
        MOS_LE_HARMLESS_SCROLL(ItemID.TELEPORTSCROLL_MOSLES, "Mos le'harmless teleport scroll"),
        TAI_BWO_WANNAI_SCROLL(ItemID.TELEPORTSCROLL_TAIBWO, "Tai bwo wannai teleport scroll");

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
            case TAI_BWO_WANNAI_SCROLL: return Consumable.TAI_BWO_WANNAI_SCROLL;
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
            case WEALTH_SPIRIT_TREE: return ChargedTeleportItemDefinition.RING_OF_WEALTH;
            case DIGSITE_BARGE: return ChargedTeleportItemDefinition.DIGSITE_PENDANT;
            case LLETYA_CRYSTAL: return ChargedTeleportItemDefinition.TELEPORT_CRYSTAL;
            case PRIFDDINAS_CRYSTAL: return ChargedTeleportItemDefinition.TELEPORT_CRYSTAL;
            case COMBAT_BRACELET: return ChargedTeleportItemDefinition.COMBAT_BRACELET;
            default: return null;
        }
    }
}
