package com.wynnmaze.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 偵測迷宮、掃描出口粒子、計算候選出口。
 *
 * 行為（合規版）：
 *   - 掃描結果 *只* 暫存於模組內部（pendingExitId）。
 *     不會自動發送任何聊天訊息給隊伍，也不會在玩家螢幕上顯示
 *     候選出口的顏色或座標 ─ 直到玩家本人實際走到「正確出口」5 格內為止。
 *   - 玩家走到 5 格內 → confirmedExitId 設定 → 訊息提示玩家本人，玩家可自行
 *     用 Wynntils 把座標分享給隊伍。
 *   - 接收端的驗證 / 顯示由 ChatListener 處理。
 */
public class MazeExitTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("wynnmaze");

    // ── 出口表（從 exits.json 載入）──────────────────────────
    private static final List<ExitEntry> EXIT_TABLE = new ArrayList<>();
    private static final int MATCH_RADIUS = 15;            // 粒子叢聚 → 出口比對半徑
    private static final int EXIT_REVEAL_RADIUS = 5;       // 玩家走到出口 N 格內才公開
    private static final int EXIT_REVEAL_RADIUS_SQ = EXIT_REVEAL_RADIUS * EXIT_REVEAL_RADIUS;

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

    /** 找 radius 格內的出口，找不到回傳 null。供 ChatListener 驗證接收端訊息使用。 */
    public static String matchExitNear(int x, int z, int radius) {
        for (ExitEntry e : EXIT_TABLE) {
            int dx = e.x - x, dz = e.z - z;
            if (dx*dx + dz*dz <= radius * radius) return e.id;
        }
        return null;
    }

    /** 根據叢聚座標查出口名稱（內部掃描用），找不到回傳 null。 */
    private static String matchExit(double x, double z) {
        ExitEntry best = null;
        double bestDist = Double.MAX_VALUE;
        for (ExitEntry e : EXIT_TABLE) {
            double dx = e.x - x, dz = e.z - z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < bestDist) { bestDist = d; best = e; }
        }
        return (best != null && bestDist <= MATCH_RADIUS) ? best.id : null;
    }

    private static ExitEntry findExitById(String id) {
        if (id == null) return null;
        for (ExitEntry e : EXIT_TABLE) if (e.id.equals(id)) return e;
        return null;
    }

    private static class ExitEntry {
        final String id;
        final int x, z;
        ExitEntry(String id, int x, int z) { this.id = id; this.x = x; this.z = z; }
    }

    // ── 迷宮幾何 ────────────────────────────────────
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

    // ── 內部狀態 ────────────────────────────────────
    /** 候選出口位置（粒子叢聚算出來的），純內部用，不對外暴露。 */
    private final AtomicReference<Vec3d> detectedExitPos = new AtomicReference<>(null);
    private volatile long exitDetectedTime = 0;

    /** 掃描偵測到、尚未經玩家親自靠近確認的出口 ID（不公開）。 */
    private volatile String pendingExitId = null;

    /** 已經公開的出口 ID（玩家走到 5 格內、或隊友驗證通過後才設定）。 */
    private volatile String confirmedExitId = null;

    private boolean isInMaze = false;
    private boolean justEnteredMaze = false;
    private boolean justLeftMaze = false;
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
        // 每 tick 開始把瞬時旗標重置（justEntered/justLeft 是「上一 tick 的事件」）
        justEnteredMaze = false;
        justLeftMaze = false;

        if (mc.player == null || mc.world == null) {
            if (isInMaze) {
                isInMaze = false;
                justLeftMaze = true;
                resetMazeState();
            }
            return;
        }

        ClientPlayerEntity player = mc.player;
        playerX = player.getX(); playerY = player.getY(); playerZ = player.getZ();

        if (!isInMaze) {
            if (isNearEntry(playerX, playerY, playerZ)) {
                isInMaze = true;
                justEnteredMaze = true;
                mazeEnteredTime = System.currentTimeMillis();
                resetMazeState();
                LOGGER.info("[Wynnmaze] Entered maze");
            }
        } else {
            if (!isInMazeBounds(playerX, playerY, playerZ)) {
                isInMaze = false;
                justLeftMaze = true;
                resetMazeState();
                LOGGER.info("[Wynnmaze] Left maze");
            } else {
                // 玩家還在迷宮中 → 檢查是否走到 pendingExit 5 格內
                checkPendingExitProximity(mc);
            }
        }

        // 出口候選位置記憶過期清掉（純內部）
        if (detectedExitPos.get() != null &&
            System.currentTimeMillis() - exitDetectedTime > EXIT_MEMORY_MS) {
            detectedExitPos.set(null);
        }
    }

    /** 進入或離開迷宮時把暫存清乾淨。 */
    private void resetMazeState() {
        detectedExitPos.set(null);
        pendingExitId = null;
        confirmedExitId = null;
        pCount = 0; pHead = 0;
    }

    /** 玩家走到 pendingExit 5 格內 → 公開給玩家本人（不發給隊伍）。 */
    private void checkPendingExitProximity(MinecraftClient mc) {
        String pid = pendingExitId;
        if (pid == null) return;
        ExitEntry exit = findExitById(pid);
        if (exit == null) return;
        double dx = exit.x - playerX, dz = exit.z - playerZ;
        if (dx*dx + dz*dz <= EXIT_REVEAL_RADIUS_SQ) {
            // 確認：清掉 pending、設 confirmed、通知玩家本人
            pendingExitId = null;
            confirmedExitId = pid;
            revealExitToSelf(mc, pid);
        }
    }

    /** 顯示給玩家本人看（不發隊伍）。 */
    private void revealExitToSelf(MinecraftClient mc, String exitId) {
        String colorName = WayfindingManager.getColorName(exitId);
        int colorInt = colorRgb(colorName);
        ExitEntry exit = findExitById(exitId);
        final int fx = exit != null ? exit.x : 0;
        final int fz = exit != null ? exit.z : 0;

        mc.execute(() -> {
            if (mc.player == null) return;
            // [WynnMaze] 已找到出口！red [x,47,z]
            MutableText msg = Text.literal("\u00a7a[WynnMaze]\u00a7r " + LangManager.exitFound())
                .append(Text.literal(colorName).styled(s -> s.withColor(TextColor.fromRgb(colorInt))))
                .append(Text.literal(" [" + fx + ",47," + fz + "]")
                    .styled(s -> s.withColor(TextColor.fromRgb(0x55FFFF))));
            mc.player.sendMessage(msg, false);
        });
    }

    private static int colorRgb(String colorName) {
        return switch (colorName) {
            case "red"    -> 0xFF5555;
            case "green"  -> 0x55FF55;
            case "blue"   -> 0x5555FF;
            case "yellow" -> 0xFFFF55;
            default       -> 0xFFFFFF;
        };
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

                        // ★ 只暫存 pendingExitId，不發訊息、不顯示給玩家
                        String matched = matchExit(bestX, bestZ);
                        if (matched != null) {
                            pendingExitId = matched;
                            LOGGER.info("[Wynnmaze] Pending exit candidate stored internally: {}", matched);
                        }
                    }
                }
            }
        } catch (Exception e) { LOGGER.error("[Wynnmaze] Analysis error", e); }
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
    public boolean justLeftMaze()    { return justLeftMaze; }

    /**
     * 已公開的出口 ID（玩家走到 5 格內 / 隊友驗證通過）。
     * 在公開前回傳 null —— 即按下快捷鍵也不會洩漏資訊。
     */
    public String getLastDetectedExitId() { return confirmedExitId; }

    /**
     * 由接收端呼叫：當隊友訊息經過驗證（顏色 + 座標 + 距離）後，
     * 把該出口 ID 設為 confirmed，這樣按下快捷鍵就能重新顯示路徑。
     */
    public void setConfirmedExitId(String exitId) { this.confirmedExitId = exitId; }
}
