package com.medgame.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class JsonLoader {

    private final Json json = new Json();

    public <T> T load(String internalPath, Class<T> type) {
        FileHandle handle = Gdx.files.internal(internalPath);
        if (!handle.exists()) {
            Gdx.app.error("JsonLoader", "File not found: " + internalPath);
            return null;
        }
        try {
            return json.fromJson(type, handle);
        } catch (Exception e) {
            Gdx.app.error("JsonLoader", "Failed to parse: " + internalPath, e);
            return null;
        }
    }

    public JsonValue loadRaw(String internalPath) {
        FileHandle handle = Gdx.files.internal(internalPath);
        if (!handle.exists()) return null;
        return new com.badlogic.gdx.utils.JsonReader().parse(handle);
    }
}
