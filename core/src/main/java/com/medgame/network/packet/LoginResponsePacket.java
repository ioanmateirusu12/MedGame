package com.medgame.network.packet;

public class LoginResponsePacket implements GamePacket {
    public boolean accepted;
    public int assignedSlot;
    public String rejectionReason;

    public LoginResponsePacket() {}
    public LoginResponsePacket(boolean accepted, int assignedSlot, String rejectionReason) {
        this.accepted = accepted;
        this.assignedSlot = assignedSlot;
        this.rejectionReason = rejectionReason;
    }
    @Override public String getType() { return "LOGIN_RESPONSE"; }
}
