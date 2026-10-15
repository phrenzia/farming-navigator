package com.farmingnavigator;

import com.farmingnavigator.poh.PohHouseLocation;
import com.farmingnavigator.poh.PohJewelleryBoxTier;
import com.farmingnavigator.poh.PohPortalDestination;
import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.routing.run.HerbRunCrop;
import com.farmingnavigator.routing.run.HerbRunCompost;
import com.farmingnavigator.routing.run.HerbTransportPreference;
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
            description = "Suggested bucket supply; bottomless bucket and Fertile Soil supplies are not checked",
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
            description = "Reusable preference favors verified unlimited methods, even if bank withdrawal is needed; travel time is not optimized",
            position = 6,
            section = GENERAL_SECTION
    )
    default HerbTransportPreference herbTransportPreference()
    {
        return HerbTransportPreference.CATALOGUE;
    }

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
