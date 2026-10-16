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
        for (com.farmingnavigator.loadout.SupplyCheck check : plan.getSupplies())
        {
            int id = check.getRequirement().getItemId();
            if (id == ItemID.FOSSIL_VOLCANIC_ASH || !plan.getCompost().isBottomless() && plan.getCompost().getItemId() > 0 && id == plan.getCompost().getItemId())
            {
                lines.add(line(check.getRequirement(), price));
            }
        }
        if (plan.getCompost().isBottomless() && replants > 0) { notes.add("Bottomless compost use value is unknown and excluded; the filled bucket is reusable."); }
        return new FarmingMaterialEstimate(lines, notes);
    }
    public FarmingMaterialEstimate gardens(GardenRunPlan plan, IntToLongFunction price)
    {
        List<FarmingMaterialEstimate.Line> lines = new ArrayList<>();
        int paymentId = plan.getCrop().protectionPayment(1).map(SupplyRequirement::getItemId).orElse(0);
        for (com.farmingnavigator.loadout.SupplyCheck check : plan.getSupplies())
        {
            SupplyRequirement requirement = check.getRequirement(); int id = requirement.getItemId();
            if (id == plan.getCrop().getSeedId() || id == ItemID.PLANT_CURE || id == ItemID.FOSSIL_VOLCANIC_ASH || id == paymentId || !plan.getCompost().isBottomless() && id == plan.getCompost().getItemId()) { lines.add(line(requirement, price)); }
        }
        List<String> notes = notes();
        if (plan.getCompost().isBottomless() && plan.getStops().stream().anyMatch(GardenRunStop::isReplant))
        {
            notes.add("Bottomless compost use value is unknown and excluded; the filled bucket is reusable.");
        }
        return new FarmingMaterialEstimate(lines, notes);
    }

    public FarmingMaterialEstimate fields(FieldRunPlan plan, IntToLongFunction price)
    {
        List<FarmingMaterialEstimate.Line> lines = new ArrayList<>();
        int paymentId = plan.getCrop().protectionPayment(1).map(SupplyRequirement::getItemId).orElse(0);
        for (com.farmingnavigator.loadout.SupplyCheck check : plan.getSupplies())
        {
            SupplyRequirement requirement = check.getRequirement(); int id = requirement.getItemId();
            if (id == plan.getCrop().getSeedId() || id == ItemID.PLANT_CURE || id == ItemID.FOSSIL_VOLCANIC_ASH || id == paymentId || !plan.getCompost().isBottomless() && id == plan.getCompost().getItemId()) { lines.add(line(requirement, price)); }
        }
        List<String> notes = notes();
        if (plan.getCompost().isBottomless() && plan.getStops().stream().anyMatch(FieldRunStop::isReplant))
        {
            notes.add("Bottomless compost use value is unknown and excluded; the filled bucket is reusable.");
        }
        return new FarmingMaterialEstimate(lines, notes);
    }

    /** Reuse the current component quotes; never price reusable combined stock as consumption. */
    public Optional<FarmingMaterialEstimate> combined(CombinedRunPlan worksheet,java.util.Set<CombinedRunKind> selected,
            java.util.Map<CombinedRunKind,FarmingMaterialEstimate> estimates)
    {
        if(worksheet==null || !worksheet.getBlockers().isEmpty() || selected==null || selected.size()<2 || estimates==null)
        { return Optional.empty(); }
        List<FarmingMaterialEstimate.Line> lines=new ArrayList<>(); List<String> common=notes(),combinedNotes=notes();
        for(CombinedRunKind kind:CombinedRunKind.values())
        {
            if(!selected.contains(kind)) { continue; }
            FarmingMaterialEstimate estimate=estimates.get(kind); if(estimate==null) { return Optional.empty(); }
            for(FarmingMaterialEstimate.Line line:estimate.getLines())
            { lines.add(new FarmingMaterialEstimate.Line(line,kind+" - "+line.getName())); }
            for(String note:estimate.getNotes()) { if(!common.contains(note)) { combinedNotes.add(kind+": "+note); } }
        }
        combinedNotes.add("Adds potential material consumption for the selected component checklists. Repeated consumables remain counted for every use; this is not additional stock to buy or a combined profit estimate.");
        return Optional.of(new FarmingMaterialEstimate(lines,combinedNotes));
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
