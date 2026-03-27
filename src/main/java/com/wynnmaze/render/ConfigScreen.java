package com.wynnmaze.render;

import com.wynnmaze.util.GuideConfig;
import com.wynnmaze.util.GuideRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    private final Screen parent;
    private final GuideConfig cfg;
    private float lineR, lineG, lineB;
    private float arrowR, arrowG, arrowB;

    private static final int LX_OFFSET = -180; // 左邊標籤 x 偏移
    private static final int CTRL_OFFSET = -10; // 控件 x 偏移

    public ConfigScreen(Screen parent) {
        super(Text.literal("Wynnmaze Config"));
        this.parent = parent;
        this.cfg = GuideConfig.get();
        lineR = cfg.lineR; lineG = cfg.lineG; lineB = cfg.lineB;
        arrowR = cfg.arrowR; arrowG = cfg.arrowG; arrowB = cfg.arrowB;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int ctrlX = cx + CTRL_OFFSET;
        int sliderW = 180;
        int y = 30;

        // 引路線顏色
        addDrawableChild(ButtonWidget.builder(Text.literal("選擇顏色"), btn ->
            client.setScreen(new ColorPickerScreen(this, lineR, lineG, lineB, rgb -> {
                lineR = rgb[0]; lineG = rgb[1]; lineB = rgb[2];
                cfg.lineR = lineR; cfg.lineG = lineG; cfg.lineB = lineB;
            }))
        ).dimensions(ctrlX, y, 100, 18).build());
        y += 26;

        // 引路線粗度
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.lineWidth / 10.0, v -> cfg.lineWidth = (float)(v * 10)));
        y += 26;

        // 引路線透明度
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.lineAlpha, v -> cfg.lineAlpha = (float)v));
        y += 36;

        // 箭頭顏色
        addDrawableChild(ButtonWidget.builder(Text.literal("選擇顏色"), btn ->
            client.setScreen(new ColorPickerScreen(this, arrowR, arrowG, arrowB, rgb -> {
                arrowR = rgb[0]; arrowG = rgb[1]; arrowB = rgb[2];
                cfg.arrowR = arrowR; cfg.arrowG = arrowG; cfg.arrowB = arrowB;
            }))
        ).dimensions(ctrlX, y, 100, 18).build());
        y += 26;

        // 箭頭粗度
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.arrowWidth / 10.0, v -> cfg.arrowWidth = (float)(v * 10)));
        y += 26;

        // 箭頭間距
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.arrowSpacing / 30.0, v -> cfg.arrowSpacing = (float)(v * 30)));
        y += 26;

        // 箭頭透明度
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.arrowAlpha, v -> cfg.arrowAlpha = (float)v));
        y += 36;

        // 儲存 / 取消
        addDrawableChild(ButtonWidget.builder(Text.literal("儲存"), btn -> {
            cfg.save();
            GuideRenderer.reloadConfig();
            close();
        }).dimensions(cx - 105, y, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), btn -> close())
            .dimensions(cx + 5, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xCC000000);
        ctx.drawCenteredTextWithShadow(textRenderer, "Wynnmaze 設定", width / 2, 10, 0xFFFFFFFF);

        int cx = width / 2;
        int labelX = cx + LX_OFFSET;
        int y = 30;

        // 畫標籤（在 super.render 之前，這樣 widget 在標籤上方）
        ctx.drawTextWithShadow(textRenderer, "引路線 顏色", labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, "引路線 粗度", labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, "引路線 透明度", labelX, y + 4, 0xFFFFFFFF);
        y += 36;
        ctx.drawTextWithShadow(textRenderer, "箭頭 顏色", labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, "箭頭 粗度", labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, "箭頭 間距", labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, "箭頭 透明度", labelX, y + 4, 0xFFFFFFFF);

        super.render(ctx, mouseX, mouseY, delta);

        // 顏色預覽（在 super.render 之後）
        int ctrlX = cx + CTRL_OFFSET;
        int previewX = ctrlX + 104;
        drawPreview(ctx, lineR, lineG, lineB, previewX, 30);
        drawPreview(ctx, arrowR, arrowG, arrowB, previewX, 30 + 26 + 26 + 36);
    }

    private void drawPreview(DrawContext ctx, float r, float g, float b, int x, int y) {
        int col = 0xFF000000 | ((int)(r*255)<<16) | ((int)(g*255)<<8) | (int)(b*255);
        ctx.fill(x, y, x+18, y+18, col);
        ctx.fill(x, y, x+18, y+1, 0xFFFFFFFF);
        ctx.fill(x, y+17, x+18, y+18, 0xFFFFFFFF);
        ctx.fill(x, y, x+1, y+18, 0xFFFFFFFF);
        ctx.fill(x+17, y, x+18, y+18, 0xFFFFFFFF);
    }

    private SliderWidget makeSlider(int x, int y, int w, double value, java.util.function.DoubleConsumer onChange) {
        return new SliderWidget(x, y, w, 18, Text.literal((int)(value*100) + "%"), value) {
            @Override protected void updateMessage() { setMessage(Text.literal((int)(this.value*100) + "%")); }
            @Override protected void applyValue() { onChange.accept(this.value); }
        };
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }
}
