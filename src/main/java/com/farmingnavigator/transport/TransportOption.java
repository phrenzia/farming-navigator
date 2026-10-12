package com.farmingnavigator.transport;

import com.farmingnavigator.routing.spell.TeleportSpellDefinition;

public class TransportOption
{
    private final int baselineRank;
    private final String displayName;
    private final String instructions;
    private final TransportCategory category;
    private final TransportVerificationKind verificationKind;
    private final TeleportSpellDefinition teleportSpellDefinition;

    public TransportOption(
            int baselineRank,
            String displayName,
            String instructions,
            TransportCategory category,
            TransportVerificationKind verificationKind
    )
    {
        this(
                baselineRank,
                displayName,
                instructions,
                category,
                verificationKind,
                null
        );
    }

    public TransportOption(
            int baselineRank,
            String displayName,
            String instructions,
            TransportCategory category,
            TransportVerificationKind verificationKind,
            TeleportSpellDefinition teleportSpellDefinition
    )
    {
        if (
                verificationKind
                        == TransportVerificationKind.TELEPORT_SPELL
                        && teleportSpellDefinition == null
        )
        {
            throw new IllegalArgumentException(
                    "Teleport spell transport requires a spell definition."
            );
        }

        if (
                verificationKind
                        != TransportVerificationKind.TELEPORT_SPELL
                        && teleportSpellDefinition != null
        )
        {
            throw new IllegalArgumentException(
                    "Only TELEPORT_SPELL transport may carry a spell definition."
            );
        }

        this.baselineRank = baselineRank;
        this.displayName = displayName;
        this.instructions = instructions;
        this.category = category;
        this.verificationKind = verificationKind;
        this.teleportSpellDefinition =
                teleportSpellDefinition;
    }

    public int getBaselineRank()
    {
        return baselineRank;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getInstructions()
    {
        return instructions;
    }

    public TransportCategory getCategory()
    {
        return category;
    }

    public TransportVerificationKind getVerificationKind()
    {
        return verificationKind;
    }

    public TeleportSpellDefinition
    getTeleportSpellDefinition()
    {
        return teleportSpellDefinition;
    }
}