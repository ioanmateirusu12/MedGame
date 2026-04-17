package com.medgame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.medgame.data.AnatomyDatabase;
import com.medgame.data.CaseDatabase;
import com.medgame.screen.LoadingScreen;
import com.medgame.util.Assets;
import com.medgame.util.SaveManager;

public class MedGame extends Game {

    public static final String TAG = "MedGame";

    private static MedGame instance;

    public Assets assets;
    public AnatomyDatabase anatomyDatabase;
    public CaseDatabase caseDatabase;
    public SaveManager saveManager;

    public static MedGame getInstance() {
        return instance;
    }

    @Override
    public void create() {
        instance = this;

        assets = new Assets();
        saveManager = new SaveManager();
        anatomyDatabase = new AnatomyDatabase();
        caseDatabase = new CaseDatabase();

        setScreen(new LoadingScreen(this));
    }

    public void onAssetsLoaded() {
        anatomyDatabase.initialize(assets);
        caseDatabase.initialize(assets);
        Gdx.app.log(TAG, "Loaded " + anatomyDatabase.getStructureCount()
                + " anatomical structures across " + anatomyDatabase.getRegionCount() + " regions");
        Gdx.app.log(TAG, "Loaded " + caseDatabase.getCaseCount() + " clinical cases");
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        assets.dispose();
    }
}
