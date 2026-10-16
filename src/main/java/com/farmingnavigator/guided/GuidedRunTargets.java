package com.farmingnavigator.guided;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.capability.TravelCapabilitySnapshot;
import com.farmingnavigator.routing.item.ChargedTeleportItemDefinition;
import com.farmingnavigator.routing.requirement.RuneSupply;
import com.farmingnavigator.routing.run.HerbRunPlan;
import com.farmingnavigator.routing.run.HerbHouseEntryMode;
import com.farmingnavigator.routing.run.HerbRunCure;
import com.farmingnavigator.routing.run.HerbRunCareResources;
import com.farmingnavigator.routing.requirement.RuneCost;
import com.farmingnavigator.routing.requirement.RunePreparation;
import com.farmingnavigator.routing.run.HerbRunCompost;
import com.farmingnavigator.routing.spell.TeleportSpellDefinition;
import com.farmingnavigator.transport.TransportCategory;
import com.farmingnavigator.transport.TransportOption;
import com.farmingnavigator.transport.VerifiedTransportResult;
import com.farmingnavigator.transport.VerifiedTransportState;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/** Cached semantic targets; renderers only test membership and draw. */
public final class GuidedRunTargets
{
    private final Set<Integer> inventoryItems;
    private final Set<Integer> bankItems;
    private final WorldPoint destination;
    private final String destinationName;
    private final HerbPatchDefinition scenePatch;
    private final boolean leprechaun;
    private final com.farmingnavigator.farming.tree.TreePatchDefinition treeScenePatch;
    private final boolean treeFarmer;
    private final com.farmingnavigator.farming.garden.GardenPatchDefinition gardenScenePatch;
    private final boolean gardenFarmer;
    private final com.farmingnavigator.farming.field.FieldPatchDefinition fieldScenePatch;
    private final boolean fieldFarmer;

    private GuidedRunTargets(Set<Integer> inventoryItems, Set<Integer> bankItems, WorldPoint destination,
            String destinationName, HerbPatchDefinition scenePatch, boolean leprechaun)
    {
        this(inventoryItems, bankItems, destination, destinationName, scenePatch, leprechaun, null, false);
    }
    private GuidedRunTargets(Set<Integer> inventoryItems, Set<Integer> bankItems, WorldPoint destination,
            String destinationName, HerbPatchDefinition scenePatch, boolean leprechaun,
            com.farmingnavigator.farming.tree.TreePatchDefinition treeScenePatch, boolean treeFarmer)
    {
        this(inventoryItems, bankItems, destination, destinationName, scenePatch, leprechaun, treeScenePatch, treeFarmer, null, false);
    }
    private GuidedRunTargets(Set<Integer> inventoryItems, Set<Integer> bankItems, WorldPoint destination,
            String destinationName, HerbPatchDefinition scenePatch, boolean leprechaun,
            com.farmingnavigator.farming.tree.TreePatchDefinition treeScenePatch, boolean treeFarmer,
            com.farmingnavigator.farming.garden.GardenPatchDefinition gardenScenePatch, boolean gardenFarmer)
    {
        this(inventoryItems, bankItems, destination, destinationName, scenePatch, leprechaun, treeScenePatch, treeFarmer,
                gardenScenePatch, gardenFarmer, null, false);
    }
    private GuidedRunTargets(Set<Integer> inventoryItems, Set<Integer> bankItems, WorldPoint destination,
            String destinationName, HerbPatchDefinition scenePatch, boolean leprechaun,
            com.farmingnavigator.farming.tree.TreePatchDefinition treeScenePatch, boolean treeFarmer,
            com.farmingnavigator.farming.garden.GardenPatchDefinition gardenScenePatch, boolean gardenFarmer,
            com.farmingnavigator.farming.field.FieldPatchDefinition fieldScenePatch, boolean fieldFarmer)
    {
        this.inventoryItems = Collections.unmodifiableSet(new HashSet<>(inventoryItems));
        this.bankItems = Collections.unmodifiableSet(new HashSet<>(bankItems));
        this.destination = destination;
        this.destinationName = destinationName;
        this.scenePatch = scenePatch;
        this.leprechaun = leprechaun;
        this.treeScenePatch = treeScenePatch;
        this.treeFarmer = treeFarmer;
        this.gardenScenePatch = gardenScenePatch; this.gardenFarmer = gardenFarmer;
        this.fieldScenePatch = fieldScenePatch; this.fieldFarmer = fieldFarmer;
    }

