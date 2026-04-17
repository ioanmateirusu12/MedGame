package com.medgame.model;

public class PlayerProfile {

    public String playerId;
    public String playerName;
    public int totalScore;
    public int casesCompleted;
    public CollectionProgress collection;

    public PlayerProfile() {
        collection = new CollectionProgress();
    }
}
