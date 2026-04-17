package com.medgame.network.packet;

public class StructureDiscoveredPacket implements GamePacket {
    public String structureId;
    public int    discoveredBySlot;
    public long   timestamp;

    public StructureDiscoveredPacket() {}
    public StructureDiscoveredPacket(String id, int slot) {
        this.structureId = id;
        this.discoveredBySlot = slot;
        this.timestamp = System.currentTimeMillis();
    }
    @Override public String getType() { return "STRUCTURE_DISCOVERED"; }
}
