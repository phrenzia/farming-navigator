package com.farmingnavigator.farming.field;

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
public class FieldPatchStateDecoderTest
{
    @Parameterized.Parameters(name = "{0} raw={1}") public static List<Object[]> cases() throws Exception
    {
        List<Object[]> cases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                FieldPatchStateDecoderTest.class.getResourceAsStream("/field-patch-states.tsv"), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.isEmpty()) { continue; }
                String[] fields = line.split("\t");
                cases.add(new Object[]{FieldFamily.valueOf(fields[0]), Integer.parseInt(fields[1]),
                        fields[2] + "|" + fields[3] + "|" + fields[4]});
            }
        }
        assertEquals(520, cases.size());
        return cases;
    }

    private final FieldFamily family;
    private final int raw;
    private final String expected;
    public FieldPatchStateDecoderTest(FieldFamily family, int raw, String expected)
    {
        this.family = family;
        this.raw = raw;
        this.expected = expected;
    }

    @Test public void matchesPinnedAllotmentAndFlowerStates()
    {
        FieldPatchState state = new FieldPatchStateDecoder().decode(family, raw);
        assertEquals(raw, state.getRawValue());
        assertEquals(expected, state.getCrop().name() + "|" + state.getCondition().name() + "|" + state.getStage());
    }
}