    public Set<Integer> getInventoryItems() { return inventoryItems; }
    public Set<Integer> getBankItems() { return bankItems; }
    public WorldPoint getDestination() { return destination; }
    public String getDestinationName() { return destinationName; }
    public HerbPatchDefinition getScenePatch() { return scenePatch; }
    public boolean isLeprechaun() { return leprechaun; }
    public com.farmingnavigator.farming.tree.TreePatchDefinition getTreeScenePatch() { return treeScenePatch; }
    public boolean isTreeFarmer() { return treeFarmer; }
    public com.farmingnavigator.farming.garden.GardenPatchDefinition getGardenScenePatch() { return gardenScenePatch; }
    public boolean isGardenFarmer() { return gardenFarmer; }
    public com.farmingnavigator.farming.field.FieldPatchDefinition getFieldScenePatch() { return fieldScenePatch; }
    public boolean isFieldFarmer() { return fieldFarmer; }

    public static GuidedRunTargets empty()
    {
        return new GuidedRunTargets(Collections.emptySet(), Collections.emptySet(), null, "", null, false);
    }

    public static GuidedRunTargets from(GuidedRunSnapshot session, HerbRunPlan plan, TravelCapabilitySnapshot travel)
    {
        if (session == null || !session.isActive())
        {
            return empty();
        }
        Set<Integer> inventory = new HashSet<>();
        Set<Integer> bank = new HashSet<>();
        WorldPoint destination = null;
        String name = "";
        HerbHouseEntryMode houseEntryMode = plan == null || plan.getTransportResources() == null
                ? null : plan.getTransportResources().getHouseEntryMode();
        switch (session.getStep())
        {
            case PREPARE:
                if (plan != null && plan.getCrop() == session.getCrop())
                {
                    plan.getSupplies().forEach(check -> {
                        inventory.addAll(check.getRequirement().getAcceptedItemIds());
                        if (check.getWithdrawQuantity() > 0)
                        {
                            bank.addAll(check.getRequirement().getAcceptedItemIds());
                        }
                    });
                    plan.getStops().forEach(stop -> addTransport(inventory, bank, stop.getRecommended(), travel, houseEntryMode));
                    if (plan.getTransportResources() != null)
                    {
                        bank.addAll(plan.getTransportResources().getBankItems());
                        plan.getTransportResources().getConsumables().forEach(check -> {
                            inventory.add(check.getRequirement().getItemId());
                            if (check.getWithdrawQuantity() > 0)
                            {
                                bank.add(check.getRequirement().getItemId());
                            }
                        });
                    }
                    if (plan.getCareResources() != null)
                    {
                        addRunes(inventory, plan.getCareResources().getRunes(), travel);
                    }
                }
                break;
            case TRAVEL:
                addTransport(inventory, bank, session.getStop().getRecommended(), travel, houseEntryMode);
                destination = HerbPatchDestination.valueOf(session.getStop().getPatch().name()).getLocation();
                name = session.getStop().getPatch().getDisplayName() + " farming area";
                break;
            case RAKE: inventory.add(ItemID.RAKE); break;
            case HARVEST:
            case CLEAR: inventory.add(ItemID.SPADE); break;
            case PLANT:
                inventory.add(session.getCrop().getSeedId());
                inventory.add(ItemID.DIBBER);
                break;
            case COMPOST:
                if (session.getCompost().isSpell())
                {
                    addRunes(inventory, HerbRunCareResources.FERTILE_SOIL_COST, travel);
                    if (session.getCompost() == HerbRunCompost.FERTILE_SOIL_ULTRA)
                    {
                        inventory.add(ItemID.FOSSIL_VOLCANIC_ASH);
                    }
                }
                else if (session.getCompost().getItemId() > 0) { inventory.add(session.getCompost().getItemId()); }
                break;
            case CURE:
                if (session.getCure() == HerbRunCure.CURE_PLANT)
                {
                    addRunes(inventory, HerbRunCareResources.CURE_PLANT_COST, travel);
                }
                else { inventory.add(ItemID.PLANT_CURE); }
                break;
            case NOTE_PRODUCE:
                Collections.addAll(inventory, ItemID.UNIDENTIFIED_GUAM, ItemID.UNIDENTIFIED_MARENTILL,
                        ItemID.UNIDENTIFIED_TARROMIN, ItemID.UNIDENTIFIED_HARRALANDER, ItemID.UNIDENTIFIED_RANARR,
                        ItemID.UNIDENTIFIED_TOADFLAX, ItemID.UNIDENTIFIED_IRIT, ItemID.UNIDENTIFIED_AVANTOE,
                        ItemID.UNIDENTIFIED_KWUARM, ItemID.UNIDENTIFIED_SNAPDRAGON, ItemID.UNIDENTIFIED_HUASCA,
                        ItemID.UNIDENTIFIED_CADANTINE, ItemID.UNIDENTIFIED_LANTADYME,
                        ItemID.UNIDENTIFIED_DWARF_WEED, ItemID.UNIDENTIFIED_TORSTOL,
                        ItemID.GUAM_LEAF, ItemID.MARENTILL, ItemID.TARROMIN, ItemID.HARRALANDER,
                        ItemID.RANARR_WEED, ItemID.TOADFLAX, ItemID.IRIT_LEAF, ItemID.AVANTOE,
                        ItemID.KWUARM, ItemID.SNAPDRAGON, ItemID.HUASCA, ItemID.CADANTINE,
                        ItemID.LANTADYME, ItemID.DWARF_WEED, ItemID.TORSTOL);
                break;
            default: break;
        }
        addSelectedRuneSource(inventory, session, plan, houseEntryMode);
        HerbPatchDefinition scenePatch = session.getStep() == GuidedRunStep.PREPARE || session.getStep() == GuidedRunStep.WAIT
                ? null : session.getStop().getPatch();
        return new GuidedRunTargets(inventory, bank, destination, name, scenePatch,
                session.getStep() == GuidedRunStep.NOTE_PRODUCE);
    }

