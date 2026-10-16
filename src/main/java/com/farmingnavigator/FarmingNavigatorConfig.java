package com.farmingnavigator;

import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.poh.PohJewelleryBoxTier;
import com.farmingnavigator.poh.PohPortalDestination;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.HerbRunCompost;
import com.farmingnavigator.routing.run.HerbTransportPreference;
import com.farmingnavigator.routing.run.HerbHouseEntryMode;
import com.farmingnavigator.routing.run.HerbRunCure;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(FarmingNavigatorConfig.GROUP)
public interface FarmingNavigatorConfig
        extends Config
{
    String GROUP =
            "farming-navigator";

    String POH_HOUSE_LOCATION_KEY =
            "pohHouseLocation";

    String POH_JEWELLERY_BOX_KEY =
            "pohJewelleryBox";

    String POH_FAIRY_RING_KEY =
            "pohFairyRing";

    String POH_SPIRIT_TREE_KEY =
            "pohSpiritTree";

    String POH_MOUNTED_XERICS_TALISMAN_KEY =
            "pohMountedXericsTalisman";

    String POH_MOUNTED_DIGSITE_PENDANT_KEY =
            "pohMountedDigsitePendant";

    String POH_PORTAL_DESTINATIONS_KEY =
            "pohPortalDestinations";

    @ConfigSection(
            name = "General",
            description = "General Farming Navigator settings",
            position = 0
    )
    String GENERAL_SECTION =
            "general";

    @ConfigItem(
            keyName = "guidedRunOverlays",
            name = "Guided run overlays",
            description = "Enable visual guidance such as patch, item, NPC, and tile highlighting during farming runs",
            position = 0,
            section = GENERAL_SECTION
    )
    default boolean guidedRunOverlays()
    {
        return true;
    }

    @ConfigItem(
            keyName = "herbRunCrop",
            name = "Herb run crop",
            description = "Herb seed to bring for the suggested run. Changing run setup stops the current guided run.",
            position = 1,
            section = GENERAL_SECTION
    )
    default HerbRunCrop herbRunCrop()
    {
        return HerbRunCrop.GUAM;
    }

    @ConfigItem(
            keyName = "herbRunPatches",
            name = "Herb run patches",
            description = "Patches to consider; locked and not-yet-ready growing patches are excluded",
            position = 2,
            section = GENERAL_SECTION
    )
    default Set<HerbPatchDefinition> herbRunPatches()
    {
        return EnumSet.allOf(HerbPatchDefinition.class);
    }

    @ConfigItem(
            keyName = "herbRunCompost",
            name = "Herb run compost",
            description = "Choose buckets or Fertile Soil; bottomless contents and the ash-covered tome unlock need manual confirmation",
            position = 3,
            section = GENERAL_SECTION
    )
    default HerbRunCompost herbRunCompost()
    {
        return HerbRunCompost.ULTRACOMPOST;
    }

    @ConfigItem(
            keyName = "herbObservedAlerts",
            name = "Observed herb alerts",
            description = "Notify when a selected herb patch is newly observed ready, diseased, or dead; uses RuneLite notification settings",
            position = 4,
            section = GENERAL_SECTION
    )
    default boolean herbObservedAlerts()
    {
        return false;
    }

    @ConfigItem(
            keyName = "herbEstimatedAlerts",
            name = "Estimated herb alerts",
            description = "Notify at the end of an estimated harvest window; healthy growth is assumed and must be confirmed by visiting",
            position = 5,
            section = GENERAL_SECTION
    )
    default boolean herbEstimatedAlerts()
    {
        return false;
    }

    @ConfigItem(
            keyName = "herbTransportPreference",
            name = "Herb transport preference",
            description = "Rank verified travel, prefer reusable methods, or try one alternate arrival against the shared budget; no travel-time optimization",
            position = 6,
            section = GENERAL_SECTION
    )
    default HerbTransportPreference herbTransportPreference()
    {
        return HerbTransportPreference.CATALOGUE;
    }

    @ConfigItem(
            keyName = "herbHouseEntryMode",
            name = "Herb POH entry supplies",
            description = "Reserve one tablet or one spell cast per recommended POH visit; spell mode shares runes with direct teleports",
            position = 7,
            section = GENERAL_SECTION
    )
    default HerbHouseEntryMode herbHouseEntryMode()
    {
        return HerbHouseEntryMode.TABLETS;
    }

    @ConfigItem(
            keyName = "herbRunCure",
            name = "Herb disease treatment",
            description = "Reserve potions or Cure Plant casts for observed diseased stops; unexpected disease may require extra supplies",
            position = 8,
            section = GENERAL_SECTION
    )
    default HerbRunCure herbRunCure()
    {
        return HerbRunCure.PLANT_CURE;
    }

    @ConfigItem(
            keyName = "herbAshTomeRead",
            name = "Ash-covered tome read (manual)",
            description = "Manually confirm that you have read the ash-covered tome for ultracompost Fertile Soil; this unlock is not automatically verified",
            position = 9,
            section = GENERAL_SECTION
    )
    default boolean herbAshTomeRead()
    {
        return false;
    }

    @ConfigItem(
            keyName = "showTreeObservations",
            name = "Show tree observations",
            description = "Display tree patch history and conditional health-check windows, including special trees",
            position = 10,
            section = GENERAL_SECTION
    )
    default boolean showTreeObservations() { return true; }

    @ConfigItem(keyName = "treeRunCrop", name = "Tree run crop",
            description = "Finished sapling to bring; supported travel is checked against current account and supplies", position = 11, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.TreeRunCrop treeRunCrop()
    {
        return com.farmingnavigator.routing.run.TreeRunCrop.OAK;
    }

    @ConfigItem(keyName = "treeRunPatches", name = "Tree run patches",
            description = "Only patches matching the selected tree crop family are considered", position = 12, section = GENERAL_SECTION)
    default Set<com.farmingnavigator.farming.tree.TreePatchDefinition> treeRunPatches()
    {
        return Collections.unmodifiableSet(EnumSet.allOf(com.farmingnavigator.farming.tree.TreePatchDefinition.class));
    }

    @ConfigItem(keyName = "treeRunProtection", name = "Reserve tree protection",
            description = "Include farmer payments for potential replants; unverified payment data is flagged", position = 13, section = GENERAL_SECTION)
    default boolean treeRunProtection() { return true; }

    @ConfigItem(keyName = "treeTransportPreference", name = "Tree transport preference",
            description = "Rank verified travel, prefer reusable methods, or try one alternate arrival against the shared budget; no travel-time optimization", position = 14, section = GENERAL_SECTION)
    default HerbTransportPreference treeTransportPreference() { return HerbTransportPreference.CATALOGUE; }

    @ConfigItem(keyName = "treeObservedAlerts", name = "Tree state alerts",
            description = "Notify on fresh health-check-ready, diseased or dead states for selected patches matching the tree run crop family",
            position = 15, section = GENERAL_SECTION)
    default boolean treeObservedAlerts() { return false; }

    @ConfigItem(keyName = "treeEstimatedAlerts", name = "Tree estimate alerts",
            description = "Notify once at the end of a selected tree's conditional growth window; readiness and disease are not verified",
            position = 16, section = GENERAL_SECTION)
    default boolean treeEstimatedAlerts() { return false; }

    @ConfigItem(keyName = "showFarmingMaterialValues", name = "Show materials value",
            description = "Estimate reserved farming materials using RuneLite cached prices; excludes travel, tools, spell runes and harvest value",
            position = 17, section = GENERAL_SECTION)
    default boolean showFarmingMaterialValues() { return true; }

    @ConfigItem(keyName = "showFarmingContract", name = "Show farming contract",
            description = "Display the current game-reported guild assignment and crop conflicts; rewards and completion are not predicted",
            position = 18, section = GENERAL_SECTION)
    default boolean showFarmingContract() { return true; }

    @ConfigItem(keyName = "showGardenObservations", name = "Show bush/cactus observations",
            description = "Display observed bush/cactus history and conditional first health-check windows",
            position = 19, section = GENERAL_SECTION)
    default boolean showGardenObservations() { return true; }

    @ConfigItem(keyName = "gardenRunCrop", name = "Bush/cactus run crop",
            description = "Seed crop for bush/cactus preparation", position = 20, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.GardenRunCrop gardenRunCrop() { return com.farmingnavigator.routing.run.GardenRunCrop.REDBERRIES; }

    @ConfigItem(keyName = "gardenRunPatches", name = "Bush/cactus run patches",
            description = "Only patches matching the selected crop family are considered", position = 21, section = GENERAL_SECTION)
    default Set<com.farmingnavigator.farming.garden.GardenPatchDefinition> gardenRunPatches()
    {
        return Collections.unmodifiableSet(EnumSet.allOf(com.farmingnavigator.farming.garden.GardenPatchDefinition.class));
    }

    @ConfigItem(keyName = "gardenRunMode", name = "Bush/cactus mature plants",
            description = "Keep mature plants by default; replacement reserves a new seed and protection if selected", position = 22, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.GardenRunMode gardenRunMode() { return com.farmingnavigator.routing.run.GardenRunMode.KEEP_MATURE; }

    @ConfigItem(keyName = "gardenRunProtection", name = "Reserve bush/cactus protection",
            description = "Reserve exact farmer payments for possible new plants; poison ivy has no payment", position = 23, section = GENERAL_SECTION)
    default boolean gardenRunProtection() { return true; }

    @ConfigItem(keyName = "gardenTransportPreference", name = "Bush/cactus transport preference",
            description = "Rank verified travel, prefer reusable methods, or try one alternate arrival against the shared budget; no travel-time optimization", position = 24, section = GENERAL_SECTION)
    default HerbTransportPreference gardenTransportPreference() { return HerbTransportPreference.CATALOGUE; }

    @ConfigItem(keyName = "gardenObservedAlerts", name = "Bush/cactus state alerts",
            description = "Notify on fresh health-check-ready, diseased or dead states for selected patches matching the bush/cactus run crop family",
            position = 25, section = GENERAL_SECTION)
    default boolean gardenObservedAlerts() { return false; }

    @ConfigItem(keyName = "gardenEstimatedAlerts", name = "Bush/cactus estimate alerts",
            description = "Notify once at the end of a selected plant's conditional first-growth window; produce quantity and regrowth are not predicted",
            position = 26, section = GENERAL_SECTION)
    default boolean gardenEstimatedAlerts() { return false; }

    @ConfigItem(keyName = "showFieldObservations", name = "Show field observations",
            description = "Display observed allotment, flower and hops/fibre history, scarecrows and conditional growth windows",
            position = 27, section = GENERAL_SECTION)
    default boolean showFieldObservations() { return true; }

    @ConfigItem(keyName = "fieldRunCrop", name = "Field run crop",
            description = "Allotment, flower or traditional hops crop for preparation", position = 28, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.FieldRunCrop fieldRunCrop() { return com.farmingnavigator.routing.run.FieldRunCrop.POTATO; }

    @ConfigItem(keyName = "fieldRunPatches", name = "Field run plots",
            description = "Only plots matching the selected crop family are considered", position = 29, section = GENERAL_SECTION)
    default Set<com.farmingnavigator.farming.field.FieldPatchDefinition> fieldRunPatches()
    {
        return Collections.unmodifiableSet(EnumSet.allOf(com.farmingnavigator.farming.field.FieldPatchDefinition.class));
    }

    @ConfigItem(keyName = "fieldRunMode", name = "Mature flowers",
            description = "Keep mature flowers by default; harvesting them may remove nearby allotment protection", position = 30, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.FieldRunMode fieldRunMode() { return com.farmingnavigator.routing.run.FieldRunMode.KEEP_MATURE_FLOWERS; }

    @ConfigItem(keyName = "fieldRunProtection", name = "Reserve field protection",
            description = "Reserve exact farmer payments for possible new allotments/hops; flowers and Harmony have no farmer payment", position = 31, section = GENERAL_SECTION)
    default boolean fieldRunProtection() { return true; }

    @ConfigItem(keyName = "fieldTransportPreference", name = "Field transport preference",
            description = "Rank verified travel or try one alternate arrival against the shared budget; adjacent plots share one arrival", position = 32, section = GENERAL_SECTION)
    default HerbTransportPreference fieldTransportPreference() { return HerbTransportPreference.CATALOGUE; }

    @ConfigItem(keyName = "fieldObservedAlerts", name = "Field state alerts",
            description = "Notify on fresh harvestable, diseased or dead states for selected plots matching the crop family; quantity is not predicted",
            position = 33, section = GENERAL_SECTION)
    default boolean fieldObservedAlerts() { return false; }

    @ConfigItem(keyName = "fieldEstimatedAlerts", name = "Field estimate alerts",
            description = "Notify once at the end of a selected crop's conditional growth window; visit to confirm health and harvestability",
            position = 34, section = GENERAL_SECTION)
    default boolean fieldEstimatedAlerts() { return false; }

    @ConfigItem(keyName = "fieldRunCompost", name = "Field compost",
            description = "Reserve application buckets separately from payments; bottomless contents/uses and all application remain manual", position = 35, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.FieldRunCompost fieldRunCompost() { return com.farmingnavigator.routing.run.FieldRunCompost.ULTRACOMPOST; }

    @ConfigItem(keyName = "saveRunHistory", name = "Save recent run history",
            description = "Keep the last 20 completed or explicitly stopped checklist statistics locally for this character and mode", position = 36, section = GENERAL_SECTION)
    default boolean saveRunHistory() { return true; }

    @ConfigItem(keyName = "showFieldOverview", name = "Show field overview",
            description = "Summarize selected accessible field plots; historical states and conditional harvest estimates stay separate", position = 37, section = GENERAL_SECTION)
    default boolean showFieldOverview() { return true; }

    @ConfigItem(keyName = "gardenRunCompost", name = "Bush/cactus compost",
            description = "Reserve application buckets or a manually checked bottomless type; application is confirmed manually", position = 38, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.GardenRunCompost gardenRunCompost() { return com.farmingnavigator.routing.run.GardenRunCompost.ULTRACOMPOST; }

    @ConfigItem(keyName = "showReadinessDashboard", name = "Show farming readiness",
            description = "Summarize selected accessible patches across all supported crop families with conditional future windows", position = 39, section = GENERAL_SECTION)
    default boolean showReadinessDashboard() { return true; }

    @ConfigItem(keyName = "fieldRunCure", name = "Field disease treatment",
            description = "Use plant cure or Lunar Cure Plant; spell runes share the travel budget", position = 40, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.HerbRunCure fieldRunCure() { return com.farmingnavigator.routing.run.HerbRunCure.PLANT_CURE; }

    @ConfigItem(keyName = "fieldAshTomeRead", name = "Field ash-covered tome read",
            description = "Manually declare the tome unlock for field Fertile Soil ultracompost; this is not automatically verified", position = 41, section = GENERAL_SECTION)
    default boolean fieldAshTomeRead() { return false; }

    @ConfigItem(keyName = "gardenRunCure", name = "Bush/cactus disease treatment",
            description = "Prune bushes / cure cacti with items, or use Lunar Cure Plant with shared travel runes", position = 42, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.HerbRunCure gardenRunCure() { return com.farmingnavigator.routing.run.HerbRunCure.PLANT_CURE; }

    @ConfigItem(keyName = "gardenAshTomeRead", name = "Bush/cactus ash-covered tome read",
            description = "Manually declare the tome unlock for bush/cactus Fertile Soil ultracompost", position = 43, section = GENERAL_SECTION)
    default boolean gardenAshTomeRead() { return false; }

    @ConfigItem(keyName = "combinedRunTypes", name = "Combined preparation run types",
            description = "Select at least two configured run types to share one preparation budget; use individual checklists for guidance", position = 44, section = GENERAL_SECTION)
    default Set<com.farmingnavigator.routing.run.CombinedRunKind> combinedRunTypes() { return java.util.Collections.emptySet(); }

    @ConfigItem(keyName = "fieldRunWatering", name = "Field watering after planting",
            description = "Optionally reserve standard-can water and a manual watering step for allotments, flowers and hops", position = 45, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.FieldRunWatering fieldRunWatering() { return com.farmingnavigator.routing.run.FieldRunWatering.NONE; }

    @ConfigItem(keyName = "treeRunCompost", name = "Tree compost",
            description = "Reserve buckets, a manually checked bottomless bucket or Fertile Soil for possible plantings; crystal composting is unverified", position = 46, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.TreeRunCompost treeRunCompost() { return com.farmingnavigator.routing.run.TreeRunCompost.NONE; }

    @ConfigItem(keyName = "treeRunCure", name = "Tree disease treatment",
            description = "Prune with secateurs or use Lunar Cure Plant; spell runes share the travel budget", position = 47, section = GENERAL_SECTION)
    default com.farmingnavigator.routing.run.TreeRunCure treeRunCure() { return com.farmingnavigator.routing.run.TreeRunCure.PRUNE; }

    @ConfigItem(keyName = "treeAshTomeRead", name = "Tree ash-covered tome read",
            description = "Manually declare the tome unlock for tree Fertile Soil ultracompost", position = 48, section = GENERAL_SECTION)
    default boolean treeAshTomeRead() { return false; }

    /*
     * POH values are intentionally hidden from RuneLite's generic
     * configuration panel.
     *
     * Farming Navigator provides its own POH editor in the plugin
     * sidebar so controls can use the full width and remain readable.
     */

    @ConfigItem(
            keyName = POH_HOUSE_LOCATION_KEY,
            name = "House location",
            description = "Current player-owned house location",
            hidden = true
    )
    default PohHouseLocation pohHouseLocation()
    {
        return PohHouseLocation.NONE;
    }

    @ConfigItem(
            keyName = POH_JEWELLERY_BOX_KEY,
            name = "Jewellery box",
            description = "Jewellery box installed in the POH",
            hidden = true
    )
    default PohJewelleryBoxTier pohJewelleryBox()
    {
        return PohJewelleryBoxTier.NONE;
    }

    @ConfigItem(
            keyName = POH_FAIRY_RING_KEY,
            name = "Fairy ring",
            description = "Whether the POH contains a usable fairy ring",
            hidden = true
    )
    default boolean pohFairyRing()
    {
        return false;
    }

    @ConfigItem(
            keyName = POH_SPIRIT_TREE_KEY,
            name = "Spirit tree",
            description = "Whether the POH contains a usable spirit tree",
            hidden = true
    )
    default boolean pohSpiritTree()
    {
        return false;
    }

    @ConfigItem(
            keyName = POH_MOUNTED_XERICS_TALISMAN_KEY,
            name = "Mounted Xeric's talisman",
            description = "Whether the POH contains a mounted Xeric's talisman",
            hidden = true
    )
    default boolean pohMountedXericsTalisman()
    {
        return false;
    }

    @ConfigItem(
            keyName = POH_MOUNTED_DIGSITE_PENDANT_KEY,
            name = "Mounted digsite pendant",
            description = "Whether the POH contains a mounted digsite pendant",
            hidden = true
    )
    default boolean pohMountedDigsitePendant()
    {
        return false;
    }

    @ConfigItem(
            keyName = POH_PORTAL_DESTINATIONS_KEY,
            name = "Installed portal destinations",
            description = "Portal Chamber and Portal Nexus destinations installed in the POH",
            hidden = true
    )
    default Set<PohPortalDestination>
    pohPortalDestinations()
    {
        return Collections.emptySet();
    }
}
