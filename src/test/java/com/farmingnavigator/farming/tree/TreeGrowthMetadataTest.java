package com.farmingnavigator.farming.tree;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeGrowthMetadataTest
{
    @Test public void metadataMatchesRuneLiteProduceForEverySupportedTree() throws Exception
    {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/tree-growth-metadata.tsv"), StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith("#") || line.isEmpty()) { continue; }
                String[] fields = line.split("\t");
                TreeCrop crop = TreeCrop.valueOf(fields[0]);
                assertEquals(TreeFamily.valueOf(fields[1]), crop.getFamily());
                assertEquals(Integer.parseInt(fields[2]), crop.getGrowthCycleMinutes());
                assertEquals(Integer.parseInt(fields[3]), crop.getStages());
                count++;
            }
        }
        assertEquals(TreeCrop.values().length - 1, count);
    }

    @Test public void missingFamilyDoesNotGuessTreeSemantics()
    {
        assertFalse(new TreePatchStateDecoder().decode(null, 12).isKnown());
    }
}
