package com.medgame.network.packet;

public class RoleAssignPacket implements GamePacket {
    public int slot;
    public String roleName;

    public RoleAssignPacket() {}
    public RoleAssignPacket(int slot, String roleName) {
        this.slot = slot;
        this.roleName = roleName;
    }
    @Override public String getType() { return "ROLE_ASSIGN"; }
}
