package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArdougneTeleportTabletRouteRequirementTest
{
    @Test
    public void completedQuestAndCarriedTabletIsReadyNow()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        inventory,
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.READY_NOW,
                result.getReadiness()
        );

        assertTrue(
                result.isReadyNow()
        );

        assertEquals(
                "Ardougne teleport tablet to East Ardougne",
                result.getRequirement()
        );
    }

    @Test
    public void completedQuestAndEquippedTabletIsReadyNow()
    {
        Map<Integer, Integer> equipment =
                new LinkedHashMap<>();

        equipment.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        equipment,
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void completedQuestAndBankedTabletIsAvailableFromBank()
    {
        Map<Integer, Long> bank =
                new LinkedHashMap<>();

        bank.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1L
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        bank,
                        true
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.AVAILABLE_FROM_BANK,
                result.getReadiness()
        );

        assertTrue(
                result.requiresBankWithdrawal()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Ardougne teleport tablet"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "AVAILABLE FROM BANK"
                        )
        );
    }

    @Test
    public void incompletePlagueCityIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.IN_PROGRESS,
                        inventory,
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Plague City"
                        )
        );
    }

    @Test
    public void notStartedPlagueCityIsBlocked()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.NOT_STARTED,
                        inventory,
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "completion of Plague City"
                        )
        );
    }

    @Test
    public void completedQuestWithoutTabletIsMissing()
    {
        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertEquals(
                RequirementReadiness.NOT_AVAILABLE,
                result.getReadiness()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "MISSING"
                        )
        );
    }

    @Test
    public void completedQuestWithUnknownBankRequiresData()
    {
        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false
                );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );

        assertFalse(
                result.isSatisfied()
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Bank verification required"
                        )
        );

        assertTrue(
                result.getDetail()
                        .contains(
                                "Open any normal bank once"
                        )
        );
    }

    @Test
    public void carriedTabletDoesNotNeedBankVerification()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        inventory,
                        Collections.emptyMap(),
                        false
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    @Test
    public void tabletDoesNotRequireStandardSpellbook()
    {
        Map<Integer, Integer> inventory =
                new LinkedHashMap<>();

        inventory.put(
                ItemID.POH_TABLET_ARDOUGNETELEPORT,
                1
        );

        RequirementResult result =
                evaluate(
                        QuestState.FINISHED,
                        SpellbookType.ANCIENT,
                        inventory,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        true
                );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );

        assertTrue(
                result.isReadyNow()
        );
    }

    private RequirementResult evaluate(
            QuestState plagueCityState,
            Map<Integer, Integer> inventory,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        return evaluate(
                plagueCityState,
                SpellbookType.STANDARD,
                inventory,
                Collections.emptyMap(),
                bank,
                bankVerified
        );
    }

    private RequirementResult evaluate(
            QuestState plagueCityState,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        return evaluate(
                plagueCityState,
                SpellbookType.STANDARD,
                inventory,
                equipment,
                bank,
                bankVerified
        );
    }

    private RequirementResult evaluate(
            QuestState plagueCityState,
            SpellbookType spellbook,
            Map<Integer, Integer> inventory,
            Map<Integer, Integer> equipment,
            Map<Integer, Long> bank,
            boolean bankVerified
    )
    {
        EnumMap<Quest, QuestState> questStates =
                new EnumMap<>(
                        Quest.class
                );

        questStates.put(
                Quest.PLAGUE_CITY,
                plagueCityState
        );

        AccountState accountState =
                new AccountState(
                        true,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        questStates,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        false
                );

        TravelCapabilitySnapshot travelSnapshot =
                new TravelCapabilitySnapshot(
                        true,
                        spellbook,
                        null,
                        inventory,
                        equipment,
                        Collections.emptyMap()
                );

        StorageSnapshot storageSnapshot =
                new StorageSnapshot(
                        convertToLongMap(
                                inventory
                        ),
                        convertToLongMap(
                                equipment
                        ),
                        bank,
                        bankVerified,
                        bankVerified
                                ? Instant.now()
                                : null
                );

        RequirementContext context =
                new RequirementContext(
                        accountState,
                        null,
                        travelSnapshot,
                        storageSnapshot
                );

        return new ArdougneTeleportTabletRouteRequirement()
                .evaluate(
                        context
                );
    }

    private Map<Integer, Long>
    convertToLongMap(
            Map<Integer, Integer> source
    )
    {
        Map<Integer, Long> result =
                new LinkedHashMap<>();

        for (
                Map.Entry<Integer, Integer> entry
                : source.entrySet()
        )
        {
            result.put(
                    entry.getKey(),
                    entry.getValue()
                            .longValue()
            );
        }

        return result;
    }
}