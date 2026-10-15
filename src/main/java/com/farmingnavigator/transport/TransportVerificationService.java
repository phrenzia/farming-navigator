package com.farmingnavigator.transport;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.account.DiaryArea;
import com.farmingnavigator.account.DiaryTier;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.poh.PohPortalDestination;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.requirement.ArdougneCloakUnlimitedRequirement;
import com.farmingnavigator.routing.requirement.ArdougneTeleportTabletRouteRequirement;
import com.farmingnavigator.routing.requirement.ChargedRingOfElementsRequirement;
import com.farmingnavigator.routing.requirement.ChargedXericsTalismanRequirement;
import com.farmingnavigator.routing.requirement.CombatBraceletRouteRequirement;
import com.farmingnavigator.routing.requirement.EctophialHarmonyRouteRequirement;
import com.farmingnavigator.routing.requirement.ExplorerRing2CabbagePortRequirement;
import com.farmingnavigator.routing.requirement.FairyRingAccessRequirement;
import com.farmingnavigator.routing.requirement.FairyRingAjpRouteRequirement;
import com.farmingnavigator.routing.requirement.KandarinHeadgear3DailyRequirement;
import com.farmingnavigator.routing.requirement.KandarinHeadgearUnlimitedRequirement;
import com.farmingnavigator.routing.requirement.MosLeHarmlessHarmonyRouteRequirement;
import com.farmingnavigator.routing.requirement.PohFeature;
import com.farmingnavigator.routing.requirement.PohFeatureRequirement;
import com.farmingnavigator.routing.requirement.PohHouseEntryRequirement;
import com.farmingnavigator.routing.requirement.PohHouseFeatureRouteRequirement;
import com.farmingnavigator.routing.requirement.PohHouseLocationRouteRequirement;
import com.farmingnavigator.routing.requirement.PohJewelleryBoxSkillsNecklaceRouteRequirement;
import com.farmingnavigator.routing.requirement.PohPortalRouteRequirement;
import com.farmingnavigator.routing.requirement.QuetzalWhistleRouteRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RequirementState;
import com.farmingnavigator.routing.requirement.RouteRequirement;
import com.farmingnavigator.routing.requirement.RuneCostRequirement;
import com.farmingnavigator.routing.requirement.WeissLarryBoatRouteRequirement;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.routing.spell.TeleportSpellRequirement;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.inject.Inject;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;

public class TransportVerificationService
{
    private static final int[] UNLIMITED_EXPLORER_RING_IDS = {
            ItemID.LUMBRIDGE_RING_ELITE,
            ItemID.LUMBRIDGE_RING_HARD
    };

    private static final int[] FARMING_CAPE_IDS = {
            ItemID.SKILLCAPE_FARMING,
            ItemID.SKILLCAPE_FARMING_TRIMMED,
            ItemID.SKILLCAPE_MAX_WORN
    };

    @Inject
    public TransportVerificationService()
    {
    }

    /*
     * Compatibility overload retained for callers that do
     * not need POH configuration.
     */
    public Map<
            HerbPatchDefinition,
            List<VerifiedTransportResult>
            > evaluateAll(
            AccountState accountState,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            Map<
                    HerbPatchDefinition,
                    List<TransportOption>
                    > optionsByPatch
    )
    {
        return evaluateAll(
                accountState,
                null,
                travelCapabilitySnapshot,
                storageSnapshot,
                optionsByPatch
        );
    }

