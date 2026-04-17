package com.medgame.network;

import com.badlogic.gdx.Gdx;
import com.medgame.model.ClinicalCase;
import com.medgame.model.SpecialistRole;
import com.medgame.network.packet.*;
import com.medgame.surgery.OperationDirector;
import com.medgame.surgery.SurgicalAction;
import com.medgame.surgery.Tool;
import com.medgame.util.Constants;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Authoritative game server. One of the 4 players acts as host.
 * Listens on TCP for player connections and UDP for LAN discovery.
 */
public class GameServer {

    private static final String TAG = "GameServer";

    private ServerSocket serverSocket;
    private DatagramSocket udpSocket;
    private final PacketSerializer serializer = new PacketSerializer();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Connected clients indexed by slot (0-3)
    private final ClientHandler[] slots = new ClientHandler[Constants.MAX_PLAYERS];
    private final String[] playerNames   = new String[Constants.MAX_PLAYERS];
    private final String[] playerRoles   = new String[Constants.MAX_PLAYERS];
    private final boolean[] slotOccupied = new boolean[Constants.MAX_PLAYERS];

    private String selectedCaseId;
    private boolean gameRunning = false;

    // Operation state (authoritative)
    private OperationDirector director;
    private ClinicalCase currentCase;
    private float syncTimer = 0f;
    private int tick = 0;
    private final float[] toolPosX = new float[4];
    private final float[] toolPosY = new float[4];

    // Callbacks to main thread
    private ServerListener listener;

    public interface ServerListener {
        void onPlayerJoined(int slot, String name);
        void onPlayerLeft(int slot);
        void onLobbyChanged(LobbyStatePacket state);
        void onGameStarted(String caseId);
    }

    public void setListener(ServerListener listener) {
        this.listener = listener;
    }

