package com.medgame.ecs.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.medgame.ecs.component.AnatomicalStructureComponent;
import com.medgame.ecs.component.SurgicalToolComponent;
import com.medgame.ecs.component.TransformComponent;

public class SurgicalToolSystem extends IteratingSystem {

    private static final ComponentMapper<TransformComponent> tm =
        ComponentMapper.getFor(TransformComponent.class);
    private static final ComponentMapper<SurgicalToolComponent> stm =
        ComponentMapper.getFor(SurgicalToolComponent.class);
    private static final ComponentMapper<AnatomicalStructureComponent> am =
        ComponentMapper.getFor(AnatomicalStructureComponent.class);

    private InteractionListener listener;

    public interface InteractionListener {
        void onToolOnTarget(Entity toolEntity, String structureId, float precision);
        void onToolLeft(Entity toolEntity, String structureId);
    }

    public SurgicalToolSystem() {
        super(Family.all(TransformComponent.class, SurgicalToolComponent.class).get());
    }

    public void setListener(InteractionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void processEntity(Entity toolEntity, float deltaTime) {
        SurgicalToolComponent tool = stm.get(toolEntity);
        TransformComponent toolTransform = tm.get(toolEntity);
        if (!tool.isActive) return;

        // Check overlap against all structure entities
        String nearestId = null;
        float nearestDist = Float.MAX_VALUE;

        for (Entity structureEntity : getEngine().getEntitiesFor(
                Family.all(TransformComponent.class, AnatomicalStructureComponent.class).get())) {
            AnatomicalStructureComponent asc = am.get(structureEntity);
            if (!asc.isInteractable) continue;
            TransformComponent st = tm.get(structureEntity);
            float dist = Vector2.dst(toolTransform.position.x, toolTransform.position.y,
                                     st.position.x, st.position.y);
            if (dist <= asc.interactionRadius && dist < nearestDist) {
                nearestDist = dist;
                nearestId = asc.structureId;
            }
        }

        // Highlight nearest
        for (Entity structureEntity : getEngine().getEntitiesFor(
                Family.all(AnatomicalStructureComponent.class).get())) {
            AnatomicalStructureComponent asc = am.get(structureEntity);
            asc.isHighlighted = asc.structureId.equals(nearestId);
        }

        if (nearestId != null) {
            tool.precision = Math.min(1f, tool.precision + tool.precisionRate * deltaTime);
            if (listener != null) listener.onToolOnTarget(toolEntity, nearestId, tool.precision);
        } else if (tool.targetStructureId != null) {
            if (listener != null) listener.onToolLeft(toolEntity, tool.targetStructureId);
            tool.precision = 0f;
        }
        tool.targetStructureId = nearestId;
    }
}
