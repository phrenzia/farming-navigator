package com.farmingnavigator.routing.run;

import com.farmingnavigator.transport.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class HerbTransportSelectorTest
{
    private VerifiedTransportResult route(int rank, TransportVerificationKind kind, VerifiedTransportState state)
    {
        return new VerifiedTransportResult(new TransportOption(rank, kind.name(), "Travel manually",
                TransportCategory.ITEM, kind), state, "");
    }

    @Test public void defaultPreservesReadyBeforeBankThenCatalogueRank()
    {
        VerifiedTransportResult bank = route(1, TransportVerificationKind.EXPLORER_RING_UNLIMITED,
                VerifiedTransportState.AVAILABLE_FROM_BANK);
        VerifiedTransportResult spell = route(2, TransportVerificationKind.ARDOUGNE_TELEPORT_TABLET, VerifiedTransportState.READY_NOW);
        assertEquals(spell, HerbTransportSelector.select(Arrays.asList(bank, spell), HerbTransportPreference.CATALOGUE).get(0));
        assertEquals(spell, HerbTransportSelector.select(Arrays.asList(bank, spell), null).get(0));
    }

    @Test public void reusablePreferenceCanChooseBankedUnlimitedOverCarriedConsumable()
    {
        VerifiedTransportResult bank = route(5, TransportVerificationKind.EXPLORER_RING_UNLIMITED,
                VerifiedTransportState.AVAILABLE_FROM_BANK);
        VerifiedTransportResult tablet = route(1, TransportVerificationKind.ARDOUGNE_TELEPORT_TABLET, VerifiedTransportState.READY_NOW);
        List<VerifiedTransportResult> result = HerbTransportSelector.select(Arrays.asList(tablet, bank),
                HerbTransportPreference.REUSABLE_FIRST);
        assertEquals(bank, result.get(0));
        assertEquals(tablet, result.get(1));
    }

    @Test public void limitedChargedAndPohEntryMethodsDoNotBecomeUnlimitedByCategory()
    {
        VerifiedTransportResult unlimited = route(20, TransportVerificationKind.FARMING_CAPE, VerifiedTransportState.READY_NOW);
        for (TransportVerificationKind kind : Arrays.asList(TransportVerificationKind.EXPLORER_RING_2_DAILY,
                TransportVerificationKind.KANDARIN_HEADGEAR_3_DAILY, TransportVerificationKind.SKILLS_NECKLACE,
                TransportVerificationKind.XERICS_TALISMAN_GLADE, TransportVerificationKind.POH_FAIRY_RING,
                TransportVerificationKind.POH_JEWELLERY_BOX_SKILLS_NECKLACE, TransportVerificationKind.RING_OF_ELEMENTS))
        {
            VerifiedTransportResult limited = route(1, kind, VerifiedTransportState.READY_NOW);
            assertEquals(kind.name(), unlimited, HerbTransportSelector.select(Arrays.asList(limited, unlimited),
                    HerbTransportPreference.REUSABLE_FIRST).get(0));
        }
    }

    @Test public void unavailableUnlimitedOptionsCannotDisplaceUsableRoutes()
    {
        for (VerifiedTransportState state : VerifiedTransportState.values())
        {
            VerifiedTransportResult candidate = route(1, TransportVerificationKind.EXPLORER_RING_UNLIMITED, state);
            VerifiedTransportResult tablet = route(2, TransportVerificationKind.ARDOUGNE_TELEPORT_TABLET, VerifiedTransportState.READY_NOW);
            List<VerifiedTransportResult> result = HerbTransportSelector.select(Arrays.asList(candidate, tablet),
                    HerbTransportPreference.REUSABLE_FIRST);
            assertEquals(candidate.isUsableForPlannedRun() ? candidate : tablet, result.get(0));
        }
    }

    @Test public void readyAndRankStillBreakTiesAmongReusableMethods()
    {
        VerifiedTransportResult bank = route(1, TransportVerificationKind.FARMING_CAPE, VerifiedTransportState.AVAILABLE_FROM_BANK);
        VerifiedTransportResult ready = route(5, TransportVerificationKind.FAIRY_RING_ACCESS, VerifiedTransportState.READY_NOW);
        VerifiedTransportResult better = route(3, TransportVerificationKind.ECTOPHIAL, VerifiedTransportState.READY_NOW);
        assertEquals(Arrays.asList(better, ready, bank), HerbTransportSelector.select(Arrays.asList(bank, ready, better),
                HerbTransportPreference.REUSABLE_FIRST));
    }
}
