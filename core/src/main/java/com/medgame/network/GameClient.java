package com.medgame.network;

import com.badlogic.gdx.Gdx;
import com.medgame.network.packet.*;
import com.medgame.util.Constants;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client side: connects to a GameServer host, sends PlayerActionPackets,
 * receives SyncTickPackets and other server events.
 * All network I/O runs on a background thread; received packets are queued
 * for consumption on the LibGDX main thread via pollReceived().
 */
public class GameClient {

    private static final String TAG = "GameClient";

    private Socket socket;
    private PrintWriter out;
    private final PacketSerializer serializer = new PacketSerializer();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ConcurrentLinkedQueue<GamePacket> inbound = new ConcurrentLinkedQueue<>();

    private int localSlot = -1;
    private boolean connected = false;
    private int actionSequence = 0;

    private ClientListener listener;

    public interface ClientListener {
        void onConnected(int assignedSlot);
        void onDisconnected(String reason);
        void onLobbyState(LobbyStatePacket pkt);
        void onGameStart(GameStartPacket pkt);
        void onSyncTick(SyncTickPacket pkt);
        void onStructureDiscovered(StructureDiscoveredPacket pkt);
        void onCaseComplete(CaseCompletePacket pkt);
        void onPhaseChange(PhaseChangePacket pkt);
    }

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port, String playerName, String playerId) {
        executor.submit(() -> {
            try {
                socket = new Socket(host, port);
                socket.setTcpNoDelay(true);
                out = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), "UTF-8"), true);
                connected = true;
                Gdx.app.log(TAG, "Connected to " + host + ":" + port);

                // Send login
                LoginPacket login = new LoginPacket(playerName, playerId, Constants.PROTOCOL_VERSION);
                send(login);

                // Read loop
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    GamePacket pkt = serializer.deserialize(line);
                    if (pkt != null) inbound.add(pkt);
                }
            } catch (IOException e) {
                Gdx.app.log(TAG, "Disconnected: " + e.getMessage());
            } finally {
                connected = false;
                inbound.add(new DisconnectPacket(-1, "connection lost"));
            }
        });
    }

    /** Must be called each frame from the LibGDX main thread to dispatch received packets. */
    public void pollReceived() {
        GamePacket pkt;
        while ((pkt = inbound.poll()) != null) {
            dispatch(pkt);
        }
    }

    private void dispatch(GamePacket pkt) {
        if (listener == null) return;
        if (pkt instanceof LoginResponsePacket) {
            LoginResponsePacket lrp = (LoginResponsePacket) pkt;
            if (lrp.accepted) {
                localSlot = lrp.assignedSlot;
                listener.onConnected(localSlot);
            } else {
                listener.onDisconnected(lrp.rejectionReason);
            }
        } else if (pkt instanceof LobbyStatePacket) {
            listener.onLobbyState((LobbyStatePacket) pkt);
        } else if (pkt instanceof GameStartPacket) {
            listener.onGameStart((GameStartPacket) pkt);
        } else if (pkt instanceof SyncTickPacket) {
            listener.onSyncTick((SyncTickPacket) pkt);
        } else if (pkt instanceof StructureDiscoveredPacket) {
            listener.onStructureDiscovered((StructureDiscoveredPacket) pkt);
        } else if (pkt instanceof CaseCompletePacket) {
            listener.onCaseComplete((CaseCompletePacket) pkt);
        } else if (pkt instanceof PhaseChangePacket) {
            listener.onPhaseChange((PhaseChangePacket) pkt);
        } else if (pkt instanceof DisconnectPacket) {
            listener.onDisconnected(((DisconnectPacket) pkt).reason);
        }
    }

    public void sendAction(String actionType, String targetStructure,
                           String toolUsed, float posX, float posY, float precision) {
        PlayerActionPacket pkt = new PlayerActionPacket();
        pkt.playerId = localSlot;
        pkt.actionType = actionType;
        pkt.targetStructure = targetStructure;
        pkt.toolUsed = toolUsed;
        pkt.posX = posX;
        pkt.posY = posY;
        pkt.precision = precision;
        pkt.clientTimestamp = System.currentTimeMillis();
        pkt.sequenceNumber = ++actionSequence;
        send(pkt);
    }

    public void sendRoleAssign(String roleName) {
        send(new RoleAssignPacket(localSlot, roleName));
    }

    public void sendCaseSelect(String caseId) {
        send(new CaseSelectPacket(caseId));
    }

    private void send(GamePacket packet) {
        if (out != null && connected) {
            out.println(serializer.serialize(packet));
        }
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        executor.shutdownNow();
    }

    public boolean isConnected() { return connected; }
    public int getLocalSlot()    { return localSlot; }
}
