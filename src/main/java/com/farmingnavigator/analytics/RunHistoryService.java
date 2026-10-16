package com.farmingnavigator.analytics;

import com.farmingnavigator.FarmingNavigatorConfig;
import com.farmingnavigator.farming.state.ObservationPersistenceBackend;
import java.time.Instant;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;

public final class RunHistoryService
{
    private final ConfigManager config;
    private final RunHistoryStore store;
    @Inject public RunHistoryService(ConfigManager config)
    {
        this.config = config;
        store = new RunHistoryStore(new ObservationPersistenceBackend()
        {
            @Override public String read(String profile, String key) { return config.getConfiguration(FarmingNavigatorConfig.GROUP, profile, key); }
            @Override public void write(String profile, String key, String value) { config.setConfiguration(FarmingNavigatorConfig.GROUP, profile, key, value); }
        });
    }
    public List<RunMetricsSnapshot> synchronize(boolean loggedIn, boolean enabled, RunMetricsSnapshot metrics, Instant now)
    {
        store.loadProfile(loggedIn ? config.getRSProfileKey() : null, now);
        store.observe(metrics, enabled, now);
        return enabled ? store.snapshot() : List.of();
    }
    public boolean clear(String expectedProfile)
    {
        if (expectedProfile == null || !expectedProfile.equals(config.getRSProfileKey())) { return false; }
        store.loadProfile(expectedProfile, Instant.now()); store.clear(); return true;
    }
    public void reset() { store.detach(); }
}
