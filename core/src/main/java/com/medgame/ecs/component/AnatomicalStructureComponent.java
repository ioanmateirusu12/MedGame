package com.medgame.ecs.component;

import com.badlogic.ashley.core.Component;

public class AnatomicalStructureComponent implements Component {
    public String structureId;
    public boolean isRevealed = false;
    public boolean isDamaged = false;
    public boolean isInteractable = true;
    public float interactionRadius = 40f;
    public boolean isHighlighted = false;
}
