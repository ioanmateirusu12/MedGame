package com.medgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.medgame.MedGame;
import com.medgame.model.AnatomicalStructure;

import java.util.*;

public class AtlasScreen extends BaseScreen {

    // Body region definitions: id, display name, body rectangle position
    private static final RegionDef[] REGIONS = {
        new RegionDef("head_neck",   "Head & Neck",   new Rectangle(490, 530, 140, 130)),
        new RegionDef("thorax",      "Thorax",        new Rectangle(450, 380, 220, 140)),
        new RegionDef("abdomen",     "Abdomen",       new Rectangle(460, 240, 200, 135)),
        new RegionDef("pelvis",      "Pelvis",        new Rectangle(470, 140, 180, 100)),
        new RegionDef("upper_limb",  "Upper Limb",    new Rectangle(330, 340, 100, 220)),
        new RegionDef("upper_limb",  "Upper Limb R",  new Rectangle(690, 340, 100, 220)),
        new RegionDef("lower_limb",  "Lower Limb",    new Rectangle(430,  20, 160, 120)),
    };

    private static class RegionDef {
        final String id, displayName;
        final Rectangle rect;
        RegionDef(String id, String name, Rectangle rect) {
            this.id = id; this.displayName = name; this.rect = rect;
        }
    }

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont labelFont;
    private String hoveredRegion = null;
    private String selectedRegion = null;

    // Right panel
    private Table structureList;
    private Label regionTitleLabel;
    private Label progressLabel;

    public AtlasScreen(MedGame game) {
        super(game);
        shapeRenderer = new ShapeRenderer();
        batch         = new SpriteBatch();
        labelFont     = new BitmapFont();
        labelFont.getData().setScale(0.7f);
    }

