package com.farmingnavigator.routing.item;

import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RouteRequirement;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

public class ChargedTeleportItemRequirement
        implements RouteRequirement
{
    private final ChargedTeleportItemDefinition definition;
    private final long requiredCharges;

    public ChargedTeleportItemRequirement(
            ChargedTeleportItemDefinition definition
    )
    {
        this(
                definition,
                1L
        );
    }

    public ChargedTeleportItemRequirement(
            ChargedTeleportItemDefinition definition,
            long requiredCharges
    )
    {
        if (definition == null)
        {
            throw new IllegalArgumentException(
                    "Charged teleport item definition cannot be null."
            );
        }

        this.definition =
                definition;

        this.requiredCharges =
                Math.max(
                        1L,
                        requiredCharges
                );
    }

    public ChargedTeleportItemDefinition
    getDefinition()
    {
        return definition;
    }

    public long getRequiredCharges()
    {
        return requiredCharges;
    }

    @Override
    public String getDisplayName()
    {
        return definition
                .getDisplayName()
                + " teleport charges";
    }

    @Override
    public RequirementResult evaluate(
            RequirementContext context
    )
    {
        if (
                context == null
                        || context
                        .getTravelCapabilitySnapshot()
                        == null
                        || !context
                        .getTravelCapabilitySnapshot()
                        .isLoggedIn()
        )
        {
            return RequirementResult.dataRequired(
                    getDisplayName(),
                    "DATA REQUIRED - log in so Farming Navigator can verify "
                            + definition.getDisplayName()
                            + " charge variants."
            );
        }

        TravelCapabilitySnapshot liveSnapshot =
                context
                        .getTravelCapabilitySnapshot();

        ChargeSummary carried =
                summarizeCarried(
                        liveSnapshot
                );

        if (
                meetsRequirement(
                        carried
                )
        )
        {
            return RequirementResult.satisfied(
                    getDisplayName(),
                    "READY NOW - required: "
                            + requiredCharges
                            + " usable teleport "
                            + chargeWord(
                            requiredCharges
                    )
                            + ". Carried usable charges: "
                            + formatCharges(
                            carried
                    )
                            + ". Carried variants: "
                            + carried
                            .getDescription()
                            + "."
            );
        }

        StorageSnapshot storageSnapshot =
                context
                        .getStorageSnapshot();

        if (
                storageSnapshot == null
                        || !storageSnapshot
                        .isBankVerified()
        )
        {
            return RequirementResult.dataRequired(
                    getDisplayName(),
                    "DATA REQUIRED - carried usable charges: "
                            + formatCharges(
                            carried
                    )
                            + ". Open any normal bank once so Farming Navigator "
                            + "can verify charged "
                            + definition
                            .getDisplayName()
                            + " variants in storage."
            );
        }

        ChargeSummary bank =
                summarizeBank(
                        storageSnapshot
                );

        if (
                meetsRequirement(
                        carried,
                        bank
                )
        )
        {
            long neededFromBank =
                    getFiniteChargesStillNeeded(
                            carried
                    );

            return RequirementResult.availableFromBank(
                    getDisplayName(),
                    "AVAILABLE FROM BANK - required: "
                            + requiredCharges
                            + " usable teleport "
                            + chargeWord(
                            requiredCharges
                    )
                            + ". Carried usable charges: "
                            + formatCharges(
                            carried
                    )
                            + ". Verified bank usable charges: "
                            + formatCharges(
                            bank
                    )
                            + ". Withdraw: "
                            + createWithdrawalPlan(
                            storageSnapshot,
                            neededFromBank
                    )
                            + ". Verified bank variants: "
                            + bank
                            .getDescription()
                            + "."
            );
        }

        long totalFiniteCharges =
                safeAdd(
                        carried
                                .getFiniteCharges(),
                        bank
                                .getFiniteCharges()
                );

        if (
                totalFiniteCharges == 0L
                        && !carried
                        .hasUnlimited()
                        && !bank
                        .hasUnlimited()
        )
        {
            return RequirementResult.blocked(
                    getDisplayName(),
                    "MISSING - no usable charged or unlimited "
                            + definition
                            .getDisplayName()
                            + " variant was found in inventory, equipment, "
                            + "or the verified bank."
            );
        }

        long missingCharges =
                Math.max(
                        0L,
                        requiredCharges
                                - totalFiniteCharges
                );

        return RequirementResult.blocked(
                getDisplayName(),
                "MISSING - requires "
                        + requiredCharges
                        + " usable teleport "
                        + chargeWord(
                        requiredCharges
                )
                        + ". Carried usable charges: "
                        + formatCharges(
                        carried
                )
                        + ". Verified bank usable charges: "
                        + formatCharges(
                        bank
                )
                        + ". Still missing: "
                        + missingCharges
                        + "."
        );
    }

    private ChargeSummary summarizeCarried(
            TravelCapabilitySnapshot snapshot
    )
    {
        long finiteCharges =
                0L;

        boolean unlimited =
                false;

        StringJoiner description =
                new StringJoiner(
                        ", "
                );

        for (
                ChargedTeleportItemDefinition.Variant variant
                : definition
                .getVariants()
        )
        {
            long quantity =
                    snapshot
                            .getCarriedQuantity(
                                    variant
                                            .getItemId()
                            );

            if (quantity <= 0L)
            {
                continue;
            }

            description.add(
                    describeVariant(
                            variant,
                            quantity
                    )
            );

            if (
                    variant
                            .isUnlimited()
            )
            {
                unlimited =
                        true;
            }
            else
            {
                finiteCharges =
                        safeAdd(
                                finiteCharges,
                                safeMultiply(
                                        quantity,
                                        variant
                                                .getCharges()
                                )
                        );
            }
        }

        return new ChargeSummary(
                finiteCharges,
                unlimited,
                description.length() == 0
                        ? "none"
                        : description
                        .toString()
        );
    }

    private ChargeSummary summarizeBank(
            StorageSnapshot snapshot
    )
    {
        long finiteCharges =
                0L;

        boolean unlimited =
                false;

        StringJoiner description =
                new StringJoiner(
                        ", "
                );

        for (
                ChargedTeleportItemDefinition.Variant variant
                : definition
                .getVariants()
        )
        {
            long quantity =
                    snapshot
                            .getVerifiedBankQuantity(
                                    variant
                                            .getItemId()
                            );

            if (quantity <= 0L)
            {
                continue;
            }

            description.add(
                    describeVariant(
                            variant,
                            quantity
                    )
            );

            if (
                    variant
                            .isUnlimited()
            )
            {
                unlimited =
                        true;
            }
            else
            {
                finiteCharges =
                        safeAdd(
                                finiteCharges,
                                safeMultiply(
                                        quantity,
                                        variant
                                                .getCharges()
                                )
                        );
            }
        }

        return new ChargeSummary(
                finiteCharges,
                unlimited,
                description.length() == 0
                        ? "none"
                        : description
                        .toString()
        );
    }

    private boolean meetsRequirement(
            ChargeSummary summary
    )
    {
        return summary
                .hasUnlimited()
                || summary
                .getFiniteCharges()
                >= requiredCharges;
    }

    private boolean meetsRequirement(
            ChargeSummary carried,
            ChargeSummary bank
    )
    {
        return carried
                .hasUnlimited()
                || bank
                .hasUnlimited()
                || safeAdd(
                carried
                        .getFiniteCharges(),
                bank
                        .getFiniteCharges()
        )
                >= requiredCharges;
    }

    private long getFiniteChargesStillNeeded(
            ChargeSummary carried
    )
    {
        if (
                carried
                        .hasUnlimited()
        )
        {
            return 0L;
        }

        return Math.max(
                0L,
                requiredCharges
                        - carried
                        .getFiniteCharges()
        );
    }

    private String createWithdrawalPlan(
            StorageSnapshot snapshot,
            long neededCharges
    )
    {
        if (neededCharges <= 0L)
        {
            return "none";
        }

        ChargeSummary bank =
                summarizeBank(
                        snapshot
                );

        if (
                bank
                        .getFiniteCharges()
                        >= neededCharges
        )
        {
            String finitePlan =
                    createFiniteWithdrawalPlan(
                            snapshot,
                            neededCharges
                    );

            if (
                    !"none"
                            .equals(
                                    finitePlan
                            )
            )
            {
                return finitePlan;
            }
        }

        for (
                ChargedTeleportItemDefinition.Variant variant
                : definition
                .getVariants()
        )
        {
            if (
                    !variant
                            .isUnlimited()
            )
            {
                continue;
            }

            long quantity =
                    snapshot
                            .getVerifiedBankQuantity(
                                    variant
                                            .getItemId()
                            );

            if (quantity > 0L)
            {
                return variant
                        .getDisplayName()
                        + " x1";
            }
        }

        return createFiniteWithdrawalPlan(
                snapshot,
                neededCharges
        );
    }

    private String createFiniteWithdrawalPlan(
            StorageSnapshot snapshot,
            long neededCharges
    )
    {
        List<ChargedTeleportItemDefinition.Variant>
                finiteVariants =
                new ArrayList<>();

        for (
                ChargedTeleportItemDefinition.Variant variant
                : definition
                .getVariants()
        )
        {
            if (
                    !variant
                            .isUnlimited()
            )
            {
                finiteVariants.add(
                        variant
                );
            }
        }

        finiteVariants.sort(
                Comparator.comparingInt(
                        ChargedTeleportItemDefinition.Variant
                                ::getCharges
                ).reversed()
        );

        long remaining =
                neededCharges;

        StringJoiner plan =
                new StringJoiner(
                        ", "
                );

        for (
                ChargedTeleportItemDefinition.Variant variant
                : finiteVariants
        )
        {
            long bankQuantity =
                    snapshot
                            .getVerifiedBankQuantity(
                                    variant
                                            .getItemId()
                            );

            if (bankQuantity <= 0L)
            {
                continue;
            }

            long itemsNeeded =
                    divideRoundUp(
                            remaining,
                            variant
                                    .getCharges()
                    );

            long itemsToWithdraw =
                    Math.min(
                            bankQuantity,
                            itemsNeeded
                    );

            if (itemsToWithdraw <= 0L)
            {
                continue;
            }

            plan.add(
                    variant
                            .getDisplayName()
                            + " x"
                            + itemsToWithdraw
            );

            long suppliedCharges =
                    safeMultiply(
                            itemsToWithdraw,
                            variant
                                    .getCharges()
                    );

            if (
                    suppliedCharges
                            >= remaining
            )
            {
                remaining =
                        0L;

                break;
            }

            remaining -=
                    suppliedCharges;
        }

        if (plan.length() == 0)
        {
            return "none";
        }

        return plan
                .toString();
    }

    private String describeVariant(
            ChargedTeleportItemDefinition.Variant variant,
            long quantity
    )
    {
        if (
                variant
                        .isUnlimited()
        )
        {
            return variant
                    .getDisplayName()
                    + " x"
                    + quantity
                    + " (unlimited)";
        }

        return variant
                .getDisplayName()
                + " x"
                + quantity;
    }

    private String formatCharges(
            ChargeSummary summary
    )
    {
        if (
                summary
                        .hasUnlimited()
        )
        {
            return "unlimited";
        }

        return Long.toString(
                summary
                        .getFiniteCharges()
        );
    }

    private String chargeWord(
            long quantity
    )
    {
        return quantity == 1L
                ? "charge"
                : "charges";
    }

    private long divideRoundUp(
            long numerator,
            int denominator
    )
    {
        if (numerator <= 0L)
        {
            return 0L;
        }

        return 1L
                + (
                numerator - 1L
        )
                / denominator;
    }

    private long safeMultiply(
            long left,
            int right
    )
    {
        if (
                left <= 0L
                        || right <= 0
        )
        {
            return 0L;
        }

        if (
                left
                        > Long.MAX_VALUE
                        / right
        )
        {
            return Long.MAX_VALUE;
        }

        return left
                * right;
    }

    private long safeAdd(
            long left,
            long right
    )
    {
        if (
                left
                        >= Long.MAX_VALUE
                        - right
        )
        {
            return Long.MAX_VALUE;
        }

        return left
                + right;
    }

    private static final class ChargeSummary
    {
        private final long finiteCharges;
        private final boolean unlimited;
        private final String description;

        private ChargeSummary(
                long finiteCharges,
                boolean unlimited,
                String description
        )
        {
            this.finiteCharges =
                    finiteCharges;

            this.unlimited =
                    unlimited;

            this.description =
                    description;
        }

        private long getFiniteCharges()
        {
            return finiteCharges;
        }

        private boolean hasUnlimited()
        {
            return unlimited;
        }

        private String getDescription()
        {
            return description;
        }
    }
}