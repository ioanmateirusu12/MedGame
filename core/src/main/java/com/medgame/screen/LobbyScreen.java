package com.medgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.medgame.MedGame;
import com.medgame.model.ClinicalCase;
import com.medgame.model.SpecialistRole;
import com.medgame.network.GameClient;
import com.medgame.network.GameServer;
import com.medgame.network.LanDiscovery;
import com.medgame.network.NetworkManager;
import com.medgame.network.packet.*;
import com.medgame.surgery.OperationDirector;
import com.medgame.util.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LobbyScreen extends BaseScreen {

    private static final String[] ROLE_NAMES = {
        SpecialistRole.SURGEON.name(),
        SpecialistRole.ASSISTANT.name(),
        SpecialistRole.ANESTHESIOLOGIST.name(),
        SpecialistRole.SCRUB_NURSE.name()
    };

    // UI
    private final Label[] slotLabels = new Label[4];
    private final Label[] roleLabels = new Label[4];
    private Label statusLabel;
    private Label caseLabel;
    private TextField ipField;
    private List<LanDiscovery.ServerInfo> discoveredServers = new ArrayList<>();
    private List<TextButton> serverButtons = new ArrayList<>();

    // State
    private boolean isHost = false;
    private int localSlot = 0;
    private String[] currentNames = new String[4];
    private String[] currentRoles = new String[4];
    private String selectedCaseId = null;
    private boolean gameStarting = false;

    // Host name from save
    private final String hostName;

    public LobbyScreen(MedGame game) {
        super(game);
        hostName = game.saveManager.getPlayerName();
        currentNames[0] = hostName;
        currentRoles[0] = SpecialistRole.SURGEON.name();
    }

    @Override
    protected void buildUI() {
        BitmapFont font = new BitmapFont();
        BitmapFont smallFont = new BitmapFont();
        smallFont.getData().setScale(0.85f);

        Label.LabelStyle h1 = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle h2 = new Label.LabelStyle(smallFont, Color.CYAN);
        Label.LabelStyle dim = new Label.LabelStyle(smallFont, Color.DARK_GRAY);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = smallFont;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = Color.RED;

        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = smallFont;
        tfStyle.fontColor = Color.WHITE;
        tfStyle.cursor = null;
        tfStyle.selection = null;
        tfStyle.background = null;

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(20);

        // Title
        root.add(new Label("LOBBY - CO-OP SURGERY", h1)).colspan(2).padBottom(20).row();

        // Left: player slots
        Table slotsTable = new Table();
        slotsTable.left().top();
        slotsTable.add(new Label("PLAYERS", h1)).colspan(3).padBottom(10).row();

        for (int i = 0; i < 4; i++) {
            final int slot = i;
            Label numLbl = new Label("Slot " + (i + 1), dim);
            slotLabels[i] = new Label("[ empty ]", dim);
            roleLabels[i] = new Label("---", h2);
            slotsTable.add(numLbl).width(60).left();
            slotsTable.add(slotLabels[i]).width(200).left();
            slotsTable.add(roleLabels[i]).width(180).left();

            if (i == 0) {
                // Host role cycle button
                TextButton cycleBtn = new TextButton("[change role]", btnStyle);
                cycleBtn.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        cycleRole(slot);
                    }
                });
                slotsTable.add(cycleBtn).padLeft(10);
            }
            slotsTable.row().padBottom(8);
        }

        // Case selection
        slotsTable.add(new Label("CASE:", h1)).colspan(4).padTop(20).padBottom(8).left().row();
        List<ClinicalCase> cases = game.caseDatabase.getAll();
        cases.sort((a, b) -> Integer.compare(a.difficulty, b.difficulty));
        for (ClinicalCase c : cases) {
            TextButton caseBtn = new TextButton("[" + c.difficulty + "] " + c.displayName, btnStyle);
            caseBtn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    selectCase(c.id);
                }
            });
            slotsTable.add(caseBtn).colspan(4).left().padBottom(4).row();
        }

        caseLabel = new Label("Selected: none", h2);
        slotsTable.add(caseLabel).colspan(4).padTop(6).left().row();

        // Status
        statusLabel = new Label("Choose HOST or JOIN", h2);
        slotsTable.add(statusLabel).colspan(4).padTop(20).left().row();

        root.add(slotsTable).top().left().expandX();

        // Right: actions
        Table actionTable = new Table();
        actionTable.top().right().padLeft(40);

        actionTable.add(new Label("HOST", h1)).padBottom(10).row();

        TextButton hostBtn = new TextButton("HOST GAME", btnStyle);
        hostBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { startHost(); }
        });
        actionTable.add(hostBtn).width(200).padBottom(30).row();

        actionTable.add(new Label("JOIN LAN", h1)).padBottom(10).row();

        TextButton scanBtn = new TextButton("SCAN LAN", btnStyle);
        scanBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { scanLan(); }
        });
        actionTable.add(scanBtn).width(200).padBottom(6).row();

        Table serverListTable = new Table();
        serverListTable.left();
        actionTable.add(serverListTable).colspan(1).padBottom(20).left().row();

        actionTable.add(new Label("JOIN ONLINE", h1)).padBottom(6).row();

        ipField = new TextField("", tfStyle);
        ipField.setMessageText("Enter IP address");
        ipField.setMaxLength(40);
        actionTable.add(ipField).width(200).padBottom(6).row();

        TextButton joinBtn = new TextButton("CONNECT", btnStyle);
        joinBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                joinByIp(ipField.getText().trim());
            }
        });
        actionTable.add(joinBtn).width(200).padBottom(30).row();

        // Start game (host only)
        TextButton startBtn = new TextButton("START GAME", btnStyle);
        startBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { tryStartGame(); }
        });
        actionTable.add(startBtn).width(200).padBottom(10).row();

        // Back
        TextButton backBtn = new TextButton("BACK", btnStyle);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                game.networkManager.stopAll();
                game.setScreen(new MainMenuScreen(game));
            }
        });
        actionTable.add(backBtn).width(200).row();

        root.add(actionTable).top().right();
        stage.addActor(root);
    }

    private void startHost() {
        isHost = true;
        localSlot = 0;
        try {
            game.networkManager.startServer();
            game.networkManager.setServerListener(new GameServer.ServerListener() {
                @Override public void onPlayerJoined(int slot, String name) {
                    Gdx.app.postRunnable(() -> {
                        currentNames[slot] = name;
                        updateSlotUI();
                    });
                }
                @Override public void onPlayerLeft(int slot) {
                    Gdx.app.postRunnable(() -> {
                        currentNames[slot] = null;
                        currentRoles[slot] = null;
                        updateSlotUI();
                    });
                }
                @Override public void onLobbyChanged(LobbyStatePacket state) {
                    Gdx.app.postRunnable(() -> applyLobbyState(state));
                }
                @Override public void onGameStarted(String caseId) {}
            });
            setStatus("Hosting on port " + Constants.SERVER_TCP_PORT + " — waiting for players...");
            updateSlotUI();
        } catch (IOException e) {
            setStatus("Failed to start server: " + e.getMessage());
        }
    }

    private void scanLan() {
        setStatus("Scanning LAN...");
        new LanDiscovery().discover(servers -> Gdx.app.postRunnable(() -> {
            discoveredServers = servers;
            setStatus("Found " + servers.size() + " server(s)");
        }));
    }

    private void joinByIp(String ip) {
        if (ip.isEmpty()) { setStatus("Enter an IP address"); return; }
        isHost = false;
        game.networkManager.connectAsClient(ip, Constants.SERVER_TCP_PORT,
            game.saveManager.getPlayerName(), java.util.UUID.randomUUID().toString());
        game.networkManager.setClientListener(buildClientListener());
        setStatus("Connecting to " + ip + "...");
    }

    private GameClient.ClientListener buildClientListener() {
        return new GameClient.ClientListener() {
            @Override public void onConnected(int slot) {
                Gdx.app.postRunnable(() -> {
                    localSlot = slot;
                    setStatus("Connected as slot " + (slot + 1));
                });
            }
            @Override public void onDisconnected(String reason) {
                Gdx.app.postRunnable(() -> setStatus("Disconnected: " + reason));
            }
            @Override public void onLobbyState(LobbyStatePacket pkt) {
                Gdx.app.postRunnable(() -> {
                    for (int i = 0; i < 4; i++) {
                        currentNames[i] = pkt.playerNames[i];
                        currentRoles[i] = pkt.playerRoles[i];
                    }
                    selectedCaseId = pkt.selectedCaseId;
                    updateSlotUI();
                });
            }
            @Override public void onGameStart(GameStartPacket pkt) {
                Gdx.app.postRunnable(() -> launchOperation(pkt.caseId,
                    pkt.assignedRoles[localSlot]));
            }
            @Override public void onSyncTick(SyncTickPacket pkt) {}
            @Override public void onStructureDiscovered(StructureDiscoveredPacket pkt) {}
            @Override public void onCaseComplete(CaseCompletePacket pkt) {}
            @Override public void onPhaseChange(PhaseChangePacket pkt) {}
        };
    }

    private void cycleRole(int slot) {
        String current = currentRoles[slot];
        int idx = 0;
        for (int i = 0; i < ROLE_NAMES.length; i++) {
            if (ROLE_NAMES[i].equals(current)) { idx = i; break; }
        }
        currentRoles[slot] = ROLE_NAMES[(idx + 1) % ROLE_NAMES.length];
        if (isHost && game.networkManager.isHost()) {
            game.networkManager.getServer().broadcastLobbyState();
        }
        updateSlotUI();
    }

    private void selectCase(String caseId) {
        selectedCaseId = caseId;
        if (isHost && game.networkManager.isHost()) {
            game.networkManager.selectCase(caseId);
        }
        ClinicalCase c = game.caseDatabase.getById(caseId);
        if (caseLabel != null && c != null)
            caseLabel.setText("Selected: " + c.displayName + "  [diff " + c.difficulty + "]");
    }

    private void tryStartGame() {
        if (!isHost) { setStatus("Only the host can start the game"); return; }
        if (selectedCaseId == null) { setStatus("Select a case first"); return; }
        ClinicalCase c = game.caseDatabase.getById(selectedCaseId);
        if (c == null) { setStatus("Case not found"); return; }

        OperationDirector director = new OperationDirector();
        game.networkManager.startGame(c, director);
        launchOperation(selectedCaseId, currentRoles[0]);
    }

    private void launchOperation(String caseId, String roleName) {
        if (gameStarting) return;
        gameStarting = true;
        ClinicalCase c = game.caseDatabase.getById(caseId);
        if (c == null) { setStatus("Case not found: " + caseId); return; }
        SpecialistRole role = SpecialistRole.SURGEON;
        try { role = SpecialistRole.valueOf(roleName); } catch (Exception ignored) {}
        game.setScreen(new OperationScreen(game, c, role, isHost, localSlot));
    }

    private void applyLobbyState(LobbyStatePacket state) {
        for (int i = 0; i < 4; i++) {
            currentNames[i] = state.playerNames[i];
            currentRoles[i] = state.playerRoles[i];
        }
        selectedCaseId = state.selectedCaseId;
        updateSlotUI();
    }

    private void updateSlotUI() {
        for (int i = 0; i < 4; i++) {
            if (slotLabels[i] == null) continue;
            boolean occupied = currentNames[i] != null;
            slotLabels[i].setText(occupied ? currentNames[i] : "[ empty ]");
            slotLabels[i].setColor(occupied ? Color.WHITE : Color.DARK_GRAY);
            roleLabels[i].setText(currentRoles[i] != null ? currentRoles[i] : "---");
        }
        if (selectedCaseId != null && caseLabel != null) {
            ClinicalCase c = game.caseDatabase.getById(selectedCaseId);
            if (c != null) caseLabel.setText("Selected: " + c.displayName);
        }
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    @Override
    protected void update(float delta) {
        game.networkManager.update(delta);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        update(delta);
        stage.act(delta);
        stage.draw();
    }
}
