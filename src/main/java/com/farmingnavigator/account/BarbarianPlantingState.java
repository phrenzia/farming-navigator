package com.farmingnavigator.account;

/** BRUT_FARMING_PLANTING states corroborated by Quest Helper 5f791937a99a676f23c02aa48c4e4d5b50e7edad.
 * Its BarbarianTraining helper uses exact eq(3) as finished; partial training is not completion. */
public enum BarbarianPlantingState
{
    UNKNOWN, NOT_STARTED, TRAINING, PLANTED_SEED, COMPLETED;
    public static BarbarianPlantingState decode(int raw)
    {
        switch(raw)
        {
            case 0:return NOT_STARTED;
            case 1:return TRAINING;
            case 2:return PLANTED_SEED;
            case 3:return COMPLETED;
            default:return UNKNOWN;
        }
    }
}
