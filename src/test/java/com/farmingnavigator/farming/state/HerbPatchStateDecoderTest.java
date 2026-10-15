package com.farmingnavigator.farming.state;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class HerbPatchStateDecoderTest
{
    @Parameterized.Parameters(name = "raw={0} {1} {2}")
    public static Collection<Object[]> upstreamStates() throws Exception
    {
        Collection<Object[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                HerbPatchStateDecoderTest.class.getResourceAsStream("/herb-patch-states.tsv"),
                StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#"))
                {
                    continue;
                }
                String[] columns = line.split("\t");
                rows.add(new Object[] {Integer.parseInt(columns[0]), PatchCondition.valueOf(columns[1]),
                        HerbCrop.valueOf(columns[2]), Integer.parseInt(columns[3])});
            }
        }
        assertEquals(256, rows.size());
        rows.add(new Object[] {-1, PatchCondition.UNKNOWN, HerbCrop.UNKNOWN, -1});
        rows.add(new Object[] {256, PatchCondition.UNKNOWN, HerbCrop.UNKNOWN, -1});
        rows.add(new Object[] {Integer.MAX_VALUE, PatchCondition.UNKNOWN, HerbCrop.UNKNOWN, -1});
        rows.add(new Object[] {Integer.MIN_VALUE, PatchCondition.UNKNOWN, HerbCrop.UNKNOWN, -1});
        return rows;
    }

    private final int raw;
    private final PatchCondition condition;
    private final HerbCrop crop;
    private final int stage;

    public HerbPatchStateDecoderTest(int raw, PatchCondition condition, HerbCrop crop, int stage)
    {
        this.raw = raw;
        this.condition = condition;
        this.crop = crop;
        this.stage = stage;
    }

    @Test
    public void decodesObservedStateWithoutInventingCropIdentity()
    {
        HerbPatchState result = new HerbPatchStateDecoder().decode(raw);
        assertEquals(raw, result.getRawValue());
        assertEquals(condition, result.getCondition());
        assertEquals(crop, result.getCrop());
        assertEquals(stage, result.getStage());
        assertEquals(condition != PatchCondition.UNKNOWN, result.isKnown());
    }
}
