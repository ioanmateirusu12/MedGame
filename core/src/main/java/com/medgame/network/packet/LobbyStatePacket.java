package com.medgame.network.packet;

public class LobbyStatePacket implements GamePacket {
    public String[] playerNames  = new String[4];
    public String[] playerRoles  = new String[4];
    public boolean[] slotReady   = new boolean[4];
    public boolean[] slotOccupied= new boolean[4];
    public String selectedCaseId;
    public int maxPlayers;

    public LobbyStatePacket() {}
    @Override public String getType() { return "LOBBY_STATE"; }
}
