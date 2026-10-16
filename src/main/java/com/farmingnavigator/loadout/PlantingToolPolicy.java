package com.farmingnavigator.loadout;

import com.farmingnavigator.account.AccountState;
import com.farmingnavigator.farming.field.FieldFamily;
import com.farmingnavigator.farming.garden.GardenFamily;

/** Scope is limited to the direct seed families listed by the pinned Barbarian Training helper.
 * Cactus, spores, belladonna and other special planting remain unverified here. */
public final class PlantingToolPolicy
{
    private PlantingToolPolicy() { }
    public static boolean herbDibberRequired(AccountState account)
    { return account==null || !account.hasCompletedBarbarianPlanting(); }
    public static boolean fieldDibberRequired(AccountState account,FieldFamily family)
    { return herbDibberRequired(account) || family!=FieldFamily.ALLOTMENT && family!=FieldFamily.FLOWER && family!=FieldFamily.HOPS; }
    public static boolean gardenDibberRequired(AccountState account,GardenFamily family)
    { return herbDibberRequired(account) || family!=GardenFamily.BUSH; }
    public static String omittedNote() { return "Completed Barbarian Planting observed; no seed dibber is reserved for this crop family."; }
    public static String retainedNote(AccountState account)
    {
        return !herbDibberRequired(account)?"Completed Barbarian Planting observed, but dibber-free planting for this special family is unverified; seed dibber retained."
                :"Planting tool; barbarian planting completion is not established. Partial training does not remove this tool.";
    }
}
