package com.farmingnavigator.poh;

import com.farmingnavigator.FarmingNavigatorConfig;
import java.util.EnumSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

public class PohProfileService
{
    private final ConfigManager configManager;

    @Inject
    public PohProfileService(
            ConfigManager configManager
    )
    {
        this.configManager =
                configManager;
    }

    public void setHouseLocation(
            PohHouseLocation location
    )
    {
        if (location == null)
        {
            location =
                    PohHouseLocation.NONE;
        }

        configManager.setConfiguration(
                FarmingNavigatorConfig.GROUP,
                FarmingNavigatorConfig.POH_HOUSE_LOCATION_KEY,
                location.name()
        );
    }

    public void setJewelleryBox(
            PohJewelleryBoxTier tier
    )
    {
        if (tier == null)
        {
            tier =
                    PohJewelleryBoxTier.NONE;
        }

        configManager.setConfiguration(
                FarmingNavigatorConfig.GROUP,
                FarmingNavigatorConfig.POH_JEWELLERY_BOX_KEY,
                tier.name()
        );
    }

    public void setFairyRing(
            boolean enabled
    )
    {
        configManager.setConfiguration(
                FarmingNavigatorConfig.GROUP,
                FarmingNavigatorConfig.POH_FAIRY_RING_KEY,
                Boolean.toString(enabled)
        );
    }

    public void setSpiritTree(
            boolean enabled
    )
    {
        configManager.setConfiguration(
                FarmingNavigatorConfig.GROUP,
                FarmingNavigatorConfig.POH_SPIRIT_TREE_KEY,
                Boolean.toString(enabled)
        );
    }

    public void setMountedXericsTalisman(
            boolean enabled
    )
    {
        configManager.setConfiguration(
                FarmingNavigatorConfig.GROUP,
                FarmingNavigatorConfig
                        .POH_MOUNTED_XERICS_TALISMAN_KEY,
                Boolean.toString(enabled)
        );
    }

    public void setMountedDigsitePendant(
            boolean enabled
    )
    {
        configManager.setConfiguration(
                FarmingNavigatorConfig.GROUP,
                FarmingNavigatorConfig
                        .POH_MOUNTED_DIGSITE_PENDANT_KEY,
                Boolean.toString(enabled)
        );
    }

    public void setPortalDestinations(
            Set<PohPortalDestination> destinations
    )
    {
        EnumSet<PohPortalDestination> copy =
                EnumSet.noneOf(
                        PohPortalDestination.class
                );

        if (destinations != null)
        {
            copy.addAll(
                    destinations
            );
        }

        configManager.setConfiguration(
                FarmingNavigatorConfig.GROUP,
                FarmingNavigatorConfig
                        .POH_PORTAL_DESTINATIONS_KEY,
                copy
        );
    }
}