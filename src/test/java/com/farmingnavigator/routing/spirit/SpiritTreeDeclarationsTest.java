package com.farmingnavigator.routing.spirit;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpiritTreeDeclarationsTest
{
    @Test public void noProfileOrDisabledFeatureCannotAcceptDeclarations()
    {
        SpiritTreeDeclarations model=new SpiritTreeDeclarations(); assertTrue(model.snapshot().getTrees().isEmpty());
        assertFalse(model.apply(null,model.snapshot().getRevision(),Set.of(PlantedSpiritTree.PORT_SARIM)));
        assertNull(model.synchronize("A",false).getProfile());
        SpiritTreeDeclarations.Snapshot enabled=model.synchronize("A",true);
        assertTrue(model.apply("A",enabled.getRevision(),Set.of(PlantedSpiritTree.PORT_SARIM)));
        assertTrue(model.synchronize("A",false).getTrees().isEmpty()); assertNull(model.snapshot().getProfile());
    }
    @Test public void snapshotsAndAppliedInputAreImmutableAndOrdinaryRefreshKeepsAccess()
    {
        SpiritTreeDeclarations model=new SpiritTreeDeclarations(); SpiritTreeDeclarations.Snapshot before=model.synchronize("A",true);
        Set<PlantedSpiritTree> input=EnumSet.of(PlantedSpiritTree.HOSIDIUS); assertTrue(model.apply("A",before.getRevision(),input)); input.clear();
        SpiritTreeDeclarations.Snapshot after=model.snapshot(); assertEquals(Set.of(PlantedSpiritTree.HOSIDIUS),after.getTrees()); assertTrue(before.getTrees().isEmpty());
        assertEquals(after.getRevision(),model.synchronize("A",true).getRevision()); assertEquals(after.getTrees(),model.snapshot().getTrees());
        try { after.getTrees().clear(); fail("Mutable declarations"); } catch(UnsupportedOperationException expected) { }
    }
    @Test public void staleQueuedButtonsCannotOverwriteNewerSelections()
    {
        SpiritTreeDeclarations model=new SpiritTreeDeclarations(); SpiritTreeDeclarations.Snapshot first=model.synchronize("A",true);
        assertTrue(model.apply("A",first.getRevision(),Set.of(PlantedSpiritTree.PORT_SARIM)));
        assertFalse(model.apply("A",first.getRevision(),Set.of())); assertEquals(Set.of(PlantedSpiritTree.PORT_SARIM),model.snapshot().getTrees());
    }
    @Test public void characterAndModeChangesNeverRestoreAnotherScope()
    {
        SpiritTreeDeclarations model=new SpiritTreeDeclarations(); SpiritTreeDeclarations.Snapshot a=model.synchronize("A-normal",true);
        assertTrue(model.apply(a.getProfile(),a.getRevision(),Set.of(PlantedSpiritTree.BRIMHAVEN)));
        SpiritTreeDeclarations.Snapshot b=model.synchronize("B-normal",true); assertTrue(b.getTrees().isEmpty());
        assertFalse(model.apply(a.getProfile(),a.getRevision(),Set.of(PlantedSpiritTree.PORT_SARIM)));
        assertTrue(model.synchronize("A-normal",true).getTrees().isEmpty());
        assertTrue(model.synchronize("A-leagues",true).getTrees().isEmpty());
    }
    @Test public void explicitSessionEndClearsTreesAndInvalidatesOldCommands()
    {
        SpiritTreeDeclarations model=new SpiritTreeDeclarations(); SpiritTreeDeclarations.Snapshot scope=model.synchronize("A",true);
        model.apply("A",scope.getRevision(),Set.of(PlantedSpiritTree.FARMING_GUILD)); SpiritTreeDeclarations.Snapshot active=model.snapshot();
        model.clear(); assertNull(model.snapshot().getProfile()); assertTrue(model.snapshot().getTrees().isEmpty());
        assertFalse(model.apply("A",active.getRevision(),Set.of(PlantedSpiritTree.PORT_SARIM)));
        assertTrue(model.synchronize("A",true).getTrees().isEmpty());
    }
}
