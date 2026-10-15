package com.farmingnavigator.routing.capability;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TravelCapabilityServiceCabbagePortTest
{
    @Test
    public void consumedCounterMapsToRemainingDailyTeleports()
    {
        for (int used = 0; used <= 3; used++)
        {
            assertEquals(Integer.valueOf(3 - used),
                    TravelCapabilityService.decodeCabbagePortRemainingUses(used));
        }
    }

    @Test
    public void unexpectedCountsRemainUnknown()
    {
        assertNull(TravelCapabilityService.decodeCabbagePortRemainingUses(-1));
        assertNull(TravelCapabilityService.decodeCabbagePortRemainingUses(4));
        assertNull(TravelCapabilityService.decodeCabbagePortRemainingUses(Integer.MAX_VALUE));
    }
}
