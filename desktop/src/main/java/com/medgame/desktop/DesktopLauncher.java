package com.medgame.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.medgame.MedGame;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("MedGame - Anatomy Co-op");
        config.setWindowedMode(1280, 720);
        config.setWindowIcon("sprites/icon.png");
        config.setForegroundFPS(60);
        config.setResizable(true);
        new Lwjgl3Application(new MedGame(), config);
    }
}
