package com.farmingnavigator.economics;

import java.util.List;
import java.util.OptionalLong;

/** Potential consumption value, including owned materials. Not a buying list, profit or total-run cost. */
public final class FarmingMaterialEstimate
{
    private final List<Line> lines;
    private final List<String> notes;
    private final OptionalLong knownSubtotal;
    FarmingMaterialEstimate(List<Line> lines, List<String> notes)
    {
        this.lines = List.copyOf(lines); this.notes = List.copyOf(notes);
        long subtotal = 0; boolean overflow = false;
        for (Line line : lines)
        {
            if (line.getValue().isPresent())
            {
                try { subtotal = Math.addExact(subtotal, line.getValue().getAsLong()); }
                catch (ArithmeticException ex) { overflow = true; }
            }
        }
        knownSubtotal = overflow ? OptionalLong.empty() : OptionalLong.of(subtotal);
    }
    public List<Line> getLines() { return lines; }
    public List<String> getNotes() { return notes; }
    public OptionalLong getKnownSubtotal() { return knownSubtotal; }
    public long getUnknownPriceCount() { return lines.stream().filter(line -> !line.getValue().isPresent()).count(); }
    public static final class Line
    {
        private final int itemId;
        private final String name;
        private final long quantity;
        private final OptionalLong value;
        Line(int itemId, String name, long quantity, long unitPrice)
        {
            this.itemId = itemId; this.name = name; this.quantity = quantity;
            OptionalLong total = OptionalLong.empty();
            if (unitPrice > 0)
            {
                try { total = OptionalLong.of(Math.multiplyExact(quantity, unitPrice)); }
                catch (ArithmeticException ex) { /* An overflowing quote is unknown, never a negative value. */ }
            }
            value = total;
        }
        // Preserve an unknown/overflowed quote exactly when presenting a component within a combined worksheet.
        Line(Line source,String name)
        {
            this.itemId=source.itemId; this.name=name; this.quantity=source.quantity; this.value=source.value;
        }
        public int getItemId() { return itemId; }
        public String getName() { return name; }
        public long getQuantity() { return quantity; }
        public OptionalLong getValue() { return value; }
    }
}
