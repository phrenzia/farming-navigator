package com.farmingnavigator.economics;

import com.farmingnavigator.loadout.SupplyRequirement;
import com.farmingnavigator.routing.run.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntToLongFunction;
import net.runelite.api.gameval.ItemID;

/** Price lookup is injected; production reads RuneLite's existing cache on the client thread. */
public final class FarmingMaterialEstimator
{
    public FarmingMaterialEstimate herbs(HerbRunPlan plan, HerbRunCompost compost, IntToLongFunction price)
    {
        List<FarmingMaterialEstimate.Line> lines = new ArrayList<>(); List<String> notes = notes();
        for (com.farmingnavigator.loadout.SupplyCheck check : plan.getSupplies())
        {
            SupplyRequirement requirement = check.getRequirement(); int id = requirement.getItemId();
            if (id == plan.getCrop().getSeedId() || id == ItemID.PLANT_CURE || id == ItemID.FOSSIL_VOLCANIC_ASH
                    || !compost.isBottomless() && compost.getItemId() > 0 && id == compost.getItemId())
            {
                lines.add(line(requirement, price));
            }
        }
        if (compost.isBottomless() && plan.getStops().stream().anyMatch(HerbRunStop::isReplant))
        {
            notes.add("Bottomless compost use value is unknown and excluded; the full bucket is reusable.");
        }
        return new FarmingMaterialEstimate(lines, notes);
    }
    public FarmingMaterialEstimate trees(TreeRunPlan plan, boolean protect, IntToLongFunction price)
    {
        List<FarmingMaterialEstimate.Line> lines = new ArrayList<>(); List<String> notes = notes();
        int replants = (int) plan.getStops().stream().filter(TreeRunStop::isReplant).count();
        if (replants > 0)
        {
            lines.add(new FarmingMaterialEstimate.Line(plan.getCrop().getSaplingId(), plan.getCrop() + " sapling",
                    replants, price.applyAsLong(plan.getCrop().getSaplingId())));
            if (protect && plan.getCrop().hasFarmerProtection())
            {
                Optional<SupplyRequirement> payment = plan.getCrop().protectionPayment(replants);
                if (payment.isPresent()) { lines.add(line(payment.get(), price)); }
                else { notes.add("Farmer payment is unverified and excluded. This subtotal does not cover protection."); }
            }
        }
        return new FarmingMaterialEstimate(lines, notes);
    }
    public FarmingMaterialEstimate gardens(GardenRunPlan plan, IntToLongFunction price)
    {
        List<FarmingMaterialEstimate.Line> lines = new ArrayList<>();
        int paymentId = plan.getCrop().protectionPayment(1).map(SupplyRequirement::getItemId).orElse(0);
        for (com.farmingnavigator.loadout.SupplyCheck check : plan.getSupplies())
        {
            SupplyRequirement requirement = check.getRequirement(); int id = requirement.getItemId();
            if (id == plan.getCrop().getSeedId() || id == ItemID.PLANT_CURE || id == paymentId) { lines.add(line(requirement, price)); }
        }
        return new FarmingMaterialEstimate(lines, notes());
    }

    public FarmingMaterialEstimate fields(FieldRunPlan plan, IntToLongFunction price)
    {
        List<FarmingMaterialEstimate.Line> lines = new ArrayList<>();
        int paymentId = plan.getCrop().protectionPayment(1).map(SupplyRequirement::getItemId).orElse(0);
        for (com.farmingnavigator.loadout.SupplyCheck check : plan.getSupplies())
        {
            SupplyRequirement requirement = check.getRequirement(); int id = requirement.getItemId();
            if (id == plan.getCrop().getSeedId() || id == ItemID.PLANT_CURE || id == paymentId || !plan.getCompost().isBottomless() && id == plan.getCompost().getItemId()) { lines.add(line(requirement, price)); }
        }
        List<String> notes = notes();
        if (plan.getCompost().isBottomless() && plan.getStops().stream().anyMatch(FieldRunStop::isReplant))
        {
            notes.add("Bottomless compost use value is unknown and excluded; the filled bucket is reusable.");
        }
        return new FarmingMaterialEstimate(lines, notes);
    }

    private FarmingMaterialEstimate.Line line(SupplyRequirement requirement, IntToLongFunction price)
    {
        return new FarmingMaterialEstimate.Line(requirement.getItemId(), requirement.getName(), requirement.getQuantity(),
                price.applyAsLong(requirement.getItemId()));
    }
    private List<String> notes()
    {
        List<String> notes = new ArrayList<>();
        notes.add("RuneLite cached item prices may be stale. Value includes owned materials and assumes reserved materials are used; unknown patches may need fewer.");
        notes.add("Excludes travel, spell runes, reusable tools, removal fees and harvest value. This is not profit, total run cost or an Ironman acquisition-cost estimate.");
        return notes;
    }
}
