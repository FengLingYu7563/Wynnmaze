package com.wynnmaze.render;

import com.wynnmaze.util.GuideConfig;
import com.wynnmaze.util.GuideRenderer;
import com.wynnmaze.util.LangManager;
import net.minecraft.client.gui.Click;
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

    private static final int LX_OFFSET  = -180; // 左邊標籤 x 偏移
    private static final int CTRL_OFFSET = -10;  // 控件 x 偏移

    // ── Language dropdown state ────────────────────────────────────────────────
    private static final LangManager.Language[] LANGS = LangManager.Language.values();
    private static final int ITEM_H      = 14;  // height per dropdown row
    private static final int LANG_BTN_W  = 150;
    private static final int LANG_BTN_H  = 18;

    private boolean langDropdownOpen = false;
    // These are set in init() once we know width
    private int langBtnX, langBtnY;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ConfigScreen(Screen parent) {
        super(Text.literal("Wynnmaze Config"));
        this.parent = parent;
        this.cfg    = GuideConfig.get();
        lineR = cfg.lineR; lineG = cfg.lineG; lineB = cfg.lineB;
        arrowR = cfg.arrowR; arrowG = cfg.arrowG; arrowB = cfg.arrowB;
    }

    // ── init ──────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        int cx      = width / 2;
        int ctrlX   = cx + CTRL_OFFSET;
        int sliderW = 180;

        // ── Language row (y=30) — label is always English "Language" ──────────
        langBtnX = ctrlX;
        langBtnY = 30;
        // The button text shows the current language's native display name.
        // Clicking toggles the dropdown open/close.
        addDrawableChild(ButtonWidget.builder(
                Text.literal(LangManager.getLanguage().displayName + " \u25be"),
                btn -> langDropdownOpen = !langDropdownOpen
        ).dimensions(langBtnX, langBtnY, LANG_BTN_W, LANG_BTN_H).build());

        int y = 56; // everything below shifts down one row (26px) compared to original

        // ── Line color ────────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal(LangManager.pickColor()), btn ->
                client.setScreen(new ColorPickerScreen(this, lineR, lineG, lineB, rgb -> {
                    lineR = rgb[0]; lineG = rgb[1]; lineB = rgb[2];
                    cfg.lineR = lineR; cfg.lineG = lineG; cfg.lineB = lineB;
                }))
        ).dimensions(ctrlX, y, 100, 18).build());
        y += 26;

        // ── Line width ────────────────────────────────────────────────────────
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.lineWidth / 10.0,
                v -> cfg.lineWidth = (float)(v * 10)));
        y += 26;

        // ── Line opacity ──────────────────────────────────────────────────────
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.lineAlpha,
                v -> cfg.lineAlpha = (float)v));
        y += 36;

        // ── Arrow color ───────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal(LangManager.pickColor()), btn ->
                client.setScreen(new ColorPickerScreen(this, arrowR, arrowG, arrowB, rgb -> {
                    arrowR = rgb[0]; arrowG = rgb[1]; arrowB = rgb[2];
                    cfg.arrowR = arrowR; cfg.arrowG = arrowG; cfg.arrowB = arrowB;
                }))
        ).dimensions(ctrlX, y, 100, 18).build());
        y += 26;

        // ── Arrow width ───────────────────────────────────────────────────────
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.arrowWidth / 10.0,
                v -> cfg.arrowWidth = (float)(v * 10)));
        y += 26;

        // ── Arrow spacing ─────────────────────────────────────────────────────
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.arrowSpacing / 30.0,
                v -> cfg.arrowSpacing = (float)(v * 30)));
        y += 26;

        // ── Arrow angle ───────────────────────────────────────────────────────
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.arrowAngle / 90.0,
                v -> cfg.arrowAngle = (float)(v * 90)));
        y += 26;

        // ── Arrow opacity ─────────────────────────────────────────────────────
        addDrawableChild(makeSlider(ctrlX, y, sliderW, cfg.arrowAlpha,
                v -> cfg.arrowAlpha = (float)v));
        y += 36;

        // ── Save / Cancel ─────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal(LangManager.save()), btn -> {
            cfg.language = LangManager.getLanguage().code;
            cfg.save();
            GuideRenderer.reloadConfig();
            close();
        }).dimensions(cx - 105, y, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(LangManager.cancel()), btn -> close())
                .dimensions(cx + 5, y, 100, 20).build());
    }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xCC000000);
        ctx.drawCenteredTextWithShadow(textRenderer, LangManager.configTitle(), width / 2, 10, 0xFFFFFFFF);

        int cx     = width / 2;
        int labelX = cx + LX_OFFSET;
        int y      = 30;

        // ── Language label (ALWAYS English) ───────────────────────────────────
        ctx.drawTextWithShadow(textRenderer, "Language", labelX, y + 4, 0xFFFFFFFF);
        y += 26;

        // ── Other setting labels (localised) ──────────────────────────────────
        ctx.drawTextWithShadow(textRenderer, LangManager.lineColor(),   labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, LangManager.lineWidth(),   labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, LangManager.lineOpacity(), labelX, y + 4, 0xFFFFFFFF);
        y += 36;
        ctx.drawTextWithShadow(textRenderer, LangManager.arrowColor(),  labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, LangManager.arrowWidth(),  labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, LangManager.arrowSpacing(),labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, LangManager.arrowAngle(),  labelX, y + 4, 0xFFFFFFFF);
        y += 26;
        ctx.drawTextWithShadow(textRenderer, LangManager.arrowOpacity(),labelX, y + 4, 0xFFFFFFFF);

        // Draw all widget children
        super.render(ctx, mouseX, mouseY, delta);

        // ── Colour previews (drawn after widgets so they sit on top) ──────────
        int ctrlX    = cx + CTRL_OFFSET;
        int previewX = ctrlX + 104;
        drawPreview(ctx, lineR,  lineG,  lineB,  previewX, 56);
        drawPreview(ctx, arrowR, arrowG, arrowB, previewX, 56 + 26 + 26 + 36);

        // ── Language dropdown panel (drawn last — topmost layer) ───────────────
        if (langDropdownOpen) {
            drawLangDropdown(ctx, mouseX, mouseY);
        }
    }

    // ── Dropdown rendering ────────────────────────────────────────────────────

    private void drawLangDropdown(DrawContext ctx, int mouseX, int mouseY) {
        int panelTop  = langBtnY + LANG_BTN_H + 1;
        int panelH    = LANGS.length * ITEM_H + 4;
        int panelX    = langBtnX;
        int panelW    = LANG_BTN_W;

        // Panel background + border
        ctx.fill(panelX,         panelTop,          panelX + panelW,     panelTop + panelH, 0xFF1A1A1A);
        ctx.fill(panelX,         panelTop,          panelX + panelW,     panelTop + 1,      0xFFAAAAAA);
        ctx.fill(panelX,         panelTop + panelH - 1, panelX + panelW, panelTop + panelH, 0xFFAAAAAA);
        ctx.fill(panelX,         panelTop,          panelX + 1,          panelTop + panelH, 0xFFAAAAAA);
        ctx.fill(panelX + panelW - 1, panelTop,    panelX + panelW,     panelTop + panelH, 0xFFAAAAAA);

        int itemY = panelTop + 2;
        for (LangManager.Language lang : LANGS) {
            boolean selected   = lang == LangManager.getLanguage();
            boolean hovered    = mouseX >= panelX && mouseX <= panelX + panelW - 1
                              && mouseY >= itemY   && mouseY <= itemY + ITEM_H - 1;

            // Highlight background
            if (selected) {
                ctx.fill(panelX + 1, itemY, panelX + panelW - 1, itemY + ITEM_H, 0xFF3A3A6A);
            } else if (hovered) {
                ctx.fill(panelX + 1, itemY, panelX + panelW - 1, itemY + ITEM_H, 0xFF333333);
            }

            int textColor = selected ? 0xFFFFFF55 : (hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            ctx.drawTextWithShadow(textRenderer, lang.displayName, panelX + 5, itemY + 3, textColor);
            itemY += ITEM_H;
        }
    }

    // ── Mouse input ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x(), mouseY = click.y();
        // When dropdown is open intercept ALL clicks before regular children.
        if (langDropdownOpen) {
            int panelTop = langBtnY + LANG_BTN_H + 1;
            int itemY    = panelTop + 2;
            for (LangManager.Language lang : LANGS) {
                if (mouseX >= langBtnX && mouseX <= langBtnX + LANG_BTN_W
                 && mouseY >= itemY    && mouseY <= itemY + ITEM_H) {
                    // Language selected
                    LangManager.setLanguage(lang);
                    cfg.language = lang.code;
                    langDropdownOpen = false;
                    // Re-open screen to refresh all button/label text
                    client.setScreen(new ConfigScreen(parent));
                    return true;
                }
                itemY += ITEM_H;
            }
            // Click outside dropdown → close it, consume the click
            langDropdownOpen = false;
            return true;
        }
        return super.mouseClicked(click, bl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawPreview(DrawContext ctx, float r, float g, float b, int x, int y) {
        int col = 0xFF000000 | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        ctx.fill(x,      y,      x + 18, y + 18, col);
        ctx.fill(x,      y,      x + 18, y + 1,  0xFFFFFFFF);
        ctx.fill(x,      y + 17, x + 18, y + 18, 0xFFFFFFFF);
        ctx.fill(x,      y,      x + 1,  y + 18, 0xFFFFFFFF);
        ctx.fill(x + 17, y,      x + 18, y + 18, 0xFFFFFFFF);
    }

    private SliderWidget makeSlider(int x, int y, int w, double value,
                                    java.util.function.DoubleConsumer onChange) {
        return new SliderWidget(x, y, w, 18, Text.literal((int)(value * 100) + "%"), value) {
            @Override protected void updateMessage() { setMessage(Text.literal((int)(this.value * 100) + "%")); }
            @Override protected void applyValue()    { onChange.accept(this.value); }
        };
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }
}