    public static GuidedRunTargets fromTree(com.farmingnavigator.guided.tree.TreeRunSnapshot session,
            com.farmingnavigator.routing.run.TreeRunPlan plan, TravelCapabilitySnapshot travel)
    {
        if (session == null || !session.isActive()) { return empty(); }
        Set<Integer> inventory = new HashSet<>();
        Set<Integer> bank = new HashSet<>();
        WorldPoint destination = null;
        String name = "";
        RuneCost spellCost = RuneCost.builder().build();
        switch (session.getStep())
        {
            case PREPARE:
                if (plan != null && plan.getCrop() == session.getCrop())
                {
                    plan.getSupplies().forEach(check -> {
                        inventory.addAll(check.getRequirement().getAcceptedItemIds());
                        if (check.getWithdrawQuantity() > 0) { bank.addAll(check.getRequirement().getAcceptedItemIds()); }
                    });
                    plan.getStops().forEach(stop -> addTransport(inventory, bank, stop.getRecommended(), travel, null));
                    if (plan.getTransportResources() != null) { bank.addAll(plan.getTransportResources().getBankItems()); }
                    if (plan.getCareResources() != null) { addRunes(inventory, plan.getCareResources().getRunes(), travel); }
                }
                break;
            case TRAVEL:
                if (!session.getStop().isLocalWalk()) { addTransport(inventory, bank, session.getStop().getRecommended(), travel, null); }
                destination = com.farmingnavigator.guided.tree.TreePatchDestination.valueOf(session.getStop().getPatch().name()).getLocation();
                name = session.getStop().getPatch().getLabel() + " tree farming area";
                if (session.getStop().getRecommended() != null
                        && session.getStop().getRecommended().getOption().getTeleportSpellDefinition() != null)
                {
                    spellCost = session.getStop().getRecommended().getOption().getTeleportSpellDefinition().getRuneCost();
                }
                break;
            case RAKE: inventory.add(ItemID.RAKE); break;
            case CLEAR: inventory.add(ItemID.SPADE); break;
            case PLANT:
                inventory.add(ItemID.SPADE); inventory.add(session.getCrop().getSaplingId()); break;
            case REMOVE:
                inventory.add(ItemID.SPADE);
                addTreeAxes(inventory, plan);
                if (session.getCrop() != com.farmingnavigator.routing.run.TreeRunCrop.CALQUAT
                        && session.getCrop() != com.farmingnavigator.routing.run.TreeRunCrop.CELASTRUS
                        && session.getCrop() != com.farmingnavigator.routing.run.TreeRunCrop.CRYSTAL) { inventory.add(ItemID.COINS); }
                break;
            case HARVEST_BARK: addTreeAxes(inventory, plan); break;
            case PROTECT:
                session.getCrop().protectionPayment(1).ifPresent(payment -> inventory.addAll(payment.getAcceptedItemIds())); break;
            case CURE: spellCost = HerbRunCareResources.CURE_PLANT_COST; addRunes(inventory, spellCost, travel); break;
            case COMPOST:
                if (session.getCompost().isSpell())
                {
                    spellCost = HerbRunCareResources.FERTILE_SOIL_COST; addRunes(inventory, spellCost, travel);
                    if (session.getCompost() == com.farmingnavigator.routing.run.TreeRunCompost.FERTILE_SOIL_ULTRA) { inventory.add(ItemID.FOSSIL_VOLCANIC_ASH); }
                }
                else if (session.getCompost().getItemId() > 0) { inventory.add(session.getCompost().getItemId()); }
                break;
            case PRUNE:
                inventory.add(ItemID.SECATEURS); inventory.add(ItemID.FAIRY_ENCHANTED_SECATEURS); break;
            default: break;
        }
        RunePreparation preparation = plan == null || plan.getTransportResources() == null
                ? null : plan.getTransportResources().getRunePreparation();
        if (preparation != null && preparation.getSource() != null)
        {
            boolean needed = session.getStep() == com.farmingnavigator.guided.tree.TreeRunStep.PREPARE;
            for (com.farmingnavigator.routing.requirement.RuneKind kind : preparation.getSource().getSuppliedKinds())
            {
                needed |= spellCost.getQuantity(kind) > 0;
            }
            if (needed) { inventory.add(preparation.getSource().getItemId()); }
        }
        boolean scene = session.getStep() != com.farmingnavigator.guided.tree.TreeRunStep.PREPARE
                && session.getStep() != com.farmingnavigator.guided.tree.TreeRunStep.WAIT;
        return new GuidedRunTargets(inventory, bank, destination, name, null, false,
                scene ? session.getStop().getPatch() : null,
                session.getStep() == com.farmingnavigator.guided.tree.TreeRunStep.PROTECT
                        || session.getStep() == com.farmingnavigator.guided.tree.TreeRunStep.REMOVE
                        && session.getCrop() != com.farmingnavigator.routing.run.TreeRunCrop.CALQUAT
                        && session.getCrop() != com.farmingnavigator.routing.run.TreeRunCrop.CELASTRUS
                        && session.getCrop() != com.farmingnavigator.routing.run.TreeRunCrop.CRYSTAL);
    }

