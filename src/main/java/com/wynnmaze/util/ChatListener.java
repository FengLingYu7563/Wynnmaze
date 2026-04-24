package com.wynnmaze.util;

import com.wynnmaze.WynnMazeClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 接收端：偵測隊友在迷宮中分享的「顏色 + 座標」，
 * 只有在三條件全部滿足時才自動展示路徑，避免誤報：
 *
 *   (1) 訊息含有有效顏色字串（red/green/blue/yellow，多語言別名也接受）
 *   (2) 訊息含有 Wynntils 格式座標 [x, y, z]
 *   (3) 該座標落在某出口的 5 格內，且該顏色與該出口顏色一致
 *       → 才視為「對方確實已抵達正確出口」
 *
 * 為了支援同一玩家「先打顏色、再貼座標」這類跨訊息情境，
 * 每位玩家在迷宮中維護一個暫存記錄；進出迷宮時清空。
 */
public class ChatListener {

    /** Wynntils 預設座標格式 [x, y, z]。 */
    private static final Pattern COORD_PATTERN =
        Pattern.compile("\\[\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\]");

    /** 顏色關鍵字 → 內部標準色名。涵蓋常見英中縮寫。 */
    private static final Map<String, String> COLOR_KEYWORDS = new HashMap<>();
    static {
        // red
        for (String k : List.of("red", "r", "紅", "红", "赤", "빨", "rot", "rouge", "rojo", "vermelho", "czerw"))
            COLOR_KEYWORDS.put(k, "red");
        // green
        for (String k : List.of("green", "g", "綠", "绿", "緑", "초록", "녹", "grün", "vert", "verde", "ziel"))
            COLOR_KEYWORDS.put(k, "green");
        // blue
        for (String k : List.of("blue", "b", "藍", "蓝", "青", "파", "푸른", "blau", "bleu", "azul", "nieb"))
            COLOR_KEYWORDS.put(k, "blue");
        // yellow
        for (String k : List.of("yellow", "y", "黃", "黄", "黄色", "노랑", "노란", "gelb", "jaune", "amarillo", "amarelo", "żółt"))
            COLOR_KEYWORDS.put(k, "yellow");
    }

    /** 每位玩家在當前迷宮回合中暫存的最近一次顏色與座標。 */
    private static class PlayerInfo {
        String color;       // "red"/"green"/"blue"/"yellow" 或 null
        int[] coords;       // [x, y, z] 或 null
        boolean validated;  // 已通過驗證並自動顯示過路徑
    }

    /** key = sender 名稱（盡力解析；解析不到時用整段 hash 作為 key） */
    private static final Map<String, PlayerInfo> PLAYER_INFO = new HashMap<>();

    /** 進入迷宮時呼叫。 */
    public static void resetRound() {
        synchronized (PLAYER_INFO) { PLAYER_INFO.clear(); }
    }

