package com.medgame.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;

public class SaveManager {

    private final Preferences prefs;
    private final Json json;

    public SaveManager() {
        prefs = Gdx.app.getPreferences(Constants.PREFS_NAME);
        json = new Json();
    }

    public void putString(String key, String value) {
        prefs.putString(key, value);
        prefs.flush();
    }

    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    public <T> void putObject(String key, T object) {
        prefs.putString(key, json.toJson(object));
        prefs.flush();
    }

    public <T> T getObject(String key, Class<T> type, T defaultValue) {
        String raw = prefs.getString(key, null);
        if (raw == null) return defaultValue;
        try {
            return json.fromJson(type, raw);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Failed to deserialize " + key, e);
            return defaultValue;
        }
    }

    public String getPlayerName() {
        return prefs.getString(Constants.PREFS_PLAYER_NAME, "Medic");
    }

    public void setPlayerName(String name) {
        prefs.putString(Constants.PREFS_PLAYER_NAME, name);
        prefs.flush();
    }
}
