package com.wynnmaze.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final Consumer<float[]> onConfirm;

    // HSV 值
    private float hue = 0f;        // 0-1
    private float saturation = 1f; // 0-1
    private float value = 1f;      // 0-1

    private TextFieldWidget hexField;

    // 版面配置
    private static final int PAD = 20;
    private static final int SV_SIZE = 150;   // 色域大小
    private static final int HUE_H = 12;      // 色相橫條高度
    private static final int PREVIEW_SIZE = 30;

    private int svX, svY;       // 色域左上角
    private int hueX, hueY;    // 色相橫條左上角
    private int hueW;

    private boolean draggingSV = false;
    private boolean draggingHue = false;

    // 預設色塊
    private static final String[] SWATCHES = {
        "#FF5555","#FF8888","#FFAA00","#FFCC44","#FFD700",
        "#55FF55","#44FF99","#55FFFF","#44CCFF","#4466FF",
        "#D796E9","#AA44FF","#FF99CC","#FFFFFF","#AAAAAA",
        "#777777","#444444","#222222","#000000","#FF0000",
    };
    private static final int SW = 16, SG = 4, SCOLS = 10;

    public ColorPickerScreen(Screen parent, float r, float g, float b, Consumer<float[]> onConfirm) {
        super(Text.literal("選擇顏色"));
        this.parent = parent;
        this.onConfirm = onConfirm;
        float[] hsv = rgbToHsv(r, g, b);
        hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
    }

    @Override
    protected void init() {
        int cx = width / 2;
        svX = cx - SV_SIZE / 2;
        svY = PAD + 20;
        hueX = svX;
        hueY = svY + SV_SIZE + 8;
        hueW = SV_SIZE;

        int swatchY = hueY + HUE_H + 12;
        int swatchTotalW = SCOLS * (SW + SG) - SG;
        int swatchStartX = cx - swatchTotalW / 2;

        // 預設色塊按鈕
        for (int i = 0; i < SWATCHES.length; i++) {
            final String hex = SWATCHES[i];
            int col = i % SCOLS;
            int row = i / SCOLS;
            int sx = swatchStartX + col * (SW + SG);
            int sy = swatchY + row * (SW + SG);
            addDrawableChild(ButtonWidget.builder(Text.empty(), btn -> {
                float[] rgb = fromHex(hex);
                if (rgb != null) {
                    float[] hsv = rgbToHsv(rgb[0], rgb[1], rgb[2]);
                    hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
                    syncHex();
                }
            }).dimensions(sx, sy, SW, SW).build());
        }

        int rows = (SWATCHES.length + SCOLS - 1) / SCOLS;
        int hexY = swatchY + rows * (SW + SG) + 10;

        hexField = new TextFieldWidget(textRenderer, cx - 45, hexY, 90, 18, Text.empty());
        hexField.setText(toHex());
        hexField.setMaxLength(7);
        hexField.setChangedListener(s -> {
            float[] rgb = fromHex(s);
            if (rgb != null) {
                float[] hsv = rgbToHsv(rgb[0], rgb[1], rgb[2]);
                hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
            }
        });
        addDrawableChild(hexField);

        int btnY = hexY + 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("確認"), btn -> {
            float[] rgb = hsvToRgb(hue, saturation, value);
            onConfirm.accept(rgb);
            if (client != null) client.setScreen(parent);
        }).dimensions(cx - 55, btnY, 50, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), btn -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(cx + 5, btnY, 50, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xCC000000);
        ctx.drawCenteredTextWithShadow(textRenderer, "選擇顏色", width / 2, 6, 0xFFFFFFFF);

        // === 色域 (SV picker) ===
        // 純色（目前色相）
        int pureColor = hsvToInt(hue, 1f, 1f);
        // 第一層：白(左) → 純色(右)
        ctx.fillGradient(svX, svY, svX + SV_SIZE, svY + SV_SIZE, 0xFFFFFFFF, pureColor | 0xFF000000);
        // 第二層：透明(上) → 黑(下)
        ctx.fillGradient(svX, svY, svX + SV_SIZE, svY + SV_SIZE, 0x00000000, 0xFF000000);
        // 外框
        ctx.fill(svX - 1, svY - 1, svX + SV_SIZE + 1, svY, 0xFFFFFFFF);
        ctx.fill(svX - 1, svY + SV_SIZE, svX + SV_SIZE + 1, svY + SV_SIZE + 1, 0xFFFFFFFF);
        ctx.fill(svX - 1, svY - 1, svX, svY + SV_SIZE + 1, 0xFFFFFFFF);
        ctx.fill(svX + SV_SIZE, svY - 1, svX + SV_SIZE + 1, svY + SV_SIZE + 1, 0xFFFFFFFF);
        // 游標（十字）
        int cursorX = svX + (int)(saturation * SV_SIZE);
        int cursorY = svY + (int)((1f - value) * SV_SIZE);
        ctx.fill(cursorX - 3, cursorY - 1, cursorX + 3, cursorY + 1, 0xFFFFFFFF);
        ctx.fill(cursorX - 1, cursorY - 3, cursorX + 1, cursorY + 3, 0xFFFFFFFF);

        // === 色相橫條（1px 細條模擬平滑水平漸層）===
        for (int px = 0; px < hueW; px++) {
            float h = (float) px / hueW;
            int col = 0xFF000000 | hsvToInt(h, 1f, 1f);
            ctx.fill(hueX + px, hueY, hueX + px + 1, hueY + HUE_H, col);
        }
        // 游標（白色直線）
        int hueCursorX = hueX + (int)(hue * hueW);
        ctx.fill(hueCursorX - 1, hueY - 2, hueCursorX + 1, hueY + HUE_H + 2, 0xFFFFFFFF);

        // === Hex + 預覽 ===
        int cx2 = width / 2;
        int swatchY = hueY + HUE_H + 12;
        int rows = (SWATCHES.length + SCOLS - 1) / SCOLS;
        int hexY2 = swatchY + rows * (SW + SG) + 10;
        ctx.drawTextWithShadow(textRenderer, "Hex:", cx2 - 65, hexY2 + 4, 0xFFAAAAAA);

        float[] curRgb = hsvToRgb(hue, saturation, value);
        int previewCol = 0xFF000000 | ((int)(curRgb[0]*255)<<16) | ((int)(curRgb[1]*255)<<8) | (int)(curRgb[2]*255);
        int px = cx2 + 50;
        ctx.fill(px, hexY2, px + 20, hexY2 + 18, previewCol);
        ctx.fill(px, hexY2, px + 20, hexY2 + 1, 0xFFFFFFFF);
        ctx.fill(px, hexY2 + 17, px + 20, hexY2 + 18, 0xFFFFFFFF);
        ctx.fill(px, hexY2, px + 1, hexY2 + 18, 0xFFFFFFFF);
        ctx.fill(px + 19, hexY2, px + 20, hexY2 + 18, 0xFFFFFFFF);

        super.render(ctx, mouseX, mouseY, delta);

        // 色塊顏色在 super.render() 後畫，蓋掉 ButtonWidget 樣式
        int swatchTotalW2 = SCOLS * (SW + SG) - SG;
        int swatchStartX2 = cx2 - swatchTotalW2 / 2;
        int swatchY2 = hueY + HUE_H + 12;
        for (int i = 0; i < SWATCHES.length; i++) {
            int scol = i % SCOLS;
            int srow = i / SCOLS;
            int sx = swatchStartX2 + scol * (SW + SG);
            int sy = swatchY2 + srow * (SW + SG);
            float[] rgb = fromHex(SWATCHES[i]);
            if (rgb == null) continue;
            int color = 0xFF000000 | ((int)(rgb[0]*255)<<16) | ((int)(rgb[1]*255)<<8) | (int)(rgb[2]*255);
            ctx.fill(sx, sy, sx + SW, sy + SW, color);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingSV) { updateSV(click.x(), click.y()); return true; }
        if (draggingHue) { updateHue(click.x()); return true; }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingSV = false; draggingHue = false;
        return super.mouseReleased(click);
    }

    // 處理 SV 色域點擊
    @Override
    public void tick() {
        // 更新 hex field
        if (hexField != null) {
            String expected = toHex();
            if (!hexField.getText().equalsIgnoreCase(expected) && !hexField.isFocused()) {
                hexField.setText(expected);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x(), mouseY = click.y();
        if (super.mouseClicked(click, bl)) return true;
        if (mouseX >= svX && mouseX <= svX + SV_SIZE && mouseY >= svY && mouseY <= svY + SV_SIZE) {
            draggingSV = true;
            updateSV(mouseX, mouseY);
            return true;
        }
        if (mouseX >= hueX && mouseX <= hueX + hueW && mouseY >= hueY && mouseY <= hueY + HUE_H) {
            draggingHue = true;
            updateHue(mouseX);
            return true;
        }
        return false;
    }

    private void updateSV(double mouseX, double mouseY) {
        saturation = (float)Math.max(0, Math.min(1, (mouseX - svX) / SV_SIZE));
        value = (float)Math.max(0, Math.min(1, 1.0 - (mouseY - svY) / SV_SIZE));
        syncHex();
    }

    private void updateHue(double mouseX) {
        hue = (float)Math.max(0, Math.min(1, (mouseX - hueX) / hueW));
        syncHex();
    }

    private void syncHex() {
        if (hexField != null && !hexField.isFocused()) hexField.setText(toHex());
    }

    private String toHex() {
        float[] rgb = hsvToRgb(hue, saturation, value);
        return String.format("#%02X%02X%02X", (int)(rgb[0]*255), (int)(rgb[1]*255), (int)(rgb[2]*255));
    }

    private float[] fromHex(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() != 6) return null;
            return new float[]{
                Integer.parseInt(h.substring(0,2),16)/255f,
                Integer.parseInt(h.substring(2,4),16)/255f,
                Integer.parseInt(h.substring(4,6),16)/255f
            };
        } catch (Exception e) { return null; }
    }

    private int hsvToInt(float h, float s, float v) {
        float[] rgb = hsvToRgb(h, s, v);
        return ((int)(rgb[0]*255)<<16) | ((int)(rgb[1]*255)<<8) | (int)(rgb[2]*255);
    }

    private float[] hsvToRgb(float h, float s, float v) {
        if (s == 0) return new float[]{v, v, v};
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0: return new float[]{v, t, p};
            case 1: return new float[]{q, v, p};
            case 2: return new float[]{p, v, t};
            case 3: return new float[]{p, q, v};
            case 4: return new float[]{t, p, v};
            default: return new float[]{v, p, q};
        }
    }

    private float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0, s = max == 0 ? 0 : d / max, v = max;
        if (d != 0) {
            if (max == r) h = (g - b) / d + (g < b ? 6 : 0);
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6;
        }
        return new float[]{h, s, v};
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }
}
