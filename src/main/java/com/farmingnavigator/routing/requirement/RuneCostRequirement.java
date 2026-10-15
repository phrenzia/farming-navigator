package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import net.runelite.api.gameval.ItemID;

public class RuneCostRequirement
        implements RouteRequirement
{
    private final String displayName;

    private final RuneCost runeCost;

    public RuneCostRequirement(
            String displayName,
            RuneCost runeCost
    )
    {
        this.displayName =
                displayName;

        this.runeCost =
                runeCost == null
                        ? RuneCost.builder().build()
                        : runeCost;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        return evaluatePreparation(context).getResult();
    }

    public RunePreparation evaluatePreparation(RequirementContext context)
    {
        if (
                context == null
                        || context.getAccountState() == null
                        || !context
                        .getAccountState()
                        .isLoggedIn()
                        || context
                        .getTravelCapabilitySnapshot()
                        == null
                        || !context
                        .getTravelCapabilitySnapshot()
                        .isLoggedIn()
        )
        {
            return new RunePreparation(RequirementResult.dataRequired(
                    displayName,
                    "Log in so Farming Navigator can verify rune availability."
            ));
        }

        if (runeCost.isEmpty())
        {
            return new RunePreparation(RequirementResult.satisfied(
                    displayName,
                    "This spell has no rune cost."
            ));
        }

        AccountState accountState =
                context.getAccountState();

        TravelCapabilitySnapshot liveSnapshot =
                context
                        .getTravelCapabilitySnapshot();

        Map<RuneSupply, Long>
                currentConsumables =
                readCurrentConsumables(
                        liveSnapshot
                );

        RunePlan currentPlan =
                findPlan(
                        accountState,
                        liveSnapshot,
                        null,
                        currentConsumables,
                        false
                );

        if (currentPlan != null)
        {
            return new RunePreparation(RequirementResult.satisfied(
                    displayName,
                    describeCurrentPlan(
                            currentPlan
                    )
            ), currentPlan.source, false, false);
        }

        StorageSnapshot storageSnapshot =
                context.getStorageSnapshot();

        if (
                storageSnapshot == null
                        || !storageSnapshot
                        .isBankVerified()
        )
        {
            return new RunePreparation(RequirementResult.dataRequired(
                    displayName,
                    "Current carried rune resources cannot satisfy "
                            + runeCost.describe()
                            + ". Open any normal bank once so Farming Navigator can verify banked runes, staves, and rune-pouch access."
            ));
        }

        /*
         * First try bank preparation without assuming that a
         * banked rune pouch must be withdrawn.
         */
        Map<RuneSupply, Long>
                preparedConsumables =
                readPreparedConsumables(
                        liveSnapshot,
                        storageSnapshot,
                        false
                );

        RunePlan preparedPlan =
                findPlan(
                        accountState,
                        liveSnapshot,
                        storageSnapshot,
                        preparedConsumables,
                        true
                );

        if (preparedPlan != null)
        {
            return new RunePreparation(RequirementResult.availableFromBank(
                    displayName,
                    describePreparedPlan(
                            preparedPlan,
                            false,
                            null
                    )
            ), preparedPlan.source, preparedPlan.location == SourceLocation.BANK, false);
        }

        /*
         * If a pouch is in the verified bank, its raw varbit
         * contents become a valid preparation source once that
         * pouch is withdrawn.
         */
        String bankedRunePouchName =
                findBankedRunePouchName(
                        storageSnapshot
                );

        if (
                bankedRunePouchName != null
                        && !liveSnapshot
                        .getStoredRunePouchQuantities()
                        .isEmpty()
        )
        {
            Map<RuneSupply, Long>
                    preparedWithBankedPouch =
                    readPreparedConsumables(
                            liveSnapshot,
                            storageSnapshot,
                            true
                    );

            RunePlan pouchPlan =
                    findPlan(
                            accountState,
                            liveSnapshot,
                            storageSnapshot,
                            preparedWithBankedPouch,
                            true
                    );

            if (pouchPlan != null)
            {
                return new RunePreparation(RequirementResult.availableFromBank(
                        displayName,
                        describePreparedPlan(
                                pouchPlan,
                                true,
                                bankedRunePouchName
                        )
                ), pouchPlan.source, pouchPlan.location == SourceLocation.BANK, true);
            }

            return new RunePreparation(createMissingOrUnknownResult(
                    accountState,
                    liveSnapshot,
                    storageSnapshot,
                    preparedWithBankedPouch
            ));
        }

        return new RunePreparation(createMissingOrUnknownResult(
                accountState,
                liveSnapshot,
                storageSnapshot,
                preparedConsumables
        ));
    }

    private Map<RuneSupply, Long>
    readCurrentConsumables(
            TravelCapabilitySnapshot snapshot
    )
    {
        Map<RuneSupply, Long> quantities =
                new EnumMap<>(
                        RuneSupply.class
                );

        for (
                RuneSupply supply
                : RuneSupply.values()
        )
        {
            long quantity =
                    snapshot.getInventoryQuantity(
                            supply.getItemId()
                    )
                            + snapshot
                            .getRunePouchQuantity(
                                    supply.getItemId()
                            );

            if (quantity > 0)
            {
                quantities.put(
                        supply,
                        quantity
                );
            }
        }

        return quantities;
    }

    private Map<RuneSupply, Long>
    readPreparedConsumables(
            TravelCapabilitySnapshot liveSnapshot,
            StorageSnapshot storageSnapshot,
            boolean includeBankedRunePouch
    )
    {
        Map<RuneSupply, Long> quantities =
                new EnumMap<>(
                        RuneSupply.class
                );

        for (
                RuneSupply supply
                : RuneSupply.values()
        )
        {
            long quantity =
                    liveSnapshot
                            .getInventoryQuantity(
                                    supply.getItemId()
                            )
                            + storageSnapshot
                            .getVerifiedBankQuantity(
                                    supply.getItemId()
                            );

            /*
             * A carried pouch is already accessible.
             *
             * A banked pouch is only included in this second
             * preparation pass, after verified bank presence
             * has been established.
             */
            if (
                    liveSnapshot.isRunePouchCarried()
                            || includeBankedRunePouch
            )
            {
                quantity +=
                        liveSnapshot
                                .getStoredRunePouchQuantity(
                                        supply.getItemId()
                                );
            }

            if (quantity > 0)
            {
                quantities.put(
                        supply,
                        quantity
                );
            }
        }

        return quantities;
    }

    private RunePlan findPlan(
            AccountState accountState,
            TravelCapabilitySnapshot liveSnapshot,
            StorageSnapshot storageSnapshot,
            Map<RuneSupply, Long> consumables,
            boolean includeBankSources
    )
    {
        /*
         * Prefer not to change equipment if loose/pouch runes
         * already satisfy the full spell cost.
         */
        if (
                coversRuneCost(
                        consumables,
                        Collections.emptySet()
                )
        )
        {
            return new RunePlan(
                    null,
                    SourceLocation.NONE
            );
        }

        /*
         * An already-equipped source is next-best.
         */
        for (
                InfiniteRuneSource source
                : InfiniteRuneSource.values()
        )
        {
            if (
                    canUseEquippedSource(
                            accountState,
                            source
                    )
                            && liveSnapshot
                            .isEquipped(
                                    source.getItemId()
                            )
                            && coversRuneCost(
                            consumables,
                            source.getSuppliedKinds()
                    )
            )
            {
                return new RunePlan(
                        source,
                        SourceLocation.EQUIPPED
                );
            }
        }

        /*
         * A staff already in inventory can be equipped without
         * visiting a bank.
         */
        for (
                InfiniteRuneSource source
                : InfiniteRuneSource.values()
        )
        {
            if (
                    canUseSource(
                            accountState,
                            source
                    )
                            && liveSnapshot
                            .hasInInventory(
                                    source.getItemId()
                            )
                            && coversRuneCost(
                            consumables,
                            source.getSuppliedKinds()
                    )
            )
            {
                return new RunePlan(
                        source,
                        SourceLocation.INVENTORY
                );
            }
        }

        if (
                !includeBankSources
                        || storageSnapshot == null
                        || !storageSnapshot.isBankVerified()
        )
        {
            return null;
        }

        /*
         * Only ONE infinite-rune weapon is considered at a time.
         *
         * This prevents false positives such as treating a Staff
         * of air and a Staff of water as though both could be
         * equipped simultaneously.
         */
        for (
                InfiniteRuneSource source
                : InfiniteRuneSource.values()
        )
        {
            if (
                    canUseSource(
                            accountState,
                            source
                    )
                            && storageSnapshot
                            .getVerifiedBankQuantity(
                                    source.getItemId()
                            ) > 0
                            && coversRuneCost(
                            consumables,
                            source.getSuppliedKinds()
                    )
            )
            {
                return new RunePlan(
                        source,
                        SourceLocation.BANK
                );
            }
        }

        return null;
    }

    private boolean canUseSource(
            AccountState accountState,
            InfiniteRuneSource source
    )
    {
        /*
         * Equipment requirements use the player's real level.
         * A temporary Magic boost does not make an otherwise
         * unwieldable staff count as a usable rune source.
         */
        return canUseEquippedSource(accountState, source)
                && (source.getMinimumRealAttackLevel() <= 1
                || accountState.getRealAttackLevel() >= source.getMinimumRealAttackLevel());
    }

    private boolean canUseEquippedSource(AccountState account, InfiniteRuneSource source)
    {
        // An already worn source proves its wield requirements without inventing a missing skill value.
        return account.getRealMagicLevel() >= source.getMinimumRealMagicLevel();
    }

    private RequirementResult createMissingOrUnknownResult(AccountState account, TravelCapabilitySnapshot live,
            StorageSnapshot storage, Map<RuneSupply, Long> consumables)
    {
        if (account.getRealAttackLevel() < 1)
        {
            for (InfiniteRuneSource source : InfiniteRuneSource.values())
            {
                if (source.getMinimumRealAttackLevel() > 1 && canUseEquippedSource(account, source)
                        && (live.hasInInventory(source.getItemId()) || storage.getVerifiedBankQuantity(source.getItemId()) > 0)
                        && coversRuneCost(consumables, source.getSuppliedKinds()))
                {
                    return RequirementResult.dataRequired(displayName,
                            "Attack level is required to verify wielding " + source.getDisplayName()
                                    + " (" + source.getMinimumRealAttackLevel() + " Attack). It is not assumed usable.");
                }
            }
        }
        return createMissingResult(account, live, storage, consumables);
    }

    private boolean coversRuneCost(
            Map<RuneSupply, Long> consumables,
            Set<RuneKind> infiniteKinds
    )
    {
        for (
                Map.Entry<RuneKind, Integer> entry
                : runeCost
                .getQuantities()
                .entrySet()
        )
        {
            RuneKind kind =
                    entry.getKey();

            int requiredQuantity =
                    entry.getValue();

            if (
                    infiniteKinds.contains(
                            kind
                    )
            )
            {
                continue;
            }

            long availableQuantity =
                    getAvailableQuantity(
                            kind,
                            consumables
                    );

            if (
                    availableQuantity
                            < requiredQuantity
            )
            {
                return false;
            }
        }

        return true;
    }

    private long getAvailableQuantity(
            RuneKind kind,
            Map<RuneSupply, Long> consumables
    )
    {
        long quantity = 0;

        for (
                Map.Entry<RuneSupply, Long> entry
                : consumables.entrySet()
        )
        {
            if (
                    entry.getKey()
                            .supplies(
                                    kind
                            )
            )
            {
                quantity +=
                        entry.getValue();
            }
        }

        return quantity;
    }

    private RequirementResult createMissingResult(
            AccountState accountState,
            TravelCapabilitySnapshot liveSnapshot,
            StorageSnapshot storageSnapshot,
            Map<RuneSupply, Long> consumables
    )
    {
        DeficitResult bestDeficit =
                calculateDeficits(
                        consumables,
                        Collections.emptySet()
                );

        for (
                InfiniteRuneSource source
                : InfiniteRuneSource.values()
        )
        {
            if (
                    !canUseSource(
                            accountState,
                            source
                    )
            )
            {
                continue;
            }

            boolean accessible =
                    liveSnapshot
                            .isEquipped(
                                    source.getItemId()
                            )
                            || liveSnapshot
                            .hasInInventory(
                                    source.getItemId()
                            )
                            || storageSnapshot
                            .getVerifiedBankQuantity(
                                    source.getItemId()
                            ) > 0;

            if (!accessible)
            {
                continue;
            }

            DeficitResult candidate =
                    calculateDeficits(
                            consumables,
                            source.getSuppliedKinds()
                    );

            if (
                    candidate.getTotalMissing()
                            < bestDeficit
                            .getTotalMissing()
            )
            {
                bestDeficit =
                        candidate;
            }
        }

        return RequirementResult.blocked(
                displayName,
                "MISSING - verified resources cannot satisfy "
                        + runeCost.describe()
                        + ". Uncovered requirements: "
                        + bestDeficit.describe()
                        + "."
        );
    }

    private DeficitResult calculateDeficits(
            Map<RuneSupply, Long> consumables,
            Set<RuneKind> infiniteKinds
    )
    {
        EnumMap<RuneKind, Integer> deficits =
                new EnumMap<>(
                        RuneKind.class
                );

        for (
                Map.Entry<RuneKind, Integer> entry
                : runeCost
                .getQuantities()
                .entrySet()
        )
        {
            RuneKind kind =
                    entry.getKey();

            int required =
                    entry.getValue();

            if (
                    infiniteKinds.contains(
                            kind
                    )
            )
            {
                continue;
            }

            long available =
                    getAvailableQuantity(
                            kind,
                            consumables
                    );

            long missing =
                    required - available;

            if (missing > 0)
            {
                deficits.put(
                        kind,
                        (int) Math.min(
                                Integer.MAX_VALUE,
                                missing
                        )
                );
            }
        }

        return new DeficitResult(
                deficits
        );
    }

    private String describeCurrentPlan(
            RunePlan plan
    )
    {
        if (
                plan.getSource()
                        == null
        )
        {
            return "READY NOW - carried runes and accessible rune-pouch contents satisfy "
                    + runeCost.describe()
                    + ".";
        }

        switch (
                plan.getLocation()
        )
        {
            case EQUIPPED:
                return "READY NOW - "
                        + plan.getSource()
                        .getDisplayName()
                        + " is equipped and supplies "
                        + describeKinds(
                        plan.getSource()
                                .getSuppliedKinds()
                )
                        + "; carried consumable runes cover the remainder.";

            case INVENTORY:
                return "READY NOW - equip "
                        + plan.getSource()
                        .getDisplayName()
                        + " from inventory to supply "
                        + describeKinds(
                        plan.getSource()
                                .getSuppliedKinds()
                )
                        + "; carried consumable runes cover the remainder.";

            case NONE:
            case BANK:
            default:
                return "READY NOW - rune cost is satisfied.";
        }
    }

    private String describePreparedPlan(
            RunePlan plan,
            boolean usesBankedRunePouch,
            String bankedRunePouchName
    )
    {
        StringBuilder detail =
                new StringBuilder(
                        "AVAILABLE FROM BANK - "
                );

        if (
                plan.getSource() != null
        )
        {
            switch (
                    plan.getLocation()
            )
            {
                case BANK:
                    detail.append(
                                    "withdraw and equip "
                            )
                            .append(
                                    plan.getSource()
                                            .getDisplayName()
                            )
                            .append(
                                    "; "
                            );
                    break;

                case INVENTORY:
                    detail.append(
                                    "equip "
                            )
                            .append(
                                    plan.getSource()
                                            .getDisplayName()
                            )
                            .append(
                                    " from inventory; "
                            );
                    break;

                case EQUIPPED:
                    detail.append(
                                    "keep "
                            )
                            .append(
                                    plan.getSource()
                                            .getDisplayName()
                            )
                            .append(
                                    " equipped; "
                            );
                    break;

                case NONE:
                default:
                    break;
            }
        }

        if (
                usesBankedRunePouch
                        && bankedRunePouchName != null
        )
        {
            detail.append(
                            "withdraw "
                    )
                    .append(
                            bankedRunePouchName
                    )
                    .append(
                            " so its stored runes are available; "
                    );
        }

        detail.append(
                        "verified bank resources can satisfy "
                )
                .append(
                        runeCost.describe()
                )
                .append(
                        "."
                );

        return detail.toString();
    }

    private String describeKinds(
            Set<RuneKind> kinds
    )
    {
        StringJoiner joiner =
                new StringJoiner(
                        " + "
                );

        for (
                RuneKind kind
                : RuneKind.values()
        )
        {
            if (
                    kinds.contains(
                            kind
                    )
            )
            {
                joiner.add(
                        kind.getDisplayName()
                );
            }
        }

        return joiner.toString();
    }

    private String findBankedRunePouchName(
            StorageSnapshot storageSnapshot
    )
    {
        if (
                storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.DIVINE_RUNE_POUCH
                        ) > 0
        )
        {
            return "Divine rune pouch";
        }

        if (
                storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.DIVINE_RUNE_POUCH_TROUVER
                        ) > 0
        )
        {
            return "Divine rune pouch (protected)";
        }

        if (
                storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.BH_RUNE_POUCH
                        ) > 0
        )
        {
            return "Rune pouch";
        }

        if (
                storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.BH_RUNE_POUCH_TROUVER
                        ) > 0
        )
        {
            return "Rune pouch (protected)";
        }

        return null;
    }

    private enum SourceLocation
    {
        NONE,
        EQUIPPED,
        INVENTORY,
        BANK
    }

    private static class RunePlan
    {
        private final InfiniteRuneSource source;

        private final SourceLocation location;

        private RunePlan(
                InfiniteRuneSource source,
                SourceLocation location
        )
        {
            this.source =
                    source;

            this.location =
                    location;
        }

        private InfiniteRuneSource getSource()
        {
            return source;
        }

        private SourceLocation getLocation()
        {
            return location;
        }
    }

    private static class DeficitResult
    {
        private final Map<RuneKind, Integer>
                deficits;

        private DeficitResult(
                Map<RuneKind, Integer> deficits
        )
        {
            this.deficits =
                    new EnumMap<>(
                            RuneKind.class
                    );

            this.deficits.putAll(
                    deficits
            );
        }

        private int getTotalMissing()
        {
            int total = 0;

            for (
                    Integer quantity
                    : deficits.values()
            )
            {
                total +=
                        quantity;
            }

            return total;
        }

        private String describe()
        {
            if (deficits.isEmpty())
            {
                return "none";
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
                        deficits.getOrDefault(
                                kind,
                                0
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
    }
}
