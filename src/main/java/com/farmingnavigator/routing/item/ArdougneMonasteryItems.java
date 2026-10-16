package com.farmingnavigator.routing.item;

import com.farmingnavigator.routing.requirement.BankBackedItemRequirement;
import com.farmingnavigator.routing.requirement.RequirementContext;
import com.farmingnavigator.routing.requirement.RequirementResult;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/** Pinned shortest-path 99735452c7c257d3fe5ae79970429cdce907d571 teleportation_items.tsv monastery row.
 * Only the named cape variants are included; this is not the separate farm destination. */
public final class ArdougneMonasteryItems
{
    private static final Map<Integer,String> VARIANTS=Map.of(ItemID.ARDY_CAPE_EASY,"Ardougne cloak 1",ItemID.ARDY_CAPE_MEDIUM,"Ardougne cloak 2",
            ItemID.ARDY_CAPE_HARD,"Ardougne cloak 3",ItemID.ARDY_CAPE_ELITE,"Ardougne cloak 4",ItemID.SKILLCAPE_MAX_ARDY,"Ardougne max cape");
    private static final BankBackedItemRequirement ITEM=new BankBackedItemRequirement("Ardougne monastery cloak",VARIANTS,1);
    private ArdougneMonasteryItems() { }
    public static Set<Integer> getItemIds(){return VARIANTS.keySet();}
    public static RequirementResult evaluate(RequirementContext context){return ITEM.evaluate(context);}
}
