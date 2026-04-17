package com.medgame.ecs.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class TransformComponent implements Component {
    public final Vector2 position = new Vector2();
    public float rotation = 0f;
    public float scaleX = 1f;
    public float scaleY = 1f;
    public int zIndex = 0;
}
