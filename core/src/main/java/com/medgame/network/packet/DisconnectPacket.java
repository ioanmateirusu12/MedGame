package com.medgame.network.packet;

public class DisconnectPacket implements GamePacket {
    public int    slot;
    public String reason;

    public DisconnectPacket() {}
    public DisconnectPacket(int slot, String reason) {
        this.slot = slot;
        this.reason = reason;
    }
    @Override public String getType() { return "DISCONNECT"; }
}
