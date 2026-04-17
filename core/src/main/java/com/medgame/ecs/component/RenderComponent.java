package com.medgame.ecs.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class RenderComponent implements Component {
    public TextureRegion region;
    public Color tint = new Color(Color.WHITE);
    public float width = 64f;
    public float height = 64f;
    public boolean visible = true;
}
