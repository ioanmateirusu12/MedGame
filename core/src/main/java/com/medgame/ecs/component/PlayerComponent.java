package com.medgame.ecs.component;

import com.badlogic.ashley.core.Component;
import com.medgame.model.SpecialistRole;

public class PlayerComponent implements Component {
    public int playerId;
    public SpecialistRole role;
    public boolean isLocal = true;
}
