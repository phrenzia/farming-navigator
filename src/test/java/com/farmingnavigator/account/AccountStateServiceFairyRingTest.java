package com.farmingnavigator.account;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountStateServiceFairyRingTest
{
    @Test
    public void progressBeforeFortyDoesNotUnlockFairyRings()
    {
        assertFalse(
                AccountStateService
                        .hasFairyRingAccessFromProgress(
                                0
                        )
        );

        assertFalse(
                AccountStateService
                        .hasFairyRingAccessFromProgress(
                                20
                        )
        );

        assertFalse(
                AccountStateService
                        .hasFairyRingAccessFromProgress(
                                39
                        )
        );
    }

    @Test
    public void progressFortyUnlocksFairyRings()
    {
        assertTrue(
                AccountStateService
                        .hasFairyRingAccessFromProgress(
                                40
                        )
        );
    }

    @Test
    public void laterQuestProgressKeepsFairyRingsUnlocked()
    {
        assertTrue(
                AccountStateService
                        .hasFairyRingAccessFromProgress(
                                45
                        )
        );

        assertTrue(
                AccountStateService
                        .hasFairyRingAccessFromProgress(
                                70
                        )
        );

        assertTrue(
                AccountStateService
                        .hasFairyRingAccessFromProgress(
                                80
                        )
        );

        assertTrue(
                AccountStateService
                        .hasFairyRingAccessFromProgress(
                                100
                        )
        );
    }
}