package com.farmingnavigator.account;

import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountStateServiceVarlamoreTest
{
    @Test
    public void zeroVarbitMeansVarlamoreHasNotBeenVisited()
    {
        assertFalse(
                AccountStateService
                        .hasVisitedVarlamoreFromVarbit(
                                0
                        )
        );
    }

    @Test
    public void positiveVarbitMeansVarlamoreHasBeenVisited()
    {
        assertTrue(
                AccountStateService
                        .hasVisitedVarlamoreFromVarbit(
                                1
                        )
        );

        assertTrue(
                AccountStateService
                        .hasVisitedVarlamoreFromVarbit(
                                2
                        )
        );
    }

    @Test
    public void fullAccountStateStoresVisitedFlag()
    {
        AccountState state =
                new AccountState(
                        true,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        true,
                        true
                );

        assertTrue(
                state.isFairyRingAccessUnlocked()
        );

        assertTrue(
                state.isVarlamoreVisited()
        );
    }

    @Test
    public void compatibilityConstructorDefaultsToNotVisited()
    {
        AccountState state =
                new AccountState(
                        true,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        99,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false,
                        true
                );

        assertTrue(
                state.isFairyRingAccessUnlocked()
        );

        assertFalse(
                state.isVarlamoreVisited()
        );
    }

    @Test
    public void loggedOutStateDefaultsToNotVisited()
    {
        assertFalse(
                AccountState
                        .loggedOut()
                        .isVarlamoreVisited()
        );
    }
}