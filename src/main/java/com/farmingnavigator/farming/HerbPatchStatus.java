package com.farmingnavigator.farming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HerbPatchStatus
{
    private final HerbPatchDefinition patch;
    private final List<String> missingRequirements;

    public HerbPatchStatus(
            HerbPatchDefinition patch,
            List<String> missingRequirements
    )
    {
        this.patch = patch;
        this.missingRequirements = Collections.unmodifiableList(
                new ArrayList<>(missingRequirements)
        );
    }

    public HerbPatchDefinition getPatch()
    {
        return patch;
    }

    public boolean isUnlocked()
    {
        return missingRequirements.isEmpty();
    }

    public List<String> getMissingRequirements()
    {
        return missingRequirements;
    }
}