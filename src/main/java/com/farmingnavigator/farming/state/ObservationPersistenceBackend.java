package com.farmingnavigator.farming.state;

/** Profile-explicit config access; implementations use RuneLite ConfigManager. */
public interface ObservationPersistenceBackend
{
    String read(String profile, String key);
    void write(String profile, String key, String value);
}