    public <P> Map<
            P,
            List<VerifiedTransportResult>
            > evaluateAll(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            Map<
                    P,
                    List<TransportOption>
                    > optionsByPatch
    )
    {
        if (
                accountState == null
                        || !accountState.isLoggedIn()
                        || travelCapabilitySnapshot == null
                        || !travelCapabilitySnapshot.isLoggedIn()
                        || storageSnapshot == null
                        || !storageSnapshot.isBankVerified()
        )
        {
            return Collections.emptyMap();
        }

        Map<P, List<VerifiedTransportResult>> results = new java.util.LinkedHashMap<>();

        for (
                Map.Entry<
                        P,
                        List<TransportOption>
                        > entry
                : optionsByPatch.entrySet()
        )
        {
            List<VerifiedTransportResult>
                    patchResults =
                    new ArrayList<>();

            for (
                    TransportOption option
                    : entry.getValue()
            )
            {
                VerifiedTransportResult result =
                        evaluateOption(
                                accountState,
                                config,
                                travelCapabilitySnapshot,
                                storageSnapshot,
                                option
                        );

                if (result != null)
                {
                    patchResults.add(result);
                }
            }

            patchResults.sort(
                    Comparator
                            .comparingInt(
                                    this::statePriority
                            )
                            .thenComparingInt(
                                    result ->
                                            result
                                                    .getOption()
                                                    .getBaselineRank()
                            )
            );

            results.put(
                    entry.getKey(),
                    Collections.unmodifiableList(
                            patchResults
                    )
            );
        }

        return Collections.unmodifiableMap(
                results
        );
    }

