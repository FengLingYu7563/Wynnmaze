package com.wynnmaze.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wynnmaze.render.ConfigScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class MazeCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("wynnmaze")

                .then(ClientCommandManager.literal("_internal_guide_")
                    .then(ClientCommandManager.argument("exitId", StringArgumentType.word()).executes(ctx -> {
                        String exitId = StringArgumentType.getString(ctx, "exitId");
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player == null) return 0;
                        // 每次點擊都用當前玩家位置重新計算路徑
                        double px = mc.player.getX(), pz = mc.player.getZ();
                        java.util.List<double[]> path = WayfindingManager.getGuidePath(exitId, px, pz);
                        if (path != null && !path.isEmpty()) {
                            GuideRenderer.setGuidePath(path, exitId);
                            String colorName = WayfindingManager.getColorName(exitId);
                            int colorInt = colorName.equals("red") ? 0xFF5555 : colorName.equals("green") ? 0x55FF55 : colorName.equals("blue") ? 0x5555FF : 0xFFFF55;
                            net.minecraft.text.MutableText msg = net.minecraft.text.Text.literal("\u00a7a[WynnMaze]\u00a7r 已顯示路線 \u2192 ")
                                .append(net.minecraft.text.Text.literal(colorName).styled(s -> s.withColor(net.minecraft.text.TextColor.fromRgb(colorInt))));
                            mc.execute(() -> { if (mc.player != null) mc.player.sendMessage(msg, false); });
                        }
                        return 1;
                    }))
                )

                .then(ClientCommandManager.literal("_share_")
                    .then(ClientCommandManager.argument("msg", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(ctx -> {
                        String msg = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "msg")
                            .replace("\u00a0", " ");
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player == null) return 0;
                        // 抑制自己收到的那條
                        ChatListener.markReceivedThisRound();
                        mc.player.networkHandler.sendChatCommand("p " + msg);
                        return 1;
                    }))
                )

                .then(ClientCommandManager.literal("config").executes(ctx -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    mc.execute(() -> mc.setScreen(new ConfigScreen(mc.currentScreen)));
                    return 1;
                }))
        );
    }
}
