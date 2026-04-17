package com.medgame.screen;

import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.medgame.MedGame;
import com.medgame.util.Constants;

public abstract class BaseScreen implements Screen {

    protected final MedGame game;
    protected final Stage stage;
    protected final OrthographicCamera camera;
    protected final Viewport viewport;

    public BaseScreen(MedGame game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT, camera);
        stage = new Stage(viewport);
        buildUI();
    }

    protected abstract void buildUI();

    protected abstract void update(float delta);

    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        com.badlogic.gdx.Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render(float delta) {
        update(delta);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
