package com.wynnmaze.util;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class GuideRenderer {

    private static List<double[]> guidePath = null;
    private static String guideExitId = null;


    private static final float GUIDE_Y = 50f;

    private static final RenderLayer GUIDE_LAYER = RenderLayer.of(
        "wynnmaze_guide_lines",
        RenderSetup.builder(RenderPipelines.LINES).build()
    );
    private static final RenderLayer ARROW_LAYER = RenderLayer.of(
        "wynnmaze_arrow_lines",
        RenderSetup.builder(RenderPipelines.LINES).build()
    );

    public static void setGuidePath(List<double[]> path, String exitId) {
        guidePath = new ArrayList<>(path);
        guideExitId = exitId;
    }

    public static void clearGuidePath() {
        guidePath = null;
        guideExitId = null;
    }

    public static boolean isActive() { return guidePath != null && !guidePath.isEmpty(); }

    public static void reloadConfig() {
        GuideConfig.load();
    }

    public static void register() {
        WorldRenderEvents.END_MAIN.register(GuideRenderer::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        if (guidePath == null || guidePath.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        double px = mc.player.getX(), pz = mc.player.getZ();

        // 每幀重算最近節點，從那裡開始顯示
        int nearest = 0;
        double nearestDist = Double.MAX_VALUE;
        for (int i = 0; i < guidePath.size(); i++) {
            double[] p = guidePath.get(i);
            double d = Math.sqrt(Math.pow(p[0]-px, 2) + Math.pow(p[1]-pz, 2));
            if (d < nearestDist) { nearestDist = d; nearest = i; }
        }
        List<double[]> visible = guidePath.subList(nearest, guidePath.size());
        if (visible.size() < 2) return;

        MatrixStack matrices = context.matrices();
        if (matrices == null) return;

        Vec3d camPos = new Vec3d(
            context.worldState().cameraRenderState.pos.x,
            context.worldState().cameraRenderState.pos.y,
            context.worldState().cameraRenderState.pos.z
        );

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        GuideConfig cfg = GuideConfig.get();

        // 畫路線
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(GUIDE_LAYER.getDrawMode(), GUIDE_LAYER.getVertexFormat());
        boolean hasVertex = false;

        for (int i = 0; i + 1 < visible.size(); i++) {
            double[] a = visible.get(i);
            double[] b = visible.get(i + 1);
            float dx = (float)(b[0]-a[0]), dz = (float)(b[1]-a[1]);
            float len = (float)Math.sqrt(dx*dx+dz*dz);
            float nx = len>0?dx/len:0, nz = len>0?dz/len:0;
            buf.vertex(matrix, (float)a[0], GUIDE_Y, (float)a[1]).color(cfg.lineR, cfg.lineG, cfg.lineB, cfg.lineAlpha).normal(nx, 0, nz).lineWidth(cfg.lineWidth);
            buf.vertex(matrix, (float)b[0], GUIDE_Y, (float)b[1]).color(cfg.lineR, cfg.lineG, cfg.lineB, cfg.lineAlpha).normal(nx, 0, nz).lineWidth(cfg.lineWidth);
            hasVertex = true;
        }
        if (hasVertex) GUIDE_LAYER.draw(buf.end());

        // 畫箭頭
        drawArrows(matrix, visible, cfg, tess);

        matrices.pop();
    }

    private static void drawArrows(Matrix4f matrix, List<double[]> path, GuideConfig cfg, Tessellator tess) {
        float distSinceLastArrow = 0;
        for (int i = 0; i + 1 < path.size(); i++) {
            double[] a = path.get(i);
            double[] b = path.get(i + 1);
            float dx = (float)(b[0]-a[0]), dz = (float)(b[1]-a[1]);
            float segLen = (float)Math.sqrt(dx*dx+dz*dz);
            if (segLen < 0.001f) continue;
            float nx = dx/segLen, nz = dz/segLen;
            float lx = -nz, lz = nx;
            distSinceLastArrow += segLen;
            if (distSinceLastArrow >= cfg.arrowSpacing) {
                distSinceLastArrow = 0;
                float cx = (float)((a[0]+b[0])/2);
                float cz = (float)((a[1]+b[1])/2);
                // 用角度計算箭翼
                float arrowLen = 2.0f;
                float angleRad = (float)Math.toRadians(cfg.arrowAngle);
                float wingLen = arrowLen / (float)Math.cos(angleRad);
                float tipX = cx + nx*arrowLen, tipZ = cz + nz*arrowLen;
                float leftX = tipX - nx*wingLen*(float)Math.cos(angleRad) + lx*wingLen*(float)Math.sin(angleRad);
                float leftZ = tipZ - nz*wingLen*(float)Math.cos(angleRad) + lz*wingLen*(float)Math.sin(angleRad);
                float rightX = tipX - nx*wingLen*(float)Math.cos(angleRad) - lx*wingLen*(float)Math.sin(angleRad);
                float rightZ = tipZ - nz*wingLen*(float)Math.cos(angleRad) - lz*wingLen*(float)Math.sin(angleRad);
                float alx = leftX-tipX, alz = leftZ-tipZ;
                float aln = (float)Math.sqrt(alx*alx+alz*alz);
                float anx = aln>0?alx/aln:0, anz = aln>0?alz/aln:0;
                BufferBuilder arrowBuf = tess.begin(ARROW_LAYER.getDrawMode(), ARROW_LAYER.getVertexFormat());
                arrowBuf.vertex(matrix, tipX, GUIDE_Y+0.2f, tipZ).color(cfg.arrowR, cfg.arrowG, cfg.arrowB, cfg.arrowAlpha).normal(anx, 0, anz).lineWidth(cfg.arrowWidth);
                arrowBuf.vertex(matrix, leftX, GUIDE_Y+0.2f, leftZ).color(cfg.arrowR, cfg.arrowG, cfg.arrowB, cfg.arrowAlpha).normal(anx, 0, anz).lineWidth(cfg.arrowWidth);
                arrowBuf.vertex(matrix, tipX, GUIDE_Y+0.2f, tipZ).color(cfg.arrowR, cfg.arrowG, cfg.arrowB, cfg.arrowAlpha).normal(-anx, 0, -anz).lineWidth(cfg.arrowWidth);
                arrowBuf.vertex(matrix, rightX, GUIDE_Y+0.2f, rightZ).color(cfg.arrowR, cfg.arrowG, cfg.arrowB, cfg.arrowAlpha).normal(-anx, 0, -anz).lineWidth(cfg.arrowWidth);
                ARROW_LAYER.draw(arrowBuf.end());
            }
        }
    }
}
