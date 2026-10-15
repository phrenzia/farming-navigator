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
            long remaining = requirement.getQuantity();
            long inventory = quantity(requirement, storage, 0);
            remaining -= Math.min(remaining, inventory);
            long equipped = quantity(requirement, storage, 1);
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
                long bank = quantity(requirement, storage, 2);
                long withdraw = Math.min(remaining, bank);
                long missing = remaining - withdraw;
                checks.add(new SupplyCheck(requirement, missing == 0 ? SupplyState.AVAILABLE_FROM_BANK : SupplyState.MISSING,
                        withdraw, missing));
            }
        }
        return Collections.unmodifiableList(checks);
    }

    private long quantity(SupplyRequirement requirement, StorageSnapshot storage, int location)
    {
        if (storage == null) { return 0; }
        long quantity = 0;
        for (int id : requirement.getAcceptedItemIds())
        {
            long amount = Math.max(0, location == 0 ? storage.getInventoryQuantity(id)
                    : location == 1 ? storage.getEquipmentQuantity(id) : storage.getVerifiedBankQuantity(id));
            quantity = quantity > Long.MAX_VALUE - amount ? Long.MAX_VALUE : quantity + amount;
        }
        return quantity;
    }
}
