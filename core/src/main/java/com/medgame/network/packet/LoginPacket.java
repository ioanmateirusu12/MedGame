package com.medgame.network.packet;

public class LoginPacket implements GamePacket {
    public String playerName;
    public String playerId;
    public int protocolVersion;

    public LoginPacket() {}
    public LoginPacket(String playerName, String playerId, int protocolVersion) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.protocolVersion = protocolVersion;
    }
    @Override public String getType() { return "LOGIN"; }
}
