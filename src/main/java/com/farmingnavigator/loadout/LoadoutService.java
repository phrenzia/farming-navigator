package com.farmingnavigator.loadout;

import com.farmingnavigator.storage.StorageSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Quantity-aware supply checks for any run type, independent of Swing and RuneLite client state. */
public final class LoadoutService
{
    public List<SupplyCheck> evaluate(List<SupplyRequirement> requirements, StorageSnapshot storage)
    {
        List<SupplyCheck> checks = new ArrayList<>();
        for (SupplyRequirement requirement : requirements)
        {
            int id = requirement.getItemId();
            long remaining = requirement.getQuantity();
            long inventory = storage == null ? 0 : Math.max(0, storage.getInventoryQuantity(id));
            remaining -= Math.min(remaining, inventory);
            long equipped = storage == null ? 0 : Math.max(0, storage.getEquipmentQuantity(id));
            remaining -= Math.min(remaining, equipped);
            if (remaining == 0)
            {
                checks.add(new SupplyCheck(requirement, inventory >= requirement.getQuantity()
                        ? SupplyState.ALREADY_CARRIED : SupplyState.ALREADY_EQUIPPED, 0, 0));
            }
            else if (storage == null || !storage.isBankVerified())
            {
                checks.add(new SupplyCheck(requirement, SupplyState.DATA_REQUIRED, 0, 0));
            }
            else
            {
                long bank = Math.max(0, storage.getVerifiedBankQuantity(id));
                long withdraw = Math.min(remaining, bank);
                long missing = remaining - withdraw;
                checks.add(new SupplyCheck(requirement, missing == 0 ? SupplyState.AVAILABLE_FROM_BANK : SupplyState.MISSING,
                        withdraw, missing));
            }
        }
        return Collections.unmodifiableList(checks);
    }
}
