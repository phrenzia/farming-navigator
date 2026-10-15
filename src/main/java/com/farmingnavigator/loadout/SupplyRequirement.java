package com.farmingnavigator.loadout;

import java.util.Objects;

public final class SupplyRequirement
{
    private final int itemId;
    private final String name;
    private final long quantity;
    private final boolean optional;
    private final String note;

    public SupplyRequirement(int itemId, String name, long quantity, boolean optional, String note)
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
    }

    public int getItemId() { return itemId; }
    public String getName() { return name; }
    public long getQuantity() { return quantity; }
    public boolean isOptional() { return optional; }
    public String getNote() { return note; }
}
