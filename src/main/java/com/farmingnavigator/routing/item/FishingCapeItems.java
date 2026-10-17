package com.farmingnavigator.routing.item;

import com.farmingnavigator.routing.requirement.BankBackedItemRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/** Pinned shortest-path99735452c7c257d3fe5ae79970429cdce907d571 Fishing Guild cape rows. */
public final class FishingCapeItems
{
    private static final Map<Integer,String> VARIANTS=Map.of(ItemID.SKILLCAPE_FISHING,"Fishing cape",
            ItemID.SKILLCAPE_FISHING_TRIMMED,"Fishing cape (t)",ItemID.SKILLCAPE_MAX,"Max cape",
            ItemID.SKILLCAPE_MAX_WORN,"Max cape");
    private static final BankBackedItemRequirement ITEM=new BankBackedItemRequirement("Fishing Guild cape",VARIANTS,1);
    private FishingCapeItems() { }
    public static Set<Integer> getItemIds() { return VARIANTS.keySet(); }
    public static RequirementResult evaluate(RequirementContext context) { return ITEM.evaluate(context); }
}
