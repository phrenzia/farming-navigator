package com.farmingnavigator.transport;

import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RequirementState;
import net.runelite.api.Quest;

/** Destination-specific access checks stay separate from the charged item itself. */
final class TreeTransportVerifier
{
    VerifiedTransportResult evaluate(TransportOption option, RequirementContext context)
    {
        if (context == null || context.getAccountState() == null || !context.getAccountState().isLoggedIn())
        {
            return new VerifiedTransportResult(option, VerifiedTransportState.DATA_REQUIRED, "Log in to check tree travel access.");
        }
        ChargedTeleportItemDefinition family;
        boolean access;
        String missing;
        switch (option.getVerificationKind())
        {
            case WEALTH_SPIRIT_TREE:
                family = ChargedTeleportItemDefinition.RING_OF_WEALTH;
                access = context.getAccountState().hasCompletedQuest(Quest.TREE_GNOME_VILLAGE);
                missing = "Complete Tree Gnome Village to travel from the Grand Exchange spirit tree. Incoming travel to Gnome Stronghold does not require The Grand Tree.";
                break;
            case DIGSITE_BARGE:
                family = ChargedTeleportItemDefinition.DIGSITE_PENDANT;
                access = context.getAccountState().hasCompletedQuest(Quest.BONE_VOYAGE);
                missing = "Complete Bone Voyage to use the Digsite barge to Fossil Island.";
                break;
            case LLETYA_CRYSTAL:
                family = ChargedTeleportItemDefinition.TELEPORT_CRYSTAL;
                access = context.getAccountState().hasStartedQuest(Quest.MOURNINGS_END_PART_I);
                missing = "Start Mourning's End Part I to use the Lletya destination.";
                break;
            default: throw new IllegalArgumentException("Unsupported tree transport kind");
        }
        if (!access) { return new VerifiedTransportResult(option, VerifiedTransportState.LOCKED, missing); }
        // A charged untradeable pendant establishes possession of a learned enchantment; this route uses
        // only its base Digsite destination, not the separately unlocked Fossil Island machine destination.
        RequirementResult result = family.createRequirement().evaluate(context);
        VerifiedTransportState state = result.isReadyNow() ? VerifiedTransportState.READY_NOW
                : result.requiresBankWithdrawal() ? VerifiedTransportState.AVAILABLE_FROM_BANK
                : result.getState() == RequirementState.DATA_REQUIRED ? VerifiedTransportState.DATA_REQUIRED
                : result.getState() == RequirementState.CONFIGURATION_REQUIRED ? VerifiedTransportState.CONFIGURATION_REQUIRED
                : VerifiedTransportState.MISSING;
        return new VerifiedTransportResult(option, state, result.getDetail());
    }
}
