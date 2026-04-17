package com.medgame.util;

public final class Constants {

    private Constants() {}

    public static final int PROTOCOL_VERSION = 1;

    public static final int SERVER_TCP_PORT = 54555;
    public static final int SERVER_UDP_PORT = 54777;
    public static final int LAN_DISCOVERY_PORT = 54776;
    public static final float LAN_DISCOVERY_TIMEOUT = 2f;

    public static final int VIEWPORT_WIDTH = 1280;
    public static final int VIEWPORT_HEIGHT = 720;

    public static final int PIXEL_SCALE = 4;
    public static final int CARD_SPRITE_SIZE = 16;

    public static final float SYNC_RATE = 1f / 20f;
    public static final float INTERPOLATION_BUFFER = 0.1f;

    public static final int MAX_PLAYERS = 4;

    public static final String PREFS_NAME = "medgame_save";
    public static final String PREFS_COLLECTION = "collection";
    public static final String PREFS_PLAYER_NAME = "player_name";
}
