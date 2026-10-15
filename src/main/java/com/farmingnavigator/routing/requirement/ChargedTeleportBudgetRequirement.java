package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition.Variant;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/** Checks a shared jewellery charge pool, rather than treating every teleport independently. */
public final class ChargedTeleportBudgetRequirement implements RouteRequirement
{
    private final ChargedTeleportItemDefinition definition;
    private final int uses;

    public ChargedTeleportBudgetRequirement(ChargedTeleportItemDefinition definition, int uses)
    {
        this.definition = Objects.requireNonNull(definition);
        if (uses <= 0)
        {
            throw new IllegalArgumentException("Run teleport uses must be positive.");
        }
        this.uses = uses;
    }

    @Override
    public String getDisplayName()
    {
        return definition.getDisplayName() + " for " + uses + " run teleports";
    }

    @Override
    public RequirementResult evaluate(RequirementContext context)
    {
        if (context == null || context.getTravelCapabilitySnapshot() == null
                || !context.getTravelCapabilitySnapshot().isLoggedIn())
        {
            return RequirementResult.dataRequired(getDisplayName(), "Log in to verify shared teleport charges.");
        }
        TravelCapabilitySnapshot travel = context.getTravelCapabilitySnapshot();
        long remaining = uses;
        for (Variant variant : definition.getVariants())
        {
            long count = (long) travel.getInventoryQuantities().getOrDefault(variant.getItemId(), 0)
                    + travel.getEquipmentQuantities().getOrDefault(variant.getItemId(), 0);
            if (count > 0)
            {
                remaining = consumeCapacity(remaining, count, variant);
            }
        }
        if (remaining == 0)
        {
            return RequirementResult.satisfied(getDisplayName(),
                    "READY NOW - carried " + definition.getDisplayName() + " charges cover all " + uses + " uses.");
        }
        StorageSnapshot storage = context.getStorageSnapshot();
        if (storage == null || !storage.isBankVerified())
        {
            return RequirementResult.dataRequired(getDisplayName(),
                    "Carried charges do not cover the run; open a bank to check additional jewellery.");
        }
        List<Variant> variants = new ArrayList<>(definition.getVariants());
        variants.sort(Comparator.comparing(Variant::isUnlimited).reversed()
                .thenComparing(Comparator.comparingInt(Variant::getCharges).reversed()));
        StringJoiner withdrawals = new StringJoiner(", ");
        for (Variant variant : variants)
        {
            long count = Math.max(0, storage.getVerifiedBankQuantity(variant.getItemId()));
            if (count == 0 || remaining == 0)
            {
                continue;
            }
            long needed = variant.isUnlimited() ? 1 : (remaining + variant.getCharges() - 1) / variant.getCharges();
            long withdraw = Math.min(count, needed);
            remaining = consumeCapacity(remaining, withdraw, variant);
            withdrawals.add(withdraw + " x " + variant.getDisplayName());
        }
        if (remaining == 0)
        {
            return RequirementResult.availableFromBank(getDisplayName(),
                    "AVAILABLE FROM BANK - withdraw " + withdrawals + " to cover " + uses + " run teleports.");
        }
        return RequirementResult.blocked(getDisplayName(),
                "MISSING - carried items and verified bank lack " + remaining + " "
                        + definition.getDisplayName() + " charges for the run.");
    }

    private long consumeCapacity(long remaining, long count, Variant variant)
    {
        if (variant.isUnlimited())
        {
            return 0;
        }
        // Cap count before multiplying, so pathological quantities cannot overflow.
        long useful = Math.min(count, (remaining + variant.getCharges() - 1) / variant.getCharges());
        return Math.max(0, remaining - useful * variant.getCharges());
    }
}
