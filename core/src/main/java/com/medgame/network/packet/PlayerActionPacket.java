package com.medgame.network.packet;

public class PlayerActionPacket implements GamePacket {
    public int    playerId;
    public String actionType;
    public String targetStructure;
    public String toolUsed;
    public float  posX;
    public float  posY;
    public float  precision;
    public long   clientTimestamp;
    public int    sequenceNumber;

    public PlayerActionPacket() {}
    @Override public String getType() { return "PLAYER_ACTION"; }
}
