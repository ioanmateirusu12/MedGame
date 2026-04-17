package com.medgame.ecs;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.medgame.ecs.component.AnatomicalStructureComponent;
import com.medgame.ecs.component.PlayerComponent;
import com.medgame.ecs.component.RenderComponent;
import com.medgame.ecs.component.SurgicalToolComponent;
import com.medgame.ecs.component.TransformComponent;
import com.medgame.model.AnatomicalStructure;
import com.medgame.model.SpecialistRole;
import com.medgame.surgery.Tool;

public class EntityFactory {

    private final Engine engine;

    public EntityFactory(Engine engine) {
        this.engine = engine;
    }

    public Entity createStructure(AnatomicalStructure structure, float x, float y, boolean startRevealed) {
        Entity entity = engine.createEntity();

        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.position.set(x, y);
        transform.zIndex = 1;
        entity.add(transform);

        RenderComponent render = engine.createComponent(RenderComponent.class);
        render.width = 80f;
        render.height = 50f;
        render.tint.set(colorForSystem(structure.system));
        entity.add(render);

        AnatomicalStructureComponent asc = engine.createComponent(AnatomicalStructureComponent.class);
        asc.structureId = structure.id;
        asc.isRevealed = startRevealed;
        asc.interactionRadius = 50f;
        entity.add(asc);

        engine.addEntity(entity);
        return entity;
    }

    public Entity createPlayerTool(int playerId, SpecialistRole role, Tool toolType,
                                   float x, float y, boolean isLocal) {
        Entity entity = engine.createEntity();

        TransformComponent transform = engine.createComponent(TransformComponent.class);
        transform.position.set(x, y);
        transform.zIndex = 10;
        entity.add(transform);

        RenderComponent render = engine.createComponent(RenderComponent.class);
        render.width = 24f;
        render.height = 24f;
        render.tint.set(colorForRole(role));
        entity.add(render);

        SurgicalToolComponent tool = engine.createComponent(SurgicalToolComponent.class);
        tool.toolType = toolType;
        tool.isActive = isLocal;
        tool.precisionRate = 0.35f;
        entity.add(tool);

        PlayerComponent player = engine.createComponent(PlayerComponent.class);
        player.playerId = playerId;
        player.role = role;
        player.isLocal = isLocal;
        entity.add(player);

        engine.addEntity(entity);
        return entity;
    }

    private Color colorForSystem(String system) {
        if (system == null) return Color.GRAY;
        switch (system) {
            case "cardiovascular": return new Color(0.8f, 0.1f, 0.1f, 1f);
            case "nervous":        return new Color(0.9f, 0.9f, 0.2f, 1f);
            case "digestive":      return new Color(0.8f, 0.5f, 0.2f, 1f);
            case "musculoskeletal":return new Color(0.6f, 0.8f, 0.6f, 1f);
            case "urinary":        return new Color(0.3f, 0.6f, 0.9f, 1f);
            default:               return Color.LIGHT_GRAY;
        }
    }

    private Color colorForRole(SpecialistRole role) {
        switch (role) {
            case SURGEON:          return Color.WHITE;
            case ASSISTANT:        return Color.CYAN;
            case ANESTHESIOLOGIST: return Color.GREEN;
            case SCRUB_NURSE:      return Color.YELLOW;
            default:               return Color.GRAY;
        }
    }
}
