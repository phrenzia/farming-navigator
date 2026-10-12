package com.farmingnavigator.poh;

public enum PohJewelleryBoxTier
{
    NONE(
            "None",
            false
    ),
    BASIC(
            "Basic jewellery box",
            false
    ),
    FANCY(
            "Fancy jewellery box",
            true
    ),
    ORNATE(
            "Ornate jewellery box",
            true
    );

    private final String displayName;

    private final boolean
            skillsNecklaceTeleports;

    PohJewelleryBoxTier(
            String displayName,
            boolean skillsNecklaceTeleports
    )
    {
        this.displayName =
                displayName;

        this.skillsNecklaceTeleports =
                skillsNecklaceTeleports;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public boolean hasSkillsNecklaceTeleports()
    {
        return skillsNecklaceTeleports;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}