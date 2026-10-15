package com.farmingnavigator.transport;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.requirement.RuneKind;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class RemainingHerbSpellTransportVerificationTest
{
    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> routes()
    {
        return Arrays.asList(new Object[][] {
                {TeleportSpellDefinition.ARDOUGNE_TELEPORT, HerbPatchDefinition.ARDOUGNE,
                        7, 51, SpellbookType.STANDARD, Quest.PLAGUE_CITY},
                {TeleportSpellDefinition.FENKENSTRAINS_CASTLE_TELEPORT, HerbPatchDefinition.MORYTANIA,
                        4, 48, SpellbookType.ARCEUUS, Quest.PRIEST_IN_PERIL}
        });
    }

    private final TeleportSpellDefinition spell;
    private final HerbPatchDefinition patch;
    private final int rank;
    private final int level;
    private final SpellbookType spellbook;
    private final Quest quest;

    public RemainingHerbSpellTransportVerificationTest(TeleportSpellDefinition spell,
            HerbPatchDefinition patch, int rank, int level, SpellbookType spellbook, Quest quest)
    {
        this.spell = spell;
        this.patch = patch;
        this.rank = rank;
        this.level = level;
        this.spellbook = spellbook;
        this.quest = quest;
    }

    @Test
    public void catalogueAndDefinitionsMatchResearchedRequirements()
    {
        TransportOption option = option();
        assertEquals(spell, option.getTeleportSpellDefinition());
        assertEquals(spell.getDisplayName(), option.getDisplayName());
        assertEquals(TransportVerificationKind.TELEPORT_SPELL, option.getVerificationKind());
        assertEquals(TransportCategory.SPELL, option.getCategory());
        assertEquals(level, spell.getRequiredMagicLevel());
        assertEquals(spellbook, spell.getSpellbook());
        assertEquals(quest, spell.getRequiredQuest());
        Map<RuneKind, Integer> expected = new HashMap<>();
        if (spell == TeleportSpellDefinition.ARDOUGNE_TELEPORT)
        {
            expected.put(RuneKind.WATER, 2);
            expected.put(RuneKind.LAW, 2);
        }
        else
        {
            expected.put(RuneKind.EARTH, 1);
            expected.put(RuneKind.SOUL, 1);
            expected.put(RuneKind.LAW, 1);
        }
        assertEquals(expected, spell.getRuneCost().getQuantities());
    }

    @Test
    public void carriedRunesAreReadyNow()
    {
        VerifiedTransportResult result = evaluate(level, spellbook, QuestState.FINISHED,
                runes(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertEquals(VerifiedTransportState.READY_NOW, result.getState());
        assertTrue(result.isUsableForPlannedRun());
    }

    @Test
    public void bankedRunesRequireWithdrawal()
    {
        assertEquals(VerifiedTransportState.AVAILABLE_FROM_BANK, evaluate(level, spellbook,
                QuestState.FINISHED, Collections.emptyMap(), runes(),
                Collections.emptyMap(), Collections.emptyMap()).getState());
    }

    @Test
    public void unfinishedQuestIsLockedEvenWithRunes()
    {
        for (QuestState state : Arrays.asList(QuestState.NOT_STARTED, QuestState.IN_PROGRESS))
        {
            assertEquals(VerifiedTransportState.LOCKED, evaluate(level, spellbook, state,
                    runes(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()).getState());
        }
    }

    @Test
    public void insufficientMagicAndWrongSpellbookAreLocked()
    {
        assertEquals(VerifiedTransportState.LOCKED, evaluate(level - 1, spellbook, QuestState.FINISHED,
                runes(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()).getState());
        SpellbookType wrong = spellbook == SpellbookType.STANDARD ? SpellbookType.ANCIENT : SpellbookType.STANDARD;
        assertEquals(VerifiedTransportState.LOCKED, evaluate(level, wrong, QuestState.FINISHED,
                runes(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()).getState());
    }

    @Test
    public void missingLawRuneIsMissing()
    {
        Map<Integer, Long> supplies = runes();
        supplies.remove(ItemID.LAWRUNE);
        VerifiedTransportResult result = evaluate(level, spellbook, QuestState.FINISHED,
                supplies, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertEquals(VerifiedTransportState.MISSING, result.getState());
        assertFalse(result.isUsableForPlannedRun());
    }

    @Test
    public void carriedRunePouchSuppliesSpell()
    {
        assertEquals(VerifiedTransportState.READY_NOW, evaluate(level, spellbook, QuestState.FINISHED,
                Collections.singletonMap(ItemID.BH_RUNE_POUCH, 1L), Collections.emptyMap(),
                Collections.emptyMap(), runes()).getState());
    }

    @Test
    public void equippedElementalStaffReplacesElementalRunes()
    {
        Map<Integer, Long> supplies = runes();
        int rune = spell == TeleportSpellDefinition.ARDOUGNE_TELEPORT ? ItemID.WATERRUNE : ItemID.EARTHRUNE;
        int staff = spell == TeleportSpellDefinition.ARDOUGNE_TELEPORT ? ItemID.STAFF_OF_WATER : ItemID.STAFF_OF_EARTH;
        supplies.remove(rune);
        assertEquals(VerifiedTransportState.READY_NOW, evaluate(level, spellbook, QuestState.FINISHED,
                supplies, Collections.emptyMap(), Collections.singletonMap(staff, 1L), Collections.emptyMap()).getState());
    }

    private TransportOption option()
    {
        return new HerbPatchTransportService().getOptions(patch).stream()
                .filter(option -> option.getBaselineRank() == rank).findFirst().get();
    }

    private Map<Integer, Long> runes()
    {
        Map<Integer, Long> result = new HashMap<>();
        spell.getRuneCost().getQuantities().forEach((rune, quantity) ->
                result.put(rune.getCanonicalItemId(), quantity.longValue()));
        return result;
    }

    private Map<Integer, Integer> integers(Map<Integer, Long> source)
    {
        Map<Integer, Integer> result = new HashMap<>();
        source.forEach((id, quantity) -> result.put(id, quantity.intValue()));
        return result;
    }

    private VerifiedTransportResult evaluate(int magic, SpellbookType active, QuestState questState,
            Map<Integer, Long> inventory, Map<Integer, Long> bank,
            Map<Integer, Long> equipment, Map<Integer, Long> pouch)
    {
        AccountState account = new AccountState(true, 99, 99, magic, magic, 99, 99, 99, 99,
                Collections.singletonMap(quest, questState), Collections.emptyMap(), Collections.emptyMap(), false);
        TravelCapabilitySnapshot travel = new TravelCapabilitySnapshot(true, active, null,
                integers(inventory), integers(equipment), integers(pouch));
        StorageSnapshot storage = new StorageSnapshot(inventory, equipment, bank, true, Instant.now());
        Map<HerbPatchDefinition, List<VerifiedTransportResult>> results = new TransportVerificationService()
                .evaluateAll(account, travel, storage, Collections.singletonMap(patch, Collections.singletonList(option())));
        return results.get(patch).get(0);
    }
}
