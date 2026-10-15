package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.loadout.LoadoutService;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/** Conservative standard-axe catalogue. Inventory tools do not need their wielding Attack level. */
public final class TreeAxePreparation
{
    private static final Map<Integer, Integer> LEVELS = Map.of(ItemID.BRONZE_AXE, 1, ItemID.IRON_AXE, 1,
            ItemID.STEEL_AXE, 6, ItemID.BLACK_AXE, 11, ItemID.MITHRIL_AXE, 21, ItemID.ADAMANT_AXE, 31,
            ItemID.RUNE_AXE, 41, ItemID.DRAGON_AXE, 61);
    private TreeAxePreparation() { }
    public static SupplyCheck evaluate(AccountState account, StorageSnapshot storage)
    {
        int level = account == null || !account.isLoggedIn() ? -1 : account.getBoostedWoodcuttingLevel();
        int[] alternatives = LEVELS.entrySet().stream().filter(entry -> entry.getKey() != ItemID.BRONZE_AXE
                && entry.getValue() <= level).mapToInt(Map.Entry::getKey).toArray();
        String note = "For celastrus bark or crystal-tree chopping. Checks standard bronze-to-dragon axes against current boosted Woodcutting; other axe variants and tree-specific chopping requirements are not checked.";
        if (level < 1) { note = (level == 0 ? "Restore Woodcutting to at least 1. " : "Woodcutting level unavailable; verify axe usability manually. ") + note; }
        SupplyRequirement requirement = new SupplyRequirement(ItemID.BRONZE_AXE, "Usable standard axe", 1, false, note, alternatives);
        return new LoadoutService().evaluate(List.of(requirement), level < 1 ? null : storage).get(0);
    }
}
