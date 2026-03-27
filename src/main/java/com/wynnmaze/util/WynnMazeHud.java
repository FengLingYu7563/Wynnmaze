package com.wynnmaze.util;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public class WynnMazeHud {

    private static final int DISPLAY_TICKS = 200;
    private static final int FADE_TICKS = 40;
    private static final int PREFIX_COLOR = 0xFF84FF84;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int CLICK_COLOR = 0xFF55FFFF;
    private static final int YELLOW_COLOR = 0xFFFFFF55;
    private static final int ORANGE_COLOR = 0xFFFFAA00;

    public static class HudSegment {
        public String text;
        public int color;
        public Runnable onClick;
        public HudSegment(String text, int color, Runnable onClick) {
            this.text = text; this.color = color; this.onClick = onClick;
        }
    }

    public static class HudLine {
        public List<HudSegment> segments = new ArrayList<>();
    }

    public static class HudMessage {
        public List<HudLine> lines = new ArrayList<>();
        public int ticksLeft = DISPLAY_TICKS;
    }

    private static final List<HudMessage> messages = new ArrayList<>();
    // 點擊區域 [x, y, w, h] + action
    private static final List<int[]> clickZones = new ArrayList<>();
    private static final List<Runnable> clickActions = new ArrayList<>();

    public static void register() {
        HudRenderCallback.EVENT.register(WynnMazeHud::render);
    }

    public static void tick() {
        messages.removeIf(m -> --m.ticksLeft <= 0);
    }

    public static void addMessage(HudMessage msg) {
        if (messages.size() >= 3) messages.remove(0);
        messages.add(msg);
    }

    public static void onMouseClick(double mouseX, double mouseY) {
        for (int i = 0; i < clickZones.size(); i++) {
            int[] z = clickZones.get(i);
            if (mouseX >= z[0] && mouseX <= z[0]+z[2] && mouseY >= z[1] && mouseY <= z[1]+z[3]) {
                if (i < clickActions.size() && clickActions.get(i) != null) {
                    clickActions.get(i).run();
                    return;
                }
            }
        }
    }

    private static void render(DrawContext ctx, RenderTickCounter counter) {
        if (messages.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        clickZones.clear();
        clickActions.clear();

        int y = 4;
        int x0 = 4;

        for (HudMessage msg : messages) {
            float alpha = msg.ticksLeft < FADE_TICKS ? msg.ticksLeft / (float) FADE_TICKS : 1.0f;
            int a = (int)(alpha * 0xFF);

            for (HudLine line : msg.lines) {
                int lineW = 4;
                for (HudSegment seg : line.segments)
                    lineW += mc.textRenderer.getWidth(seg.text) + 2;

                ctx.fill(x0 - 2, y - 1, x0 + lineW, y + mc.textRenderer.fontHeight + 1,
                    (Math.min(a, 0xAA) << 24));

                int sx = x0;
                for (HudSegment seg : line.segments) {
                    int col = (seg.color & 0x00FFFFFF) | (a << 24);
                    if (seg.onClick != null) {
                        col = (CLICK_COLOR & 0x00FFFFFF) | (a << 24);
                        int w = mc.textRenderer.getWidth(seg.text);
                        clickZones.add(new int[]{sx, y, w, mc.textRenderer.fontHeight});
                        clickActions.add(seg.onClick);
                        ctx.fill(sx, y + mc.textRenderer.fontHeight, sx + w, y + mc.textRenderer.fontHeight + 1, col);
                    }
                    ctx.drawText(mc.textRenderer, seg.text, sx, y, col, false);
                    sx += mc.textRenderer.getWidth(seg.text) + 2;
                }
                y += mc.textRenderer.fontHeight + 3;
            }
            y += 2;
        }
    }

    public static void showFoundExit(String exitId, String colorName, int x, int z, Runnable shareAction, Runnable waypointAction) {
        HudMessage msg = new HudMessage();
        HudLine line = new HudLine();
        line.segments.add(new HudSegment("[Wynnmaze] ", PREFIX_COLOR, null));
        line.segments.add(new HudSegment("已找到出口！ ", 0xFFAAFFAA, null));
        line.segments.add(new HudSegment(colorName, ORANGE_COLOR, null));
        line.segments.add(new HudSegment(" [" + x + ",47," + z + "]", CLICK_COLOR, waypointAction));
        line.segments.add(new HudSegment("  [分享給隊伍]", YELLOW_COLOR, shareAction));
        msg.lines.add(line);
        addMessage(msg);
    }

    public static void showSent(String colorName, int x, int z) {
        HudMessage msg = new HudMessage();
        HudLine line = new HudLine();
        line.segments.add(new HudSegment("[Wynnmaze] ", PREFIX_COLOR, null));
        line.segments.add(new HudSegment(colorName + " [" + x + ",47," + z + "]", TEXT_COLOR, null));
        msg.lines.add(line);
        addMessage(msg);
    }

    public static void showReceived(String colorName, String coords, Runnable guideAction) {
        HudMessage msg = new HudMessage();
        HudLine line1 = new HudLine();
        line1.segments.add(new HudSegment("[Wynnmaze] ", PREFIX_COLOR, null));
        line1.segments.add(new HudSegment(colorName + " [" + coords + "]", TEXT_COLOR, null));
        msg.lines.add(line1);

        HudLine line2 = new HudLine();
        line2.segments.add(new HudSegment("    ", TEXT_COLOR, null));
        line2.segments.add(new HudSegment("[點擊顯示路線]", CLICK_COLOR, guideAction));
        msg.lines.add(line2);

        addMessage(msg);
    }
}
