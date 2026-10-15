package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import java.time.Instant;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WidgetNode;
import net.runelite.api.WorldView;
import net.runelite.api.widgets.WidgetModalMode;

/** Reads one region-specific herb varbit per settled game tick; never scans the scene. */
public class HerbPatchObservationService
{
    private final Client client;
    private final HerbPatchObservationStore store = new HerbPatchObservationStore();
    private final HerbPatchObservationSampler sampler = new HerbPatchObservationSampler(store);

    @Inject
    public HerbPatchObservationService(Client client)
    {
        this.client = client;
    }

    public boolean sample()
    {
        Player player = client.getLocalPlayer();
        boolean modalOpen = false;
        for (WidgetNode node : client.getComponentTable())
        {
            if (node.getModalMode() != WidgetModalMode.NON_MODAL)
            {
                modalOpen = true;
                break;
            }
        }
        WorldView worldView = client.getTopLevelWorldView();
        return sampler.sample(player == null ? null : player.getWorldLocation(),
                client.getGameState() == GameState.LOGGED_IN, modalOpen,
                worldView == null || worldView.isInstance(),
                client.getTickCount(), client::getVarbitValue, Instant.now());
    }

    public Map<HerbPatchDefinition, HerbPatchObservation> snapshot()
    {
        return store.snapshot();
    }

    public void reset()
    {
        sampler.reset();
    }

    public void invalidateRegion()
    {
        sampler.invalidateRegion();
    }
}
