package com.farmingnavigator.farming.state;

import com.farmingnavigator.farming.HerbPatchDefinition;
import com.farmingnavigator.FarmingNavigatorConfig;
import java.time.Instant;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WidgetNode;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.config.ConfigManager;

/** Reads one region-specific herb varbit per settled game tick; never scans the scene. */
public class HerbPatchObservationService
{
    private final Client client;
    private final ConfigManager configManager;
    private final HerbObservationPersistence persistence;
    private final HerbPatchObservationStore store = new HerbPatchObservationStore();
    private final HerbPatchObservationSampler sampler = new HerbPatchObservationSampler(store);

    @Inject
    public HerbPatchObservationService(Client client, ConfigManager configManager)
    {
        this.client = client;
        this.configManager = configManager;
        this.persistence = new HerbObservationPersistence(new HerbObservationPersistence.Backend()
        {
            @Override public String read(String profile, String key)
            {
                return configManager.getConfiguration(FarmingNavigatorConfig.GROUP, profile, key);
            }

            @Override public void write(String profile, String key, String value)
            {
                configManager.setConfiguration(FarmingNavigatorConfig.GROUP, profile, key, value);
            }
        });
    }

    public boolean sample()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            reset();
            return false;
        }
        Instant now = Instant.now();
        boolean restored = persistence.loadProfile(configManager.getRSProfileKey(), store, now);
        if (restored)
        {
            sampler.invalidateRegion();
        }
        // Do not record observations until RuneLite identifies the character and world-mode profile.
        if (configManager.getRSProfileKey() == null)
        {
            return restored;
        }
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
        boolean changed = sampler.sample(player == null ? null : player.getWorldLocation(),
                client.getGameState() == GameState.LOGGED_IN, modalOpen,
                worldView == null || worldView.isInstance(),
                client.getTickCount(), client::getVarbitValue, now,
                FarmingGrowthRate.fromWorldTypes(client.getWorldType()));
        if (changed)
        {
            persistence.save(store.snapshot());
        }
        return restored || changed;
    }

    public Map<HerbPatchDefinition, HerbPatchObservation> snapshot()
    {
        return store.snapshot();
    }

    public void reset()
    {
        sampler.reset();
        persistence.detach();
    }

    public void invalidateRegion()
    {
        sampler.invalidateRegion();
    }

    public boolean hasObservations()
    {
        return !store.isEmpty();
    }

    public WorldPoint currentLocation()
    {
        WorldView view = client.getTopLevelWorldView();
        Player player = client.getLocalPlayer();
        if (client.getGameState() != GameState.LOGGED_IN || player == null || view == null || view.isInstance())
        {
            return null;
        }
        return player.getWorldLocation();
    }
}
