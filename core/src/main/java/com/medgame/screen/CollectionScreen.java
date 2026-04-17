package com.medgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.medgame.MedGame;
import com.medgame.collection.CardCollection;
import com.medgame.collection.SurgicalAbility;
import com.medgame.model.AnatomicalStructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class CollectionScreen extends BaseScreen {

    private static final String[] REGIONS = {
        "All", "abdomen", "thorax", "head_neck", "pelvis", "upper_limb", "lower_limb"
    };
    private static final String[] SYSTEMS = {
        "All", "digestive", "cardiovascular", "nervous", "musculoskeletal", "urinary"
    };
    private static final String[] RARITIES = { "All", "common", "uncommon", "rare", "epic" };

    private ShapeRenderer shapeRenderer;

    // Filter state
    private String filterRegion   = "All";
    private String filterSystem   = "All";
    private String filterRarity   = "All";
    private boolean showOnlyFound = false;

    // Detail panel
    private AnatomicalStructure selectedStructure;
    private Table detailTable;
    private Label detailName, detailLatin, detailFunction, detailLocation,
                  detailBlood, detailInnervation, detailClinical,
                  detailNetter, detailGray, detailRarity, detailAbility;

    // Card list
    private Table cardGrid;
    private ScrollPane cardScroll;
    private Label statsLabel;

    // Fonts
    private BitmapFont titleFont, bodyFont, dimFont;

    public CollectionScreen(MedGame game) {
        super(game);
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    protected void buildUI() {
        titleFont = new BitmapFont();
        titleFont.getData().setScale(1.1f);
        bodyFont = new BitmapFont();
        bodyFont.getData().setScale(0.75f);
        dimFont = new BitmapFont();
        dimFont.getData().setScale(0.65f);

        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.WHITE);
        Label.LabelStyle bodyStyle  = new Label.LabelStyle(bodyFont, Color.WHITE);
        Label.LabelStyle dimStyle   = new Label.LabelStyle(dimFont, Color.LIGHT_GRAY);
        Label.LabelStyle goldStyle  = new Label.LabelStyle(bodyFont, Color.GOLD);
        Label.LabelStyle cyanStyle  = new Label.LabelStyle(bodyFont, Color.CYAN);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = bodyFont;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = Color.YELLOW;

        // ---- Root layout ----
        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(10);

        // Header row
        root.add(new Label("ANATOMICAL CARD COLLECTION", titleStyle)).colspan(3).padBottom(8).row();

        int total     = game.anatomyDatabase.getStructureCount();
        int found     = game.collection.getDiscoveredCount();
        int abilities = game.collection.unlockedAbilities.size();
        statsLabel = new Label(found + " / " + total + " discovered   |   "
            + abilities + " abilities unlocked", cyanStyle);
        root.add(statsLabel).colspan(3).padBottom(12).row();

        // ---- Left: filters ----
        Table filterPanel = new Table();
        filterPanel.top().left().padRight(10);

        filterPanel.add(new Label("REGION", goldStyle)).left().padBottom(4).row();
        for (String r : REGIONS) {
            TextButton b = new TextButton(r, btnStyle);
            final String region = r;
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    filterRegion = region;
                    rebuildCardGrid();
                }
            });
            filterPanel.add(b).left().padBottom(2).row();
        }

        filterPanel.add(new Label("SYSTEM", goldStyle)).left().padTop(10).padBottom(4).row();
        for (String s : SYSTEMS) {
            TextButton b = new TextButton(s, btnStyle);
            final String sys = s;
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    filterSystem = sys;
                    rebuildCardGrid();
                }
            });
            filterPanel.add(b).left().padBottom(2).row();
        }

        filterPanel.add(new Label("RARITY", goldStyle)).left().padTop(10).padBottom(4).row();
        for (String r : RARITIES) {
            TextButton b = new TextButton(r, btnStyle);
            final String rar = r;
            b.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    filterRarity = rar;
                    rebuildCardGrid();
                }
            });
            filterPanel.add(b).left().padBottom(2).row();
        }

        TextButton onlyFoundBtn = new TextButton("[Only found]", btnStyle);
        onlyFoundBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                showOnlyFound = !showOnlyFound;
                rebuildCardGrid();
            }
        });
        filterPanel.add(onlyFoundBtn).left().padTop(10).row();

        TextButton backBtn = new TextButton("< BACK", btnStyle);
        backBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        filterPanel.add(backBtn).left().padTop(20).row();

        root.add(filterPanel).top().left().width(160);

        // ---- Center: card grid ----
        cardGrid = new Table();
        cardGrid.top().left();
        cardScroll = new ScrollPane(cardGrid);
        cardScroll.setFadeScrollBars(false);
        root.add(cardScroll).top().left().width(600).expandY().fillY();

        // ---- Right: detail panel ----
        Table detailOuter = new Table();
        detailOuter.top().left().padLeft(10);
        detailOuter.add(new Label("CARD DETAIL", goldStyle)).left().padBottom(8).row();

        detailTable = new Table();
        detailTable.top().left();
        detailName      = addDetail(detailTable, "Name:",        bodyStyle);
        detailLatin     = addDetail(detailTable, "Latin:",       dimStyle);
        detailRarity    = addDetail(detailTable, "Rarity:",      bodyStyle);
        detailFunction  = addDetail(detailTable, "Function:",    bodyStyle);
        detailLocation  = addDetail(detailTable, "Location:",    bodyStyle);
        detailBlood     = addDetail(detailTable, "Blood supply:",bodyStyle);
        detailInnervation = addDetail(detailTable, "Innervation:", bodyStyle);
        detailClinical  = addDetail(detailTable, "Clinical:",    dimStyle);
        detailNetter    = addDetail(detailTable, "Netter:",      dimStyle);
        detailGray      = addDetail(detailTable, "Gray's:",      dimStyle);
        detailAbility   = addDetail(detailTable, "Unlocks:",     goldStyle);

        detailOuter.add(detailTable).top().left().row();
        root.add(detailOuter).top().left().width(500).expandY().fillY();

        stage.addActor(root);
        rebuildCardGrid();
    }

    private Label addDetail(Table table, String caption, Label.LabelStyle style) {
        BitmapFont capFont = new BitmapFont();
        capFont.getData().setScale(0.65f);
        Label cap = new Label(caption, new Label.LabelStyle(capFont, Color.GOLD));
        Label val = new Label("—", style);
        val.setWrap(true);
        table.add(cap).left().padTop(6);
        table.add(val).left().padLeft(4).width(310).row();
        return val;
    }

    private void rebuildCardGrid() {
        cardGrid.clear();
        Collection<AnatomicalStructure> all = game.anatomyDatabase.getAll().values();
        java.util.List<AnatomicalStructure> filtered = new ArrayList<>();

        for (AnatomicalStructure s : all) {
            if (!filterRegion.equals("All") && !filterRegion.equals(s.region)) continue;
            if (!filterSystem.equals("All") && !filterSystem.equals(s.system)) continue;
            if (!filterRarity.equals("All") && !filterRarity.equals(s.rarity)) continue;
            if (showOnlyFound && !game.collection.isDiscovered(s.id)) continue;
            filtered.add(s);
        }
        filtered.sort(Comparator.comparing(s -> ((AnatomicalStructure) s).name));

        BitmapFont cardFont = new BitmapFont();
        cardFont.getData().setScale(0.65f);

        int cols = 4;
        int col  = 0;
        Table row = null;
        for (AnatomicalStructure s : filtered) {
            if (col == 0) {
                row = new Table();
                cardGrid.add(row).left().padBottom(6).row();
            }

            boolean found = game.collection.isDiscovered(s.id);
            TextButton card = makeCardButton(s, found, cardFont);
            row.add(card).width(130).height(60).padRight(6);
            col = (col + 1) % cols;
        }

        // Update stats
        int total = game.anatomyDatabase.getStructureCount();
        int disc  = game.collection.getDiscoveredCount();
        int ab    = game.collection.unlockedAbilities.size();
        if (statsLabel != null)
            statsLabel.setText(disc + " / " + total + " discovered   |   " + ab + " abilities unlocked");
    }

    private TextButton makeCardButton(AnatomicalStructure s, boolean found, BitmapFont font) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = found ? Color.WHITE : new Color(0.35f, 0.35f, 0.35f, 1f);
        style.overFontColor = Color.YELLOW;

        String label = found ? s.name : "???";
        TextButton btn = new TextButton(label, style);
        if (found) {
            btn.addListener(new ClickListener() {
                @Override public void clicked(InputEvent e, float x, float y) {
                    showDetail(s);
                }
            });
        }
        return btn;
    }

    private void showDetail(AnatomicalStructure s) {
        selectedStructure = s;
        detailName.setText(s.name);
        detailLatin.setText(s.latinName != null ? s.latinName : "—");
        detailRarity.setText(s.rarity != null ? s.rarity.toUpperCase() : "—");
        detailRarity.setColor(rarityColor(s.rarity));
        detailFunction.setText(s.function != null ? s.function : "—");
        detailLocation.setText(s.location != null ? s.location : "—");
        detailBlood.setText(s.bloodSupply != null ? s.bloodSupply : "—");
        detailInnervation.setText(s.innervation != null ? s.innervation : "—");
        detailClinical.setText(s.clinicalNotes != null ? s.clinicalNotes : "—");
        detailNetter.setText(s.netterReference != null ? s.netterReference : "—");
        detailGray.setText(s.grayReference != null ? s.grayReference : "—");

        // Ability
        SurgicalAbility ability = SurgicalAbility.forStructure(s.id);
        if (ability != null) {
            boolean has = game.collection.hasAbility(ability);
            detailAbility.setText((has ? "[UNLOCKED] " : "[LOCKED] ") + ability.displayName
                + "\n" + ability.description);
            detailAbility.setColor(has ? Color.GOLD : Color.GRAY);
        } else {
            detailAbility.setText("—");
        }
    }

    private Color rarityColor(String rarity) {
        if (rarity == null) return Color.WHITE;
        switch (rarity) {
            case "uncommon": return Color.GREEN;
            case "rare":     return Color.CYAN;
            case "epic":     return Color.PURPLE;
            default:         return Color.LIGHT_GRAY;
        }
    }

    @Override
    protected void update(float delta) {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw card backgrounds with ShapeRenderer
        drawCardBackgrounds();

        stage.act(delta);
        stage.draw();
    }

    private void drawCardBackgrounds() {
        // Highlight selected card region in detail panel
        if (selectedStructure != null) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.12f, 0.12f, 0.25f, 1f);
            shapeRenderer.rect(840, 0, 440, 720);
            shapeRenderer.end();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        shapeRenderer.dispose();
        titleFont.dispose();
        bodyFont.dispose();
        dimFont.dispose();
    }
}
