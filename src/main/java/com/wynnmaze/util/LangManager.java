package com.wynnmaze.util;

/**
 * Centralised localisation for all UI strings.
 * The "Language" label in ConfigScreen is always English as requested.
 */
public class LangManager {

    // ── Languages ──────────────────────────────────────────────────────────────

    public enum Language {
        EN    ("en",    "English"),
        ZH_TW ("zh_tw", "繁體中文"),
        ZH_CN ("zh_cn", "简体中文"),
        JA    ("ja",    "日本語"),
        KO    ("ko",    "한국어"),
        DE    ("de",    "Deutsch"),
        FR    ("fr",    "Français"),
        ES    ("es",    "Español"),
        PT    ("pt",    "Português"),
        PL    ("pl",    "Polski");

        public final String code;
        public final String displayName;

        Language(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public static Language fromCode(String code) {
            if (code == null) return EN;
            for (Language l : values()) {
                if (l.code.equals(code)) return l;
            }
            return EN;
        }
    }

    private static Language current = Language.EN;

    public static void setLanguage(Language lang) { current = lang; }
    public static Language getLanguage()           { return current; }
    public static void setFromCode(String code)    { current = Language.fromCode(code); }

    // ── Strings ────────────────────────────────────────────────────────────────

    public static String configTitle() {
        return switch (current) {
            case ZH_TW -> "Wynnmaze 設定";
            case ZH_CN -> "Wynnmaze 设置";
            case JA    -> "Wynnmaze 設定";
            case KO    -> "Wynnmaze 설정";
            case DE    -> "Wynnmaze Einstellungen";
            case FR    -> "Paramètres Wynnmaze";
            case ES    -> "Configuración Wynnmaze";
            case PT    -> "Configurações Wynnmaze";
            case PL    -> "Ustawienia Wynnmaze";
            default    -> "Wynnmaze Settings";
        };
    }

    public static String lineColor() {
        return switch (current) {
            case ZH_TW -> "引路線 顏色";
            case ZH_CN -> "引路线 颜色";
            case JA    -> "ライン 色";
            case KO    -> "선 색상";
            case DE    -> "Linienfarbe";
            case FR    -> "Couleur de ligne";
            case ES    -> "Color de línea";
            case PT    -> "Cor da linha";
            case PL    -> "Kolor linii";
            default    -> "Line Color";
        };
    }

    public static String lineWidth() {
        return switch (current) {
            case ZH_TW -> "引路線 粗度";
            case ZH_CN -> "引路线 粗细";
            case JA    -> "ライン 太さ";
            case KO    -> "선 굵기";
            case DE    -> "Linienbreite";
            case FR    -> "Épaisseur ligne";
            case ES    -> "Grosor de línea";
            case PT    -> "Largura da linha";
            case PL    -> "Grubość linii";
            default    -> "Line Width";
        };
    }

    public static String lineOpacity() {
        return switch (current) {
            case ZH_TW -> "引路線 透明度";
            case ZH_CN -> "引路线 透明度";
            case JA    -> "ライン 透明度";
            case KO    -> "선 투명도";
            case DE    -> "Linien-Deckkraft";
            case FR    -> "Opacité ligne";
            case ES    -> "Opacidad línea";
            case PT    -> "Opacidade linha";
            case PL    -> "Przezrocz. linii";
            default    -> "Line Opacity";
        };
    }

    public static String arrowColor() {
        return switch (current) {
            case ZH_TW -> "箭頭 顏色";
            case ZH_CN -> "箭头 颜色";
            case JA    -> "矢印 色";
            case KO    -> "화살표 색상";
            case DE    -> "Pfeilfarbe";
            case FR    -> "Couleur flèche";
            case ES    -> "Color de flecha";
            case PT    -> "Cor da seta";
            case PL    -> "Kolor strzałki";
            default    -> "Arrow Color";
        };
    }

