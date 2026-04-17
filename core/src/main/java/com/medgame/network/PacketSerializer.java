package com.medgame.network;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;
import com.medgame.network.packet.*;

/**
 * Serializes GamePackets to/from JSON strings for transmission over TCP.
 * Wire format: {"type":"LOGIN","data":{...}}\n
 */
public class PacketSerializer {

    private final Json json = new Json();
    private final JsonReader reader = new JsonReader();

    public String serialize(GamePacket packet) {
        String type = packet.getType();
        String data = json.toJson(packet);
        return "{\"type\":\"" + type + "\",\"data\":" + data + "}\n";
    }

    public GamePacket deserialize(String raw) {
        try {
            JsonValue root = reader.parse(raw.trim());
            String type = root.getString("type");
            String dataStr = root.get("data").toJson(com.badlogic.gdx.utils.JsonWriter.OutputType.json);
            switch (type) {
                case "LOGIN":                return json.fromJson(LoginPacket.class,             dataStr);
                case "LOGIN_RESPONSE":       return json.fromJson(LoginResponsePacket.class,     dataStr);
                case "LOBBY_STATE":          return json.fromJson(LobbyStatePacket.class,        dataStr);
                case "ROLE_ASSIGN":          return json.fromJson(RoleAssignPacket.class,        dataStr);
                case "CASE_SELECT":          return json.fromJson(CaseSelectPacket.class,        dataStr);
                case "GAME_START":           return json.fromJson(GameStartPacket.class,         dataStr);
                case "PLAYER_ACTION":        return json.fromJson(PlayerActionPacket.class,      dataStr);
                case "SYNC_TICK":            return json.fromJson(SyncTickPacket.class,          dataStr);
                case "STRUCTURE_DISCOVERED": return json.fromJson(StructureDiscoveredPacket.class, dataStr);
                case "CASE_COMPLETE":        return json.fromJson(CaseCompletePacket.class,      dataStr);
                case "VITAL_UPDATE":         return json.fromJson(VitalUpdatePacket.class,       dataStr);
                case "PHASE_CHANGE":         return json.fromJson(PhaseChangePacket.class,       dataStr);
                case "DISCONNECT":           return json.fromJson(DisconnectPacket.class,        dataStr);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
