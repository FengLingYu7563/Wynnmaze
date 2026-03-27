package com.wynnmaze.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

public class GuideConfig {

    private static final Path CONFIG_PATH = Paths.get("config", "wynnmaze", "guide_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 引路線
    public float lineR = 0.84313726f, lineG = 0.5882353f, lineB = 0.9137255f;
    public float lineWidth = 4.0f;
    public float lineAlpha = 0.9f;
    // 箭頭
    public float arrowR = 1.0f, arrowG = 0.6666667f, arrowB = 0.0f;
    public float arrowWidth = 5.0f;
    public float arrowSpacing = 5.0f;
    public float arrowAlpha = 0.9f;

    private static GuideConfig INSTANCE = null;

    public static GuideConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                    INSTANCE = GSON.fromJson(r, GuideConfig.class);
                }
            }
        } catch (Exception e) {
            LoggerFactory.getLogger("wynnmaze").warn("[Wynnmaze] Failed to load guide_config.json, using defaults");
        }
        if (INSTANCE == null) INSTANCE = new GuideConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger("wynnmaze").warn("[Wynnmaze] Failed to save guide_config.json");
        }
    }
}
