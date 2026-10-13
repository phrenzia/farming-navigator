package com.farmingnavigator.transport;

import com.farmingnavigator.poh.PohPortalDestination;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;

public class TransportOption
{
    private final int baselineRank;
    private final String displayName;
    private final String instructions;
    private final TransportCategory category;
    private final TransportVerificationKind verificationKind;

    private final TeleportSpellDefinition
            teleportSpellDefinition;

    private final PohPortalDestination
            pohPortalDestination;

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
                null,
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
        this(
                baselineRank,
                displayName,
                instructions,
                category,
                verificationKind,
                teleportSpellDefinition,
                null
        );
    }

    private TransportOption(
            int baselineRank,
            String displayName,
            String instructions,
            TransportCategory category,
            TransportVerificationKind verificationKind,
            TeleportSpellDefinition teleportSpellDefinition,
            PohPortalDestination pohPortalDestination
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

        if (
                verificationKind
                        == TransportVerificationKind.POH_PORTAL
                        && pohPortalDestination == null
        )
        {
            throw new IllegalArgumentException(
                    "POH portal transport requires a portal destination."
            );
        }

        if (
                verificationKind
                        != TransportVerificationKind.POH_PORTAL
                        && pohPortalDestination != null
        )
        {
            throw new IllegalArgumentException(
                    "Only POH_PORTAL transport may carry a POH portal destination."
            );
        }

        this.baselineRank =
                baselineRank;

        this.displayName =
                displayName;

        this.instructions =
                instructions;

        this.category =
                category;

        this.verificationKind =
                verificationKind;

        this.teleportSpellDefinition =
                teleportSpellDefinition;

        this.pohPortalDestination =
                pohPortalDestination;
    }

    public static TransportOption pohPortal(
            int baselineRank,
            String displayName,
            String instructions,
            PohPortalDestination destination
    )
    {
        if (destination == null)
        {
            throw new IllegalArgumentException(
                    "POH portal destination cannot be null."
            );
        }

        return new TransportOption(
                baselineRank,
                displayName,
                instructions,
                TransportCategory.PLAYER_OWNED_HOUSE,
                TransportVerificationKind.POH_PORTAL,
                null,
                destination
        );
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

    public PohPortalDestination
    getPohPortalDestination()
    {
        return pohPortalDestination;
    }
}