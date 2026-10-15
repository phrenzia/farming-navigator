package com.farmingnavigator.ui;

import com.farmingnavigator.farming.state.HerbPatchObservation;
import com.farmingnavigator.farming.state.HerbPatchStateDecoder;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HerbPatchObservationTextTest
{
    @Test
    public void unobservedPatchRequestsData()
    {
        assertEquals("Patch state: DATA REQUIRED - visit this patch to observe it.",
                HerbPatchObservationText.describe(null, ZoneId.of("UTC")));
    }

    @Test
    public void observedHarvestIsTimestampedRatherThanClaimedAsLive()
    {
        assertEquals("Last observed 12:00:00: Ranarr - Ready to harvest.", describe(36));
        assertEquals("Last observed 12:00:00: Empty.", describe(0));
        assertEquals("Last observed 12:00:00: Weeds present.", describe(3));
    }

    @Test
    public void deadHerbDoesNotInventCropIdentityAndUnknownIsExplicit()
    {
        assertEquals("Last observed 12:00:00: Dead.", describe(170));
        assertEquals("Last observed 12:00:00: DATA REQUIRED - unrecognized patch state.", describe(220));
        assertFalse(describe(170).contains("Guam"));
    }

    @Test
    public void displaysObservationTimeInSelectedZone()
    {
        HerbPatchObservation observation = observation(36);
        assertEquals("Last observed 08:00:00: Ranarr - Ready to harvest.",
                HerbPatchObservationText.describe(observation, ZoneId.of("America/New_York")));
    }

    private String describe(int raw)
    {
        return HerbPatchObservationText.describe(observation(raw), ZoneId.of("UTC"));
    }

    private HerbPatchObservation observation(int raw)
    {
        return new HerbPatchObservation(new HerbPatchStateDecoder().decode(raw),
                Instant.parse("2026-10-15T12:00:00Z"));
    }
}
