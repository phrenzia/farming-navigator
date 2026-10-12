package com.farmingnavigator.routing.requirement;

import com.farmingnavigator.account.AccountState;

public enum AccountSkill
{
    FARMING("Farming")
            {
                @Override
                public int getRealLevel(
                        AccountState accountState
                )
                {
                    return accountState
                            .getRealFarmingLevel();
                }

                @Override
                public int getBoostedLevel(
                        AccountState accountState
                )
                {
                    return accountState
                            .getBoostedFarmingLevel();
                }
            },

    MAGIC("Magic")
            {
                @Override
                public int getRealLevel(
                        AccountState accountState
                )
                {
                    return accountState
                            .getRealMagicLevel();
                }

                @Override
                public int getBoostedLevel(
                        AccountState accountState
                )
                {
                    return accountState
                            .getBoostedMagicLevel();
                }
            },

    AGILITY("Agility")
            {
                @Override
                public int getRealLevel(
                        AccountState accountState
                )
                {
                    return accountState
                            .getRealAgilityLevel();
                }

                @Override
                public int getBoostedLevel(
                        AccountState accountState
                )
                {
                    return accountState
                            .getBoostedAgilityLevel();
                }
            },

    HUNTER("Hunter")
            {
                @Override
                public int getRealLevel(
                        AccountState accountState
                )
                {
                    return accountState
                            .getRealHunterLevel();
                }

                @Override
                public int getBoostedLevel(
                        AccountState accountState
                )
                {
                    return accountState
                            .getBoostedHunterLevel();
                }
            };

    private final String displayName;

    AccountSkill(
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

    public abstract int getRealLevel(
            AccountState accountState
    );

    public abstract int getBoostedLevel(
            AccountState accountState
    );
}