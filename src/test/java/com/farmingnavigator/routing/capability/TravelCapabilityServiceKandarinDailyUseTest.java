package com.farmingnavigator.routing.capability;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TravelCapabilityServiceKandarinDailyUseTest
{
    @Test
    public void unusedSherlockVarbitMeansOneUseRemaining()
    {
        assertEquals(
                Integer.valueOf(
                        1
                ),
                TravelCapabilityService
                        .decodeKandarinSherlockRemainingUses(
                                0
                        )
        );
    }

    @Test
    public void usedSherlockVarbitMeansZeroUsesRemaining()
    {
        assertEquals(
                Integer.valueOf(
                        0
                ),
                TravelCapabilityService
                        .decodeKandarinSherlockRemainingUses(
                                1
                        )
        );
    }

    @Test
    public void unexpectedSherlockVarbitValuesRemainUnknown()
    {
        assertNull(
                TravelCapabilityService
                        .decodeKandarinSherlockRemainingUses(
                                -1
                        )
        );

        assertNull(
                TravelCapabilityService
                        .decodeKandarinSherlockRemainingUses(
                                2
                        )
        );

        assertNull(
                TravelCapabilityService
                        .decodeKandarinSherlockRemainingUses(
                                3
                        )
        );
    }
}