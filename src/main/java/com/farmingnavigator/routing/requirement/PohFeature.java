package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.FarmingNavigatorConfig;

public enum PohFeature
{
    HOUSE(
            "Player-owned house"
    )
            {
                @Override
                public boolean isPresent(
                        FarmingNavigatorConfig config
                )
                {
                    return config
                            .pohHouseLocation()
                            .hasHouse();
                }
            },

    FAIRY_RING(
            "POH fairy ring"
    )
            {
                @Override
                public boolean isPresent(
                        FarmingNavigatorConfig config
                )
                {
                    return config.pohFairyRing();
                }
            },

    SPIRIT_TREE(
            "POH spirit tree"
    )
            {
                @Override
                public boolean isPresent(
                        FarmingNavigatorConfig config
                )
                {
                    return config.pohSpiritTree();
                }
            },

    MOUNTED_XERICS_TALISMAN(
            "Mounted Xeric's talisman"
    )
            {
                @Override
                public boolean isPresent(
                        FarmingNavigatorConfig config
                )
                {
                    return config
                            .pohMountedXericsTalisman();
                }
            },

    MOUNTED_DIGSITE_PENDANT(
            "Mounted digsite pendant"
    )
            {
                @Override
                public boolean isPresent(
                        FarmingNavigatorConfig config
                )
                {
                    return config
                            .pohMountedDigsitePendant();
                }
            },

    SKILLS_NECKLACE_JEWELLERY_BOX(
            "POH Skills necklace teleports"
    )
            {
                @Override
                public boolean isPresent(
                        FarmingNavigatorConfig config
                )
                {
                    return config
                            .pohJewelleryBox()
                            .hasSkillsNecklaceTeleports();
                }
            };

    private final String displayName;

    PohFeature(
            String displayName
    )
    {
        this.displayName =
                displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public abstract boolean isPresent(
            FarmingNavigatorConfig config
    );
}