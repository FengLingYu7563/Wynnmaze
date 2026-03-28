package com.wynnmaze.util;

import java.util.*;

public class WayfindingManager {

    public static final double ENTRANCE_X = 10936;
    public static final double ENTRANCE_Z = 3478;

    private static final Map<String, String> COLOR_FIRST_ROAD = new HashMap<>();
    static {
        COLOR_FIRST_ROAD.put("B", "BR1");
        COLOR_FIRST_ROAD.put("G", "GR1");
        COLOR_FIRST_ROAD.put("R", "RR1");
        COLOR_FIRST_ROAD.put("Y", "YR1");
    }

    /**
     * 主要引路方法：根據出口 ID 和玩家位置，計算完整引路路徑。
     * 支援：同色路線中間開始、跨色回退、從路段中間插入起點。
     */
    public static List<double[]> getGuidePath(String exitId, double px, double pz) {
        List<String> targetRoute = RoadManager.ROUTES.get(exitId);
        if (targetRoute == null || targetRoute.isEmpty()) return null;

        // 1. 找玩家最近的路段
        String nearestRoad = findNearestRoad(px, pz);

        List<double[]> path = new ArrayList<>();

        if (nearestRoad == null) {
            // 找不到附近路段，從入口開始
            buildForward(path, targetRoute, 0, 0);
        } else {
            // 找玩家所在路段在目標路線中的位置
            int posInTarget = targetRoute.indexOf(nearestRoad);

            if (posInTarget >= 0) {
                // 玩家在目標路線上，從最近節點開始
                int nearestNode = findNearestNode(nearestRoad, px, pz);
                // 加入玩家當前位置作為起點
                path.add(new double[]{px, pz});
                // 從最近節點之後繼續
                List<int[]> pts = RoadManager.ROADS.get(nearestRoad);
                if (pts != null) {
                    for (int i = nearestNode; i < pts.size(); i++) {
                        path.add(new double[]{pts.get(i)[0], pts.get(i)[1]});
                    }
                }
                // 繼續後面的路段
                for (int ri = posInTarget + 1; ri < targetRoute.size(); ri++) {
                    List<int[]> rpts = RoadManager.ROADS.get(targetRoute.get(ri));
                    if (rpts == null) continue;
                    for (int[] p : rpts) path.add(new double[]{p[0], p[1]});
                }
            } else {
                // 玩家不在目標路線上，需要找分岔點回退
                // 找玩家所在的路線（包含 nearestRoad 的路線）
                List<String> playerRoute = findRouteContaining(nearestRoad);

                // 找目標路線和玩家路線的最長共同前綴
                int commonLen = 0;
                if (playerRoute != null) {
                    for (int i = 0; i < Math.min(playerRoute.size(), targetRoute.size()); i++) {
                        if (playerRoute.get(i).equals(targetRoute.get(i))) commonLen = i + 1;
                        else break;
                    }
                }

                // 加入玩家當前位置
                path.add(new double[]{px, pz});

                // 從玩家位置反向退到分岔點
                if (playerRoute != null) {
                    int playerRoadIdx = playerRoute.indexOf(nearestRoad);
                    int nearestNode = findNearestNode(nearestRoad, px, pz);

                    // 反向走完當前路段
                    List<int[]> curPts = RoadManager.ROADS.get(nearestRoad);
                    if (curPts != null) {
                        for (int i = nearestNode - 1; i >= 0; i--) {
                            path.add(new double[]{curPts.get(i)[0], curPts.get(i)[1]});
                        }
                    }

                    // 反向走回到分岔點
                    for (int ri = playerRoadIdx - 1; ri >= commonLen; ri--) {
                        List<int[]> rpts = RoadManager.ROADS.get(playerRoute.get(ri));
                        if (rpts == null) continue;
                        for (int i = rpts.size() - 1; i >= 0; i--) {
                            path.add(new double[]{rpts.get(i)[0], rpts.get(i)[1]});
                        }
                    }
                }

                // 如果完全沒有共同路段（跨色），插入迷宮入口作為橋接點
                if (commonLen == 0) {
                    path.add(new double[]{ENTRANCE_X, ENTRANCE_Z});
                }

                // 從分岔點往目標路線前進
                for (int ri = commonLen; ri < targetRoute.size(); ri++) {
                    List<int[]> rpts = RoadManager.ROADS.get(targetRoute.get(ri));
                    if (rpts == null) continue;
                    for (int[] p : rpts) path.add(new double[]{p[0], p[1]});
                }
            }
        }

        return path.isEmpty() ? null : path;
    }

    /** 找距離玩家最近的路段 ID */
    private static String findNearestRoad(double px, double pz) {
        String best = null;
        double bestDist = 25.0; // 25格內
        for (Map.Entry<String, List<int[]>> e : RoadManager.ROADS.entrySet()) {
            for (int[] p : e.getValue()) {
                double d = dist(p[0], p[1], px, pz);
                if (d < bestDist) { bestDist = d; best = e.getKey(); }
            }
        }
        return best;
    }

    /** 找路段中距離玩家最近的節點索引 */
    private static int findNearestNode(String roadId, double px, double pz) {
        List<int[]> pts = RoadManager.ROADS.get(roadId);
        if (pts == null) return 0;
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < pts.size(); i++) {
            double d = dist(pts.get(i)[0], pts.get(i)[1], px, pz);
            if (d < bestDist) { bestDist = d; best = i; }
        }
        return best;
    }

    /** 找包含某路段的路線（取最短的，代表玩家最可能在的那條） */
    private static List<String> findRouteContaining(String roadId) {
        List<String> best = null;
        for (List<String> route : RoadManager.ROUTES.values()) {
            if (route.contains(roadId)) {
                if (best == null || route.size() < best.size()) best = route;
            }
        }
        return best;
    }

    /** 從指定路段和節點開始，把後面的路段全部加入 path */
    private static void buildForward(List<double[]> path, List<String> route, int roadIdx, int nodeIdx) {
        for (int ri = roadIdx; ri < route.size(); ri++) {
            List<int[]> pts = RoadManager.ROADS.get(route.get(ri));
            if (pts == null) continue;
            int start = (ri == roadIdx) ? nodeIdx : 0;
            for (int i = start; i < pts.size(); i++) {
                path.add(new double[]{pts.get(i)[0], pts.get(i)[1]});
            }
        }
    }

    private static double dist(double x1, double z1, double x2, double z2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(z1-z2, 2));
    }

    public static String getColorName(String exitId) {
        if (exitId == null) return "unknown";
        switch (exitId.charAt(0)) {
            case 'R': return "red";
            case 'G': return "green";
            case 'B': return "blue";
            case 'Y': return "yellow";
            default: return exitId.toLowerCase();
        }
    }
}
