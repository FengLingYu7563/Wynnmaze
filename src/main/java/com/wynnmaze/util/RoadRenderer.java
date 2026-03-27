package com.wynnmaze.util;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

public class RoadRenderer {

    private static boolean showing = false;

    private static final RenderLayer ROAD_LAYER = RenderLayer.of(
        "tcc_road_lines",
        RenderSetup.builder(RenderPipelines.LINES).build()
    );

    public static boolean isShowing() { return showing; }
    public static void toggle() { showing = !showing; }

    public static void register() {
        WorldRenderEvents.END_MAIN.register(RoadRenderer::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        if (!showing || RoadManager.ROADS.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

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

        float[][] colors = {
            {1f, 0.3f, 0.3f}, {0.3f, 1f, 0.3f}, {0.3f, 0.5f, 1f},
            {1f, 1f, 0.3f}, {1f, 0.5f, 0f}, {0.8f, 0.3f, 1f},
        };

        int colorIdx = 0;
        for (Map.Entry<String, List<int[]>> entry : RoadManager.ROADS.entrySet()) {
            float[] c = colors[colorIdx % colors.length];
            colorIdx++;
            List<int[]> points = entry.getValue();
            if (points.size() < 2) continue;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(
                ROAD_LAYER.getDrawMode(),
                ROAD_LAYER.getVertexFormat()
            );

            for (int i = 0; i + 1 < points.size(); i++) {
                int[] a = points.get(i);
                int[] b = points.get(i + 1);
                float dx = b[0] - a[0], dz = b[1] - a[1];
                float len = (float) Math.sqrt(dx*dx + dz*dz);
                float nx = len > 0 ? dx/len : 0, nz = len > 0 ? dz/len : 0;
                buffer.vertex(matrix, a[0], 77f, a[1]).color(c[0], c[1], c[2], 1f).normal(nx, 0, nz).lineWidth(3f);
                buffer.vertex(matrix, b[0], 77f, b[1]).color(c[0], c[1], c[2], 1f).normal(nx, 0, nz).lineWidth(3f);
            }

            ROAD_LAYER.draw(buffer.end());
        }

        matrices.pop();
    }
}
