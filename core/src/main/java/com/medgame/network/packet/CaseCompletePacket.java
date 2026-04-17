package com.medgame.network.packet;

public class CaseCompletePacket implements GamePacket {
    public String   caseId;
    public boolean  success;
    public int      score;
    public int      perfectBonus;
    public String[] discoveredStructureIds;
    public long     durationMs;

    public CaseCompletePacket() {}
    @Override public String getType() { return "CASE_COMPLETE"; }
}
