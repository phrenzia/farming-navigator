package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementState;
import com.farmingnavigator.routing.requirement.RuneCostRequirement;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import com.farmingnavigator.transport.TransportCategory;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.TransportVerificationKind;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunTransportResourcePlannerTest
{
    @Test
    public void individuallyReadySpellsCannotReuseLastLawRune()
    {
        RequirementContext context = context(Map.of(ItemID.AIRRUNE, 6, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 1),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertTrue(new RuneCostRequirement("One spell", TeleportSpellDefinition.FALADOR_TELEPORT.getRuneCost())
                .evaluate(context).isReadyNow());
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(twoSpellStops(), context);
        assertEquals(RequirementState.BLOCKED, result.getChecks().get(0).getState());
        assertTrue(result.getChecks().get(0).getDetail().contains("Law rune x2"));
    }

    @Test
    public void combinedSpellRunesCanBePreparedFromBank()
    {
        RequirementContext context = context(Map.of(ItemID.AIRRUNE, 6, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 1),
                Collections.emptyMap(), Collections.singletonMap(ItemID.LAWRUNE, 1L), Collections.emptyMap());
        assertTrue(new RunTransportResourcePlanner().evaluate(twoSpellStops(), context).getChecks().get(0).requiresBankWithdrawal());
        assertTrue(new RunTransportResourcePlanner().evaluate(twoSpellStops(), context).getBankItems().contains(ItemID.LAWRUNE));
    }

    @Test
    public void sharedRuneBudgetUsesEquippedStaffAndCarriedPouch()
    {
        RequirementContext staff = context(Map.of(ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2),
                Collections.singletonMap(ItemID.STAFF_OF_AIR, 1), Collections.emptyMap(), Collections.emptyMap());
        assertTrue(new RunTransportResourcePlanner().evaluate(twoSpellStops(), staff).getChecks().get(0).isReadyNow());
        RequirementContext pouch = context(Collections.singletonMap(ItemID.BH_RUNE_POUCH, 1), Collections.emptyMap(),
                Collections.emptyMap(), Map.of(ItemID.AIRRUNE, 6, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2));
        assertTrue(new RunTransportResourcePlanner().evaluate(twoSpellStops(), pouch).getChecks().get(0).isReadyNow());
    }

    @Test
    public void fallbackSpellIsNotAddedToRecommendedSpellCost()
    {
        HerbRunStop stop = new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", spell(), spell(), true);
        RequirementContext context = context(Map.of(ItemID.AIRRUNE, 3, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 1),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertTrue(new RunTransportResourcePlanner().evaluate(Collections.singletonList(stop), context).getChecks().get(0).isReadyNow());
    }

    @Test
    public void jewelleryUsesAreAggregatedAcrossDestinations()
    {
        VerifiedTransportResult necklace = new VerifiedTransportResult(new TransportOption(1, "Skills necklace", "Teleport",
                TransportCategory.JEWELLERY, TransportVerificationKind.SKILLS_NECKLACE), VerifiedTransportState.READY_NOW, "");
        List<HerbRunStop> stops = Arrays.asList(new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", necklace, null, true),
                new HerbRunStop(HerbPatchDefinition.FARMING_GUILD, "Inspect", necklace, null, true));
        RequirementContext context = context(Collections.singletonMap(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(stops, context);
        assertEquals(1, result.getChecks().size());
        assertEquals(RequirementState.BLOCKED, result.getChecks().get(0).getState());
        assertTrue(result.getChecks().get(0).getRequirement().contains("2 run teleports"));
    }

    @Test
    public void sharedJewelleryWithdrawalTargetsOnlyBankedChargedVariants()
    {
        VerifiedTransportResult necklace = consumable(TransportVerificationKind.SKILLS_NECKLACE);
        List<HerbRunStop> stops = Arrays.asList(new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", necklace, null, true),
                new HerbRunStop(HerbPatchDefinition.FARMING_GUILD, "Inspect", necklace, null, true));
        RequirementContext context = context(Map.of(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1), Collections.emptyMap(),
                Map.of(ItemID.JEWL_NECKLACE_OF_SKILLS_1, 1L, ItemID.JEWL_NECKLACE_OF_SKILLS, 1L), Collections.emptyMap());
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(stops, context);
        assertTrue(result.getChecks().get(0).requiresBankWithdrawal());
        assertEquals(Collections.singleton(ItemID.JEWL_NECKLACE_OF_SKILLS_1), result.getBankItems());
    }

    @Test
    public void satisfiedSharedRuneBudgetDoesNotHighlightUnneededBankRunes()
    {
        RequirementContext context = context(Map.of(ItemID.AIRRUNE, 6, ItemID.WATERRUNE, 2, ItemID.LAWRUNE, 2),
                Collections.emptyMap(), Map.of(ItemID.LAWRUNE, 10L), Collections.emptyMap());
        assertTrue(new RunTransportResourcePlanner().evaluate(twoSpellStops(), context).getBankItems().isEmpty());
    }

    @Test
    public void missingRouteRemainsAnExplicitPlanningNote()
    {
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(Collections.singletonList(
                new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", null, null, true)), null);
        assertTrue(result.getChecks().isEmpty());
        assertEquals("Choose transport for Falador.", result.getNotes().get(0));
    }

    @Test
    public void singleUseTeleportsAreCountedAcrossTheWholeRun()
    {
        VerifiedTransportResult tablet = consumable(TransportVerificationKind.ARDOUGNE_TELEPORT_TABLET);
        List<HerbRunStop> stops = Arrays.asList(new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", tablet, tablet, true),
                new HerbRunStop(HerbPatchDefinition.ARDOUGNE, "Inspect", tablet, null, true));
        SupplyCheck check = new RunTransportResourcePlanner().evaluate(stops,
                context(Map.of(ItemID.POH_TABLET_ARDOUGNETELEPORT, 1), Collections.emptyMap(),
                        Collections.emptyMap(), Collections.emptyMap())).getConsumables().get(0);
        assertEquals(2, check.getRequirement().getQuantity());
        assertEquals(SupplyState.MISSING, check.getState());
        assertEquals(1, check.getMissingQuantity());
    }

    @Test
    public void consumableBudgetCanWithdrawTheShortfallFromBank()
    {
        VerifiedTransportResult tablet = consumable(TransportVerificationKind.ARDOUGNE_TELEPORT_TABLET);
        List<HerbRunStop> stops = Arrays.asList(new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", tablet, null, true),
                new HerbRunStop(HerbPatchDefinition.ARDOUGNE, "Inspect", tablet, null, true));
        SupplyCheck check = new RunTransportResourcePlanner().evaluate(stops,
                context(Map.of(ItemID.POH_TABLET_ARDOUGNETELEPORT, 1), Collections.emptyMap(),
                        Map.of(ItemID.POH_TABLET_ARDOUGNETELEPORT, 1L), Collections.emptyMap())).getConsumables().get(0);
        assertEquals(SupplyState.AVAILABLE_FROM_BANK, check.getState());
        assertEquals(1, check.getWithdrawQuantity());
        assertEquals(0, check.getMissingQuantity());
    }

    @Test
    public void basaltAndHarmonyScrollUseTheirExistingVerifiedItemIds()
    {
        TransportVerificationKind[] kinds = {TransportVerificationKind.STONY_BASALT, TransportVerificationKind.ICY_BASALT,
                TransportVerificationKind.MOS_LE_HARMLESS_HARMONY};
        int[] ids = {ItemID.STRONGHOLD_TELEPORT_BASALT, ItemID.WEISS_TELEPORT_BASALT, ItemID.TELEPORTSCROLL_MOSLES};
        for (int i = 0; i < kinds.length; i++)
        {
            SupplyCheck check = new RunTransportResourcePlanner().evaluate(Collections.singletonList(
                    new HerbRunStop(HerbPatchDefinition.WEISS, "Inspect", consumable(kinds[i]), null, true)),
                    context(Map.of(ids[i], 1), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()))
                    .getConsumables().get(0);
            assertEquals(ids[i], check.getRequirement().getItemId());
            assertEquals(SupplyState.ALREADY_CARRIED, check.getState());
        }
    }

    @Test
    public void consumableBudgetDoesNotGuessWhenStorageIsUnknown()
    {
        SupplyCheck check = new RunTransportResourcePlanner().evaluate(Collections.singletonList(
                new HerbRunStop(HerbPatchDefinition.WEISS, "Inspect", consumable(TransportVerificationKind.ICY_BASALT), null, true)),
                null).getConsumables().get(0);
        assertEquals(SupplyState.DATA_REQUIRED, check.getState());
        assertEquals(0, check.getMissingQuantity());
    }

    private VerifiedTransportResult consumable(TransportVerificationKind kind)
    {
        return new VerifiedTransportResult(new TransportOption(1, "Consumable", "Use manually", TransportCategory.ITEM,
                kind), VerifiedTransportState.READY_NOW, "");
    }

    @Test
    public void eachHouseVisitReservesATabletAndBankCanSupplyShortfall()
    {
        RequirementContext base = context(Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Collections.emptyMap(),
                Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1L), Collections.emptyMap());
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(twoHouseStops(), houseContext(base, HerbHouseEntryMode.TABLETS));
        SupplyCheck check = result.getConsumables().get(0);
        assertEquals(2, check.getRequirement().getQuantity());
        assertEquals(1, check.getWithdrawQuantity());
        assertEquals(SupplyState.AVAILABLE_FROM_BANK, check.getState());
        assertEquals(HerbHouseEntryMode.TABLETS, result.getHouseEntryMode());
        assertTrue(result.getNotes().get(0).contains("2 x House tablets"));
    }

    @Test
    public void singleTabletCannotFundTwoHouseVisits()
    {
        RequirementContext base = context(Map.of(ItemID.POH_TABLET_TELEPORTTOHOUSE, 1), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap());
        SupplyCheck check = new RunTransportResourcePlanner().evaluate(twoHouseStops(), houseContext(base, HerbHouseEntryMode.TABLETS))
                .getConsumables().get(0);
        assertEquals(SupplyState.MISSING, check.getState());
        assertEquals(1, check.getMissingQuantity());
    }

    @Test
    public void houseCastsAndDirectTeleportsShareOneLawRuneBudget()
    {
        RequirementContext base = context(Map.of(ItemID.AIRRUNE, 5, ItemID.EARTHRUNE, 2, ItemID.WATERRUNE, 1, ItemID.LAWRUNE, 2),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        List<HerbRunStop> stops = new java.util.ArrayList<>(twoHouseStops());
        stops.add(new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", spell(), null, true));
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(stops, houseContext(base, HerbHouseEntryMode.SPELL));
        assertEquals(RequirementState.BLOCKED, result.getChecks().get(0).getState());
        assertTrue(result.getChecks().get(0).getDetail().contains("Law rune x3"));
        assertTrue(result.getConsumables().isEmpty());
    }

    @Test
    public void equippedInfiniteSourcesAndBankedRunesCanSupportAllHouseCasts()
    {
        RequirementContext base = context(Map.of(ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 1), Map.of(ItemID.STAFF_OF_AIR, 1),
                Map.of(ItemID.LAWRUNE, 1L), Collections.emptyMap());
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(twoHouseStops(), houseContext(base, HerbHouseEntryMode.SPELL));
        assertTrue(result.getChecks().get(0).requiresBankWithdrawal());
        assertTrue(result.getChecks().get(1).isSatisfied());
    }

    @Test
    public void houseSpellModeStillRequiresCorrectSpellbookAndConfiguredHouse()
    {
        RequirementContext base = context(Map.of(ItemID.AIRRUNE, 2, ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 2),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        TravelCapabilitySnapshot wrongBook = new TravelCapabilitySnapshot(true, SpellbookType.ANCIENT, null,
                Map.of(ItemID.AIRRUNE, 2, ItemID.EARTHRUNE, 2, ItemID.LAWRUNE, 2), Collections.emptyMap(), Collections.emptyMap());
        RequirementContext configured = houseContext(base, HerbHouseEntryMode.SPELL);
        RequirementContext wrong = new RequirementContext(base.getAccountState(), configured.getConfig(), wrongBook, base.getStorageSnapshot());
        assertEquals(RequirementState.BLOCKED, new RunTransportResourcePlanner().evaluate(twoHouseStops(), wrong).getChecks().get(1).getState());
        FarmingNavigatorConfig noHouse = new FarmingNavigatorConfig()
        {
            @Override public HerbHouseEntryMode herbHouseEntryMode() { return HerbHouseEntryMode.SPELL; }
        };
        RequirementContext unconfigured = new RequirementContext(base.getAccountState(), noHouse,
                base.getTravelCapabilitySnapshot(), base.getStorageSnapshot());
        assertEquals(RequirementState.BLOCKED, new RunTransportResourcePlanner()
                .evaluate(twoHouseStops(), unconfigured).getChecks().get(1).getState());
    }

    @Test
    public void houseEntryWithUnknownStorageRemainsDataRequired()
    {
        RunTransportResources result = new RunTransportResourcePlanner().evaluate(twoHouseStops(), null);
        assertEquals(RequirementState.DATA_REQUIRED, result.getChecks().get(0).getState());
        assertEquals(SupplyState.DATA_REQUIRED, result.getConsumables().get(0).getState());
    }

    private RequirementContext houseContext(RequirementContext base, HerbHouseEntryMode mode)
    {
        FarmingNavigatorConfig config = new FarmingNavigatorConfig()
        {
            @Override public HerbHouseEntryMode herbHouseEntryMode() { return mode; }
            @Override public PohHouseLocation pohHouseLocation() { return PohHouseLocation.RIMMINGTON; }
        };
        return new RequirementContext(base.getAccountState(), config, base.getTravelCapabilitySnapshot(), base.getStorageSnapshot());
    }

    private List<HerbRunStop> twoHouseStops()
    {
        VerifiedTransportResult house = new VerifiedTransportResult(new TransportOption(1, "POH feature", "Enter house manually",
                TransportCategory.PLAYER_OWNED_HOUSE, TransportVerificationKind.POH_MOUNTED_XERICS_TALISMAN),
                VerifiedTransportState.READY_NOW, "");
        return Arrays.asList(new HerbRunStop(HerbPatchDefinition.HOSIDIUS, "Inspect", house, null, true),
                new HerbRunStop(HerbPatchDefinition.CATHERBY, "Inspect", house, null, true));
    }

    private List<HerbRunStop> twoSpellStops()
    {
        return Arrays.asList(new HerbRunStop(HerbPatchDefinition.FALADOR, "Inspect", spell(), null, true),
                new HerbRunStop(HerbPatchDefinition.CATHERBY, "Inspect", spell(), null, true));
    }

    private VerifiedTransportResult spell()
    {
        return new VerifiedTransportResult(new TransportOption(1, "Falador Teleport", "Cast manually", TransportCategory.SPELL,
                TransportVerificationKind.TELEPORT_SPELL, TeleportSpellDefinition.FALADOR_TELEPORT), VerifiedTransportState.READY_NOW, "");
    }

    private RequirementContext context(Map<Integer, Integer> inventory, Map<Integer, Integer> equipment,
            Map<Integer, Long> bank, Map<Integer, Integer> pouch)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), false);
        Map<Integer, Long> inv = new HashMap<>();
        Map<Integer, Long> eq = new HashMap<>();
        inventory.forEach((id, quantity) -> inv.put(id, quantity.longValue()));
        equipment.forEach((id, quantity) -> eq.put(id, quantity.longValue()));
        return new RequirementContext(account, null, new TravelCapabilitySnapshot(true, SpellbookType.STANDARD, null,
                inventory, equipment, pouch), new StorageSnapshot(inv, eq, bank, true, Instant.now()));
    }
}
