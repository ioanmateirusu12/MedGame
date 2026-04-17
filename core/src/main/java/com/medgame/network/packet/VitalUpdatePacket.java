package com.medgame.network.packet;

public class VitalUpdatePacket implements GamePacket {
    public float   heartRate;
    public float   bloodPressure;
    public float   oxygenSat;
    public String  triggerAction;
    public boolean isCritical;

    public VitalUpdatePacket() {}
    @Override public String getType() { return "VITAL_UPDATE"; }
}