    public void start() throws IOException {
        // Slot 0 = host (local player)
        slots[0] = null; // host has no socket handler
        slotOccupied[0] = true;

        serverSocket = new ServerSocket(Constants.SERVER_TCP_PORT);
        serverSocket.setSoTimeout(0);
        Gdx.app.log(TAG, "Server listening on port " + Constants.SERVER_TCP_PORT);

        // Accept loop
        executor.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    int slot = findFreeSlot();
                    if (slot == -1) {
                        socket.close();
                        continue;
                    }
                    ClientHandler handler = new ClientHandler(socket, slot);
                    slots[slot] = handler;
                    executor.submit(handler);
                } catch (IOException e) {
                    if (!serverSocket.isClosed())
                        Gdx.app.error(TAG, "Accept error", e);
                }
            }
        });

        // UDP LAN discovery
        startLanBeacon();
    }

    private void startLanBeacon() {
        executor.submit(() -> {
            try {
                udpSocket = new DatagramSocket(Constants.LAN_DISCOVERY_PORT);
                byte[] buf = new byte[256];
                while (!udpSocket.isClosed()) {
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(dp);
                    String msg = new String(dp.getData(), 0, dp.getLength()).trim();
                    if (msg.startsWith("MEDGAME_DISCOVER")) {
                        // Respond with server info
                        int occupied = 0;
                for (boolean b : slotOccupied) if (b) occupied++;
                        String response = "MEDGAME_SERVER|" + Constants.PROTOCOL_VERSION
                            + "|" + occupied + "|" + Constants.MAX_PLAYERS
                            + "|" + (selectedCaseId != null ? selectedCaseId : "none")
                            + "|" + Constants.SERVER_TCP_PORT;
                        byte[] respBytes = response.getBytes();
                        DatagramPacket respPkt = new DatagramPacket(
                            respBytes, respBytes.length,
                            dp.getAddress(), dp.getPort());
                        udpSocket.send(respPkt);
                    }
                }
            } catch (IOException e) {
                if (udpSocket != null && !udpSocket.isClosed())
                    Gdx.app.error(TAG, "UDP beacon error", e);
            }
        });
    }

    /** Called each frame by the host to advance authoritative game state. */
    public void update(float delta) {
        if (!gameRunning || director == null) return;
        syncTimer += delta;
        if (syncTimer >= Constants.SYNC_RATE) {
            syncTimer = 0f;
            broadcastSyncTick();
        }
    }

    /** Host calls this to set their own tool position. */
    public void setHostToolPosition(float x, float y) {
        toolPosX[0] = x;
        toolPosY[0] = y;
    }

    /** Host submits a surgical action directly. */
    public boolean submitAction(com.medgame.surgery.SurgicalAction action,
                                String structureId, Tool tool, float precision) {
        if (director == null) return false;
        return director.submitAction(action, structureId, tool, precision);
    }

    public void selectCase(String caseId) {
        this.selectedCaseId = caseId;
        broadcastLobbyState();
    }

    public void startGame(ClinicalCase clinicalCase, OperationDirector dir) {
        this.currentCase = clinicalCase;
        this.director = dir;
        this.gameRunning = true;

        GameStartPacket pkt = new GameStartPacket();
        pkt.caseId = clinicalCase.id;
        pkt.serverTimestamp = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) pkt.assignedRoles[i] = playerRoles[i];
        broadcast(pkt, -1);
    }

    private void broadcastSyncTick() {
        if (director == null) return;
        SyncTickPacket pkt = new SyncTickPacket();
        pkt.tick = ++tick;
        pkt.serverTimestamp = System.currentTimeMillis();
        pkt.heartRate = director.heartRate;
        pkt.bloodPressure = director.bloodPressure;
        pkt.oxygenSat = director.oxygenSat;
        pkt.currentPhaseId = director.getCurrentPhase().name();
        pkt.score = director.getScore();
        pkt.toolPosX = Arrays.copyOf(toolPosX, 4);
        pkt.toolPosY = Arrays.copyOf(toolPosY, 4);
        StringBuilder sb = new StringBuilder();
        for (String id : director.getDiscoveredStructures()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        pkt.revealedStructures = sb.toString();
        broadcast(pkt, -1);
    }

    public void broadcastLobbyState() {
        LobbyStatePacket pkt = new LobbyStatePacket();
        pkt.selectedCaseId = selectedCaseId;
        pkt.maxPlayers = Constants.MAX_PLAYERS;
        for (int i = 0; i < 4; i++) {
            pkt.playerNames[i]   = playerNames[i];
            pkt.playerRoles[i]   = playerRoles[i];
            pkt.slotOccupied[i]  = slotOccupied[i];
        }
        broadcast(pkt, -1);
        if (listener != null) listener.onLobbyChanged(pkt);
    }

    private void broadcast(GamePacket packet, int excludeSlot) {
        String data = serializer.serialize(packet);
        for (int i = 1; i < 4; i++) { // skip slot 0 (host, no socket)
            if (i == excludeSlot) continue;
            if (slots[i] != null) slots[i].send(data);
        }
    }

    private void send(int slot, GamePacket packet) {
        if (slot == 0) return; // host handles locally
        if (slots[slot] != null) slots[slot].send(serializer.serialize(packet));
    }

    private int findFreeSlot() {
        for (int i = 1; i < 4; i++) {
            if (!slotOccupied[i]) return i;
        }
        return -1;
    }

    public void stop() {
        try {
            if (serverSocket != null) serverSocket.close();
            if (udpSocket != null) udpSocket.close();
        } catch (IOException ignored) {}
        executor.shutdownNow();
    }

    // ---- Inner class: per-client handler running on its own thread ----

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final int slot;
        private PrintWriter out;

        ClientHandler(Socket socket, int slot) throws IOException {
            this.socket = socket;
            this.slot = slot;
            this.out = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), "UTF-8"), true);
            slotOccupied[slot] = true;
        }

        void send(String data) {
            if (out != null) out.println(data);
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    GamePacket packet = serializer.deserialize(line);
                    if (packet != null) handlePacket(packet);
                }
            } catch (IOException e) {
                Gdx.app.log(TAG, "Client slot " + slot + " disconnected");
            } finally {
                slotOccupied[slot] = false;
                slots[slot] = null;
                playerNames[slot] = null;
                playerRoles[slot] = null;
                broadcast(new DisconnectPacket(slot, "disconnected"), -1);
                broadcastLobbyState();
                if (listener != null) listener.onPlayerLeft(slot);
            }
        }

        private void handlePacket(GamePacket packet) {
            if (packet instanceof LoginPacket) {
                LoginPacket lp = (LoginPacket) packet;
                if (lp.protocolVersion != Constants.PROTOCOL_VERSION) {
                    send(serializer.serialize(
                        new LoginResponsePacket(false, -1, "Protocol version mismatch")));
                    return;
                }
                playerNames[slot] = lp.playerName;
                playerRoles[slot] = SpecialistRole.ASSISTANT.name();
                send(serializer.serialize(new LoginResponsePacket(true, slot, null)));
                broadcastLobbyState();
                if (listener != null) listener.onPlayerJoined(slot, lp.playerName);

            } else if (packet instanceof RoleAssignPacket) {
                RoleAssignPacket rp = (RoleAssignPacket) packet;
                playerRoles[slot] = rp.roleName;
                broadcastLobbyState();

            } else if (packet instanceof CaseSelectPacket) {
                CaseSelectPacket cp = (CaseSelectPacket) packet;
                selectedCaseId = cp.caseId;
                broadcastLobbyState();

            } else if (packet instanceof PlayerActionPacket) {
                PlayerActionPacket ap = (PlayerActionPacket) packet;
                toolPosX[slot] = ap.posX;
                toolPosY[slot] = ap.posY;
                if (director != null && director.isActive()) {
                    try {
                        SurgicalAction action = SurgicalAction.valueOf(ap.actionType);
                        Tool tool = Tool.valueOf(ap.toolUsed);
                        boolean ok = director.submitAction(action, ap.targetStructure,
                                                           tool, ap.precision);
                        if (ok) broadcastSyncTick();
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    // Getters for lobby UI
    public String[] getPlayerNames()   { return playerNames; }
    public String[] getPlayerRoles()   { return playerRoles; }
    public boolean[] getSlotOccupied() { return slotOccupied; }
    public String getSelectedCaseId()  { return selectedCaseId; }
    public boolean isGameRunning()     { return gameRunning; }
}
