package com.wynnmaze.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MazeExitTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("wynnmaze");

    // 出口座標表（從 exits.json 載入）
    private static final List<ExitEntry> EXIT_TABLE = new ArrayList<>();
    private static final int MATCH_RADIUS = 8;

    static { loadExitTable(); }

    public static void loadExitTable() {
        EXIT_TABLE.clear();
        try {
            InputStream in = MazeExitTracker.class.getResourceAsStream("/assets/wynnmaze/exits.json");
            if (in == null) { LOGGER.warn("[Wynnmaze] 找不到 exits.json"); return; }
            String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            in.close();
            int idx = 0;
            while (true) {
                int idStart = json.indexOf("\"id\"", idx);
                if (idStart < 0) break;
                int q1 = json.indexOf("\"", json.indexOf(":", idStart) + 1);
                int q2 = json.indexOf("\"", q1 + 1);
                if (q1 < 0 || q2 < 0) break;
                String id = json.substring(q1 + 1, q2);
                int xIdx = json.indexOf("\"x\"", q2);
                int zIdx = json.indexOf("\"z\"", q2);
                if (xIdx < 0 || zIdx < 0) break;
                int x = Integer.parseInt(json.substring(json.indexOf(":", xIdx) + 1, json.indexOf(",", xIdx)).trim());
                int z = Integer.parseInt(json.substring(json.indexOf(":", zIdx) + 1, Math.min(json.indexOf(",", zIdx) < 0 ? json.length() : json.indexOf(",", zIdx), json.indexOf("}", zIdx))).trim());
                EXIT_TABLE.add(new ExitEntry(id, x, z));
                LOGGER.info("[Wynnmaze] Loaded exit: {} -> ({}, {})", id, x, z);
                idx = zIdx + 1;
            }
            LOGGER.info("[Wynnmaze] Loaded {} exits total", EXIT_TABLE.size());
        } catch (Exception e) {
            LOGGER.error("[Wynnmaze] Failed to load exits.json", e);
        }
    }

    private static String extractString(String obj, String key) {
        String k = "\"" + key + "\"";
        int i = obj.indexOf(k);
        if (i < 0) return null;
        int q1 = obj.indexOf('"', obj.indexOf(':', i) + 1);
        if (q1 < 0) return null;
        int q2 = obj.indexOf('"', q1 + 1);
        return q2 > q1 ? obj.substring(q1 + 1, q2) : null;
    }

    private static int extractInt(String obj, String key, int def) {
        String k = "\"" + key + "\"";
        int i = obj.indexOf(k);
        if (i < 0) return def;
        int colon = obj.indexOf(':', i);
        if (colon < 0) return def;
        StringBuilder sb = new StringBuilder();
        for (int j = colon + 1; j < obj.length(); j++) {
            char c = obj.charAt(j);
            if (Character.isDigit(c) || c == '-') sb.append(c);
            else if (sb.length() > 0) break;
        }
        try { return Integer.parseInt(sb.toString()); } catch (Exception e) { return def; }
    }

    public static int getExitCount() { return EXIT_TABLE.size(); }

    /** 找 radius 格內的出口，找不到回傳 null */
    public static String matchExitNear(int x, int z, int radius) {
        for (ExitEntry e : EXIT_TABLE) {
            if (Math.abs(e.x - x) <= radius && Math.abs(e.z - z) <= radius) return e.id;
        }
        return null;
    }

    /** 根據叢聚座標查出口名稱，找不到回傳 null */
    public static String matchExit(double x, double z) {
        ExitEntry best = null;
        double bestDist = Double.MAX_VALUE;
        for (ExitEntry e : EXIT_TABLE) {
            double dx = e.x - x, dz = e.z - z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < bestDist) { bestDist = d; best = e; }
        }
        return (best != null && bestDist <= MATCH_RADIUS) ? best.id : null;
    }

    private static class ExitEntry {
        final String id;
        final int x, z;
        ExitEntry(String id, int x, int z) { this.id = id; this.x = x; this.z = z; }
    }

    // ── 迷宮偵測 ────────────────────────────────────

    private static final double ENTRY_X = 10936.8;
    private static final double ENTRY_Y = 45.0;
    private static final double ENTRY_Z = 3478.7;
    private static final double ENTRY_TRIGGER_XZ   = 15.0;
    private static final double ENTRY_TRIGGER_YMIN = ENTRY_Y - 5.0;
    private static final double ENTRY_TRIGGER_YMAX = ENTRY_Y + 10.0;
    private static final double MAZE_EXTENT_XZ = 200.0;
    private static final double MAZE_YMIN      = ENTRY_Y - 10.0;
    private static final double MAZE_YMAX      = ENTRY_Y + 80.0;
    private static final long   EXIT_MEMORY_MS = 30_000;
    private static final double SCAN_Y_MIN = 75.0;
    private static final double SCAN_Y_MAX = 80.0;

    private final AtomicReference<Vec3d> detectedExitPos = new AtomicReference<>(null);
    private volatile long exitDetectedTime = 0;

    private boolean isInMaze = false;
    private boolean justEnteredMaze = false;
    private long mazeEnteredTime = 0;

    private static final int PARTICLE_BUF_SIZE = 128;
    private final double[] pxBuf = new double[PARTICLE_BUF_SIZE];
    private final double[] pyBuf = new double[PARTICLE_BUF_SIZE];
    private final double[] pzBuf = new double[PARTICLE_BUF_SIZE];
    private int pHead = 0, pCount = 0;

    private volatile double playerX, playerY, playerZ;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tcc-maze-analyzer");
            t.setDaemon(true);
            return t;
        });

    public MazeExitTracker() {
        scheduler.scheduleAtFixedRate(this::analyzeAsync, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) { isInMaze = false; return; }

        ClientPlayerEntity player = mc.player;
        playerX = player.getX(); playerY = player.getY(); playerZ = player.getZ();

        if (!isInMaze) {
            if (isNearEntry(playerX, playerY, playerZ)) {
                isInMaze = true;
                justEnteredMaze = true;
                mazeEnteredTime = System.currentTimeMillis();
                detectedExitPos.set(null);
                pCount = 0; pHead = 0;
                LOGGER.info("[Wynnmaze] Entered maze");
            }
        } else {
            if (!isInMazeBounds(playerX, playerY, playerZ)) {
                isInMaze = false;
                justEnteredMaze = false;
                LOGGER.info("[Wynnmaze] Left maze");
            }
        }

        if (detectedExitPos.get() != null &&
            System.currentTimeMillis() - exitDetectedTime > EXIT_MEMORY_MS)
            detectedExitPos.set(null);
    }

    public void onParticle(ParticleEffect effect, double x, double y, double z) {
        if (!isInMaze) return;
        String typeName = getTypeName(effect);

        if (typeName.equals("minecraft:firework") && y >= SCAN_Y_MIN && y <= SCAN_Y_MAX) {
            pxBuf[pHead] = x; pyBuf[pHead] = y; pzBuf[pHead] = z;
            pHead = (pHead + 1) % PARTICLE_BUF_SIZE;
            if (pCount < PARTICLE_BUF_SIZE) pCount++;
        }
    }

    private void analyzeAsync() {
        try {
            if (pCount < 3 || !isInMaze) return;
            int snap = Math.min(pCount, PARTICLE_BUF_SIZE);
            double[] sx = new double[snap], sy = new double[snap], sz = new double[snap];
            System.arraycopy(pxBuf, 0, sx, 0, snap);
            System.arraycopy(pyBuf, 0, sy, 0, snap);
            System.arraycopy(pzBuf, 0, sz, 0, snap);

            double bestX = 0, bestY = 0, bestZ = 0; int bestCount = 0;
            for (int i = 0; i < snap; i++) {
                int cnt = 0; double sumX = 0, sumY = 0, sumZ = 0;
                for (int j = 0; j < snap; j++) {
                    double dx = sx[i]-sx[j], dy = sy[i]-sy[j], dz = sz[i]-sz[j];
                    if (dx*dx+dy*dy+dz*dz < 16.0) { cnt++; sumX+=sx[j]; sumY+=sy[j]; sumZ+=sz[j]; }
                }
                if (cnt > bestCount) { bestCount=cnt; bestX=sumX/cnt; bestY=sumY/cnt; bestZ=sumZ/cnt; }
            }

            if (bestCount >= 3) {
                double dpx=bestX-playerX, dpy=bestY-playerY, dpz=bestZ-playerZ;
                if (dpx*dpx+dpy*dpy+dpz*dpz > 25.0) {
                    Vec3d candidate = new Vec3d(bestX, bestY, bestZ);
                    Vec3d current = detectedExitPos.get();
                    if (current == null || candidate.distanceTo(current) > 2.0) {
                        detectedExitPos.set(candidate);
                        exitDetectedTime = System.currentTimeMillis();
                        LOGGER.info("[Wynnmaze] Exit candidate: ({},{},{})", (int)bestX, (int)bestY, (int)bestZ);
                        notifyChat(candidate);
                    }
                }
            }
        } catch (Exception e) { LOGGER.error("[Wynnmaze] Analysis error", e); }
    }

    private void notifyChat(Vec3d pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int x = (int) pos.x;
        int z = (int) pos.z;

        String exitId = matchExit(pos.x, pos.z);
        String colorName = WayfindingManager.getColorName(exitId);
        String coordStr = x + ",47," + z;
        String shareMsg = colorName + " [" + coordStr + "]";
        String chatCmd = "/wynnmaze _share_ " + shareMsg.replace(" ", "\u00a0");
        final String finalExitId = exitId;
        final int fx = x, fz = z;
        final String fColorName = colorName;

        mc.execute(() -> {
            if (mc.player == null) return;
            // [Wynnmaze] 前綴：深灰底黑括號+紫字
            net.minecraft.text.MutableText prefix = net.minecraft.text.Text.literal("\u00a7a[WynnMaze]\u00a7r ");
            // 顏色名
            net.minecraft.text.MutableText colorPart = net.minecraft.text.Text.literal(fColorName + " ");
            colorPart.styled(s -> s.withColor(net.minecraft.text.TextColor.parse(fColorName.equals("red") ? "#FF5555" : fColorName.equals("green") ? "#55FF55" : fColorName.equals("blue") ? "#5555FF" : "#FFFF55").getOrThrow()));
            // 座標（可點擊，開啟 wynntils compass）
            net.minecraft.text.MutableText coordPart = net.minecraft.text.Text.literal("[" + fx + ",47," + fz + "]");
            coordPart.styled(s -> s
                .withColor(net.minecraft.text.TextColor.fromRgb(0x55FFFF))
                .withUnderline(true)
                .withClickEvent(new ClickEvent.SuggestCommand("/compass setbeacon " + fx + " 47 " + fz)));
            // 分享按鈕
            net.minecraft.text.MutableText shareBtn = net.minecraft.text.Text.literal("  \u00a7e\u00a7n[分享給隊伍]");
            shareBtn.styled(s -> s.withClickEvent(new ClickEvent.RunCommand(chatCmd)));

            net.minecraft.text.MutableText msg = prefix.append(colorPart).append(coordPart).append(shareBtn);
            mc.player.sendMessage(msg, false);
        });
    }

    private String getTypeName(ParticleEffect effect) {
        try { return Registries.PARTICLE_TYPE.getId(effect.getType()).toString(); }
        catch (Exception e) { return ""; }
    }

    private boolean isNearEntry(double x, double y, double z) {
        if (y < ENTRY_TRIGGER_YMIN || y > ENTRY_TRIGGER_YMAX) return false;
        return Math.abs(x-ENTRY_X) < ENTRY_TRIGGER_XZ && Math.abs(z-ENTRY_Z) < ENTRY_TRIGGER_XZ;
    }

    private boolean isInMazeBounds(double x, double y, double z) {
        if (y < MAZE_YMIN || y > MAZE_YMAX) return false;
        return Math.abs(x-ENTRY_X) < MAZE_EXTENT_XZ && Math.abs(z-ENTRY_Z) < MAZE_EXTENT_XZ;
    }

    public boolean isInMaze()        { return isInMaze; }
    public boolean justEnteredMaze() { return justEnteredMaze; }
    public long getMazeEnteredTime() { return mazeEnteredTime; }
    public @Nullable Vec3d getDetectedExitPos() { return detectedExitPos.get(); }

    public void setManualExit(Vec3d pos) {
        detectedExitPos.set(pos);
        exitDetectedTime = System.currentTimeMillis();
        notifyChat(pos);
    }

    public void clearExit() { detectedExitPos.set(null); }
}
