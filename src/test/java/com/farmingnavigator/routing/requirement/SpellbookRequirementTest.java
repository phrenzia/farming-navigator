package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.routing.capability.SpellbookType;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import java.util.Collections;
import java.util.EnumSet;
import net.runelite.api.Quest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpellbookRequirementTest
{
    @Test
    public void acceptsMatchingSpellbook()
    {
        RequirementContext context =
                createContext(
                        SpellbookType.LUNAR
                );

        SpellbookRequirement requirement =
                new SpellbookRequirement(
                        SpellbookType.LUNAR
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertTrue(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.SATISFIED,
                result.getState()
        );
    }

    @Test
    public void blocksWrongSpellbook()
    {
        RequirementContext context =
                createContext(
                        SpellbookType.STANDARD
                );

        SpellbookRequirement requirement =
                new SpellbookRequirement(
                        SpellbookType.LUNAR
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertFalse(
                result.isSatisfied()
        );

        assertEquals(
                RequirementState.BLOCKED,
                result.getState()
        );

        assertTrue(
                result
                        .getDetail()
                        .contains(
                                "Standard"
                        )
        );
    }

    @Test
    public void unknownSpellbookRequiresData()
    {
        RequirementContext context =
                createContext(
                        SpellbookType.UNKNOWN
                );

        SpellbookRequirement requirement =
                new SpellbookRequirement(
                        SpellbookType.ARCEUUS
                );

        RequirementResult result =
                requirement.evaluate(
                        context
                );

        assertEquals(
                RequirementState.DATA_REQUIRED,
                result.getState()
        );
    }

    @Test
    public void runePouchQuantitiesAreReadable()
    {
        TravelCapabilitySnapshot snapshot =
                new TravelCapabilitySnapshot(
                        true,
                        SpellbookType.STANDARD,
                        null,
                        Collections.singletonMap(
                                563,
                                250
                        )
                );

        assertEquals(
                250,
                snapshot.getRunePouchQuantity(
                        563
                )
        );

        assertTrue(
                snapshot.hasRuneInPouch(
                        563
                )
        );

        assertFalse(
                snapshot.hasRuneInPouch(
                        999999
                )
        );
    }

    private RequirementContext createContext(
            SpellbookType spellbook
    )
    {
        AccountState accountState =
                new AccountState(
                        true,
                        41,
                        41,
                        EnumSet.noneOf(
                                Quest.class
                        ),
                        false
                );

        TravelCapabilitySnapshot snapshot =
                new TravelCapabilitySnapshot(
                        true,
                        spellbook,
                        null,
                        Collections.emptyMap()
                );

        return new RequirementContext(
                accountState,
                null,
                snapshot
        );
    }
}