    public static GuidedRunTargets fromGarden(com.farmingnavigator.guided.garden.GardenRunSnapshot session,
            com.farmingnavigator.routing.run.GardenRunPlan plan, TravelCapabilitySnapshot travel)
    {
        if (session == null || !session.isActive()) { return empty(); }
        Set<Integer> inventory = new HashSet<>(); Set<Integer> bank = new HashSet<>();
        WorldPoint destination = null; String name = ""; RuneCost spellCost = RuneCost.builder().build();
        switch (session.getStep())
        {
            case PREPARE:
                if (plan != null && plan.getCrop() == session.getCrop())
                {
                    plan.getSupplies().forEach(check -> {
                        inventory.addAll(check.getRequirement().getAcceptedItemIds());
                        if (check.getWithdrawQuantity() > 0) { bank.addAll(check.getRequirement().getAcceptedItemIds()); }
                    });
                    plan.getStops().forEach(stop -> addTransport(inventory, bank, stop.getRecommended(), travel, null));
                    if (plan.getTransportResources() != null) { bank.addAll(plan.getTransportResources().getBankItems()); }
                    if (plan.getCareResources() != null) { addRunes(inventory, plan.getCareResources().getRunes(), travel); }
                }
                break;
            case TRAVEL:
                addTransport(inventory, bank, session.getStop().getRecommended(), travel, null);
                destination = session.getStop().getPatch().getLocation(); name = session.getStop().getPatch().getLabel() + " bush/cactus farming area";
                if (session.getStop().getRecommended() != null && session.getStop().getRecommended().getOption().getTeleportSpellDefinition() != null)
                {
                    spellCost = session.getStop().getRecommended().getOption().getTeleportSpellDefinition().getRuneCost();
                }
                break;
            case RAKE: inventory.add(ItemID.RAKE); break;
            case CLEAR: inventory.add(ItemID.SPADE); break;
            case PLANT: inventory.add(ItemID.DIBBER); inventory.add(session.getCrop().getSeedId()); break;
            case PROTECT: session.getCrop().protectionPayment(1).ifPresent(payment -> inventory.addAll(payment.getAcceptedItemIds())); break;
            case PRUNE: inventory.add(ItemID.SECATEURS); inventory.add(ItemID.FAIRY_ENCHANTED_SECATEURS); break;
            case CURE:
                if (session.getCure() == HerbRunCure.CURE_PLANT)
                {
                    spellCost = HerbRunCareResources.CURE_PLANT_COST; addRunes(inventory, spellCost, travel);
                }
                else { inventory.add(ItemID.PLANT_CURE); }
                break;
            case COMPOST:
                if (session.getCompost().isSpell())
                {
                    spellCost = HerbRunCareResources.FERTILE_SOIL_COST; addRunes(inventory, spellCost, travel);
                    if (session.getCompost() == com.farmingnavigator.routing.run.GardenRunCompost.FERTILE_SOIL_ULTRA) { inventory.add(ItemID.FOSSIL_VOLCANIC_ASH); }
                }
                else if (session.getCompost().getItemId() > 0) { inventory.add(session.getCompost().getItemId()); }
                break;
            default: break;
        }
        RunePreparation preparation = plan == null || plan.getTransportResources() == null ? null : plan.getTransportResources().getRunePreparation();
        if (preparation != null && preparation.getSource() != null)
        {
            boolean needed = session.getStep() == com.farmingnavigator.guided.garden.GardenRunStep.PREPARE;
            for (com.farmingnavigator.routing.requirement.RuneKind kind : preparation.getSource().getSuppliedKinds()) { needed |= spellCost.getQuantity(kind) > 0; }
            if (needed) { inventory.add(preparation.getSource().getItemId()); }
        }
        boolean scene = session.getStep() != com.farmingnavigator.guided.garden.GardenRunStep.PREPARE
                && session.getStep() != com.farmingnavigator.guided.garden.GardenRunStep.WAIT
                && session.getStep() != com.farmingnavigator.guided.garden.GardenRunStep.MAKE_SPACE;
        return new GuidedRunTargets(inventory, bank, destination, name, null, false, null, false,
                scene ? session.getStop().getPatch() : null, session.getStep() == com.farmingnavigator.guided.garden.GardenRunStep.PROTECT);
    }

