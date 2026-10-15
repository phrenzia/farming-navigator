package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.requirement.ChargedTeleportBudgetRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeTransportVerifierTest
{
    private RequirementContext context(Map<Quest, QuestState> quests, Map<Integer, Integer> carried, Map<Integer, Long> bank)
    {
        AccountState account = new AccountState(true, 99, 99, 99, 99, 99, 99, 99, 99, quests, Map.of(), Map.of());
        return new RequirementContext(account, null, new TravelCapabilitySnapshot(true, SpellbookType.STANDARD,
                null, carried, Map.of(), Map.of()), new StorageSnapshot(carried.entrySet().stream().collect(
                        Collectors.toMap(Map.Entry::getKey, e -> e.getValue().longValue())), Map.of(), bank, true, Instant.now()));
    }
    private VerifiedTransportResult result(TransportVerificationKind kind, RequirementContext context)
    {
        return new TreeTransportVerifier().evaluate(new TransportOption(1, "Tree transport", "Travel manually",
                TransportCategory.JEWELLERY, kind), context);
    }
    @Test public void wealthSpiritRouteRequiresVillageCompleteButNotGrandTreeForIncomingTravel()
    {
        Map<Integer, Integer> ring = Map.of(ItemID.RING_OF_WEALTH_1, 1);
        assertEquals(VerifiedTransportState.LOCKED, result(TransportVerificationKind.WEALTH_SPIRIT_TREE,
                context(Map.of(Quest.TREE_GNOME_VILLAGE, QuestState.IN_PROGRESS), ring, Map.of())).getState());
        assertEquals(VerifiedTransportState.READY_NOW, result(TransportVerificationKind.WEALTH_SPIRIT_TREE,
                context(Map.of(Quest.TREE_GNOME_VILLAGE, QuestState.FINISHED), ring, Map.of())).getState());
    }
    @Test public void digsiteBargeRequiresVoyageAndDoesNotAssumeMachineUnlock()
    {
        Map<Integer, Integer> pendant = Map.of(ItemID.NECKLACE_OF_DIGSITE_1, 1);
        assertEquals(VerifiedTransportState.LOCKED, result(TransportVerificationKind.DIGSITE_BARGE,
                context(Map.of(Quest.BONE_VOYAGE, QuestState.IN_PROGRESS), pendant, Map.of())).getState());
        assertEquals(VerifiedTransportState.READY_NOW, result(TransportVerificationKind.DIGSITE_BARGE,
                context(Map.of(Quest.BONE_VOYAGE, QuestState.FINISHED), pendant, Map.of())).getState());
    }
    @Test public void bankedPendantRequiresWithdrawal()
    {
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, result(TransportVerificationKind.DIGSITE_BARGE,
                context(Map.of(Quest.BONE_VOYAGE, QuestState.FINISHED), Map.of(), Map.of(ItemID.NECKLACE_OF_DIGSITE_5, 1L))).getState());
    }
    @Test public void crystalRequiresStartedQuestAndEmptySeedNeverCounts()
    {
        assertEquals(VerifiedTransportState.LOCKED, result(TransportVerificationKind.LLETYA_CRYSTAL,
                context(Map.of(), Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_1, 1), Map.of())).getState());
        assertEquals(VerifiedTransportState.READY_NOW, result(TransportVerificationKind.LLETYA_CRYSTAL,
                context(Map.of(Quest.MOURNINGS_END_PART_I, QuestState.IN_PROGRESS), Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_1, 1), Map.of())).getState());
        assertEquals(VerifiedTransportState.MISSING, result(TransportVerificationKind.LLETYA_CRYSTAL,
                context(Map.of(Quest.MOURNINGS_END_PART_I, QuestState.IN_PROGRESS), Map.of(ItemID.ELF_CRYSTAL_TINY, 1), Map.of())).getState());
    }
    @Test public void allFiveFiniteCrystalVariantsHaveExactCapacity()
    {
        int[] ids = {ItemID.MOURNING_TELEPORT_CRYSTAL_1, ItemID.MOURNING_TELEPORT_CRYSTAL_2,
                ItemID.MOURNING_TELEPORT_CRYSTAL_3, ItemID.MOURNING_TELEPORT_CRYSTAL_4, ItemID.MOURNING_TELEPORT_CRYSTAL_5};
        for (int i = 0; i < ids.length; i++)
        {
            RequirementContext context = context(Map.of(), Map.of(ids[i], 1), Map.of());
            assertTrue(new ChargedTeleportBudgetRequirement(ChargedTeleportItemDefinition.TELEPORT_CRYSTAL, i + 1).evaluate(context).isReadyNow());
            assertFalse(new ChargedTeleportBudgetRequirement(ChargedTeleportItemDefinition.TELEPORT_CRYSTAL, i + 2).evaluate(context).isReadyNow());
        }
    }

    @Test public void prifddinasDestinationRequiresFinishedQuestAndRealChargedCrystal()
    {
        TransportVerificationKind kind = TransportVerificationKind.PRIFDDINAS_CRYSTAL;
        assertEquals(VerifiedTransportState.LOCKED, result(kind, context(Map.of(Quest.SONG_OF_THE_ELVES, QuestState.IN_PROGRESS),
                Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_1, 1), Map.of())).getState());
        Map<Quest, QuestState> quests = Map.of(Quest.SONG_OF_THE_ELVES, QuestState.FINISHED);
        assertEquals(VerifiedTransportState.READY_NOW, result(kind,
                context(quests, Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_1, 1), Map.of())).getState());
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, result(kind,
                context(quests, Map.of(), Map.of(ItemID.PRIF_TELEPORT_CRYSTAL, 1L))).getState());
        assertEquals(VerifiedTransportState.MISSING, result(kind,
                context(quests, Map.of(ItemID.ELF_CRYSTAL_TINY, 1), Map.of())).getState());
    }
    @Test public void eternalCrystalSupportsManyUsesAndBankedFiniteCrystalsCombine()
    {
        assertTrue(new ChargedTeleportBudgetRequirement(ChargedTeleportItemDefinition.TELEPORT_CRYSTAL, 100)
                .evaluate(context(Map.of(), Map.of(ItemID.PRIF_TELEPORT_CRYSTAL, 1), Map.of())).isReadyNow());
        assertTrue(new ChargedTeleportBudgetRequirement(ChargedTeleportItemDefinition.TELEPORT_CRYSTAL, 6)
                .evaluate(context(Map.of(), Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_1, 1),
                        Map.of(ItemID.MOURNING_TELEPORT_CRYSTAL_5, 1L))).requiresBankWithdrawal());
    }
}
