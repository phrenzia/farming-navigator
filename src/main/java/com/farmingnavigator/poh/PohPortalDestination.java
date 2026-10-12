package com.farmingnavigator.poh;

public enum PohPortalDestination
{
    BATTLEFRONT(
            "Battlefront"
    ),
    FALADOR(
            "Falador"
    ),
    CAMELOT_SEERS(
            "Camelot / Seers' Village"
    ),
    CATHERBY(
            "Catherby"
    ),
    SALVE_GRAVEYARD(
            "Salve Graveyard"
    ),
    KOUREND_CASTLE(
            "Kourend Castle"
    ),
    FENKENSTRAINS_CASTLE(
            "Fenkenstrain's Castle"
    ),
    EAST_ARDOUGNE(
            "East Ardougne"
    ),
    TROLL_STRONGHOLD(
            "Troll Stronghold"
    ),
    WEISS(
            "Weiss"
    ),
    HARMONY_ISLAND(
            "Harmony Island"
    ),
    CIVITAS_ILLA_FORTIS(
            "Civitas illa Fortis"
    );

    private final String displayName;

    PohPortalDestination(
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

    @Override
    public String toString()
    {
        return displayName;
    }
}