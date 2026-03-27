package com.wynnmaze.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RoadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("wynnmaze");

    public static final Map<String, List<int[]>> ROADS = new LinkedHashMap<>();
    public static final Map<String, List<String>> ROUTES = new LinkedHashMap<>();

    public static void reload() {
        ROADS.clear();
        ROUTES.clear();
        loadRoads();
        loadRoutes();
        LOGGER.info("[Wynnmaze] 路段: {} 條，路線: {} 條", ROADS.size(), ROUTES.size());
    }

    private static String readResource(String name) {
        try (InputStream in = RoadManager.class.getResourceAsStream("/assets/wynnmaze/" + name)) {
            if (in == null) { LOGGER.warn("[Wynnmaze] 找不到資源: {}", name); return null; }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { LOGGER.error("[Wynnmaze] 讀取 {} 失敗", name, e); return null; }
    }

    private static void loadRoads() {
        String json = readResource("roads.json");
        if (json == null) return;
        int idx = 0;
        while (true) {
            int idStart = json.indexOf("\"id\"", idx);
            if (idStart < 0) break;
            int q1 = json.indexOf("\"", json.indexOf(":", idStart) + 1);
            int q2 = json.indexOf("\"", q1 + 1);
            if (q1 < 0 || q2 < 0) break;
            String id = json.substring(q1 + 1, q2);
            int pStart = json.indexOf("\"points\"", q2);
            if (pStart < 0) break;
            int arrStart = json.indexOf("[", pStart);
            int arrEnd = json.indexOf("]", json.indexOf("]", arrStart) + 1);
            if (arrStart < 0 || arrEnd < 0) break;
            String pts = json.substring(arrStart + 1, arrEnd);
            List<int[]> points = new ArrayList<>();
            int pi = 0;
            while (true) {
                int ps = pts.indexOf("[", pi);
                int pe = pts.indexOf("]", ps + 1);
                if (ps < 0 || pe < 0) break;
                String pair = pts.substring(ps + 1, pe).trim();
                String[] nums = pair.split(",");
                if (nums.length >= 2) {
                    try { points.add(new int[]{Integer.parseInt(nums[0].trim()), Integer.parseInt(nums[1].trim())}); }
                    catch (Exception ignored) {}
                }
                pi = pe + 1;
            }
            ROADS.put(id, points);
            idx = arrEnd + 1;
        }
    }

    private static void loadRoutes() {
        String json = readResource("routes.json");
        if (json == null) return;
        int idx = 0;
        while (true) {
            int exitStart = json.indexOf("\"exit\"", idx);
            if (exitStart < 0) break;
            int q1 = json.indexOf("\"", json.indexOf(":", exitStart) + 1);
            int q2 = json.indexOf("\"", q1 + 1);
            if (q1 < 0 || q2 < 0) break;
            String exit = json.substring(q1 + 1, q2);
            int roadsStart = json.indexOf("\"roads\"", q2);
            if (roadsStart < 0) break;
            int arrStart = json.indexOf("[", roadsStart);
            int arrEnd = json.indexOf("]", arrStart);
            if (arrStart < 0 || arrEnd < 0) break;
            String arr = json.substring(arrStart + 1, arrEnd);
            List<String> roads = new ArrayList<>();
            for (String s : arr.split(",")) {
                String r = s.trim().replace("\"", "");
                if (!r.isEmpty()) roads.add(r);
            }
            ROUTES.put(exit, roads);
            idx = arrEnd + 1;
        }
    }
}
