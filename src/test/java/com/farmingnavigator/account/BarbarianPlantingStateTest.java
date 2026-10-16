package com.farmingnavigator.account;

import java.util.EnumSet;
import net.runelite.api.Quest;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarbarianPlantingStateTest
{
    private AccountState account(){return new AccountState(true,99,99,EnumSet.noneOf(Quest.class),false);}
    @Test public void onlyExactCompletedRawValueRemovesTrainingUncertainty()
    {
        assertEquals(BarbarianPlantingState.NOT_STARTED,BarbarianPlantingState.decode(0));
        assertEquals(BarbarianPlantingState.TRAINING,BarbarianPlantingState.decode(1));
        assertEquals(BarbarianPlantingState.PLANTED_SEED,BarbarianPlantingState.decode(2));
        for(int raw:new int[]{Integer.MIN_VALUE,-1,0,1,2,3,4,99,Integer.MAX_VALUE})
        {
            AccountState value=account().withBarbarianPlanting(BarbarianPlantingState.decode(raw));assertEquals(raw==3,value.hasCompletedBarbarianPlanting());
            if(raw<0 || raw>3){assertEquals(BarbarianPlantingState.UNKNOWN,value.getBarbarianPlanting());}
        }
    }
    @Test public void legacyAndLoggedOutSnapshotsCannotInventAnUnlock()
    {
        assertEquals(BarbarianPlantingState.UNKNOWN,account().getBarbarianPlanting());assertFalse(account().hasCompletedBarbarianPlanting());
        assertFalse(account().withBarbarianPlanting(null).hasCompletedBarbarianPlanting());
        AccountState loggedOut=AccountState.loggedOut().withBarbarianPlanting(BarbarianPlantingState.COMPLETED);
        assertEquals(BarbarianPlantingState.UNKNOWN,loggedOut.getBarbarianPlanting());assertFalse(loggedOut.hasCompletedBarbarianPlanting());
    }
    @Test public void immutableCopiesPreserveSkillsQuestsAndTheVerifiedStateInEitherOrder()
    {
        AccountState original=account().withRealAttackLevel(61).withBoostedWoodcuttingLevel(71);
        AccountState complete=original.withBarbarianPlanting(BarbarianPlantingState.COMPLETED);
        assertFalse(original.hasCompletedBarbarianPlanting());assertEquals(61,complete.getRealAttackLevel());assertEquals(71,complete.getBoostedWoodcuttingLevel());
        AccountState copied=complete.withRealAttackLevel(99).withBoostedWoodcuttingLevel(1);
        assertTrue(copied.hasCompletedBarbarianPlanting());assertEquals(99,copied.getRealAttackLevel());assertEquals(1,copied.getBoostedWoodcuttingLevel());
        assertEquals(61,complete.getRealAttackLevel());assertTrue(complete.withBarbarianPlanting(BarbarianPlantingState.TRAINING).withBarbarianPlanting(BarbarianPlantingState.COMPLETED).hasCompletedBarbarianPlanting());
    }
}
