package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.requirement.RequirementResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RunTransportResources
{
    private final List<RequirementResult> checks;
    private final List<String> notes;

    RunTransportResources(List<RequirementResult> checks, List<String> notes)
    {
        this.checks = Collections.unmodifiableList(new ArrayList<>(checks));
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
    }

    public List<RequirementResult> getChecks() { return checks; }
    public List<String> getNotes() { return notes; }
}
