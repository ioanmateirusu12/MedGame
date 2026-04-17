package com.medgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.medgame.MedGame;
import com.medgame.util.Constants;

public class LoadingScreen extends BaseScreen {

    private final ShapeRenderer shapeRenderer;
    private boolean assetsQueued = false;

    public LoadingScreen(MedGame game) {
        super(game);
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    protected void buildUI() {
        // No Scene2D UI on loading screen
    }

    @Override
    public void show() {
        if (!assetsQueued) {
            game.assets.queueAll();
            assetsQueued = true;
        }
    }

    @Override
    protected void update(float delta) {
        if (game.assets.update()) {
            game.onAssetsLoaded();
            game.setScreen(new MainMenuScreen(game));
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        update(delta);

        float progress = game.assets.getProgress();
        float barWidth = Constants.VIEWPORT_WIDTH * 0.6f;
        float barHeight = 20f;
        float barX = (Constants.VIEWPORT_WIDTH - barWidth) / 2f;
        float barY = Constants.VIEWPORT_HEIGHT / 2f - barHeight / 2f;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(barX, barY, barWidth * progress, barHeight);
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        super.dispose();
        shapeRenderer.dispose();
    }
}
