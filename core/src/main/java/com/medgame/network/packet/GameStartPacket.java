package com.medgame.network.packet;

public class GameStartPacket implements GamePacket {
    public String caseId;
    public String[] assignedRoles = new String[4]; // role per slot
    public long serverTimestamp;

    public GameStartPacket() {}
    @Override public String getType() { return "GAME_START"; }
}