    public static GuidedRunTargets fromField(com.farmingnavigator.guided.field.FieldRunSnapshot session,
            com.farmingnavigator.routing.run.FieldRunPlan plan, TravelCapabilitySnapshot travel)
    {
        if (session == null || !session.isActive()) { return empty(); }
        Set<Integer> inventory = new HashSet<>(); Set<Integer> bank = new HashSet<>();
        WorldPoint destination = null; String name = ""; RuneCost spellCost = RuneCost.builder().build();
        HerbHouseEntryMode houseEntryMode = plan == null || plan.getTransportResources() == null
                ? null : plan.getTransportResources().getHouseEntryMode();
        switch (session.getStep())
        {
            case PREPARE:
                if (plan != null && plan.getCrop() == session.getCrop())
                {
                    plan.getSupplies().forEach(check -> {
                        inventory.addAll(check.getRequirement().getAcceptedItemIds());
                        if (check.getWithdrawQuantity() > 0) { bank.addAll(check.getRequirement().getAcceptedItemIds()); }
                    });
                    plan.getStops().forEach(stop -> addTransport(inventory, bank, stop.getRecommended(), travel, houseEntryMode));
                    if (plan.getTransportResources() != null) { bank.addAll(plan.getTransportResources().getBankItems()); }
                    if (plan.getWateringApplications() > 0)
                    {
                        inventory.addAll(com.farmingnavigator.routing.run.WateringCanBudget.getFilledCanIds());
                        if (plan.getWateringBudget() != null) { bank.addAll(plan.getWateringBudget().getBankItems()); }
                    }
                    if (plan.getCareResources() != null) { addRunes(inventory, plan.getCareResources().getRunes(), travel); }
                }
                break;
            case TRAVEL:
                if (session.getCrop() == com.farmingnavigator.routing.run.FieldRunCrop.SEAWEED)
                {
                    inventory.add(ItemID.HUNDRED_PIRATE_DIVING_HELMET); inventory.add(ItemID.HUNDRED_PIRATE_DIVING_BACKPACK);
                }
                addTransport(inventory, bank, session.getStop().getRecommended(), travel, houseEntryMode);
                destination = session.getStop().getPatch().getLocation(); name = session.getStop().getPatch().getLabel() + " farming area";
                if (session.getStop().getRecommended() != null && session.getStop().getRecommended().getOption().getTeleportSpellDefinition() != null)
                {
                    spellCost = session.getStop().getRecommended().getOption().getTeleportSpellDefinition().getRuneCost();
                }
                else if (session.getStop().getRecommended() != null
                        && session.getStop().getRecommended().getOption().getCategory() == TransportCategory.PLAYER_OWNED_HOUSE
                        && houseEntryMode == HerbHouseEntryMode.SPELL)
                {
                    spellCost = TeleportSpellDefinition.TELEPORT_TO_HOUSE.getRuneCost();
                }
                break;
            case RAKE: inventory.add(ItemID.RAKE); break;
            case HARVEST:
                if (session.getCrop() == com.farmingnavigator.routing.run.FieldRunCrop.BELLADONNA) { inventory.add(ItemID.LEATHER_GLOVES); }
                if (session.getStop().getPatch().getFamily() != com.farmingnavigator.farming.field.FieldFamily.FLOWER) { inventory.add(ItemID.SPADE); }
                break;
            case CLEAR: inventory.add(ItemID.SPADE); break;
            case PLANT: inventory.add(ItemID.DIBBER); inventory.add(session.getCrop().getSeedId()); break;
            case PROTECT:
                if (session.getStop().getPatch() != com.farmingnavigator.farming.field.FieldPatchDefinition.HARMONY_ISLAND_ALLOTMENT)
                {
                    session.getCrop().protectionPayment(1).ifPresent(payment -> inventory.addAll(payment.getAcceptedItemIds()));
                }
                break;
            case WATER: inventory.addAll(com.farmingnavigator.routing.run.WateringCanBudget.getFilledCanIds()); break;
            case CURE:
                if (session.getCure() == HerbRunCure.CURE_PLANT)
                {
                    spellCost = HerbRunCareResources.CURE_PLANT_COST; addRunes(inventory, spellCost, travel);
                }
                else { inventory.add(ItemID.PLANT_CURE); }
                break;
            case COMPOST:
                if (session.getCompost().isSpell())
                {
                    spellCost = HerbRunCareResources.FERTILE_SOIL_COST; addRunes(inventory, spellCost, travel);
                    if (session.getCompost() == com.farmingnavigator.routing.run.FieldRunCompost.FERTILE_SOIL_ULTRA) { inventory.add(ItemID.FOSSIL_VOLCANIC_ASH); }
                }
                else if (session.getCompost().getItemId() > 0) { inventory.add(session.getCompost().getItemId()); }
                break;
            default: break;
        }
        RunePreparation preparation = plan == null || plan.getTransportResources() == null ? null : plan.getTransportResources().getRunePreparation();
        if (preparation != null && preparation.getSource() != null)
        {
            boolean needed = session.getStep() == com.farmingnavigator.guided.field.FieldRunStep.PREPARE;
            for (com.farmingnavigator.routing.requirement.RuneKind kind : preparation.getSource().getSuppliedKinds()) { needed |= spellCost.getQuantity(kind) > 0; }
            if (needed) { inventory.add(preparation.getSource().getItemId()); }
        }
        boolean scene = session.getStep() != com.farmingnavigator.guided.field.FieldRunStep.PREPARE
                && session.getStep() != com.farmingnavigator.guided.field.FieldRunStep.WAIT
                && session.getStep() != com.farmingnavigator.guided.field.FieldRunStep.SCARECROW
                && session.getStep() != com.farmingnavigator.guided.field.FieldRunStep.REPREPARE
                && session.getStep() != com.farmingnavigator.guided.field.FieldRunStep.MAKE_SPACE;
        return new GuidedRunTargets(inventory, bank, destination, name, null, false, null, false, null, false,
                scene ? session.getStop().getPatch() : null, session.getStep() == com.farmingnavigator.guided.field.FieldRunStep.PROTECT);
    }

