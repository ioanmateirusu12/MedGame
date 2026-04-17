package com.medgame.network.packet;

public class CaseSelectPacket implements GamePacket {
    public String caseId;

    public CaseSelectPacket() {}
    public CaseSelectPacket(String caseId) { this.caseId = caseId; }
    @Override public String getType() { return "CASE_SELECT"; }
}