    private VerifiedTransportResult evaluateOption(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        switch (
                option.getVerificationKind()
        )
        {
            case WEALTH_SPIRIT_TREE:
            case DIGSITE_BARGE:
            case LLETYA_CRYSTAL:
            case PRIFDDINAS_CRYSTAL:
                return new TreeTransportVerifier().evaluate(option,
                        new RequirementContext(accountState, config, travelCapabilitySnapshot, storageSnapshot));
            case EXPLORER_RING_UNLIMITED:
                return evaluateExplorerRingUnlimited(
                        storageSnapshot,
                        option
                );

            case ARDOUGNE_CLOAK_UNLIMITED:
                return evaluateArdougneCloakUnlimited(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case KANDARIN_HEADGEAR_UNLIMITED:
                return evaluateKandarinHeadgearUnlimited(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case EXPLORER_RING_2_DAILY:
                return evaluateExplorerRing2Daily(
                        accountState, config, travelCapabilitySnapshot, storageSnapshot, option);

            case KANDARIN_HEADGEAR_3_DAILY:
                return evaluateKandarinHeadgear3Daily(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case SKILLS_NECKLACE:
                return evaluateSkillsNecklace(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case AMULET_OF_GLORY:
                return evaluateAmuletOfGlory(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case RING_OF_WEALTH:
                return evaluateRingOfWealth(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case COMBAT_BRACELET:
                return evaluateCombatBracelet(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case ARDOUGNE_TELEPORT_TABLET:
                return evaluateArdougneTeleportTablet(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case ECTOPHIAL:
                return evaluateEctophial(
                        storageSnapshot,
                        option
                );

            case STONY_BASALT:
                return evaluateBasalt(
                        accountState,
                        storageSnapshot,
                        option,
                        ItemID.STRONGHOLD_TELEPORT_BASALT,
                        "Stony basalt"
                );

            case ICY_BASALT:
                return evaluateBasalt(
                        accountState,
                        storageSnapshot,
                        option,
                        ItemID.WEISS_TELEPORT_BASALT,
                        "Icy basalt"
                );

            case RING_OF_ELEMENTS:
                return evaluateItemRequirement(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option,
                        new ChargedRingOfElementsRequirement()
                );

            case XERICS_TALISMAN_GLADE:
                return evaluateItemRequirement(
                        accountState, config, travelCapabilitySnapshot, storageSnapshot,
                        option, new ChargedXericsTalismanRequirement());

            case MOS_LE_HARMLESS_HARMONY:
                return evaluateMosLeHarmlessHarmony(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case ECTOPHIAL_HARMONY:
                return evaluateEctophialHarmony(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case FARMING_CAPE:
                return evaluateFarmingCape(
                        accountState,
                        storageSnapshot,
                        option
                );

            case FAIRY_RING_ACCESS:
                return evaluateFairyRingAccess(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case FAIRY_RING_AJP:
                return evaluateFairyRingAjp(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case WEISS_LARRY_BOAT:
                return evaluateWeissLarryBoat(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case QUETZAL_WHISTLE:
                return evaluateQuetzalWhistle(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case POH_FAIRY_RING:
                return evaluatePohFairyRing(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case POH_JEWELLERY_BOX_SKILLS_NECKLACE:
                return evaluatePohJewelleryBoxSkillsNecklace(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case POH_MOUNTED_XERICS_TALISMAN:
                return evaluatePohHouseFeature(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option,
                        PohFeature.MOUNTED_XERICS_TALISMAN
                );

            case POH_HOSIDIUS_HOUSE_LOCATION:
                return evaluatePohHouseLocation(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option,
                        PohHouseLocation.HOSIDIUS
                );

            case POH_PORTAL:
                return evaluatePohPortal(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case TELEPORT_SPELL:
                return evaluateTeleportSpell(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot,
                        option
                );

            case NOT_IMPLEMENTED:
            default:
                return null;
        }
    }

    private VerifiedTransportResult
    evaluateExplorerRingUnlimited(
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        String carriedVariant =
                findFirstCarriedVariant(
                        storageSnapshot,
                        UNLIMITED_EXPLORER_RING_IDS,
                        new String[] {
                                "Explorer's ring 4",
                                "Explorer's ring 3"
                        }
                );

        if (carriedVariant != null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    carriedVariant
                            + " carried - Cabbage-port is unlimited"
            );
        }

        String bankVariant =
                findFirstBankVariant(
                        storageSnapshot,
                        UNLIMITED_EXPLORER_RING_IDS,
                        new String[] {
                                "Explorer's ring 4",
                                "Explorer's ring 3"
                        }
                );

        if (bankVariant != null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    bankVariant
                            + " in bank - unlimited Cabbage-port; withdraw before run"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "No Explorer's ring 3 or 4 found in inventory, equipment, or verified bank"
        );
    }

    private VerifiedTransportResult
    evaluateArdougneCloakUnlimited(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new ArdougneCloakUnlimitedRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateKandarinHeadgearUnlimited(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new KandarinHeadgearUnlimitedRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - Kandarin headgear 4 carried; Sherlock teleport is unlimited."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                !accountState.hasClaimedDiaryTier(
                        DiaryArea.KANDARIN,
                        DiaryTier.ELITE
                )
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateKandarinHeadgear3Daily(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new KandarinHeadgear3DailyRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - Kandarin headgear 3 carried; the daily Sherlock teleport remains available."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                !accountState.hasClaimedDiaryTier(
                        DiaryArea.KANDARIN,
                        DiaryTier.HARD
                )
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateExplorerRing2Daily(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new ExplorerRing2CabbagePortRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - Explorer's ring 2 carried; a daily Cabbage-port teleport remains available."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                !accountState.hasClaimedDiaryTier(
                        DiaryArea.LUMBRIDGE_AND_DRAYNOR,
                        DiaryTier.MEDIUM
                )
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateItemRequirement(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option,
            RouteRequirement requirement
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                requirement
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateMosLeHarmlessHarmony(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new MosLeHarmlessHarmonyRouteRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - Mos Le'Harmless teleport scroll and Harmony Island access requirements are satisfied."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                !accountState.hasCompletedQuest(
                        Quest.CABIN_FEVER
                )
                        || !accountState.hasStartedQuest(
                        Quest.THE_GREAT_BRAIN_ROBBERY
                )
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateEctophialHarmony(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new EctophialHarmonyRouteRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - filled Ectophial and Harmony Island boat access requirements are satisfied."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                !accountState.hasCompletedQuest(
                        Quest.CABIN_FEVER
                )
                        || !accountState.hasStartedQuest(
                        Quest.THE_GREAT_BRAIN_ROBBERY
                )
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluatePohHouseLocation(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option,
            PohHouseLocation requiredLocation
    )
    {
        if (config == null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    "Configure your POH profile in Farming Navigator."
            );
        }

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new PohHouseLocationRouteRequirement(
                        requiredLocation
                )
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - POH entry method and configured "
                            + requiredLocation.getDisplayName()
                            + " house location are ready."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateSkillsNecklace(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                ChargedTeleportItemDefinition
                        .SKILLS_NECKLACE
                        .createRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateAmuletOfGlory(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                ChargedTeleportItemDefinition
                        .AMULET_OF_GLORY
                        .createRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateRingOfWealth(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                ChargedTeleportItemDefinition
                        .RING_OF_WEALTH
                        .createRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateCombatBracelet(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new CombatBraceletRouteRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - a charged Combat bracelet is carried and can teleport to the Ranging Guild."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateArdougneTeleportTablet(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new ArdougneTeleportTabletRouteRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - Ardougne teleport tablet carried and Plague City is complete."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                !accountState.hasCompletedQuest(
                        Quest.PLAGUE_CITY
                )
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateFairyRingAccess(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        VerifiedTransportState blockedState =
                accountState
                        .isFairyRingAccessUnlocked()
                        ? VerifiedTransportState.MISSING
                        : VerifiedTransportState.LOCKED;

        return new VerifiedTransportResult(
                option,
                blockedState,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateFairyRingAjp(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new FairyRingAjpRouteRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - fairy ring AJP access requirements are satisfied."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                !accountState
                        .isFairyRingAccessUnlocked()
                        || !accountState
                        .isVarlamoreVisited()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateWeissLarryBoat(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new WeissLarryBoatRouteRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - fairy ring DKS and Larry's boat access to Weiss are ready."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                !accountState.hasCompletedQuest(
                        Quest.MAKING_FRIENDS_WITH_MY_ARM
                )
                        || !accountState
                        .isFairyRingAccessUnlocked()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateQuetzalWhistle(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new QuetzalWhistleRouteRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluatePohFairyRing(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult houseEntryResult =
                new PohHouseEntryRequirement()
                        .evaluate(
                                context
                        );

        RequirementResult pohFairyRingResult =
                new PohFeatureRequirement(
                        PohFeature.FAIRY_RING
                )
                        .evaluate(
                                context
                        );

        RequirementResult fairyRingAccessResult =
                new FairyRingAccessRequirement()
                        .evaluate(
                                context
                        );

        if (
                houseEntryResult.isSatisfied()
                        && pohFairyRingResult.isSatisfied()
                        && fairyRingAccessResult.isSatisfied()
        )
        {
            StringJoiner preparationSteps =
                    new StringJoiner(
                            " "
                    );

            if (
                    houseEntryResult
                            .requiresBankWithdrawal()
            )
            {
                preparationSteps.add(
                        houseEntryResult
                                .getDetail()
                );
            }

            if (
                    fairyRingAccessResult
                            .requiresBankWithdrawal()
            )
            {
                preparationSteps.add(
                        fairyRingAccessResult
                                .getDetail()
                );
            }

            if (
                    preparationSteps.length()
                            > 0
            )
            {
                return new VerifiedTransportResult(
                        option,
                        VerifiedTransportState.AVAILABLE_FROM_BANK,
                        preparationSteps.toString()
                );
            }

            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - "
                            + houseEntryResult.getDetail()
                            + " Configured POH fairy ring and fairy-ring access are ready."
            );
        }

        StringJoiner blockers =
                new StringJoiner(
                        " "
                );

        boolean hasLockedRequirement =
                false;

        boolean hasMissingRequirement =
                false;

        boolean hasDataRequired =
                false;

        boolean hasConfigurationRequired =
                false;

        if (
                !houseEntryResult
                        .isSatisfied()
        )
        {
            blockers.add(
                    houseEntryResult
                            .getDetail()
            );

            switch (
                    houseEntryResult
                            .getState()
            )
            {
                case BLOCKED:
                    hasMissingRequirement =
                            true;
                    break;

                case DATA_REQUIRED:
                    hasDataRequired =
                            true;
                    break;

                case CONFIGURATION_REQUIRED:
                    hasConfigurationRequired =
                            true;
                    break;

                case SATISFIED:
                default:
                    break;
            }
        }

        if (
                !pohFairyRingResult
                        .isSatisfied()
        )
        {
            blockers.add(
                    pohFairyRingResult
                            .getDetail()
            );

            switch (
                    pohFairyRingResult
                            .getState()
            )
            {
                case BLOCKED:
                    hasMissingRequirement =
                            true;
                    break;

                case DATA_REQUIRED:
                    hasDataRequired =
                            true;
                    break;

                case CONFIGURATION_REQUIRED:
                    hasConfigurationRequired =
                            true;
                    break;

                case SATISFIED:
                default:
                    break;
            }
        }

        if (
                !fairyRingAccessResult
                        .isSatisfied()
        )
        {
            blockers.add(
                    fairyRingAccessResult
                            .getDetail()
            );

            switch (
                    fairyRingAccessResult
                            .getState()
            )
            {
                case BLOCKED:
                    if (
                            accountState
                                    .isFairyRingAccessUnlocked()
                    )
                    {
                        hasMissingRequirement =
                                true;
                    }
                    else
                    {
                        hasLockedRequirement =
                                true;
                    }
                    break;

                case DATA_REQUIRED:
                    hasDataRequired =
                            true;
                    break;

                case CONFIGURATION_REQUIRED:
                    hasConfigurationRequired =
                            true;
                    break;

                case SATISFIED:
                default:
                    break;
            }
        }

        VerifiedTransportState state;

        if (hasLockedRequirement)
        {
            state =
                    VerifiedTransportState.LOCKED;
        }
        else if (hasMissingRequirement)
        {
            state =
                    VerifiedTransportState.MISSING;
        }
        else if (hasDataRequired)
        {
            state =
                    VerifiedTransportState.DATA_REQUIRED;
        }
        else if (hasConfigurationRequired)
        {
            state =
                    VerifiedTransportState.CONFIGURATION_REQUIRED;
        }
        else
        {
            state =
                    VerifiedTransportState.LOCKED;
        }

        return new VerifiedTransportResult(
                option,
                state,
                blockers.toString()
        );
    }

    private VerifiedTransportResult
    evaluatePohJewelleryBoxSkillsNecklace(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        if (config == null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    "Configure your POH profile in Farming Navigator."
            );
        }

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new PohJewelleryBoxSkillsNecklaceRouteRequirement()
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - POH entry method and configured Fancy/Ornate jewellery box Skills necklace teleport are ready."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluatePohHouseFeature(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option,
            PohFeature feature
    )
    {
        if (config == null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    "Configure your POH profile in Farming Navigator."
            );
        }

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new PohHouseFeatureRouteRequirement(
                        feature
                )
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - POH entry method and configured "
                            + feature.getDisplayName()
                            + " are ready."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluatePohPortal(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        PohPortalDestination destination =
                option.getPohPortalDestination();

        if (destination == null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    "DATA REQUIRED - POH portal destination is missing."
            );
        }

        if (config == null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    "Configure your POH profile in Farming Navigator."
            );
        }

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        RequirementResult requirementResult =
                new PohPortalRouteRequirement(
                        destination
                )
                        .evaluate(
                                context
                        );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - POH entry method and configured "
                            + destination.getDisplayName()
                            + " portal destination are ready."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportResult
    evaluateTeleportSpell(
            AccountState accountState,
            FarmingNavigatorConfig config,
            TravelCapabilitySnapshot travelCapabilitySnapshot,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        TeleportSpellDefinition spellDefinition =
                option.getTeleportSpellDefinition();

        if (spellDefinition == null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    "DATA REQUIRED - teleport spell definition is missing."
            );
        }

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        config,
                        travelCapabilitySnapshot,
                        storageSnapshot
                );

        TeleportSpellRequirement requirement =
                spellDefinition
                        .createRequirement();

        RequirementResult requirementResult =
                requirement.evaluate(
                        context
                );

        if (
                requirementResult
                        .isReadyNow()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "READY NOW - "
                            + spellDefinition
                            .getDisplayName()
                            + " requirements are satisfied."
            );
        }

        if (
                requirementResult
                        .requiresBankWithdrawal()
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.DATA_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.DATA_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        if (
                requirementResult.getState()
                        == RequirementState.CONFIGURATION_REQUIRED
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.CONFIGURATION_REQUIRED,
                    requirementResult
                            .getDetail()
            );
        }

        return new VerifiedTransportResult(
                option,
                classifyBlockedSpellRequirement(
                        requirement,
                        context
                ),
                requirementResult
                        .getDetail()
        );
    }

    private VerifiedTransportState
    classifyBlockedSpellRequirement(
            TeleportSpellRequirement requirement,
            RequirementContext context
    )
    {
        boolean hasLockedRequirement =
                false;

        boolean hasMissingRequirement =
                false;

        boolean hasDataRequired =
                false;

        boolean hasConfigurationRequired =
                false;

        for (
                RouteRequirement component
                : requirement
                .getComponentRequirements()
        )
        {
            RequirementResult componentResult =
                    component.evaluate(
                            context
                    );

            if (
                    componentResult
                            .isSatisfied()
            )
            {
                continue;
            }

            if (
                    componentResult.getState()
                            == RequirementState.BLOCKED
            )
            {
                if (
                        component
                                instanceof RuneCostRequirement
                                || component
                                instanceof PohFeatureRequirement
                )
                {
                    hasMissingRequirement =
                            true;
                }
                else
                {
                    hasLockedRequirement =
                            true;
                }

                continue;
            }

            if (
                    componentResult.getState()
                            == RequirementState.DATA_REQUIRED
            )
            {
                hasDataRequired =
                        true;

                continue;
            }

            if (
                    componentResult.getState()
                            == RequirementState.CONFIGURATION_REQUIRED
            )
            {
                hasConfigurationRequired =
                        true;
            }
        }

        if (hasLockedRequirement)
        {
            return VerifiedTransportState.LOCKED;
        }

        if (hasMissingRequirement)
        {
            return VerifiedTransportState.MISSING;
        }

        if (hasDataRequired)
        {
            return VerifiedTransportState.DATA_REQUIRED;
        }

        if (hasConfigurationRequired)
        {
            return VerifiedTransportState.CONFIGURATION_REQUIRED;
        }

        return VerifiedTransportState.LOCKED;
    }

    private VerifiedTransportResult
    evaluateEctophial(
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        long carriedFull =
                storageSnapshot.getCarriedQuantity(
                        ItemID.ECTOPHIAL
                );

        long bankFull =
                storageSnapshot.getVerifiedBankQuantity(
                        ItemID.ECTOPHIAL
                );

        long emptyTotal =
                storageSnapshot.getCarriedQuantity(
                        ItemID.ECTOPHIAL_EMPTY
                )
                        + storageSnapshot
                        .getVerifiedBankQuantity(
                                ItemID.ECTOPHIAL_EMPTY
                        );

        if (carriedFull > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    "Filled Ectophial carried - ready now"
            );
        }

        if (bankFull > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    "Filled Ectophial in bank - withdraw before run"
            );
        }

        if (emptyTotal > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.MISSING,
                    "Only empty Ectophial found - refill it before this transport is usable"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "No filled Ectophial found in inventory, equipment, or verified bank"
        );
    }

    private VerifiedTransportResult
    evaluateBasalt(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            TransportOption option,
            int itemId,
            String itemName
    )
    {
        if (
                !accountState.hasCompletedQuest(
                        Quest.MAKING_FRIENDS_WITH_MY_ARM
                )
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    "LOCKED - requires completion of "
                            + Quest.MAKING_FRIENDS_WITH_MY_ARM
                            .getName()
                            + " before "
                            + itemName
                            + " can be used."
            );
        }

        return evaluateStackItem(
                storageSnapshot,
                option,
                itemId,
                itemName
        );
    }

    private VerifiedTransportResult
    evaluateStackItem(
            StorageSnapshot storageSnapshot,
            TransportOption option,
            int itemId,
            String itemName
    )
    {
        long inventoryQuantity =
                storageSnapshot.getInventoryQuantity(
                        itemId
                );

        long bankQuantity =
                storageSnapshot
                        .getVerifiedBankQuantity(
                                itemId
                        );

        if (inventoryQuantity > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    itemName
                            + ": "
                            + inventoryQuantity
                            + " in inventory, "
                            + bankQuantity
                            + " in bank"
            );
        }

        if (bankQuantity > 0)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    itemName
                            + ": "
                            + bankQuantity
                            + " in bank - withdraw 1 before run"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "No "
                        + itemName
                        + " found in inventory or verified bank"
        );
    }

    private VerifiedTransportResult
    evaluateFarmingCape(
            AccountState accountState,
            StorageSnapshot storageSnapshot,
            TransportOption option
    )
    {
        if (
                accountState.getRealFarmingLevel()
                        < 99
        )
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.LOCKED,
                    "Requires 99 Farming"
            );
        }

        String carriedVariant =
                findFirstCarriedVariant(
                        storageSnapshot,
                        FARMING_CAPE_IDS,
                        new String[] {
                                "Farming cape",
                                "Farming cape(t)",
                                "Max cape"
                        }
                );

        if (carriedVariant != null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.READY_NOW,
                    carriedVariant
                            + " carried - Farming Guild teleport ready"
            );
        }

        String bankVariant =
                findFirstBankVariant(
                        storageSnapshot,
                        FARMING_CAPE_IDS,
                        new String[] {
                                "Farming cape",
                                "Farming cape(t)",
                                "Max cape"
                        }
                );

        if (bankVariant != null)
        {
            return new VerifiedTransportResult(
                    option,
                    VerifiedTransportState.AVAILABLE_FROM_BANK,
                    bankVariant
                            + " in bank - withdraw before run"
            );
        }

        return new VerifiedTransportResult(
                option,
                VerifiedTransportState.MISSING,
                "99 Farming confirmed, but no supported Farming cape or Max cape found"
        );
    }

    private String findFirstCarriedVariant(
            StorageSnapshot storageSnapshot,
            int[] itemIds,
            String[] names
    )
    {
        for (
                int index = 0;
                index < itemIds.length;
                index++
        )
        {
            if (
                    storageSnapshot
                            .getCarriedQuantity(
                                    itemIds[index]
                            ) > 0
            )
            {
                return names[index];
            }
        }

        return null;
    }

    private String findFirstBankVariant(
            StorageSnapshot storageSnapshot,
            int[] itemIds,
            String[] names
    )
    {
        for (
                int index = 0;
                index < itemIds.length;
                index++
        )
        {
            if (
                    storageSnapshot
                            .getVerifiedBankQuantity(
                                    itemIds[index]
                            ) > 0
            )
            {
                return names[index];
            }
        }

        return null;
    }

    private int statePriority(
            VerifiedTransportResult result
    )
    {
        switch (result.getState())
        {
            case READY_NOW:
                return 0;

            case AVAILABLE_FROM_BANK:
                return 1;

            case DATA_REQUIRED:
                return 2;

            case CONFIGURATION_REQUIRED:
                return 3;

            case MISSING:
                return 4;

            case LOCKED:
            default:
                return 5;
        }
    }
}
