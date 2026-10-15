package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.loadout.SupplyCheck;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RunTransportResources
{
    private final List<RequirementResult> checks;
    private final List<String> notes;
    private final List<SupplyCheck> consumables;
    private final HerbHouseEntryMode houseEntryMode;

    RunTransportResources(List<RequirementResult> checks, List<String> notes)
    {
        this(checks, notes, Collections.emptyList());
    }

    RunTransportResources(List<RequirementResult> checks, List<String> notes, List<SupplyCheck> consumables)
    {
        this(checks, notes, consumables, null);
    }

    RunTransportResources(List<RequirementResult> checks, List<String> notes, List<SupplyCheck> consumables,
            HerbHouseEntryMode houseEntryMode)
    {
        this.checks = Collections.unmodifiableList(new ArrayList<>(checks));
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        this.consumables = Collections.unmodifiableList(new ArrayList<>(consumables));
        this.houseEntryMode = houseEntryMode;
    }

    public List<RequirementResult> getChecks() { return checks; }
    public List<String> getNotes() { return notes; }
    public List<SupplyCheck> getConsumables() { return consumables; }
    public HerbHouseEntryMode getHouseEntryMode() { return houseEntryMode; }
}