    @Override
    protected void buildUI() {
        BitmapFont font = new BitmapFont();
        BitmapFont smallFont = new BitmapFont();
        smallFont.getData().setScale(0.75f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle bodyStyle  = new Label.LabelStyle(smallFont, Color.WHITE);
        Label.LabelStyle goldStyle  = new Label.LabelStyle(smallFont, Color.GOLD);
        Label.LabelStyle cyanStyle  = new Label.LabelStyle(smallFont, Color.CYAN);
        Label.LabelStyle dimStyle   = new Label.LabelStyle(smallFont, Color.GRAY);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = smallFont;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = Color.YELLOW;

        // Title
        Label title = new Label("ANATOMICAL ATLAS", titleStyle);
        title.setBounds(10, 686, 600, 30);
        stage.addActor(title);

        int total = game.anatomyDatabase.getStructureCount();
        int found = game.collection.getDiscoveredCount();
        Label stats = new Label(found + " / " + total + " structures discovered", cyanStyle);
        stats.setBounds(10, 662, 600, 24);
        stage.addActor(stats);

        // Instructions
        Label instr = new Label("Click a body region to explore", dimStyle);
        instr.setBounds(10, 640, 500, 20);
        stage.addActor(instr);

        // Right panel: region detail
        Table rightPanel = new Table();
        rightPanel.top().left().pad(10);
        rightPanel.setBounds(840, 10, 430, 680);

        regionTitleLabel = new Label("Select a region", goldStyle);
        rightPanel.add(regionTitleLabel).left().padBottom(6).row();

        progressLabel = new Label("", cyanStyle);
        rightPanel.add(progressLabel).left().padBottom(10).row();

        // Unlocked abilities row
        rightPanel.add(new Label("ABILITIES IN THIS REGION:", goldStyle)).left().padBottom(4).row();

        structureList = new Table();
        structureList.top().left();
        ScrollPane scroll = new ScrollPane(structureList);
        scroll.setFadeScrollBars(false);
        rightPanel.add(scroll).left().expandY().fillY().width(420).row();

        stage.addActor(rightPanel);

        // Back button
        TextButton back = new TextButton("< BACK", btnStyle);
        back.setBounds(10, 10, 100, 30);
        back.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        stage.addActor(back);
    }

    @Override
    protected void update(float delta) {
        // Detect mouse hover over body regions
        float mx = Gdx.input.getX();
        float my = Gdx.graphics.getHeight() - Gdx.input.getY(); // flip Y
        hoveredRegion = null;
        for (RegionDef rd : REGIONS) {
            if (rd.rect.contains(mx, my)) { hoveredRegion = rd.id; break; }
        }
        if (Gdx.input.justTouched() && hoveredRegion != null) {
            selectRegion(hoveredRegion);
        }
    }

    private void selectRegion(String regionId) {
        selectedRegion = regionId;
        Collection<AnatomicalStructure> all = game.anatomyDatabase.getAll().values();

        List<AnatomicalStructure> inRegion = new ArrayList<>();
        for (AnatomicalStructure s : all) {
            if (regionId.equals(s.region)) inRegion.add(s);
        }
        inRegion.sort(Comparator.comparing(s -> s.name));

        int total = inRegion.size();
        int found = 0;
        for (AnatomicalStructure s : inRegion) {
            if (game.collection.isDiscovered(s.id)) found++;
        }

        String displayName = regionId.replace("_", " ").toUpperCase();
        if (regionTitleLabel != null) regionTitleLabel.setText(displayName);
        if (progressLabel != null)
            progressLabel.setText(found + " / " + total + " discovered ("
                + Math.round(100f * found / Math.max(1, total)) + "%)");

        if (structureList == null) return;
        structureList.clear();

        BitmapFont sf = new BitmapFont();
        sf.getData().setScale(0.68f);

        for (AnatomicalStructure s : inRegion) {
            boolean disc = game.collection.isDiscovered(s.id);
            Color c = disc ? systemColor(s.system) : new Color(0.3f, 0.3f, 0.3f, 1f);
            Label.LabelStyle ls = new Label.LabelStyle(sf, c);
            String text = (disc ? "  " : "? ") + (disc ? s.name : "???")
                + (disc ? "  [" + (s.system != null ? s.system : "") + "]" : "");
            Label lbl = new Label(text, ls);
            structureList.add(lbl).left().padBottom(3).row();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        update(delta);
        drawBodyMap();

        stage.act(delta);
        stage.draw();
    }

    private void drawBodyMap() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Background body silhouette
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 1f);
        // Head
        shapeRenderer.ellipse(490, 530, 140, 130);
        // Neck
        shapeRenderer.rect(535, 490, 50, 50);
        // Torso
        shapeRenderer.rect(450, 140, 220, 380);
        // Left arm
        shapeRenderer.rect(330, 140, 100, 370);
        // Right arm
        shapeRenderer.rect(690, 140, 100, 370);
        // Left leg
        shapeRenderer.rect(450, 10, 90, 140);
        // Right leg
        shapeRenderer.rect(580, 10, 90, 140);
        shapeRenderer.end();

        // Draw region fills colored by discovery progress
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Set<String> drawnIds = new HashSet<>();
        for (RegionDef rd : REGIONS) {
            if (drawnIds.contains(rd.id)) continue; // avoid double counting for bilateral regions
            drawnIds.add(rd.id);

            float progress = game.collection.getRegionProgress(rd.id,
                game.anatomyDatabase.getAll());
            Color base = regionBaseColor(rd.id);
            boolean hovered = rd.id.equals(hoveredRegion);
            boolean selected = rd.id.equals(selectedRegion);

            float alpha = 0.15f + progress * 0.6f;
            shapeRenderer.setColor(base.r, base.g, base.b, alpha + (hovered ? 0.2f : 0f));
            shapeRenderer.rect(rd.rect.x, rd.rect.y, rd.rect.width, rd.rect.height);

            if (selected) {
                shapeRenderer.end();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(Color.YELLOW);
                shapeRenderer.rect(rd.rect.x - 2, rd.rect.y - 2, rd.rect.width + 4, rd.rect.height + 4);
                shapeRenderer.end();
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            }
        }
        shapeRenderer.end();

        // Region labels + progress %
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawnIds.clear();
        for (RegionDef rd : REGIONS) {
            if (drawnIds.contains(rd.id)) continue;
            drawnIds.add(rd.id);
            float progress = game.collection.getRegionProgress(rd.id, game.anatomyDatabase.getAll());
            labelFont.setColor(rd.id.equals(hoveredRegion) ? Color.YELLOW : Color.WHITE);
            String label = rd.displayName + " " + Math.round(progress * 100) + "%";
            labelFont.draw(batch, label, rd.rect.x, rd.rect.y + rd.rect.height + 14);
        }
        batch.end();
    }

    private Color regionBaseColor(String region) {
        switch (region) {
            case "head_neck":  return Color.CYAN;
            case "thorax":     return Color.RED;
            case "abdomen":    return new Color(0.9f, 0.6f, 0.1f, 1f);
            case "pelvis":     return new Color(0.8f, 0.3f, 0.8f, 1f);
            case "upper_limb": return Color.GREEN;
            case "lower_limb": return new Color(0.3f, 0.7f, 1f, 1f);
            default:           return Color.GRAY;
        }
    }

    private Color systemColor(String system) {
        if (system == null) return Color.WHITE;
        switch (system) {
            case "cardiovascular": return new Color(1f, 0.4f, 0.4f, 1f);
            case "nervous":        return new Color(1f, 1f, 0.4f, 1f);
            case "digestive":      return new Color(1f, 0.7f, 0.3f, 1f);
            case "musculoskeletal":return new Color(0.6f, 1f, 0.6f, 1f);
            case "urinary":        return new Color(0.4f, 0.7f, 1f, 1f);
            default:               return Color.LIGHT_GRAY;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        shapeRenderer.dispose();
        batch.dispose();
        labelFont.dispose();
    }
}
