package com.wynnmaze;

import com.wynnmaze.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WynnMazeClient implements ClientModInitializer {

    public static final String MOD_ID = "wynnmaze";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final MazeExitTracker EXIT_TRACKER = new MazeExitTracker();

    // 追蹤上一 tick 的按鍵狀態，用來偵測「剛按下」
    private static boolean guideKeyWasDown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Wynnmaze] Loaded!");

        GuideConfig.load();
        RoadManager.reload();

        // 啟動時非同步檢查 GitHub 上有沒有新版本（不會自動下載/取代）
        UpdateChecker.checkAsync();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EXIT_TRACKER.tick(client);

            // 進入迷宮：清上一輪暫存與殘留路徑
            if (EXIT_TRACKER.justEnteredMaze()) {
                ChatListener.resetRound();
                GuideRenderer.clearGuidePath();
            }

            // 離開迷宮：清隊友訊息暫存 + 清殘留路徑（修上次回報的殘留 bug）
            if (EXIT_TRACKER.justLeftMaze()) {
                ChatListener.onLeftMaze();
                GuideRenderer.clearGuidePath();
            }

            // 玩家加入世界後，跳一次「有新版本」通知（如果有的話）
            UpdateChecker.tickNotify(client);

            // 快捷鍵偵測（自己管理，不用 Fabric keybinding 系統）
            if (client.player == null || client.currentScreen != null) {
                guideKeyWasDown = false;
                return;
            }
            int keyCode = GuideConfig.get().guideKeyCode;
            boolean isDown = org.lwjgl.glfw.GLFW.glfwGetKey(client.getWindow().getHandle(), keyCode) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            boolean justPressed = isDown && !guideKeyWasDown;
            guideKeyWasDown = isDown;

            if (justPressed) {
                String exitId = EXIT_TRACKER.getLastDetectedExitId();
                if (exitId == null) return;

                double px = client.player.getX(), pz = client.player.getZ();
                List<double[]> path = WayfindingManager.getGuidePath(exitId, px, pz);
                if (path == null || path.isEmpty()) return;

                GuideRenderer.setGuidePath(path, exitId);

                String colorName = WayfindingManager.getColorName(exitId);
                int colorInt = colorName.equals("red") ? 0xFF5555
                             : colorName.equals("green") ? 0x55FF55
                             : colorName.equals("blue") ? 0x5555FF
                             : 0xFFFF55;

                net.minecraft.text.MutableText msg = Text.literal("\u00a7a[WynnMaze]\u00a7r " + LangManager.routeShown())
                    .append(Text.literal(colorName).styled(s -> s.withColor(TextColor.fromRgb(colorInt))));
                client.player.sendMessage(msg, false);
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

    /** 取得目前快捷鍵的顯示名稱（例如 "."），供訊息動態顯示 */
    public static String getGuideKeyName() {
        int code = GuideConfig.get().guideKeyCode;
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(code, 0);
        return (name != null && !name.isEmpty()) ? name : "key:" + code;
    }
}
