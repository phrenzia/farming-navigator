package com.farmingnavigator.routing.item;

import com.farmingnavigator.routing.requirement.BankBackedItemRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/** Pinned shortest-path99735452c7c257d3fe5ae79970429cdce907d571 construction/max cape portal rows. */
public final class ConstructionCapeItems
{
    private static final Map<Integer,String> VARIANTS=Map.of(ItemID.SKILLCAPE_CONSTRUCTION,"Construction cape",
            ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED,"Construction cape (t)",ItemID.SKILLCAPE_MAX,"Max cape",
            ItemID.SKILLCAPE_MAX_WORN,"Max cape");
    private static final BankBackedItemRequirement ITEM=new BankBackedItemRequirement("Construction portal cape",VARIANTS,1);
    private ConstructionCapeItems() { }
    public static Set<Integer> getItemIds() { return VARIANTS.keySet(); }
    public static RequirementResult evaluate(RequirementContext context) { return ITEM.evaluate(context); }
}
