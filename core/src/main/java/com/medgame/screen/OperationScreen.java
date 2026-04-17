package com.medgame.screen;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.medgame.MedGame;
import com.medgame.ecs.EntityFactory;
import com.medgame.ecs.component.AnatomicalStructureComponent;
import com.medgame.ecs.component.PlayerComponent;
import com.medgame.ecs.component.SurgicalToolComponent;
import com.medgame.ecs.component.TransformComponent;
import com.medgame.ecs.system.RenderSystem;
import com.medgame.ecs.system.SurgicalToolSystem;
import com.medgame.model.AnatomicalStructure;
import com.medgame.model.CasePhase;
import com.medgame.model.CaseStep;
import com.medgame.model.ClinicalCase;
import com.medgame.model.SpecialistRole;
import com.medgame.surgery.OperationDirector;
import com.medgame.surgery.RoleController;
import com.medgame.surgery.SurgicalAction;
import com.medgame.surgery.SurgicalPhase;
import com.medgame.surgery.Tool;

import com.medgame.network.NetworkManager;
import com.medgame.network.packet.SyncTickPacket;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OperationScreen extends BaseScreen {

    // ECS
    private Engine engine;
    private EntityFactory entityFactory;
    private Entity playerToolEntity;
    private final Entity[] remoteToolEntities = new Entity[4];

    // Rendering
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private RenderSystem renderSystem;
    private SurgicalToolSystem toolSystem;

    // Game logic
    private final ClinicalCase clinicalCase;
    private final SpecialistRole localRole;
    private final boolean isHost;
    private final int localSlot;
    private OperationDirector director;
    private final RoleController roleController;

    // HUD labels (updated each frame)
    private Label phaseLabel;
    private Label stepLabel;
    private Label heartRateLabel;
    private Label oxygenLabel;
    private Label bpLabel;
    private Label scoreLabel;
    private Label toolLabel;
    private Label discoveryLabel;
    private Table hudTop;
    private Table hudBottom;

    // Vital bar positions (drawn via ShapeRenderer)
    private float hrBar = 1f, o2Bar = 1f;
    private boolean showResult = false;
    private boolean operationSuccess = false;
    private int finalScore = 0;

    // Convenience constructor for single-player
    public OperationScreen(MedGame game, ClinicalCase clinicalCase) {
        this(game, clinicalCase, SpecialistRole.SURGEON, true, 0);
    }

    public OperationScreen(MedGame game, ClinicalCase clinicalCase,
                           SpecialistRole role, boolean isHost, int localSlot) {
        super(game);
        this.clinicalCase = clinicalCase;
        this.localRole = role;
        this.isHost = isHost;
        this.localSlot = localSlot;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();

        roleController = new RoleController(localRole, camera);

        setupECS();

        // Only host runs the authoritative OperationDirector
        if (isHost) {
            director = new OperationDirector();
            setupOperationDirector();
        }

        populateOperatingTable();

        // Client registers sync listener
        if (!isHost && game.networkManager.isClient()) {
            setupClientSync();
        }

        if (isHost && director != null) director.startOperation(clinicalCase);
    }

    private void setupECS() {
        engine = new Engine();
        entityFactory = new EntityFactory(engine);

        renderSystem = new RenderSystem(batch, shapeRenderer, camera);
        toolSystem = new SurgicalToolSystem();

        engine.addSystem(toolSystem);
        engine.addSystem(renderSystem);

        // Wire tool interactions
        toolSystem.setListener(new SurgicalToolSystem.InteractionListener() {
            @Override
            public void onToolOnTarget(Entity toolEntity, String structureId, float precision) {
                // Update step hint when hovering
            }
            @Override
            public void onToolLeft(Entity toolEntity, String structureId) {}
        });

        // Wire role controller
        roleController.setListener((action, tool, pos) -> {
            String targetId = getHoveredStructureId();
            if (targetId == null) return;
            Entity te = playerToolEntity;
            if (te == null) return;
            SurgicalToolComponent stc = te.getComponent(SurgicalToolComponent.class);
            float precision = stc != null ? stc.precision : 0f;

            if (isHost && director != null) {
                director.submitAction(action, targetId, tool, precision);
                if (game.networkManager.isHost())
                    game.networkManager.getServer().setHostToolPosition(pos.x, pos.y);
            } else if (!isHost && game.networkManager.isClient()) {
                game.networkManager.getClient().sendAction(
                    action.name(), targetId, tool.name(), pos.x, pos.y, precision);
            }
        });
    }

    private void setupClientSync() {
        game.networkManager.setClientListener(new com.medgame.network.GameClient.ClientListener() {
            @Override public void onConnected(int slot) {}
            @Override public void onDisconnected(String reason) {}
            @Override public void onLobbyState(com.medgame.network.packet.LobbyStatePacket pkt) {}
            @Override public void onGameStart(com.medgame.network.packet.GameStartPacket pkt) {}

            @Override
            public void onSyncTick(SyncTickPacket pkt) {
                updateVitalsUI(pkt.heartRate, pkt.bloodPressure, pkt.oxygenSat);
                if (scoreLabel != null) scoreLabel.setText("Score: " + pkt.score);
                if (phaseLabel != null) phaseLabel.setText("Phase: " + pkt.currentPhaseId);
                // Reveal structures
                if (pkt.revealedStructures != null && !pkt.revealedStructures.isEmpty()) {
                    for (String id : pkt.revealedStructures.split(",")) {
                        revealEntity(id.trim());
                    }
                }
                // Update remote tool positions
                for (int i = 0; i < 4; i++) {
                    if (i == localSlot) continue;
                    if (remoteToolEntities[i] != null) {
                        TransformComponent t = remoteToolEntities[i].getComponent(TransformComponent.class);
                        if (t != null) { t.position.x = pkt.toolPosX[i]; t.position.y = pkt.toolPosY[i]; }
                    }
                }
            }

            @Override
            public void onStructureDiscovered(com.medgame.network.packet.StructureDiscoveredPacket pkt) {
                revealEntity(pkt.structureId);
                AnatomicalStructure s = game.anatomyDatabase.getById(pkt.structureId);
                if (discoveryLabel != null && s != null)
                    discoveryLabel.setText("Discovered: " + s.name + "!");
            }

            @Override
            public void onCaseComplete(com.medgame.network.packet.CaseCompletePacket pkt) {
                showResult = true;
                operationSuccess = pkt.success;
                finalScore = pkt.score;
                updateResultUI(pkt.success, pkt.score,
                    new HashSet<>(Arrays.asList(pkt.discoveredStructureIds != null
                        ? pkt.discoveredStructureIds : new String[0])));
            }

            @Override
            public void onPhaseChange(com.medgame.network.packet.PhaseChangePacket pkt) {
                if (phaseLabel != null) phaseLabel.setText("Phase: " + pkt.newPhase);
            }
        });
    }

    private void setupOperationDirector() {
        director.setListener(new OperationDirector.OperationListener() {
            @Override
            public void onPhaseChanged(SurgicalPhase old, SurgicalPhase next, CasePhase data) {
                if (phaseLabel != null && data != null)
                    phaseLabel.setText("Phase: " + data.displayName);
            }

            @Override
            public void onStepChanged(CaseStep step) {
                if (stepLabel != null && step != null)
                    stepLabel.setText(step.instruction);
            }

            @Override
            public void onStructureDiscovered(String structureId) {
                AnatomicalStructure s = game.anatomyDatabase.getById(structureId);
                String name = s != null ? s.name : structureId;
                if (discoveryLabel != null)
                    discoveryLabel.setText("Discovered: " + name + "!");
                // Reveal entity
                revealEntity(structureId);
            }

            @Override
            public void onVitalsChanged(float hr, float bp, float o2) {
                updateVitalsUI(hr, bp, o2);
            }

            @Override
            public void onOperationComplete(boolean success, int score, Set<String> discovered) {
                showResult = true;
                operationSuccess = success;
                finalScore = score;
                updateResultUI(success, score, discovered);
            }
        });
    }

    /** Place structure entities on the operating table */
    private void populateOperatingTable() {
        if (clinicalCase == null) return;

        List<String> opportunities = clinicalCase.discoveryOpportunities;
        if (opportunities == null || opportunities.isEmpty()) return;

        int count = opportunities.size();
        float startX = 200f;
        float startY = 400f;
        float spacingX = 100f;
        float spacingY = 80f;
        int cols = 5;

        for (int i = 0; i < count; i++) {
            String id = opportunities.get(i);
            AnatomicalStructure structure = game.anatomyDatabase.getById(id);
            if (structure == null) continue;
            float x = startX + (i % cols) * spacingX;
            float y = startY - (i / cols) * spacingY;
            entityFactory.createStructure(structure, x, y, false);
        }

        // Player tool (surgeon cursor)
        playerToolEntity = entityFactory.createPlayerTool(
            localSlot, localRole, Tool.SCALPEL, 640, 360, true);

        // Create ghost entities for remote players (visible as colored dots)
        SpecialistRole[] remoteRoles = {
            SpecialistRole.SURGEON, SpecialistRole.ASSISTANT,
            SpecialistRole.ANESTHESIOLOGIST, SpecialistRole.SCRUB_NURSE
        };
        for (int i = 0; i < 4; i++) {
            if (i == localSlot) continue;
            remoteToolEntities[i] = entityFactory.createPlayerTool(
                i, remoteRoles[i], Tool.FORCEPS, -100, -100, false);
        }
    }

    @Override
    protected void buildUI() {
        BitmapFont font = new BitmapFont();
        BitmapFont smallFont = new BitmapFont();
        smallFont.getData().setScale(0.8f);
        Label.LabelStyle style      = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle smallStyle = new Label.LabelStyle(smallFont, Color.CYAN);
        Label.LabelStyle warnStyle  = new Label.LabelStyle(font, Color.RED);

        // Top HUD: phase + step instruction
        hudTop = new Table();
        hudTop.setFillParent(false);
        hudTop.top().left().pad(10);
        hudTop.setBounds(0, 620, 900, 100);

        phaseLabel = new Label("Phase: ---", style);
        stepLabel  = new Label("Select a tool and act on a structure", smallStyle);
        hudTop.add(phaseLabel).padBottom(4).left().row();
        hudTop.add(stepLabel).left().row();
        stage.addActor(hudTop);

        // Discovery notification
        discoveryLabel = new Label("", new Label.LabelStyle(font, Color.GOLD));
        discoveryLabel.setBounds(300, 60, 680, 30);
        stage.addActor(discoveryLabel);

        // Score
        scoreLabel = new Label("Score: 0", style);
        scoreLabel.setBounds(1100, 680, 180, 30);
        stage.addActor(scoreLabel);

        // Vital labels
        heartRateLabel = new Label("HR: --", style);
        heartRateLabel.setBounds(10, 50, 140, 30);
        stage.addActor(heartRateLabel);

        oxygenLabel = new Label("SpO2: --", style);
        oxygenLabel.setBounds(160, 50, 140, 30);
        stage.addActor(oxygenLabel);

        bpLabel = new Label("BP: --", style);
        bpLabel.setBounds(310, 50, 140, 30);
        stage.addActor(bpLabel);

        // Tool label
        toolLabel = new Label("Tool: SCALPEL [1-8 to switch]", smallStyle);
        toolLabel.setBounds(10, 20, 400, 24);
        stage.addActor(toolLabel);

        // Back button
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = font;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = Color.RED;
        TextButton backBtn = new TextButton("EXIT", btnStyle);
        backBtn.setBounds(1180, 10, 90, 30);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        stage.addActor(backBtn);
    }

    @Override
    public void show() {
        InputMultiplexer mx = new InputMultiplexer();
        mx.addProcessor(stage);
        Gdx.input.setInputProcessor(mx);
    }

    @Override
    protected void update(float delta) {
        // Poll network (host advances server; client drains inbound queue)
        game.networkManager.update(delta);

        if (!showResult) {
            roleController.update(delta);

            // Move player tool to mouse position
            if (playerToolEntity != null) {
                TransformComponent t = playerToolEntity.getComponent(TransformComponent.class);
                Vector2 pos = roleController.getToolPosition();
                t.position.set(pos);
            }

            engine.update(delta);

            // Update tool label
            int ti = roleController.getSelectedToolIndex();
            Tool[] tools = roleController.getAvailableTools();
            if (tools.length > 0 && ti < tools.length && toolLabel != null)
                toolLabel.setText("Tool: " + tools[ti].name() + "  [1-8]");

            // Host updates vitals from local director; client gets them via SyncTickPacket
            if (director != null) {
                updateVitalsUI(director.heartRate, director.bloodPressure, director.oxygenSat);
                if (scoreLabel != null) scoreLabel.setText("Score: " + director.getScore());
            }
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.06f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw operating table background
        drawOperatingTable();

        // ECS + rendering happen inside update via engine.update
        update(delta);

        // Draw vital bars
        drawVitalBars();

        // Draw structure labels
        drawStructureLabels();

        // Stage (HUD)
        stage.act(delta);
        stage.draw();

        // Result overlay
        if (showResult) drawResultOverlay();
    }

    private void drawOperatingTable() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Table surface
        shapeRenderer.setColor(0.15f, 0.35f, 0.15f, 1f);
        shapeRenderer.rect(100, 100, 1000, 500);
        // Table border
        shapeRenderer.setColor(0.3f, 0.5f, 0.3f, 1f);
        shapeRenderer.rect(100, 100, 1000, 4);
        shapeRenderer.rect(100, 596, 1000, 4);
        shapeRenderer.rect(100, 100, 4, 500);
        shapeRenderer.rect(1096, 100, 4, 500);
        shapeRenderer.end();

        // Draw structure entities as colored boxes
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Entity entity : engine.getEntitiesFor(
                Family.all(TransformComponent.class, AnatomicalStructureComponent.class).get())) {
            TransformComponent t = entity.getComponent(TransformComponent.class);
            AnatomicalStructureComponent asc = entity.getComponent(AnatomicalStructureComponent.class);
            float w = 80f, h = 40f;

            if (!asc.isRevealed) {
                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f);
            } else {
                AnatomicalStructure s = game.anatomyDatabase.getById(asc.structureId);
                shapeRenderer.setColor(colorForSystem(s != null ? s.system : null));
            }
            shapeRenderer.rect(t.position.x - w / 2f, t.position.y - h / 2f, w, h);

            if (asc.isHighlighted) {
                shapeRenderer.end();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.YELLOW);
                shapeRenderer.rect(t.position.x - w / 2f - 2, t.position.y - h / 2f - 2, w + 4, h + 4);
                shapeRenderer.end();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            }
        }
        shapeRenderer.end();

        // Draw tool cursor
        if (playerToolEntity != null) {
            TransformComponent t = playerToolEntity.getComponent(TransformComponent.class);
            SurgicalToolComponent stc = playerToolEntity.getComponent(SurgicalToolComponent.class);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.circle(t.position.x, t.position.y, 8f);
            shapeRenderer.end();

            // Precision arc
            if (stc != null && stc.precision > 0) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(stc.precision >= 0.7f ? Color.GREEN : Color.ORANGE);
                shapeRenderer.arc(t.position.x, t.position.y, 14f, 0, stc.precision * 360f);
                shapeRenderer.end();
            }
        }
    }

    private void drawStructureLabels() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        BitmapFont font = new BitmapFont();
        font.getData().setScale(0.7f);
        for (Entity entity : engine.getEntitiesFor(
                Family.all(TransformComponent.class, AnatomicalStructureComponent.class).get())) {
            TransformComponent t = entity.getComponent(TransformComponent.class);
            AnatomicalStructureComponent asc = entity.getComponent(AnatomicalStructureComponent.class);
            if (!asc.isRevealed) {
                font.setColor(Color.DARK_GRAY);
                font.draw(batch, "?", t.position.x - 4, t.position.y + 6);
            } else {
                AnatomicalStructure s = game.anatomyDatabase.getById(asc.structureId);
                if (s != null) {
                    font.setColor(Color.WHITE);
                    String short_name = s.name.length() > 14 ? s.name.substring(0, 14) + ".." : s.name;
                    font.draw(batch, short_name, t.position.x - 38f, t.position.y + 7f);
                }
            }
        }
        font.dispose();
        batch.end();
    }

    private void drawVitalBars() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        // HR bar
        float hr = director.heartRate;
        float o2 = director.oxygenSat;
        hrBar = hr / 200f;
        o2Bar = o2 / 100f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // HR
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(10, 80, 130, 14);
        shapeRenderer.setColor(hr > 130 || hr < 50 ? Color.RED : Color.GREEN);
        shapeRenderer.rect(10, 80, 130 * hrBar, 14);
        // O2
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(160, 80, 130, 14);
        shapeRenderer.setColor(o2 < 90 ? Color.RED : Color.CYAN);
        shapeRenderer.rect(160, 80, 130 * o2Bar, 14);
        shapeRenderer.end();
    }

    private void drawResultOverlay() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.75f);
        shapeRenderer.rect(300, 200, 680, 320);
        shapeRenderer.end();
    }

    private void updateVitalsUI(float hr, float bp, float o2) {
        if (heartRateLabel != null) heartRateLabel.setText(String.format("HR: %.0f bpm", hr));
        if (oxygenLabel != null)    oxygenLabel.setText(String.format("SpO2: %.0f%%", o2));
        if (bpLabel != null)        bpLabel.setText(String.format("BP: %.0f", bp));
    }

    private void updateResultUI(boolean success, int score, Set<String> discovered) {
        if (phaseLabel != null)
            phaseLabel.setText(success ? "OPERATION COMPLETE!" : "OPERATION FAILED");
        if (stepLabel != null)
            stepLabel.setText("Score: " + score + "  |  Structures found: " + discovered.size());
    }

    private void revealEntity(String structureId) {
        for (Entity e : engine.getEntitiesFor(
                Family.all(AnatomicalStructureComponent.class).get())) {
            AnatomicalStructureComponent asc = e.getComponent(AnatomicalStructureComponent.class);
            if (asc.structureId.equals(structureId)) {
                asc.isRevealed = true;
                break;
            }
        }
    }

    private String getHoveredStructureId() {
        if (playerToolEntity == null) return null;
        SurgicalToolComponent stc = playerToolEntity.getComponent(SurgicalToolComponent.class);
        return stc != null ? stc.targetStructureId : null;
    }

    private Color colorForSystem(String system) {
        if (system == null) return Color.GRAY;
        switch (system) {
            case "cardiovascular": return new Color(0.8f, 0.2f, 0.2f, 1f);
            case "nervous":        return new Color(0.9f, 0.85f, 0.2f, 1f);
            case "digestive":      return new Color(0.8f, 0.55f, 0.2f, 1f);
            case "musculoskeletal":return new Color(0.5f, 0.75f, 0.5f, 1f);
            case "urinary":        return new Color(0.3f, 0.55f, 0.9f, 1f);
            default:               return Color.LIGHT_GRAY;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        shapeRenderer.dispose();
    }
}
