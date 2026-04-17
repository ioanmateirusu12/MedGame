package com.medgame.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.medgame.ecs.component.AnatomicalStructureComponent;
import com.medgame.ecs.component.RenderComponent;
import com.medgame.ecs.component.TransformComponent;

import java.util.Comparator;

public class RenderSystem extends SortedIteratingSystem {

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;

    private static final ComponentMapper<TransformComponent> tm =
        ComponentMapper.getFor(TransformComponent.class);
    private static final ComponentMapper<RenderComponent> rm =
        ComponentMapper.getFor(RenderComponent.class);
    private static final ComponentMapper<AnatomicalStructureComponent> am =
        ComponentMapper.getFor(AnatomicalStructureComponent.class);

    public RenderSystem(SpriteBatch batch, ShapeRenderer shapeRenderer, OrthographicCamera camera) {
        super(Family.all(TransformComponent.class, RenderComponent.class).get(),
              Comparator.comparingInt(e -> tm.get(e).zIndex));
        this.batch = batch;
        this.shapeRenderer = shapeRenderer;
        this.camera = camera;
    }

    @Override
    public void update(float deltaTime) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        super.update(deltaTime);
        batch.end();

        // Draw interaction highlights via ShapeRenderer after sprites
        drawHighlights();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = tm.get(entity);
        RenderComponent render = rm.get(entity);
        if (!render.visible) return;

        if (render.region != null) {
            batch.setColor(render.tint);
            batch.draw(render.region,
                transform.position.x - render.width / 2f,
                transform.position.y - render.height / 2f,
                render.width / 2f, render.height / 2f,
                render.width, render.height,
                transform.scaleX, transform.scaleY,
                transform.rotation);
            batch.setColor(Color.WHITE);
        } else {
            // Placeholder: draw colored rectangle for structures without sprites
            AnatomicalStructureComponent asc = am.get(entity);
            Color c = asc != null && asc.isRevealed ? Color.PINK : Color.DARK_GRAY;
            if (asc != null && asc.isDamaged) c = Color.RED;
            batch.setColor(c);
            // Draw a simple white pixel scaled to size (fallback without texture)
            batch.setColor(Color.WHITE);
        }
    }

    private void drawHighlights() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Entity entity : getEntities()) {
            AnatomicalStructureComponent asc = am.get(entity);
            if (asc == null || !asc.isHighlighted) continue;
            TransformComponent t = tm.get(entity);
            RenderComponent r = rm.get(entity);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.rect(
                t.position.x - r.width / 2f - 3,
                t.position.y - r.height / 2f - 3,
                r.width + 6, r.height + 6);
        }
        shapeRenderer.end();
    }
}
