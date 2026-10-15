package com.farmingnavigator.loadout;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;

public final class SupplyRequirement
{
    private final int itemId;
    private final String name;
    private final long quantity;
    private final boolean optional;
    private final String note;
    private final Set<Integer> acceptedItemIds;

    public SupplyRequirement(int itemId, String name, long quantity, boolean optional, String note)
    {
        this(itemId, name, quantity, optional, note, new int[0]);
    }

    public SupplyRequirement(int itemId, String name, long quantity, boolean optional, String note, int... equivalentItemIds)
    {
        if (itemId <= 0 || quantity <= 0)
        {
            throw new IllegalArgumentException("Supply item and quantity must be positive.");
        }
        this.itemId = itemId;
        this.name = Objects.requireNonNull(name);
        this.quantity = quantity;
        this.optional = optional;
        this.note = Objects.requireNonNull(note);
        Set<Integer> accepted = new TreeSet<>();
        accepted.add(itemId);
        for (int alternative : equivalentItemIds)
        {
            if (alternative <= 0) { throw new IllegalArgumentException("Supply alternatives must be positive item IDs."); }
            accepted.add(alternative);
        }
        this.acceptedItemIds = Collections.unmodifiableSet(accepted);
    }

    public int getItemId() { return itemId; }
    public String getName() { return name; }
    public long getQuantity() { return quantity; }
    public boolean isOptional() { return optional; }
    public String getNote() { return note; }
    /** Equivalent usable items/forms only; a noted sapling must not be declared usable for planting. */
    public Set<Integer> getAcceptedItemIds() { return acceptedItemIds; }
}
