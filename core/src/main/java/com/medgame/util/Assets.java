package com.medgame.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

public class Assets {

    private final AssetManager manager = new AssetManager();

    // Atlas paths
    public static final String UI_ATLAS       = "sprites/ui.atlas";
    public static final String CARDS_ATLAS    = "sprites/anatomy_cards.atlas";
    public static final String TOOLS_ATLAS    = "sprites/tools.atlas";

    // Font paths
    public static final String PIXEL_FONT     = "fonts/pixel-font.fnt";

    // Anatomy data paths (loaded directly via FileHandle, not AssetManager)
    public static final String ANATOMY_DIR    = "data/anatomy/";
    public static final String CASES_DIR      = "data/cases/";

    public void queueAll() {
        // Only queue assets that exist; data JSON is loaded via FileHandle
        if (Gdx.files.internal(PIXEL_FONT).exists()) {
            manager.load(PIXEL_FONT, BitmapFont.class);
        }
        if (Gdx.files.internal(UI_ATLAS).exists()) {
            manager.load(UI_ATLAS, TextureAtlas.class);
        }
        if (Gdx.files.internal(CARDS_ATLAS).exists()) {
            manager.load(CARDS_ATLAS, TextureAtlas.class);
        }
        if (Gdx.files.internal(TOOLS_ATLAS).exists()) {
            manager.load(TOOLS_ATLAS, TextureAtlas.class);
        }
    }

    public boolean update() {
        return manager.update();
    }

    public float getProgress() {
        return manager.getProgress();
    }

    public <T> T get(String path, Class<T> type) {
        return manager.get(path, type);
    }

    public boolean isLoaded(String path) {
        return manager.isLoaded(path);
    }

    public BitmapFont getPixelFont() {
        if (manager.isLoaded(PIXEL_FONT)) return manager.get(PIXEL_FONT, BitmapFont.class);
        return new BitmapFont();
    }

    public TextureAtlas getUiAtlas() {
        return manager.isLoaded(UI_ATLAS) ? manager.get(UI_ATLAS, TextureAtlas.class) : null;
    }

    public TextureAtlas getCardsAtlas() {
        return manager.isLoaded(CARDS_ATLAS) ? manager.get(CARDS_ATLAS, TextureAtlas.class) : null;
    }

    public TextureAtlas getToolsAtlas() {
        return manager.isLoaded(TOOLS_ATLAS) ? manager.get(TOOLS_ATLAS, TextureAtlas.class) : null;
    }

    public void dispose() {
        manager.dispose();
    }
}
