package com.farmingnavigator.account;

import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class CraftingAccountStateTest
{
    private AccountState account()
    { return new AccountState(true, 99, 100, EnumSet.noneOf(net.runelite.api.Quest.class), false); }

    @Test public void baseCraftingHasConservativeLegacyInvalidAndLoggedOutDefaults()
    {
        assertEquals(-1, account().getRealCraftingLevel());
        for (int invalid : new int[]{Integer.MIN_VALUE, -1, 0, 100, Integer.MAX_VALUE})
        { assertEquals(-1, account().withRealCraftingLevel(invalid).getRealCraftingLevel()); }
        for (int level : new int[]{1, 98, 99})
        { assertEquals(level, account().withRealCraftingLevel(level).getRealCraftingLevel()); }
        assertEquals(-1, AccountState.loggedOut().withRealCraftingLevel(99).getRealCraftingLevel());
    }

    @Test public void everyCopyPreservesBothCapeSkillsAndPriorEvidenceInEitherOrder()
    {
        AccountState initial=account();
        AccountState first=initial.withRealCraftingLevel(99).withRealConstructionLevel(98)
                .withRealAttackLevel(61).withBoostedWoodcuttingLevel(71).withBarbarianPlanting(BarbarianPlantingState.COMPLETED);
        AccountState second=initial.withBarbarianPlanting(BarbarianPlantingState.COMPLETED).withRealAttackLevel(61)
                .withBoostedWoodcuttingLevel(71).withRealConstructionLevel(98).withRealCraftingLevel(99);
        for(AccountState copy:new AccountState[]{first,second})
        {
            assertEquals(99,copy.getRealCraftingLevel()); assertEquals(98,copy.getRealConstructionLevel());
            assertEquals(61,copy.getRealAttackLevel()); assertEquals(71,copy.getBoostedWoodcuttingLevel());
            assertTrue(copy.hasCompletedBarbarianPlanting()); assertEquals(100,copy.getBoostedFarmingLevel());
        }
        assertEquals(-1,initial.getRealCraftingLevel()); assertEquals(-1,initial.getRealConstructionLevel());
        assertEquals(98,first.withRealCraftingLevel(98).getRealCraftingLevel()); assertEquals(99,first.getRealCraftingLevel());
    }

    @Test public void fishingSnapshotsKeepUnknownLegacyAndInvalidSamplesConservative()
    {
        assertEquals(-1,account().getRealFishingLevel());
        for(int invalid:new int[]{Integer.MIN_VALUE,-1,0,100,Integer.MAX_VALUE})
        {assertEquals(-1,account().withRealFishingLevel(invalid).getRealFishingLevel());}
        for(int level:new int[]{1,98,99})
        {assertEquals(level,account().withRealFishingLevel(level).getRealFishingLevel());}
        assertEquals(-1,AccountState.loggedOut().withRealFishingLevel(99).getRealFishingLevel());
    }

    @Test public void allSkillCopiesPreserveFishingAndFishingCopiesPreserveOtherSkills()
    {
        AccountState initial=account();
        AccountState first=initial.withRealFishingLevel(99).withRealCraftingLevel(98).withRealConstructionLevel(97)
                .withRealAttackLevel(61).withBoostedWoodcuttingLevel(71).withBarbarianPlanting(BarbarianPlantingState.COMPLETED);
        AccountState second=initial.withBarbarianPlanting(BarbarianPlantingState.COMPLETED).withRealAttackLevel(61)
                .withBoostedWoodcuttingLevel(71).withRealConstructionLevel(97).withRealCraftingLevel(98).withRealFishingLevel(99);
        for(AccountState copy:new AccountState[]{first,second})
        {
            assertEquals(99,copy.getRealFishingLevel());assertEquals(98,copy.getRealCraftingLevel());assertEquals(97,copy.getRealConstructionLevel());
            assertEquals(61,copy.getRealAttackLevel());assertEquals(71,copy.getBoostedWoodcuttingLevel());assertTrue(copy.hasCompletedBarbarianPlanting());
        }
        assertEquals(-1,initial.getRealFishingLevel());assertEquals(98,first.withRealFishingLevel(98).getRealFishingLevel());
        assertEquals(99,first.getRealFishingLevel());
    }
}