    /** 離開迷宮時呼叫，釋放暫存空間。 */
    public static void onLeftMaze() {
        synchronized (PLAYER_INFO) { PLAYER_INFO.clear(); }
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (!WynnMazeClient.EXIT_TRACKER.isInMaze()) return;

            String raw = message.getString();
            // 跳過模組自己發的訊息
            if (raw.contains("[WynnMaze]")) return;

            // 不採信自己打的訊息（避免自己 wynntils 打座標就被當成「隊友」）
            String selfName = mc.player.getName().getString();

            // 解析 sender（盡力而為，失敗就用訊息本身的 hash 做 key）
            String sender = parseSender(raw, selfName);
            if (sender == null) {
                // 用內容區段 hash 做退化 key，仍能跨訊息合併同一玩家短時間內的兩條
                sender = "anon_" + Math.abs(raw.hashCode() % 100000);
            }
            if (sender.equalsIgnoreCase(selfName)) return;

            // 解析這條訊息中的顏色 + 座標（任一存在即更新）
            String parsedColor = extractColor(raw);
            int[] parsedCoords = extractCoords(raw);
            if (parsedColor == null && parsedCoords == null) return;

            PlayerInfo info;
            synchronized (PLAYER_INFO) {
                info = PLAYER_INFO.computeIfAbsent(sender, k -> new PlayerInfo());
                if (info.validated) return;
                if (parsedColor != null) info.color = parsedColor;
                if (parsedCoords != null) info.coords = parsedCoords;
            }

            tryValidateAndShow(mc, sender, info);
        });
    }

    /** 三條件驗證，全部通過才自動顯示路徑。 */
    private static void tryValidateAndShow(MinecraftClient mc, String sender, PlayerInfo info) {
        String color;
        int cx, cz;
        synchronized (PLAYER_INFO) {
            if (info.color == null || info.coords == null) return;
            color = info.color;
            cx = info.coords[0];
            cz = info.coords[2];
        }

        // (3) 座標距某出口 ≤5 格
        String exitId = MazeExitTracker.matchExitNear(cx, cz, 5);
        if (exitId == null) return;

        // (1)+(3) 顏色與該出口顏色一致
        String exitColor = WayfindingManager.getColorName(exitId);
        if (!exitColor.equals(color)) return;

        // 三條件通過 → 標記為已驗證 → 自動展示路徑
        synchronized (PLAYER_INFO) { info.validated = true; }

        final String fExitId = exitId;
        final String fSender = sender;
        final String fColor = exitColor;

        mc.execute(() -> {
            if (mc.player == null) return;
            double px = mc.player.getX(), pz = mc.player.getZ();
            List<double[]> path = WayfindingManager.getGuidePath(fExitId, px, pz);
            if (path == null || path.isEmpty()) return;

            GuideRenderer.setGuidePath(path, fExitId);
            // 同步給 ExitTracker，這樣按快捷鍵能重新顯示
            WynnMazeClient.EXIT_TRACKER.setConfirmedExitId(fExitId);

            int colorInt = colorRgb(fColor);
            MutableText msg = Text.literal("\u00a7a[WynnMaze]\u00a7r " + LangManager.routeFromTeammate(fSender))
                .append(Text.literal(fColor).styled(s -> s.withColor(TextColor.fromRgb(colorInt))));
            mc.player.sendMessage(msg, false);
        });
    }

    /**
     * 解析 sender 名稱。Wynncraft 的隊伍頻常見格式：
     *   "Party » NickName: msg"
     *   "[1] Party » NickName: msg"
     *   "<NickName> msg"
     * 抓 ":" 之前的最後一個 word 當 sender。失敗回 null。
     */
    private static String parseSender(String raw, String selfName) {
        int colon = raw.indexOf(':');
        if (colon <= 0) return null;
        String before = raw.substring(0, colon).trim();
        // 取最後一段非空白字元做 sender
        int sp = Math.max(before.lastIndexOf(' '), before.lastIndexOf('»'));
        String name = sp >= 0 ? before.substring(sp + 1).trim() : before;
        // Wynncraft 通常會有 rank prefix 顏色碼，這裡簡單把標點清掉
        name = name.replaceAll("[<>\\[\\]()]", "").trim();
        if (name.isEmpty()) return null;
        return name;
    }

    /** 取訊息中第一個合法顏色關鍵字。支援部分匹配 (yel→yellow) 與拼寫容錯 (yellw→yellow)。 */
    private static String extractColor(String raw) {
        String lowerRaw = raw.toLowerCase();
        
        // 第一輪：精確匹配（包含多語言別名）
        String[] tokens = lowerRaw.split("[\\s,;:.!?\\[\\]()]+");
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            String c = COLOR_KEYWORDS.get(t);
            if (c != null) return c;
        }
        
        // 第二輪：英文顏色的模糊匹配（前綴 + 拼寫容錯）
        for (String t : tokens) {
            if (t.length() < 2) continue; // 至少 2 字元才匹配
            String fuzzy = fuzzyMatchColor(t);
            if (fuzzy != null) return fuzzy;
        }
        
        // 第三輪：中日字元子字串掃描（這些語言不會被 split 拆開）
        for (Map.Entry<String, String> e : COLOR_KEYWORDS.entrySet()) {
            String k = e.getKey();
            if (k.length() <= 2) continue; // 短英文已在 token 裡處理
            if (lowerRaw.contains(k)) return e.getValue();
        }
        
        // 第四輪：單字元中文顏色字
        for (Map.Entry<String, String> e : COLOR_KEYWORDS.entrySet()) {
            String k = e.getKey();
            if (k.length() == 1 && lowerRaw.contains(k)) return e.getValue();
        }
        
        return null;
    }
    
    /**
     * 模糊匹配英文顏色名。支援：
     *   - 前綴匹配：yel/yello/yel... → yellow (至少 2 字元)
     *   - 拼寫容錯：編輯距離 ≤1 (yellw/yello/yelow → yellow)
     */
    private static String fuzzyMatchColor(String token) {
        String[] standardColors = {"red", "green", "blue", "yellow"};
        
        for (String color : standardColors) {
            // 前綴匹配（至少 2 字元）
            if (token.length() >= 2 && color.startsWith(token)) {
                return color;
            }
            
            // 編輯距離 ≤1（允許一個拼錯/遺漏）
            if (editDistance(token, color) <= 1) {
                return color;
            }
        }
        
        return null;
    }
    
    /** Levenshtein 編輯距離（簡化版，只計算到 2 就放棄） */
    private static int editDistance(String s1, String s2) {
        int len1 = s1.length(), len2 = s2.length();
        
        // 長度差太多就不用算了
        if (Math.abs(len1 - len2) > 2) return 999;
        
        int[] prev = new int[len2 + 1];
        int[] curr = new int[len2 + 1];
        
        for (int j = 0; j <= len2; j++) prev[j] = j;
        
        for (int i = 1; i <= len1; i++) {
            curr[0] = i;
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                    Math.min(prev[j] + 1, curr[j - 1] + 1),
                    prev[j - 1] + cost
                );
                // 提早放棄
                if (curr[j] > 2) curr[j] = 999;
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        
        return prev[len2];
    }

    /** 取訊息中第一組 [x,y,z] 座標。 */
    private static int[] extractCoords(String raw) {
        Matcher m = COORD_PATTERN.matcher(raw);
        if (!m.find()) return null;
        try {
            return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3))
            };
        } catch (NumberFormatException e) {
            return null;
        }
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
}
