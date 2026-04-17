package com.medgame.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.medgame.model.AnatomicalStructure;
import com.medgame.util.Assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnatomyDatabase {

    private static final String[] REGION_FILES = {
        "head_neck.json", "thorax.json", "abdomen.json",
        "pelvis.json", "upper_limb.json", "lower_limb.json"
    };

    private final Map<String, AnatomicalStructure> byId = new HashMap<>();
    private final Map<String, List<AnatomicalStructure>> byRegion = new HashMap<>();
    private final Map<String, List<AnatomicalStructure>> bySystem = new HashMap<>();
    private int regionsLoaded = 0;

    // Wrapper to deserialize the { "region": "...", "structures": [...] } envelope
    private static class RegionFile {
        public String region;
        public List<AnatomicalStructure> structures;
        public RegionFile() { structures = new ArrayList<>(); }
    }

    public void initialize(Assets assets) {
        Json json = new Json();
        for (String fileName : REGION_FILES) {
            String path = Assets.ANATOMY_DIR + fileName;
            FileHandle handle = Gdx.files.internal(path);
            if (!handle.exists()) continue;
            try {
                RegionFile rf = json.fromJson(RegionFile.class, handle);
                if (rf == null || rf.structures == null) continue;
                for (AnatomicalStructure s : rf.structures) {
                    byId.put(s.id, s);
                    byRegion.computeIfAbsent(s.region, k -> new ArrayList<>()).add(s);
                    if (s.system != null) {
                        bySystem.computeIfAbsent(s.system, k -> new ArrayList<>()).add(s);
                    }
                }
                regionsLoaded++;
            } catch (Exception e) {
                Gdx.app.error("AnatomyDatabase", "Error loading " + path, e);
            }
        }
    }

    public AnatomicalStructure getById(String id) {
        return byId.get(id);
    }

    public List<AnatomicalStructure> getByRegion(String region) {
        return byRegion.getOrDefault(region, new ArrayList<>());
    }

    public List<AnatomicalStructure> getBySystem(String system) {
        return bySystem.getOrDefault(system, new ArrayList<>());
    }

    public Map<String, AnatomicalStructure> getAll() {
        return byId;
    }

    public int getStructureCount() {
        return byId.size();
    }

    public int getRegionCount() {
        return regionsLoaded;
    }
}
