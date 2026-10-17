package com.farmingnavigator.account;

import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConstructionAccountStateTest
{
    private AccountState account()
    {
        return new AccountState(true, 99, 95, 80, 82, 70, 72, 75, 77,
                Map.of(Quest.SONG_OF_THE_ELVES, QuestState.FINISHED), Map.of(DiaryArea.ARDOUGNE, DiaryTier.HARD),
                Map.of(DiaryArea.ARDOUGNE, DiaryTier.MEDIUM), true, true, true);
    }
    @Test public void legacyLoggedOutAndInvalidBaseLevelsRemainUnknown()
    {
        assertEquals(-1, account().getRealConstructionLevel());
        assertEquals(-1, new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false).getRealConstructionLevel());
        for (int invalid : new int[]{Integer.MIN_VALUE, -1, 0, 100, Integer.MAX_VALUE})
        { assertEquals(-1, account().withRealConstructionLevel(invalid).getRealConstructionLevel()); }
        for (int valid : new int[]{1, 98, 99})
        { assertEquals(valid, account().withRealConstructionLevel(valid).getRealConstructionLevel()); }
        assertEquals(-1, AccountState.loggedOut().withRealConstructionLevel(99).getRealConstructionLevel());
    }
    @Test public void everyImmutableCopyPreservesConstructionAndPriorAccountEvidence()
    {
        AccountState initial = account();
        AccountState construction = initial.withRealConstructionLevel(99);
        assertEquals(-1, initial.getRealConstructionLevel());
        AccountState first = construction.withRealAttackLevel(61).withBoostedWoodcuttingLevel(71).withBarbarianPlanting(BarbarianPlantingState.COMPLETED);
        AccountState second = account().withBarbarianPlanting(BarbarianPlantingState.COMPLETED).withBoostedWoodcuttingLevel(71)
                .withRealAttackLevel(61).withRealConstructionLevel(99);
        for (AccountState copy : new AccountState[]{first, second})
        {
            assertEquals(99, copy.getRealConstructionLevel()); assertEquals(61, copy.getRealAttackLevel());
            assertEquals(71, copy.getBoostedWoodcuttingLevel()); assertTrue(copy.hasCompletedBarbarianPlanting());
            assertTrue(copy.hasCompletedQuest(Quest.SONG_OF_THE_ELVES));
            assertTrue(copy.hasCompletedDiaryTier(DiaryArea.ARDOUGNE, DiaryTier.HARD));
            assertTrue(copy.hasClaimedDiaryTier(DiaryArea.ARDOUGNE, DiaryTier.MEDIUM));
            assertEquals(95, copy.getBoostedFarmingLevel()); assertEquals(82, copy.getBoostedMagicLevel());
        }
        assertEquals(98, first.withRealConstructionLevel(98).getRealConstructionLevel());
        assertEquals(99, first.getRealConstructionLevel());
    }
}
