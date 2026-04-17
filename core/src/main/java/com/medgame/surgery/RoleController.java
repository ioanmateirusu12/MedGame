package com.medgame.surgery;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.medgame.model.SpecialistRole;

/**
 * Translates raw input into surgical actions appropriate for a player's role.
 * Moves the tool entity based on mouse/touch and fires actions on click.
 */
public class RoleController {

    private final SpecialistRole role;
    private final OrthographicCamera camera;
    private final Vector2 toolPosition = new Vector2();
    private ActionListener listener;

    public interface ActionListener {
        void onActionFired(SurgicalAction action, Tool tool, Vector2 toolPosition);
    }

    public RoleController(SpecialistRole role, OrthographicCamera camera) {
        this.role = role;
        this.camera = camera;
    }

    public void setListener(ActionListener listener) {
        this.listener = listener;
    }

    public void update(float delta) {
        // Track mouse position in world coordinates
        Vector3 world = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(world);
        toolPosition.set(world.x, world.y);

        // Fire action on left click
        if (Gdx.input.justTouched()) {
            firePrimaryAction();
        }
    }

    private void firePrimaryAction() {
        if (listener == null) return;
        SurgicalAction action;
        Tool tool;

        switch (role) {
            case SURGEON:
                // Surgeon: cycle through surgical tools; default INCISE with SCALPEL
                action = getPrimarySurgeonAction();
                tool   = getPrimarySurgeonTool();
                break;
            case ASSISTANT:
                action = SurgicalAction.RETRACT;
                tool   = Tool.RETRACTOR;
                break;
            case ANESTHESIOLOGIST:
                action = SurgicalAction.ADMINISTER_DRUG;
                tool   = Tool.LARYNGOSCOPE;
                break;
            case SCRUB_NURSE:
                action = SurgicalAction.IDENTIFY;
                tool   = Tool.FORCEPS;
                break;
            default:
                return;
        }
        listener.onActionFired(action, tool, toolPosition);
    }

    // Tool cycling for surgeon: number keys 1-9 select tool
    private static final SurgicalAction[] SURGEON_ACTIONS = {
        SurgicalAction.INCISE, SurgicalAction.IDENTIFY, SurgicalAction.LIGATE,
        SurgicalAction.STAPLE, SurgicalAction.SUTURE, SurgicalAction.IRRIGATE,
        SurgicalAction.EXTRACT, SurgicalAction.INTUBATE
    };
    private static final Tool[] SURGEON_TOOLS = {
        Tool.SCALPEL, Tool.FORCEPS, Tool.CLIP_APPLIER,
        Tool.ENDOSTAPLER, Tool.SUTURE, Tool.IRRIGATOR,
        Tool.RETRIEVAL_BAG, Tool.LARYNGOSCOPE
    };
    private int selectedToolIndex = 0;

    private SurgicalAction getPrimarySurgeonAction() {
        updateSelectedTool();
        return SURGEON_ACTIONS[selectedToolIndex];
    }

    private Tool getPrimarySurgeonTool() {
        return SURGEON_TOOLS[selectedToolIndex];
    }

    private void updateSelectedTool() {
        for (int i = 0; i < SURGEON_TOOLS.length; i++) {
            if (Gdx.input.isKeyPressed(Input.Keys.NUM_1 + i)) {
                selectedToolIndex = i;
                break;
            }
        }
    }

    public Vector2 getToolPosition() { return toolPosition; }

    public int getSelectedToolIndex() { return selectedToolIndex; }

    public Tool[] getAvailableTools() {
        return role == SpecialistRole.SURGEON ? SURGEON_TOOLS : new Tool[0];
    }
}
