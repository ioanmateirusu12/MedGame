package com.medgame.network.packet;

public class PhaseChangePacket implements GamePacket {
    public String previousPhase;
    public String newPhase;
    public String displayMessage;

    public PhaseChangePacket() {}
    @Override public String getType() { return "PHASE_CHANGE"; }
}
