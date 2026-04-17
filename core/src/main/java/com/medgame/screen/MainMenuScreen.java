package com.medgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.medgame.MedGame;
import com.medgame.util.Constants;

public class MainMenuScreen extends BaseScreen {

    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont menuFont;

    public MainMenuScreen(MedGame game) {
        super(game);
        batch = new SpriteBatch();
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3f);
        titleFont.setColor(Color.RED);
        menuFont = new BitmapFont();
        menuFont.getData().setScale(1.5f);
    }

    @Override
    protected void buildUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.center();
        root.pad(20);

        BitmapFont font = new BitmapFont();
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = Color.RED;

        root.add(new Label("MEDGAME", labelStyle)).padBottom(60).row();

        addMenuButton(root, "PLAY", buttonStyle, () -> {
            com.medgame.model.ClinicalCase firstCase = game.caseDatabase.getByDifficulty(1)
                .stream().findFirst().orElse(null);
            game.setScreen(new OperationScreen(game, firstCase));
        });
        addMenuButton(root, "COLLECTION", buttonStyle, () -> game.setScreen(new CollectionScreen(game)));
        addMenuButton(root, "ATLAS", buttonStyle, () -> game.setScreen(new AtlasScreen(game)));
        addMenuButton(root, "EXIT", buttonStyle, () -> Gdx.app.exit());

        stage.addActor(root);
    }

    private void addMenuButton(Table table, String text, TextButton.TextButtonStyle style, Runnable action) {
        TextButton btn = new TextButton(text, style);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        table.add(btn).padBottom(20).width(300).row();
    }

    @Override
    protected void update(float delta) {}

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render(delta);
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        titleFont.dispose();
        menuFont.dispose();
    }
}
