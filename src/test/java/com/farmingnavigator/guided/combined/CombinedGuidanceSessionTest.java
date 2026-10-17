package com.farmingnavigator.guided.combined;

import com.farmingnavigator.guided.ChecklistCommandGuard;
import com.farmingnavigator.routing.run.CombinedRunKind;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.farmingnavigator.guided.combined.CombinedGuidanceSession.Progress.*;
import static com.farmingnavigator.guided.combined.CombinedGuidanceSession.State.BETWEEN;
import static com.farmingnavigator.routing.run.CombinedRunKind.*;

public class CombinedGuidanceSessionTest
{
    @Test public void everySelectedSubsetKeepsCatalogueOrderAndWaitsForExplicitNext()
    {
        for (int mask = 0; mask < 16; mask++)
        {
            Set<CombinedRunKind> selected = EnumSet.noneOf(CombinedRunKind.class);
            for (CombinedRunKind kind : CombinedRunKind.values()) { if ((mask & 1 << kind.ordinal()) != 0) { selected.add(kind); } }
            if (selected.size() < 2) { continue; }
            CombinedGuidanceSession session = new CombinedGuidanceSession();
            List<CombinedRunKind> started = new ArrayList<>();
            assertTrue(session.begin("profile", selected, kind -> started.add(kind)));
            List<CombinedRunKind> expected = List.copyOf(selected);
            assertEquals(expected, session.snapshot().getSequence());
            selected.clear(); // the session owns its immutable order
            for (int index = 0; index < expected.size(); index++)
            {
                assertEquals(index + 1, started.size());
                assertEquals(expected.get(index), session.snapshot().getCurrent());
                assertFalse(session.next("profile", kind -> { fail("Cannot advance an active checklist"); return true; }));
                long runningRevision = session.snapshot().getRevision();
                session.observe("profile", Map.of(expected.get(index), ACTIVE));
                assertEquals(runningRevision, session.snapshot().getRevision());
                session.observe("profile", Map.of(expected.get(index), COMPLETE));
                long finishedRevision = session.snapshot().getRevision();
                session.observe("profile", Map.of(expected.get(index), COMPLETE));
                assertEquals(finishedRevision, session.snapshot().getRevision());
                assertEquals(index + 1, started.size()); // observations cannot start anything
                assertEquals(index + 1, session.snapshot().getFinished());
                if (index + 1 < expected.size())
                {
                    assertEquals(BETWEEN, session.snapshot().getState());
                    assertTrue(session.next("profile", kind -> started.add(kind)));
                }
            }
            assertEquals(CombinedGuidanceSession.State.COMPLETE, session.snapshot().getState());
            assertNull(session.snapshot().getNext());
            assertFalse(session.next("profile", kind -> true));
            assertEquals(expected, started);
        }
    }

    @Test public void failedStartOrNextRetainsAnUnchangedRecoverableState()
    {
        CombinedGuidanceSession session = new CombinedGuidanceSession();
        assertFalse(session.begin("profile", Set.of(HERB, FIELD), kind -> false));
        assertEquals(CombinedGuidanceSession.State.INACTIVE, session.snapshot().getState()); assertEquals(0, session.snapshot().getRevision());
        assertTrue(session.begin("profile", Set.of(HERB, FIELD), kind -> true));
        session.observe("profile", Map.of(HERB, COMPLETE));
        long before = session.snapshot().getRevision();
        assertFalse(session.next("profile", kind -> false));
        assertEquals(BETWEEN, session.snapshot().getState()); assertEquals(before, session.snapshot().getRevision());
        assertEquals(FIELD, session.snapshot().getNext());
        assertTrue(session.next("profile", kind -> true)); assertEquals(FIELD, session.snapshot().getCurrent());
    }

    @Test public void invalidSelectionsAndMissingProfilesCannotInvokeTheStarter()
    {
        CombinedGuidanceSession session = new CombinedGuidanceSession();
        AtomicInteger calls = new AtomicInteger();
        for (String profile : new String[]{null, "", "  "})
        { assertFalse(session.begin(profile, Set.of(HERB, TREE), kind -> { calls.incrementAndGet(); return true; })); }
        assertFalse(session.begin("profile", null, kind -> { calls.incrementAndGet(); return true; }));
        for (Set<CombinedRunKind> selected : List.of(Set.<CombinedRunKind>of(), Set.of(TREE)))
        { assertFalse(session.begin("profile", selected, kind -> { calls.incrementAndGet(); return true; })); }
        assertEquals(0, calls.get());
        session.begin("profile", Set.of(HERB, TREE), kind -> true);
        assertFalse(session.begin("profile", Set.of(GARDEN, FIELD), kind -> { calls.incrementAndGet(); return true; }));
        assertEquals(0, calls.get()); assertEquals(HERB, session.snapshot().getCurrent());
    }

    @Test public void cancellationProfileChangesAndForeignActiveChecklistsEndTheSequence()
    {
        for (boolean between : new boolean[]{false, true})
        {
            for (int reason = 0; reason < 4; reason++)
            {
                CombinedGuidanceSession session = new CombinedGuidanceSession();
                session.begin("profile", Set.of(HERB, TREE), kind -> true);
                if (between) { session.observe("profile", Map.of(HERB, COMPLETE)); }
                long revision = session.snapshot().getRevision();
                if (reason == 0) { session.observe("other-profile", Map.of(HERB, ACTIVE)); }
                if (reason == 1) { session.observe("profile", Map.of(HERB, INACTIVE)); }
                if (reason == 2) { session.observe("profile", Map.of(HERB, COMPLETE, FIELD, ACTIVE)); }
                if (reason == 3) { session.clear(); }
                assertEquals(CombinedGuidanceSession.State.INACTIVE, session.snapshot().getState());
                assertTrue(session.snapshot().getSequence().isEmpty());
                assertTrue(session.snapshot().getRevision() > revision);
                assertFalse(session.next("profile", kind -> { fail("Cleared sequence"); return true; }));
            }
        }
    }

    @Test public void priorSnapshotsAndStaleCommandsCannotBorrowAReplacementSequence()
    {
        CombinedGuidanceSession session = new CombinedGuidanceSession();
        session.begin("one", Set.of(HERB, TREE), kind -> true);
        session.observe("one", Map.of(HERB, COMPLETE));
        CombinedGuidanceSession.Snapshot old = session.snapshot();
        session.clear(); session.begin("two", Set.of(GARDEN, FIELD), kind -> true);
        assertFalse(ChecklistCommandGuard.dispatch(old.getRevision(), session.snapshot().getRevision(),
                () -> { fail("Stale sidebar callback"); }));
        assertEquals(BETWEEN, old.getState()); assertEquals(List.of(HERB, TREE), old.getSequence());
        assertEquals(GARDEN, session.snapshot().getCurrent());
        try { old.getSequence().clear(); fail("Mutable snapshot"); } catch (UnsupportedOperationException expected) { }
        assertFalse(session.next("one", kind -> true));
    }
}
