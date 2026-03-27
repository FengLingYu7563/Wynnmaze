package com.wynnmaze;

import com.wynnmaze.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WynnMazeClient implements ClientModInitializer {

    public static final String MOD_ID = "wynnmaze";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final MazeExitTracker EXIT_TRACKER = new MazeExitTracker();

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Wynnmaze] Loaded!");

        GuideConfig.load();
        RoadManager.reload();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EXIT_TRACKER.tick(client);
            if (EXIT_TRACKER.justEnteredMaze()) {
                ChatListener.resetRound();
            }
        });

        RoadRenderer.register();
        GuideRenderer.register();
        ChatListener.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            MazeCommands.register(dispatcher)
        );

        LOGGER.info("[Wynnmaze] Events registered");
    }
}