    private static void addTreeAxes(Set<Integer> items, com.farmingnavigator.routing.run.TreeRunPlan plan)
    {
        if (plan == null) { return; }
        plan.getSupplies().stream().filter(check -> check.getRequirement().getItemId() == ItemID.BRONZE_AXE)
                .forEach(check -> items.addAll(check.getRequirement().getAcceptedItemIds()));
    }

    private static void addSelectedRuneSource(Set<Integer> items, GuidedRunSnapshot session, HerbRunPlan plan,
            HerbHouseEntryMode houseEntryMode)
    {
        RunePreparation preparation = plan == null || plan.getTransportResources() == null
                ? null : plan.getTransportResources().getRunePreparation();
        if (preparation == null || preparation.getSource() == null) { return; }
        RuneCost cost = RuneCost.builder().build();
        switch (session.getStep())
        {
            case PREPARE:
                items.add(preparation.getSource().getItemId());
                return;
            case CURE:
                if (session.getCure() == HerbRunCure.CURE_PLANT) { cost = HerbRunCareResources.CURE_PLANT_COST; }
                break;
            case COMPOST:
                if (session.getCompost().isSpell()) { cost = HerbRunCareResources.FERTILE_SOIL_COST; }
                break;
            case TRAVEL:
                if (session.getStop().getRecommended() == null) { return; }
                TransportOption route = session.getStop().getRecommended().getOption();
                if (route.getTeleportSpellDefinition() != null) { cost = route.getTeleportSpellDefinition().getRuneCost(); }
                else if (route.getCategory() == TransportCategory.PLAYER_OWNED_HOUSE && houseEntryMode == HerbHouseEntryMode.SPELL)
                {
                    cost = TeleportSpellDefinition.TELEPORT_TO_HOUSE.getRuneCost();
                }
                break;
            default: return;
        }
        for (com.farmingnavigator.routing.requirement.RuneKind kind : preparation.getSource().getSuppliedKinds())
        {
            if (cost.getQuantity(kind) > 0) { items.add(preparation.getSource().getItemId()); return; }
        }
    }

