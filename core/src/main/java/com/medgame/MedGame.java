package com.medgame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.medgame.collection.AbilityUnlockSystem;
import com.medgame.collection.CardCollection;
import com.medgame.data.AnatomyDatabase;
import com.medgame.data.CaseDatabase;
import com.medgame.network.NetworkManager;
import com.medgame.screen.LoadingScreen;
import com.medgame.util.Assets;
import com.medgame.util.Constants;
import com.medgame.util.SaveManager;

public class MedGame extends Game {

    public static final String TAG = "MedGame";

    private static MedGame instance;

    public Assets assets;
    public AnatomyDatabase anatomyDatabase;
    public CaseDatabase caseDatabase;
    public SaveManager saveManager;
    public NetworkManager networkManager;
    public CardCollection collection;
    public AbilityUnlockSystem abilitySystem;

    public static MedGame getInstance() { return instance; }

    @Override
    public void create() {
        instance = this;

        assets       = new Assets();
        saveManager  = new SaveManager();
        networkManager = new NetworkManager();
        anatomyDatabase = new AnatomyDatabase();
        caseDatabase    = new CaseDatabase();

        // Load or create collection from save
        collection = saveManager.getObject(Constants.PREFS_COLLECTION,
                                           CardCollection.class, new CardCollection());
        abilitySystem = new AbilityUnlockSystem(collection);

        setScreen(new LoadingScreen(this));
    }

    public void onAssetsLoaded() {
        anatomyDatabase.initialize(assets);
        caseDatabase.initialize(assets);
        Gdx.app.log(TAG, "Loaded " + anatomyDatabase.getStructureCount()
                + " structures, " + caseDatabase.getCaseCount() + " cases");
        Gdx.app.log(TAG, "Collection: " + collection.getDiscoveredCount() + " structures discovered");
    }

    /** Persists the current collection state. */
    public void saveCollection() {
        saveManager.putObject(Constants.PREFS_COLLECTION, collection);
    }

    @Override
    public void render() { super.render(); }

    @Override
    public void dispose() {
        super.dispose();
        saveCollection();
        assets.dispose();
        if (networkManager != null) networkManager.stopAll();
    }
}
