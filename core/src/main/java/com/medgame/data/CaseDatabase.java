package com.medgame.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.medgame.model.ClinicalCase;
import com.medgame.util.Assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaseDatabase {

    private final Map<String, ClinicalCase> byId = new HashMap<>();

    public void initialize(Assets assets) {
        Json json = new Json();
        FileHandle casesDir = Gdx.files.internal(Assets.CASES_DIR);
        if (!casesDir.exists()) return;

        for (FileHandle file : casesDir.list(".json")) {
            try {
                ClinicalCase c = json.fromJson(ClinicalCase.class, file);
                if (c != null && c.id != null) {
                    byId.put(c.id, c);
                }
            } catch (Exception e) {
                Gdx.app.error("CaseDatabase", "Error loading " + file.name(), e);
            }
        }
    }

    public ClinicalCase getById(String id) {
        return byId.get(id);
    }

    public List<ClinicalCase> getByDifficulty(int maxDifficulty) {
        List<ClinicalCase> result = new ArrayList<>();
        for (ClinicalCase c : byId.values()) {
            if (c.difficulty <= maxDifficulty) result.add(c);
        }
        return result;
    }

    public List<ClinicalCase> getAll() {
        return new ArrayList<>(byId.values());
    }

    public int getCaseCount() {
        return byId.size();
    }
}
