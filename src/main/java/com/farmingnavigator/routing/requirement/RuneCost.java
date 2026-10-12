package com.farmingnavigator.routing.requirement;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;

public class RuneCost
{
    private final Map<RuneKind, Integer>
            quantities;

    private RuneCost(
            Map<RuneKind, Integer> quantities
    )
    {
        EnumMap<RuneKind, Integer> copy =
                new EnumMap<>(
                        RuneKind.class
                );

        if (quantities != null)
        {
            for (
                    Map.Entry<RuneKind, Integer> entry
                    : quantities.entrySet()
            )
            {
                RuneKind kind =
                        entry.getKey();

                Integer quantity =
                        entry.getValue();

                if (
                        kind == null
                                || quantity == null
                                || quantity <= 0
                )
                {
                    continue;
                }

                copy.merge(
                        kind,
                        quantity,
                        Integer::sum
                );
            }
        }

        this.quantities =
                Collections.unmodifiableMap(
                        copy
                );
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public Map<RuneKind, Integer>
    getQuantities()
    {
        return quantities;
    }

    public int getQuantity(
            RuneKind kind
    )
    {
        return quantities.getOrDefault(
                kind,
                0
        );
    }

    public boolean isEmpty()
    {
        return quantities.isEmpty();
    }

    public String describe()
    {
        if (quantities.isEmpty())
        {
            return "No runes";
        }

        StringJoiner joiner =
                new StringJoiner(
                        ", "
                );

        for (
                RuneKind kind
                : RuneKind.values()
        )
        {
            int quantity =
                    getQuantity(
                            kind
                    );

            if (quantity <= 0)
            {
                continue;
            }

            joiner.add(
                    kind.getDisplayName()
                            + " x"
                            + quantity
            );
        }

        return joiner.toString();
    }

    public static class Builder
    {
        private final EnumMap<RuneKind, Integer>
                quantities =
                new EnumMap<>(
                        RuneKind.class
                );

        public Builder add(
                RuneKind kind,
                int quantity
        )
        {
            if (
                    kind != null
                            && quantity > 0
            )
            {
                quantities.merge(
                        kind,
                        quantity,
                        Integer::sum
                );
            }

            return this;
        }

        public RuneCost build()
        {
            return new RuneCost(
                    quantities
            );
        }
    }
}