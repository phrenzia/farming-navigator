package com.farmingnavigator.contracts;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarbitID;

/** Fresh account varbits only; no cross-character contract cache or inferred reward ownership. */
public final class FarmingContractService
{
    private final Client client;
    @Inject public FarmingContractService(Client client) { this.client = client; }
    public FarmingContractSnapshot snapshot()
    {
        if (client.getGameState() != GameState.LOGGED_IN) { return FarmingContractSnapshot.decode(false, 0, 0); }
        return FarmingContractSnapshot.decode(true, client.getVarbitValue(VarbitID.FARMGUILD_CONTRACT_TYPE),
                client.getVarbitValue(VarbitID.FARMGUILD_CONTRACT_COMPLETE));
    }
}
