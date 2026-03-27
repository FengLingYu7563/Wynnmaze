package com.wynnmaze.util;

import com.wynnmaze.WynnMazeClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener {

    private static boolean receivedThisRound = false;
    private static final Pattern COORD_PATTERN = Pattern.compile("\\[([-\\d]+),\\s*([-\\d]+),\\s*([-\\d]+)\\]");

    public static void resetRound() { receivedThisRound = false; }
    public static void markReceivedThisRound() { receivedThisRound = true; }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (!WynnMazeClient.EXIT_TRACKER.isInMaze()) return;
            if (receivedThisRound) return;
            if (message.getString().contains("[WynnMaze]")) return;

            String playerName = mc.player.getName().getString();
            String raw = message.getString();
            if (raw.contains(playerName)) return;

            Matcher m = COORD_PATTERN.matcher(raw);
            while (m.find()) {
                try {
                    int cx = Integer.parseInt(m.group(1));
                    int cz = Integer.parseInt(m.group(3));
                    String exitId = MazeExitTracker.matchExitNear(cx, cz, 8);
                    if (exitId == null) continue;

                    receivedThisRound = true;
                    final String fExitId = exitId;
                    final String fColorName = WayfindingManager.getColorName(exitId);
                    final String fCoords = m.group(1) + "," + m.group(2) + "," + m.group(3);

                    mc.execute(() -> {
                        if (mc.player == null) return;
                        int colorInt = fColorName.equals("red") ? 0xFF5555 :
                                       fColorName.equals("green") ? 0x55FF55 :
                                       fColorName.equals("blue") ? 0x5555FF : 0xFFFF55;

                        MutableText line1 = Text.literal("\u00a7a[WynnMaze]\u00a7r ")
                            .append(Text.literal(fColorName + " ").styled(s -> s.withColor(TextColor.fromRgb(colorInt))))
                            .append(Text.literal("[" + fCoords + "]").styled(s -> s.withColor(TextColor.fromRgb(0x55FFFF))));
                        mc.player.sendMessage(line1, false);

                        MutableText line2 = Text.literal("\u00a7a[WynnMaze]\u00a7r ")
                            .append(Text.literal("\u00a7e\u00a7n[點擊顯示路線]")
                                .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("wynnmaze _internal_guide_ " + fExitId))));
                        mc.player.sendMessage(line2, false);
                    });
                    return;
                } catch (Exception ignored) {}
            }
        });
    }
}
