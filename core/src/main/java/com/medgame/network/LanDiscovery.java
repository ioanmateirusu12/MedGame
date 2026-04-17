package com.medgame.network;

import com.badlogic.gdx.Gdx;
import com.medgame.util.Constants;

import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Broadcasts a UDP discovery packet and collects responses from GameServers on LAN.
 * Run on a background thread; results handed back via callback on the calling thread.
 */
public class LanDiscovery {

    private static final String TAG = "LanDiscovery";

    public static class ServerInfo {
        public String host;
        public int tcpPort;
        public int playerCount;
        public int maxPlayers;
        public String caseId;
        public int protocolVersion;

        @Override public String toString() {
            return host + "  [" + playerCount + "/" + maxPlayers + "]  " + caseId;
        }
    }

    public interface DiscoveryCallback {
        void onResult(List<ServerInfo> servers);
    }

    public void discover(DiscoveryCallback callback) {
        Thread t = new Thread(() -> {
            List<ServerInfo> found = new ArrayList<>();
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout(2000);

                byte[] req = "MEDGAME_DISCOVER".getBytes();
                DatagramPacket sendPkt = new DatagramPacket(
                    req, req.length,
                    InetAddress.getByName("255.255.255.255"),
                    Constants.LAN_DISCOVERY_PORT);
                socket.send(sendPkt);

                long deadline = System.currentTimeMillis() + (long)(Constants.LAN_DISCOVERY_TIMEOUT * 1000);
                byte[] buf = new byte[512];
                while (System.currentTimeMillis() < deadline) {
                    try {
                        DatagramPacket recvPkt = new DatagramPacket(buf, buf.length);
                        socket.receive(recvPkt);
                        String resp = new String(recvPkt.getData(), 0, recvPkt.getLength());
                        ServerInfo info = parseResponse(resp, recvPkt.getAddress().getHostAddress());
                        if (info != null) found.add(info);
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }
            } catch (Exception e) {
                Gdx.app.error(TAG, "Discovery error", e);
            }
            callback.onResult(found);
        }, "LanDiscovery");
        t.setDaemon(true);
        t.start();
    }

    // Format: MEDGAME_SERVER|protocolVer|playerCount|maxPlayers|caseId|tcpPort
    private ServerInfo parseResponse(String raw, String host) {
        try {
            if (!raw.startsWith("MEDGAME_SERVER|")) return null;
            String[] parts = raw.split("\\|");
            ServerInfo info = new ServerInfo();
            info.host = host;
            info.protocolVersion = Integer.parseInt(parts[1]);
            info.playerCount = Integer.parseInt(parts[2]);
            info.maxPlayers = Integer.parseInt(parts[3]);
            info.caseId = parts[4];
            info.tcpPort = Integer.parseInt(parts[5]);
            return info;
        } catch (Exception e) {
            return null;
        }
    }
}
