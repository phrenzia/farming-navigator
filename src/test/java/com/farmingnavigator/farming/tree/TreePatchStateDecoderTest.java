package com.farmingnavigator.farming.tree;

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
public class TreePatchStateDecoderTest
{
    @Parameterized.Parameters(name = "{0} raw={1}") public static List<Object[]> cases() throws Exception
    {
        List<Object[]> cases = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                TreePatchStateDecoderTest.class.getResourceAsStream("/tree-patch-states.tsv"), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.isEmpty()) { continue; }
                String[] fields = line.split("\t");
                cases.add(new Object[]{TreeFamily.valueOf(fields[0]), Integer.parseInt(fields[1]),
                        fields[2] + "|" + fields[3] + "|" + fields[4]});
            }
        }
        assertEquals(780, cases.size());
        return cases;
    }

    private final TreeFamily family;
    private final int raw;
    private final String expected;
    public TreePatchStateDecoderTest(TreeFamily family, int raw, String expected)
    {
        this.family = family;
        this.raw = raw;
        this.expected = expected;
    }

    @Test public void matchesRuneLiteDecoderAndDistinguishesStumpsFromHealthChecks()
    {
        TreePatchState state = new TreePatchStateDecoder().decode(family, raw);
        assertEquals(raw, state.getRawValue());
        assertEquals(expected, state.getCrop().name() + "|" + state.getCondition().name() + "|" + state.getStage());
    }
}