    private static void addTransport(Set<Integer> inventory, Set<Integer> bank, VerifiedTransportResult route,
            TravelCapabilitySnapshot travel, HerbHouseEntryMode houseEntryMode)
    {
        if (route == null || !route.isUsableForPlannedRun())
        {
            return;
        }
        Set<Integer> items = new HashSet<>();
        TransportOption option = route.getOption();
        if (option.getCategory() == TransportCategory.PLAYER_OWNED_HOUSE)
        {
            if (houseEntryMode != HerbHouseEntryMode.SPELL)
            {
                items.add(ItemID.POH_TABLET_TELEPORTTOHOUSE);
            }
            if (houseEntryMode != HerbHouseEntryMode.TABLETS)
            {
                addSpell(items, TeleportSpellDefinition.TELEPORT_TO_HOUSE, travel);
            }
        }
        switch (option.getVerificationKind())
        {
            case EXPLORER_RING_UNLIMITED:
                Collections.addAll(items, ItemID.LUMBRIDGE_RING_HARD, ItemID.LUMBRIDGE_RING_ELITE); break;
            case EXPLORER_RING_2_DAILY: items.add(ItemID.LUMBRIDGE_RING_MEDIUM); break;
            case ARDOUGNE_CLOAK_UNLIMITED:
                Collections.addAll(items, ItemID.ARDY_CAPE_ELITE, ItemID.SKILLCAPE_MAX_ARDY); break;
            case KANDARIN_HEADGEAR_UNLIMITED: items.add(ItemID.SEERS_HEADBAND_ELITE); break;
            case KANDARIN_HEADGEAR_3_DAILY: items.add(ItemID.SEERS_HEADBAND_HARD); break;
            case SKILLS_NECKLACE: addJewellery(items, ChargedTeleportItemDefinition.SKILLS_NECKLACE); break;
            case AMULET_OF_GLORY: addJewellery(items, ChargedTeleportItemDefinition.AMULET_OF_GLORY); break;
            case RING_OF_WEALTH: addJewellery(items, ChargedTeleportItemDefinition.RING_OF_WEALTH); break;
            case WEALTH_SPIRIT_TREE:
            case WEALTH_PLANTED_SPIRIT_TREE: addJewellery(items, ChargedTeleportItemDefinition.RING_OF_WEALTH); break;
            case DIGSITE_BARGE: addJewellery(items, ChargedTeleportItemDefinition.DIGSITE_PENDANT); break;
            case LLETYA_CRYSTAL: addJewellery(items, ChargedTeleportItemDefinition.TELEPORT_CRYSTAL); break;
            case PRIFDDINAS_CRYSTAL: addJewellery(items, ChargedTeleportItemDefinition.TELEPORT_CRYSTAL); break;
            case COMBAT_BRACELET: addJewellery(items, ChargedTeleportItemDefinition.COMBAT_BRACELET); break;
            case ARDOUGNE_TELEPORT_TABLET: items.add(ItemID.POH_TABLET_ARDOUGNETELEPORT); break;
            case STONY_BASALT: items.add(ItemID.STRONGHOLD_TELEPORT_BASALT); break;
            case ICY_BASALT: items.add(ItemID.WEISS_TELEPORT_BASALT); break;
            case ECTOPHIAL:
            case ECTOPHIAL_HARMONY: items.add(ItemID.ECTOPHIAL); break;
            case XERICS_TALISMAN_GLADE: items.add(ItemID.XERIC_TALISMAN); break;
            case RING_OF_ELEMENTS: items.add(ItemID.RING_OF_ELEMENTS_CHARGED); break;
            case QUETZAL_WHISTLE: items.add(ItemID.HG_QUETZALWHISTLE_PERFECTED_INFINITE); break;
            case FARMING_CAPE:
                Collections.addAll(items, ItemID.SKILLCAPE_FARMING, ItemID.SKILLCAPE_FARMING_TRIMMED, ItemID.SKILLCAPE_MAX_WORN); break;
            case FAIRY_RING_ACCESS:
            case FAIRY_RING_AJP:
            case POH_FAIRY_RING:
            case WEISS_LARRY_BOAT:
                Collections.addAll(items, ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF); break;
            case MOS_LE_HARMLESS_HARMONY: items.add(ItemID.TELEPORTSCROLL_MOSLES); break;
            case TAI_BWO_WANNAI_SCROLL: items.add(ItemID.TELEPORTSCROLL_TAIBWO); break;
            case TELEPORT_SPELL: addSpell(items, option.getTeleportSpellDefinition(), travel); break;
            default: break;
        }
        inventory.addAll(items);
        if (route.getState() == VerifiedTransportState.AVAILABLE_FROM_BANK)
        {
            bank.addAll(items);
        }
    }

    private static void addJewellery(Set<Integer> items, ChargedTeleportItemDefinition family)
    {
        family.getVariants().forEach(variant -> items.add(variant.getItemId()));
    }

    private static void addSpell(Set<Integer> items, TeleportSpellDefinition spell, TravelCapabilitySnapshot travel)
    {
        if (spell == null) { return; }
        addRunes(items, spell.getRuneCost(), travel);
    }

    private static void addRunes(Set<Integer> items, RuneCost cost, TravelCapabilitySnapshot travel)
    {
        boolean pouchUseful = false;
        for (RuneSupply supply : RuneSupply.values())
        {
            if (supply.getSuppliedKinds().stream().anyMatch(kind -> cost.getQuantity(kind) > 0))
            {
                items.add(supply.getItemId());
                pouchUseful |= travel != null && travel.getStoredRunePouchQuantities().getOrDefault(supply.getItemId(), 0) > 0;
            }
        }
        if (pouchUseful)
        {
            Collections.addAll(items, ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER,
                    ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER);
        }
    }
}
