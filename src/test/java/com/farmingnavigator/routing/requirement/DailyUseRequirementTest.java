package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.routing.capability.DailyTransportUseKind;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DailyUseRequirementTest
{
    @Test
    public void knownRemainingUseIsReadyNow()
    {
        Map<
                DailyTransportUseKind,
                Integer
                > dailyUses =
                new EnumMap<>(
                        DailyTransportUseKind.class
                );

        dailyUses.put(
                DailyTransportUseKind
                        .EXPLORER_RING_CABBAGE_PORT,
                2
        );

        RequirementResult result =
                evaluate(
                        DailyTransportUseKind
                                .EXPLORER_RING_CABBAGE_PORT,
                        1,
                        dailyUses
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

        assertTrue(
                result.getDetail()
                        .contains(
                                "2 daily uses remaining"
                        )
        );
    }

    @Test
    public void verifiedZeroUsesIsBlocked()
    {
        Map<
                DailyTransportUseKind,
                Integer
                > dailyUses =
                new EnumMap<>(
                        DailyTransportUseKind.class
                );

        dailyUses.put(
                DailyTransportUseKind
                        .EXPLORER_RING_CABBAGE_PORT,
                0
        );

        RequirementResult result =
                evaluate(
                        DailyTransportUseKind
                                .EXPLORER_RING_CABBAGE_PORT,
                        1,
                        dailyUses
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
                                "0 remain today"
                        )
        );
    }

    @Test
    public void unknownUseDataRequiresData()
    {
        RequirementResult result =
                evaluate(
                        DailyTransportUseKind
                                .EXPLORER_RING_CABBAGE_PORT,
                        1,
                        Collections.emptyMap()
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
                                "have not been verified"
                        )
        );
    }

    @Test
    public void multipleRequiredUsesAreSupported()
    {
        Map<
                DailyTransportUseKind,
                Integer
                > dailyUses =
                new EnumMap<>(
                        DailyTransportUseKind.class
                );

        dailyUses.put(
                DailyTransportUseKind
                        .ARDOUGNE_FARM_TELEPORT,
                1
        );

        RequirementResult result =
                evaluate(
                        DailyTransportUseKind
                                .ARDOUGNE_FARM_TELEPORT,
                        2,
                        dailyUses
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
                                "requires at least 2"
                        )
        );
    }

    @Test
    public void zeroRemainsKnownInSnapshot()
    {
        Map<
                DailyTransportUseKind,
                Integer
                > dailyUses =
                new EnumMap<>(
                        DailyTransportUseKind.class
                );

        dailyUses.put(
                DailyTransportUseKind
                        .ARDOUGNE_FARM_TELEPORT,
                0
        );

        TravelCapabilitySnapshot snapshot =
                createSnapshot(
                        dailyUses
                );

        assertTrue(
                snapshot.hasDailyUseData(
                        DailyTransportUseKind
                                .ARDOUGNE_FARM_TELEPORT
                )
        );

        assertEquals(
                0,
                snapshot.getRemainingDailyUses(
                        DailyTransportUseKind
                                .ARDOUGNE_FARM_TELEPORT
                )
        );

        assertFalse(
                snapshot.hasRemainingDailyUse(
                        DailyTransportUseKind
                                .ARDOUGNE_FARM_TELEPORT
                )
        );
    }

    @Test
    public void absentDailyUseRemainsUnknown()
    {
        TravelCapabilitySnapshot snapshot =
                createSnapshot(
                        Collections.emptyMap()
                );

        assertFalse(
                snapshot.hasDailyUseData(
                        DailyTransportUseKind
                                .EXPLORER_RING_CABBAGE_PORT
                )
        );
    }

    @Test
    public void negativeDailyUseIsRejectedAsUnknownData()
    {
        Map<
                DailyTransportUseKind,
                Integer
                > dailyUses =
                new EnumMap<>(
                        DailyTransportUseKind.class
                );

        dailyUses.put(
                DailyTransportUseKind
                        .EXPLORER_RING_CABBAGE_PORT,
                -1
        );

        TravelCapabilitySnapshot snapshot =
                createSnapshot(
                        dailyUses
                );

        assertFalse(
                snapshot.hasDailyUseData(
                        DailyTransportUseKind
                                .EXPLORER_RING_CABBAGE_PORT
                )
        );
    }

    @Test
    public void snapshotDefensivelyCopiesDailyUses()
    {
        Map<
                DailyTransportUseKind,
                Integer
                > dailyUses =
                new EnumMap<>(
                        DailyTransportUseKind.class
                );

        dailyUses.put(
                DailyTransportUseKind
                        .EXPLORER_RING_CABBAGE_PORT,
                2
        );

        TravelCapabilitySnapshot snapshot =
                createSnapshot(
                        dailyUses
                );

        dailyUses.put(
                DailyTransportUseKind
                        .EXPLORER_RING_CABBAGE_PORT,
                0
        );

        assertEquals(
                2,
                snapshot.getRemainingDailyUses(
                        DailyTransportUseKind
                                .EXPLORER_RING_CABBAGE_PORT
                )
        );
    }

    @Test
    public void compatibilityConstructorLeavesDailyUseUnknown()
    {
        TravelCapabilitySnapshot snapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.STANDARD,
                        null,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap()
                );

        assertFalse(
                snapshot.hasDailyUseData(
                        DailyTransportUseKind
                                .EXPLORER_RING_CABBAGE_PORT
                )
        );

        assertFalse(
                snapshot.hasDailyUseData(
                        DailyTransportUseKind
                                .ARDOUGNE_FARM_TELEPORT
                )
        );
    }

    @Test
    public void loggedOutSnapshotHasNoDailyUseData()
    {
        TravelCapabilitySnapshot snapshot =
                TravelCapabilitySnapshot
                        .loggedOut();

        assertFalse(
                snapshot.hasDailyUseData(
                        DailyTransportUseKind
                                .EXPLORER_RING_CABBAGE_PORT
                )
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDailyUseKindIsRejected()
    {
        new DailyUseRequirement(
                null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroRequiredUsesIsRejected()
    {
        new DailyUseRequirement(
                "Invalid daily requirement",
                DailyTransportUseKind
                        .EXPLORER_RING_CABBAGE_PORT,
                0
        );
    }

    private RequirementResult evaluate(
            DailyTransportUseKind kind,
            int requiredUses,
            Map<
                    DailyTransportUseKind,
                    Integer
                    > dailyUses
    )
    {
        TravelCapabilitySnapshot snapshot =
                createSnapshot(
                        dailyUses
                );

        RequirementContext context =
                new RequirementContext(
                        null,
                        null,
                        snapshot,
                        null
                );

        return new DailyUseRequirement(
                kind.getDisplayName(),
                kind,
                requiredUses
        )
                .evaluate(
                        context
                );
    }

    private TravelCapabilitySnapshot
    createSnapshot(
            Map<
                    DailyTransportUseKind,
                    Integer
                    > dailyUses
    )
    {
        return new TravelCapabilitySnapshot(
                true,
                SpellbookType.STANDARD,
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                false,
                dailyUses
        );
    }
}