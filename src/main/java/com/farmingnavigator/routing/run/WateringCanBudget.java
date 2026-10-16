package com.farmingnavigator.routing.run;

import com.farmingnavigator.routing.requirement.RequirementResult;
import com.farmingnavigator.storage.StorageSnapshot;
import java.util.*;
import net.runelite.api.gameval.ItemID;

/** Standard can item forms establish 1-8 carried/verified-bank doses; empty and Gricoller contents are not inferred.
 * https://oldschoolrunescape.fandom.com/wiki/Watering_can
 */
public final class WateringCanBudget
{
    private static final Map<Integer,Integer> DOSES = Map.of(ItemID.WATERING_CAN_1,1,ItemID.WATERING_CAN_2,2,
            ItemID.WATERING_CAN_3,3,ItemID.WATERING_CAN_4,4,ItemID.WATERING_CAN_5,5,ItemID.WATERING_CAN_6,6,
            ItemID.WATERING_CAN_7,7,ItemID.WATERING_CAN_8,8);
    private final RequirementResult check;
    private final Set<Integer> bankItems;
    private WateringCanBudget(RequirementResult check,Set<Integer> bankItems) { this.check = check; this.bankItems = Set.copyOf(bankItems); }
    public static Set<Integer> getFilledCanIds() { return DOSES.keySet(); }
    public static WateringCanBudget evaluate(int applications,StorageSnapshot storage)
    {
        if(applications<0) { throw new IllegalArgumentException("Water applications cannot be negative."); }
        String label="Water for " + applications + " first-cycle applications";
        if(applications==0) { return new WateringCanBudget(RequirementResult.satisfied(label,"No watering applications reserved."),Set.of()); }
        long carried=capacity(storage==null?Map.of():storage.getInventoryItems(),applications);
        if(carried>=applications) { return new WateringCanBudget(RequirementResult.satisfied(label,"Enough water in carried standard cans. Later growth cycles need watering again; no disease-free lifetime is promised."),Set.of()); }
        if(storage==null || !storage.isBankVerified()) { return new WateringCanBudget(RequirementResult.dataRequired(label,"Verify your bank or fill and carry standard watering cans. Gricoller's can charges and leprechaun storage are unchecked."),Set.of()); }
        long missing=applications-carried;
        Set<Integer> bank=new HashSet<>(); DOSES.keySet().forEach(id -> { if(storage.getVerifiedBankQuantity(id)>0) { bank.add(id); } });
        if(capacity(storage.getBankItems(),missing)>=missing)
        {
            return new WateringCanBudget(RequirementResult.availableFromBank(label,"Withdraw filled standard cans covering at least " + missing + " more water applications. This reserves only one watering after each possible planting."),bank);
        }
        return new WateringCanBudget(RequirementResult.blocked(label,"Fill or obtain standard cans for at least " + (missing-capacity(storage.getBankItems(),missing))
                + " more applications beyond carried and verified-bank water. Empty cans, refills, Gricoller charges and leprechaun storage are not assumed."),bank);
    }
    private static long capacity(Map<Integer,Long> items,long needed)
    {
        long total=0;
        for(Map.Entry<Integer,Integer> entry:DOSES.entrySet())
        {
            long count=Math.max(0,items.getOrDefault(entry.getKey(),0L));
            long useful=Math.min(count,(needed-total+entry.getValue()-1)/entry.getValue());
            total+=useful*entry.getValue(); if(total>=needed) { return total; }
        }
        return total;
    }
    public RequirementResult getCheck() { return check; }
    public Set<Integer> getBankItems() { return bankItems; }
}
