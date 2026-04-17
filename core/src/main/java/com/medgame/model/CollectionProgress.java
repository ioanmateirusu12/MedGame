package com.medgame.model;

import java.util.HashSet;
import java.util.Set;

public class CollectionProgress {

    public Set<String> discoveredIds = new HashSet<>();

    public CollectionProgress() {}

    public boolean isDiscovered(String structureId) {
        return discoveredIds.contains(structureId);
    }

    public void discover(String structureId) {
        discoveredIds.add(structureId);
    }

    public int getCount() {
        return discoveredIds.size();
    }
}
