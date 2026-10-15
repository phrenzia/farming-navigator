package com.farmingnavigator.farming.garden;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class GardenPatchStateDecoderTest
{
    @Parameterized.Parameters(name = "{0} raw={1}") public static List<Object[]> cases() throws Exception
    {
        List<Object[]> cases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                GardenPatchStateDecoderTest.class.getResourceAsStream("/garden-patch-states.tsv"), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.isEmpty()) { continue; }
                String[] fields = line.split("\t");
                cases.add(new Object[]{GardenFamily.valueOf(fields[0]), Integer.parseInt(fields[1]),
                        fields[2] + "|" + fields[3] + "|" + fields[4]});
            }
        }
        assertEquals(520, cases.size());
        return cases;
    }

    private final GardenFamily family;
    private final int raw;
    private final String expected;
    public GardenPatchStateDecoderTest(GardenFamily family, int raw, String expected)
    {
        this.family = family;
        this.raw = raw;
        this.expected = expected;
    }

    @Test public void matchesPinnedBushAndCactusStates()
    {
        GardenPatchState state = new GardenPatchStateDecoder().decode(family, raw);
        assertEquals(raw, state.getRawValue());
        assertEquals(expected, state.getCrop().name() + "|" + state.getCondition().name() + "|" + state.getStage());
    }
}
