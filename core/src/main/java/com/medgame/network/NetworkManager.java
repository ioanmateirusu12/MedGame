package com.medgame.network;

import com.medgame.model.ClinicalCase;
import com.medgame.surgery.OperationDirector;

import java.io.IOException;

/**
 * Facade: a session is either a server-host or a client.
 * Only one mode is active at a time.
 */
public class NetworkManager {

    public enum Mode { NONE, SERVER, CLIENT }

    private Mode mode = Mode.NONE;
    private GameServer server;
    private GameClient client;

    // ---- Server operations ----

    public void startServer() throws IOException {
        stopAll();
        server = new GameServer();
        server.start();
        mode = Mode.SERVER;
    }

    public void startGame(ClinicalCase clinicalCase, OperationDirector director) {
        if (mode == Mode.SERVER && server != null) {
            server.startGame(clinicalCase, director);
        }
    }

    public void selectCase(String caseId) {
        if (mode == Mode.SERVER && server != null) server.selectCase(caseId);
    }

    public void setServerListener(GameServer.ServerListener l) {
        if (server != null) server.setListener(l);
    }

    public GameServer getServer() { return server; }

    // ---- Client operations ----

    public void connectAsClient(String host, int port, String playerName, String playerId) {
        stopAll();
        client = new GameClient();
        client.connect(host, port, playerName, playerId);
        mode = Mode.CLIENT;
    }

    public void setClientListener(GameClient.ClientListener l) {
        if (client != null) client.setListener(l);
    }

    public GameClient getClient() { return client; }

    // ---- Common ----

    public void update(float delta) {
        if (mode == Mode.SERVER && server != null) server.update(delta);
        if (mode == Mode.CLIENT && client != null) client.pollReceived();
    }

    public void stopAll() {
        if (server != null) { server.stop(); server = null; }
        if (client != null) { client.disconnect(); client = null; }
        mode = Mode.NONE;
    }

    public Mode getMode() { return mode; }
    public boolean isHost() { return mode == Mode.SERVER; }
    public boolean isClient() { return mode == Mode.CLIENT; }
}
