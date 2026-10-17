package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.loadout.LoadoutService;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/** Conservative verified-axe catalogue. Inventory tools do not need their wielding Attack level. */
public final class TreeAxePreparation
{
    // Community equipment tables: https://oldschool.runescape.wiki/w/Woodcutting_quest
    // Non-crystal felling axes use the same Woodcutting tiers as their ordinary counterparts.
    // Infernal (Firemaking) and crystal (quest/charge) requirements are not established by this table.
    private static final Map<Integer, Integer> LEVELS = Map.ofEntries(
            Map.entry(ItemID.BRONZE_AXE, 1), Map.entry(ItemID.IRON_AXE, 1),
            Map.entry(ItemID.STEEL_AXE, 6), Map.entry(ItemID.BLACK_AXE, 11),
            Map.entry(ItemID.MITHRIL_AXE, 21), Map.entry(ItemID.ADAMANT_AXE, 31),
            Map.entry(ItemID.RUNE_AXE, 41), Map.entry(ItemID.TRAIL_GILDED_AXE, 41),
            Map.entry(ItemID.DRAGON_AXE, 61), Map.entry(ItemID._3A_AXE, 61),
            Map.entry(ItemID.BRONZE_AXE_2H, 1), Map.entry(ItemID.IRON_AXE_2H, 1),
            Map.entry(ItemID.STEEL_AXE_2H, 6), Map.entry(ItemID.BLACK_AXE_2H, 11),
            Map.entry(ItemID.MITHRIL_AXE_2H, 21), Map.entry(ItemID.ADAMANT_AXE_2H, 31),
            Map.entry(ItemID.RUNE_AXE_2H, 41), Map.entry(ItemID.DRAGON_AXE_2H, 61),
            Map.entry(ItemID._3A_AXE_2H, 61));
    private TreeAxePreparation() { }
    public static SupplyCheck evaluate(AccountState account, StorageSnapshot storage)
    {
        int level = account == null || !account.isLoggedIn() ? -1 : account.getBoostedWoodcuttingLevel();
        int[] alternatives = LEVELS.entrySet().stream().filter(entry -> entry.getKey() != ItemID.BRONZE_AXE
                && entry.getValue() <= level).mapToInt(Map.Entry::getKey).sorted().toArray();
        String note = "For celastrus bark or crystal-tree chopping. Checks standard bronze-to-dragon, 3rd age, gilded and non-crystal felling axes against current boosted Woodcutting. Forester's rations and yield/experience effects are not budgeted. Blessed axes only work on undead trees; infernal, crystal, ornamental and other axe variants and tree-specific chopping requirements are not checked.";
        if (level < 1) { note = (level == 0 ? "Restore Woodcutting to at least 1. " : "Woodcutting level unavailable; verify axe usability manually. ") + note; }
        SupplyRequirement requirement = new SupplyRequirement(ItemID.BRONZE_AXE, "Usable axe (verified variants)", 1, false, note, alternatives);
        return new LoadoutService().evaluate(List.of(requirement), level < 1 ? null : storage).get(0);
    }
}
