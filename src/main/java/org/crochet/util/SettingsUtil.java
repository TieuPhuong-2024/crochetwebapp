package org.crochet.util;

import org.crochet.model.Settings;
import org.crochet.repository.SettingsRepo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Component
public class SettingsUtil {
    private final SettingsRepo settingsRepo;

    public SettingsUtil(SettingsRepo settingsRepo) {
        this.settingsRepo = settingsRepo;
    }

    /**
     * Retrieves all settings as a map with setting key as map key
     * Uses Spring's cache mechanism to avoid repeated database queries
     *
     * @return Map of settings with key-value pairs, or empty map if no settings found
     */
    @Cacheable(value = "settings", key = "'allSettings'")
    public Map<String, Settings> getSettingsMap() {
        var settings = settingsRepo.findSettings();
        if (ObjectUtils.isEmpty(settings)) {
            return Map.of();
        }
        return ObjectUtils.toMap(settings, Settings::getKey, Function.identity());
    }

    /**
     * Clears the settings cache, forcing a refresh on the next access
     */
    @CacheEvict(value = "settings", allEntries = true)
    public void clearCache() {
        // Cache is evicted automatically by the annotation
    }
}
