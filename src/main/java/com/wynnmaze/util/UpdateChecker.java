package com.wynnmaze.util;

import com.wynnmaze.WynnMazeClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 啟動時非同步檢查 GitHub Releases 上的最新版號。
 *
 * 設計刻意保守：
 *   - 只「比對」並「通知」，不會自動下載、不會替換 mod 檔。
 *     原因：在 Fabric 執行期間，正在使用的 .jar 在 Windows 上是被 lock 的，
 *     直接刪除 / 覆蓋會失敗；做 staging + 下次啟動置換的方案複雜度高、
 *     且容易因為網路 / 簽章 / 校驗等任一環節出錯導致玩家裝壞 mod。
 *   - 通知只在「玩家加入世界後」跳一次，不會洗螢幕。
 *   - 點擊通知會帶玩家到 release 頁面（OpenUrl ClickEvent），由玩家自行下載。
 *
 * 怎麼知道有新版？比對 GitHub API `releases/latest` 的 `tag_name` 與
 * fabric.mod.json 中的 version 字串。tag 通常會帶 "v" 前綴，比對時去掉。
 */
public class UpdateChecker {

    private static final String GITHUB_REPO = "FengLingYu7563/Wynnmaze";

    private static final String API_URL =
        "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    private static final AtomicReference<UpdateInfo> PENDING = new AtomicReference<>(null);
    private static volatile boolean notified = false;

    private static class UpdateInfo {
        final String latestTag;
        final String htmlUrl;
        UpdateInfo(String latestTag, String htmlUrl) {
            this.latestTag = latestTag;
            this.htmlUrl = htmlUrl;
        }
    }

    /** 啟動時呼叫，背景執行緒查詢 GitHub。失敗就靜默放過。 */
    public static void checkAsync() {
        Thread t = new Thread(UpdateChecker::doCheck, "wynnmaze-update-check");
        t.setDaemon(true);
        t.start();
    }

    private static void doCheck() {
        try {
            String currentVersion = getCurrentVersion();
            if (currentVersion == null) return;

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "wynnmaze-update-check")
                .GET()
                .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                WynnMazeClient.LOGGER.info("[Wynnmaze] Update check: HTTP {}", resp.statusCode());
                return;
            }

            String body = resp.body();
            String latestTag = extractJsonString(body, "tag_name");
            String htmlUrl   = extractJsonString(body, "html_url");
            if (latestTag == null || htmlUrl == null) return;

            String normalizedLatest = normalizeVersion(latestTag);
            String normalizedCurrent = normalizeVersion(currentVersion);

            if (compareVersions(normalizedLatest, normalizedCurrent) > 0) {
                WynnMazeClient.LOGGER.info(
                    "[Wynnmaze] Update available: {} (current {})", latestTag, currentVersion);
                PENDING.set(new UpdateInfo(latestTag, htmlUrl));
            } else {
                WynnMazeClient.LOGGER.info("[Wynnmaze] Up to date (current {})", currentVersion);
            }
        } catch (Exception e) {
            // 網路 / 解析失敗都靜默
            WynnMazeClient.LOGGER.debug("[Wynnmaze] Update check failed: {}", e.toString());
        }
    }

    /** 在 ClientTick 中呼叫，玩家進入世界後跳一次提示。 */
    public static void tickNotify(MinecraftClient mc) {
        if (notified) return;
        if (mc.player == null) return;
        UpdateInfo info = PENDING.get();
        if (info == null) return;
        notified = true;

        final String tag = info.latestTag;
        final String url = info.htmlUrl;

        MutableText msg = Text.literal("\u00a7a[WynnMaze]\u00a7r ")
            .append(Text.literal(LangManager.updateAvailable(tag) + " ")
                .styled(s -> s.withColor(TextColor.fromRgb(0xFFFF55))))
            .append(Text.literal("[" + LangManager.openReleasePage() + "]")
                .styled(s -> s
                    .withColor(TextColor.fromRgb(0x55FFFF))
                    .withUnderline(true)
                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))));
        mc.player.sendMessage(msg, false);
    }

    private static String getCurrentVersion() {
        try {
            return FabricLoader.getInstance()
                .getModContainer(WynnMazeClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** "v1.2.3" → "1.2.3"，去前後空白 */
    private static String normalizeVersion(String v) {
        if (v == null) return "";
        String t = v.trim();
        if (t.startsWith("v") || t.startsWith("V")) t = t.substring(1);
        return t;
    }

    /** 數字段比較。a > b 回正、相等 0、小於回負。非數字段做字典比較。 */
    private static int compareVersions(String a, String b) {
        String[] pa = a.split("[.-]");
        String[] pb = b.split("[.-]");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            String sa = i < pa.length ? pa[i] : "0";
            String sb = i < pb.length ? pb[i] : "0";
            try {
                int ia = Integer.parseInt(sa);
                int ib = Integer.parseInt(sb);
                if (ia != ib) return Integer.compare(ia, ib);
            } catch (NumberFormatException e) {
                int c = sa.compareTo(sb);
                if (c != 0) return c;
            }
        }
        return 0;
    }

    /** 從 JSON 抓 "key": "value"。簡單但夠用，避免拉 gson 進來 (其實也已存在)。 */
    private static String extractJsonString(String json, String key) {
        String needle = "\"" + key + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon);
        if (q1 < 0) return null;
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int p = q1 + 1; p < json.length(); p++) {
            char c = json.charAt(p);
            if (esc) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    default:  sb.append(c);
                }
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return null;
    }
}
