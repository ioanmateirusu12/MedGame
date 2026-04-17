package com.medgame.ecs.component;

import com.badlogic.ashley.core.Component;
import com.medgame.surgery.Tool;

public class SurgicalToolComponent implements Component {
    public Tool toolType;
    public boolean isActive = false;
    public float precision = 0f;      // 0.0 - 1.0, accumulated while held on target
    public float precisionRate = 0.4f; // precision gained per second when on target
    public String targetStructureId;   // current interaction target
}
