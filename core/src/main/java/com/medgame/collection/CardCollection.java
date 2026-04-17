package com.medgame.collection;

import com.medgame.model.AnatomicalStructure;

import java.util.*;

/**
 * Persistent collection of all discovered anatomical structures.
 * Serialized to JSON via SaveManager.
 */
public class CardCollection {

    // Serialized
    public Set<String> discoveredIds    = new LinkedHashSet<>();
    public Set<String> unlockedAbilities = new LinkedHashSet<>();

    // Transient listeners
    private final List<DiscoveryListener> listeners = new ArrayList<>();

    public interface DiscoveryListener {
        void onDiscovered(String structureId, SurgicalAbility unlockedAbility);
    }

    public CardCollection() {}

    public void addListener(DiscoveryListener l) { listeners.add(l); }

    public boolean discover(String structureId) {
        if (discoveredIds.contains(structureId)) return false;
        discoveredIds.add(structureId);

        SurgicalAbility ability = SurgicalAbility.forStructure(structureId);
        if (ability != null) unlockedAbilities.add(ability.name());

        for (DiscoveryListener l : listeners) l.onDiscovered(structureId, ability);
        return true;
    }

    public boolean isDiscovered(String id) { return discoveredIds.contains(id); }

    public boolean hasAbility(SurgicalAbility ability) {
        return unlockedAbilities.contains(ability.name());
    }

    public int getDiscoveredCount() { return discoveredIds.size(); }

    public Set<String> getDiscoveredIds() { return Collections.unmodifiableSet(discoveredIds); }

    /** Returns discovered structures filtered by anatomical region. */
    public List<String> getDiscoveredByRegion(String region,
                                               Map<String, AnatomicalStructure> allStructures) {
        List<String> result = new ArrayList<>();
        for (String id : discoveredIds) {
            AnatomicalStructure s = allStructures.get(id);
            if (s != null && region.equals(s.region)) result.add(id);
        }
        return result;
    }

    /** Per-region completion: 0.0 – 1.0 */
    public float getRegionProgress(String region, Map<String, AnatomicalStructure> allStructures) {
        int total = 0, found = 0;
        for (AnatomicalStructure s : allStructures.values()) {
            if (region.equals(s.region)) {
                total++;
                if (discoveredIds.contains(s.id)) found++;
            }
        }
        return total == 0 ? 0f : (float) found / total;
    }
}
