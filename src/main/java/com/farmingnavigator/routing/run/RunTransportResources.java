package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.routing.requirement.RunePreparation;
import com.farmingnavigator.loadout.SupplyCheck;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public final class RunTransportResources
{
    private final List<RequirementResult> checks;
    private final List<String> notes;
    private final List<SupplyCheck> consumables;
    private final HerbHouseEntryMode houseEntryMode;
    private final Set<Integer> bankItems;
    private final RunePreparation runePreparation;

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
        this(checks, notes, consumables, houseEntryMode, Collections.emptySet());
    }

    RunTransportResources(List<RequirementResult> checks, List<String> notes, List<SupplyCheck> consumables,
            HerbHouseEntryMode houseEntryMode, Set<Integer> bankItems)
    {
        this(checks, notes, consumables, houseEntryMode, bankItems, null);
    }

    RunTransportResources(List<RequirementResult> checks, List<String> notes, List<SupplyCheck> consumables,
            HerbHouseEntryMode houseEntryMode, Set<Integer> bankItems, RunePreparation runePreparation)
    {
        this.checks = Collections.unmodifiableList(new ArrayList<>(checks));
        this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        this.consumables = Collections.unmodifiableList(new ArrayList<>(consumables));
        this.houseEntryMode = houseEntryMode;
        this.bankItems = Collections.unmodifiableSet(new HashSet<>(bankItems));
        this.runePreparation = runePreparation;
    }

    public List<RequirementResult> getChecks() { return checks; }
    public List<String> getNotes() { return notes; }
    public List<SupplyCheck> getConsumables() { return consumables; }
    public HerbHouseEntryMode getHouseEntryMode() { return houseEntryMode; }
    /** Banked candidates for pools that require withdrawal; not a minimal withdrawal solution. */
    public Set<Integer> getBankItems() { return bankItems; }
    public RunePreparation getRunePreparation() { return runePreparation; }
}