    public static String arrowWidth() {
        return switch (current) {
            case ZH_TW -> "箭頭 粗度";
            case ZH_CN -> "箭头 粗细";
            case JA    -> "矢印 太さ";
            case KO    -> "화살표 굵기";
            case DE    -> "Pfeilbreite";
            case FR    -> "Épaisseur flèche";
            case ES    -> "Grosor flecha";
            case PT    -> "Largura seta";
            case PL    -> "Grubość strzałki";
            default    -> "Arrow Width";
        };
    }

    public static String arrowSpacing() {
        return switch (current) {
            case ZH_TW -> "箭頭 間距";
            case ZH_CN -> "箭头 间距";
            case JA    -> "矢印 間隔";
            case KO    -> "화살표 간격";
            case DE    -> "Pfeil-Abstand";
            case FR    -> "Espacement flèche";
            case ES    -> "Espacio flecha";
            case PT    -> "Espaço seta";
            case PL    -> "Odstęp strzałki";
            default    -> "Arrow Spacing";
        };
    }

    public static String arrowAngle() {
        return switch (current) {
            case ZH_TW -> "箭翼 角度";
            case ZH_CN -> "箭翼 角度";
            case JA    -> "矢印 角度";
            case KO    -> "화살표 각도";
            case DE    -> "Pfeilwinkel";
            case FR    -> "Angle flèche";
            case ES    -> "Ángulo flecha";
            case PT    -> "Ângulo seta";
            case PL    -> "Kąt strzałki";
            default    -> "Arrow Angle";
        };
    }

    public static String arrowOpacity() {
        return switch (current) {
            case ZH_TW -> "箭頭 透明度";
            case ZH_CN -> "箭头 透明度";
            case JA    -> "矢印 透明度";
            case KO    -> "화살표 투명도";
            case DE    -> "Pfeil-Deckkraft";
            case FR    -> "Opacité flèche";
            case ES    -> "Opacidad flecha";
            case PT    -> "Opacidade seta";
            case PL    -> "Przezrocz. strzałki";
            default    -> "Arrow Opacity";
        };
    }

    public static String pickColor() {
        return switch (current) {
            case ZH_TW -> "選擇顏色";
            case ZH_CN -> "选择颜色";
            case JA    -> "色を選択";
            case KO    -> "색상 선택";
            case DE    -> "Farbe wählen";
            case FR    -> "Choisir couleur";
            case ES    -> "Elegir color";
            case PT    -> "Escolher cor";
            case PL    -> "Wybierz kolor";
            default    -> "Pick Color";
        };
    }

    public static String save() {
        return switch (current) {
            case ZH_TW -> "儲存";
            case ZH_CN -> "保存";
            case JA    -> "保存";
            case KO    -> "저장";
            case DE    -> "Speichern";
            case FR    -> "Enregistrer";
            case ES    -> "Guardar";
            case PT    -> "Salvar";
            case PL    -> "Zapisz";
            default    -> "Save";
        };
    }

    public static String cancel() {
        return switch (current) {
            case ZH_TW -> "取消";
            case ZH_CN -> "取消";
            case JA    -> "キャンセル";
            case KO    -> "취소";
            case DE    -> "Abbrechen";
            case FR    -> "Annuler";
            case ES    -> "Cancelar";
            case PT    -> "Cancelar";
            case PL    -> "Anuluj";
            default    -> "Cancel";
        };
    }

    public static String colorPickerTitle() {
        return switch (current) {
            case ZH_TW -> "選擇顏色";
            case ZH_CN -> "选择颜色";
            case JA    -> "色を選択";
            case KO    -> "색상 선택";
            case DE    -> "Farbe wählen";
            case FR    -> "Choisir une couleur";
            case ES    -> "Elegir color";
            case PT    -> "Escolher cor";
            case PL    -> "Wybierz kolor";
            default    -> "Pick Color";
        };
    }

    public static String confirm() {
        return switch (current) {
            case ZH_TW -> "確認";
            case ZH_CN -> "确认";
            case JA    -> "確認";
            case KO    -> "확인";
            case DE    -> "Bestätigen";
            case FR    -> "Confirmer";
            case ES    -> "Confirmar";
            case PT    -> "Confirmar";
            case PL    -> "Potwierdź";
            default    -> "Confirm";
        };
    }
}
