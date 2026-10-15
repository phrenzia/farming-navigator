package com.farmingnavigator.contracts;

import java.util.HashSet;
import java.util.Set;
import net.runelite.client.plugins.timetracking.farming.Produce;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingContractSnapshotTest
{
    @Test public void all51AssignmentIdsMatchRuneLiteProduceAndMapToSupportedRunsWhenAvailable()
    {
        Set<Integer> ids = new HashSet<>();
        for (Produce produce : Produce.values())
        {
            int id = produce.getContractVarbitValue();
            if (id <= 0) { continue; }
            ContractCrop crop = ContractCrop.fromId(id); assertNotNull(produce.name(), crop);
            assertEquals(produce.name(), crop.name()); assertTrue(ids.add(id));
            assertEquals(FarmingContractSnapshot.State.ACTIVE, FarmingContractSnapshot.decode(true, id, 0).getState());
        }
        assertEquals(51, ids.size()); assertEquals(51, ContractCrop.values().length);
        for (ContractCrop crop : ContractCrop.values())
        {
            if (crop.getFamily() == ContractCrop.Family.HERB) { assertEquals(crop.name(), crop.getHerbRunCrop().name()); }
            if (crop.getFamily() == ContractCrop.Family.BUSH || crop.getFamily() == ContractCrop.Family.CACTUS)
            {
                assertEquals(crop.name(), crop.getGardenRunCrop().name());
            }
            if (crop.getFamily() == ContractCrop.Family.TREE || crop.getFamily() == ContractCrop.Family.FRUIT_TREE
                    || crop.getFamily() == ContractCrop.Family.REDWOOD || crop.getFamily() == ContractCrop.Family.CELASTRUS)
            {
                assertEquals(crop.name(), crop.getTreeRunCrop().name());
            }
        }
    }
    @Test public void completeFlagSuppressesStaleAssignmentWithoutClaimingRewardOwnership()
    {
        FarmingContractSnapshot result = FarmingContractSnapshot.decode(true, 11, 1);
        assertEquals(FarmingContractSnapshot.State.REPORTED_COMPLETE, result.getState()); assertNull(result.getCrop());
        assertEquals(FarmingContractSnapshot.State.REPORTED_COMPLETE, FarmingContractSnapshot.decode(true, 1000, 1).getState());
    }
    @Test public void absentUnknownAndLoggedOutAreDifferentStates()
    {
        assertEquals(FarmingContractSnapshot.State.NONE, FarmingContractSnapshot.decode(true, 0, 0).getState());
        assertEquals(FarmingContractSnapshot.State.UNKNOWN, FarmingContractSnapshot.decode(true, 52, 0).getState());
        assertEquals(FarmingContractSnapshot.State.UNKNOWN, FarmingContractSnapshot.decode(true, -1, 0).getState());
        assertEquals(FarmingContractSnapshot.State.UNKNOWN, FarmingContractSnapshot.decode(true, 11, -1).getState());
        assertEquals(FarmingContractSnapshot.State.LOGGED_OUT, FarmingContractSnapshot.decode(false, 11, 1).getState());
    }
}
