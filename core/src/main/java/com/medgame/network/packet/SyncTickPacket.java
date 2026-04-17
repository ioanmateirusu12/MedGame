package com.medgame.network.packet;

public class SyncTickPacket implements GamePacket {
    public int    tick;
    public long   serverTimestamp;
    // Tool positions per slot
    public float[] toolPosX     = new float[4];
    public float[] toolPosY     = new float[4];
    public boolean[] toolActive = new boolean[4];
    // Vitals
    public float heartRate;
    public float bloodPressure;
    public float oxygenSat;
    // Operation state
    public String currentPhaseId;
    public int    currentStepIndex;
    public int    score;
    // Revealed structures (comma-separated ids)
    public String revealedStructures;

    public SyncTickPacket() {}
    @Override public String getType() { return "SYNC_TICK"; }
}
