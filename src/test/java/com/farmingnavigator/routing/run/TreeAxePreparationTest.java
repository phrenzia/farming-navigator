package com.farmingnavigator.routing.run;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.loadout.SupplyCheck;
import com.farmingnavigator.loadout.SupplyState;
import com.farmingnavigator.storage.StorageSnapshot;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class TreeAxePreparationTest
{
    private AccountState account(int level)
    {
        return new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false).withBoostedWoodcuttingLevel(level);
    }
    private StorageSnapshot storage(Map<Integer, Long> inventory, Map<Integer, Long> equipment, Map<Integer, Long> bank, boolean verified)
    {
        return new StorageSnapshot(inventory, equipment, bank, verified, Instant.now());
    }
    @Test public void standardAxesRequireCurrentWoodcuttingRatherThanWieldingAttackLevel()
    {
        int[][] cases = {{ItemID.BRONZE_AXE, 1}, {ItemID.IRON_AXE, 1}, {ItemID.STEEL_AXE, 6}, {ItemID.BLACK_AXE, 11},
                {ItemID.MITHRIL_AXE, 21}, {ItemID.ADAMANT_AXE, 31}, {ItemID.RUNE_AXE, 41}, {ItemID.DRAGON_AXE, 61}};
        for (int[] row : cases)
        {
            StorageSnapshot carried = storage(Map.of(row[0], 1L), Map.of(), Map.of(), true);
            assertEquals(SupplyState.ALREADY_CARRIED, TreeAxePreparation.evaluate(account(row[1]).withRealAttackLevel(1), carried).getState());
            assertNotEquals(SupplyState.ALREADY_CARRIED, TreeAxePreparation.evaluate(account(row[1] - 1), carried).getState());
        }
    }
    @Test public void equipmentAndVerifiedBankCanSupplyAUsableAxe()
    {
        assertEquals(SupplyState.ALREADY_EQUIPPED, TreeAxePreparation.evaluate(account(41),
                storage(Map.of(), Map.of(ItemID.RUNE_AXE, 1L), Map.of(), true)).getState());
        SupplyCheck bank = TreeAxePreparation.evaluate(account(41), storage(Map.of(), Map.of(), Map.of(ItemID.RUNE_AXE, 1L), true));
        assertEquals(SupplyState.AVAILABLE_FROM_BANK, bank.getState()); assertEquals(1, bank.getWithdrawQuantity());
        assertEquals(SupplyState.MISSING, TreeAxePreparation.evaluate(account(40),
                storage(Map.of(), Map.of(), Map.of(ItemID.RUNE_AXE, 1L), true)).getState());
        assertEquals(SupplyState.DATA_REQUIRED, TreeAxePreparation.evaluate(account(41),
                storage(Map.of(), Map.of(), Map.of(ItemID.RUNE_AXE, 1L), false)).getState());
    }
    @Test public void unknownOrDrainedSkillCannotClaimReadinessEvenWithCarriedAxe()
    {
        for (AccountState account : new AccountState[]{account(-1), account(0), AccountState.loggedOut(), null})
        {
            assertEquals(SupplyState.DATA_REQUIRED, TreeAxePreparation.evaluate(account,
                    storage(Map.of(ItemID.BRONZE_AXE, 1L), Map.of(), Map.of(), true)).getState());
        }
    }
    @Test public void notedAxesAndUnverifiedVariantsCannotCountAsUsableTools()
    {
        SupplyCheck result = TreeAxePreparation.evaluate(account(99), storage(Map.of(ItemID.Cert.RUNE_AXE, 1L,
                ItemID.INFERNAL_AXE, 1L), Map.of(), Map.of(), true));
        assertEquals(SupplyState.MISSING, result.getState());
        assertTrue(result.getRequirement().getNote().contains("other axe variants"));
    }
    @Test public void skillCopyMethodsPreserveOtherAccountEvidenceInEitherOrder()
    {
        AccountState first = account(61).withRealAttackLevel(1);
        AccountState second = account(-1).withRealAttackLevel(1).withBoostedWoodcuttingLevel(61);
        for (AccountState state : new AccountState[]{first, second})
        {
            assertEquals(61, state.getBoostedWoodcuttingLevel()); assertEquals(1, state.getRealAttackLevel());
            assertEquals(99, state.getBoostedFarmingLevel()); assertTrue(state.isLoggedIn());
        }
        assertEquals(-1, new AccountState(true, 99, 99, EnumSet.noneOf(Quest.class), false).getBoostedWoodcuttingLevel());
    }
}
