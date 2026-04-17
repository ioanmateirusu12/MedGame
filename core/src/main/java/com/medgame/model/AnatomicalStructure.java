package com.medgame.model;

import java.util.List;

public class AnatomicalStructure {

    public String id;
    public String name;
    public String latinName;
    public String region;
    public String subRegion;
    public String system;
    public String rarity;          // common | uncommon | rare | epic
    public String function;
    public String location;
    public String bloodSupply;
    public String innervation;
    public List<String> branches;
    public List<String> relatedStructures;
    public String clinicalNotes;
    public String unlockAbility;
    public String netterReference;
    public String grayReference;
    public String pixelArtSprite;
    public int cardTier;           // 1-3

    // Required by LibGDX Json deserializer
    public AnatomicalStructure() {}

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
