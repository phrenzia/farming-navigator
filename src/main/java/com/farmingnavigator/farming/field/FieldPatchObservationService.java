package com.farmingnavigator.farming.field;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.farming.state.FarmingGrowthRate;
import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import java.time.Instant;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WidgetNode;
import net.runelite.api.WorldView;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.config.ConfigManager;

public final class FieldPatchObservationService
{
    private final Client client;
    private final ConfigManager config;
    private final FieldPatchObservationStore store = new FieldPatchObservationStore();
    private final FieldPatchObservationSampler sampler = new FieldPatchObservationSampler(store);
    private final FieldObservationPersistence persistence;

    @Inject public FieldPatchObservationService(Client client, ConfigManager config)
    {
        this.client = client;
        this.config = config;
        this.persistence = new FieldObservationPersistence(new ObservationPersistenceBackend()
        {
            @Override public String read(String profile, String key) { return config.getConfiguration(FarmingNavigatorConfig.GROUP, profile, key); }
            @Override public void write(String profile, String key, String value) { config.setConfiguration(FarmingNavigatorConfig.GROUP, profile, key, value); }
        });
    }

    public boolean sample()
    {
        if (client.getGameState() != GameState.LOGGED_IN) { reset(); return false; }
        Instant now = Instant.now();
        boolean restored = persistence.loadProfile(config.getRSProfileKey(), store, now);
        if (restored) { sampler.invalidateRegion(); }
        if (config.getRSProfileKey() == null) { return restored; }
        boolean modal = false;
        for (WidgetNode node : client.getComponentTable())
        {
            if (node.getModalMode() != WidgetModalMode.NON_MODAL) { modal = true; break; }
        }
        Player player = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        boolean changed = sampler.sample(player == null ? null : player.getWorldLocation(), true, modal,
                view == null || view.isInstance(), client.getTickCount(), client::getVarbitValue, now,
                FarmingGrowthRate.fromWorldTypes(client.getWorldType()));
        if (changed) { persistence.save(store.snapshot()); }
        return changed || restored;
    }

    public Map<FieldPatchDefinition, FieldPatchObservation> snapshot() { return store.snapshot(); }
    public boolean hasObservations() { return !store.isEmpty(); }
    public void reset() { sampler.reset(); persistence.detach(); }
    public void invalidateRegion() { sampler.invalidateRegion(); }
}
