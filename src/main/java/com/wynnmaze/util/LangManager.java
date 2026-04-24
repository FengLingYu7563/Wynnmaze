package com.wynnmaze.util;

/**
 * Centralised localisation for all UI strings.
 * The "Language" label in ConfigScreen is always English as requested.
 */
public class LangManager {

    // ── Languages ─────────────────────────────────────────────────────────────

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

    // ── Config UI strings ─────────────────────────────────────────────────────

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

    // ── Chat / HUD strings ────────────────────────────────────────────────────

    /**
     * 訊息第二行：點擊或按快捷鍵顯示路線。
     * keyName 會動態帶入目前綁定的按鍵名稱（例如 "."）。
     */
    public static String clickToGuide(String keyName) {
        return switch (current) {
            case ZH_TW -> "[點擊訊息或 " + keyName + " 鍵顯示路線]";
            case ZH_CN -> "[点击消息或 " + keyName + " 键显示路线]";
            case JA    -> "[クリックか " + keyName + " キーでルート表示]";
            case KO    -> "[클릭 또는 " + keyName + " 키로 경로 표시]";
            case DE    -> "[Klicken oder " + keyName + " für Route]";
            case FR    -> "[Cliquer ou " + keyName + " pour itinéraire]";
            case ES    -> "[Clic o " + keyName + " para ruta]";
            case PT    -> "[Clique ou " + keyName + " para rota]";
            case PL    -> "[Kliknij lub " + keyName + " aby trasę]";
            default    -> "[Click or press " + keyName + " to show route]";
        };
    }

    /** Chat message: route is now displayed */
    public static String routeShown() {
        return switch (current) {
            case ZH_TW -> "已顯示路線 \u2192 ";
            case ZH_CN -> "已显示路线 \u2192 ";
            case JA    -> "ルート表示中 \u2192 ";
            case KO    -> "경로 표시됨 \u2192 ";
            case DE    -> "Route angezeigt \u2192 ";
            case FR    -> "Itinéraire affiché \u2192 ";
            case ES    -> "Ruta mostrada \u2192 ";
            case PT    -> "Rota exibida \u2192 ";
            case PL    -> "Trasa wyświetlona \u2192 ";
            default    -> "Route shown \u2192 ";
        };
    }

    /** HUD: exit found notification */
    public static String exitFound() {
        return switch (current) {
            case ZH_TW -> "已找到出口！ ";
            case ZH_CN -> "已找到出口！ ";
            case JA    -> "出口発見！ ";
            case KO    -> "출구 발견! ";
            case DE    -> "Ausgang gefunden! ";
            case FR    -> "Sortie trouvée ! ";
            case ES    -> "¡Salida encontrada! ";
            case PT    -> "Saída encontrada! ";
            case PL    -> "Znaleziono wyjście! ";
            default    -> "Exit found! ";
        };
    }

    /** Keybind reset button label */
    public static String resetKey() {
        return switch (current) {
            case ZH_TW -> "重設";
            case ZH_CN -> "重置";
            case JA    -> "リセット";
            case KO    -> "초기화";
            case DE    -> "Zurücksetzen";
            case FR    -> "Réinitialiser";
            case ES    -> "Restablecer";
            case PT    -> "Redefinir";
            case PL    -> "Resetuj";
            default    -> "Reset";
        };
    }

    /** Keybind listening prompt: shown inside the button while waiting for input */
    public static String pressAKey() {
        return switch (current) {
            case ZH_TW -> "按下按鍵";
            case ZH_CN -> "按下按键";
            case JA    -> "キーを押して";
            case KO    -> "키를 누르세요";
            case DE    -> "Taste drücken";
            case FR    -> "Appuyez sur une touche";
            case ES    -> "Presiona tecla";
            case PT    -> "Pressione tecla";
            case PL    -> "Naciśnij klawisz";
            default    -> "Press a key";
        };
    }

    /** Config label: the keybinding row */
    public static String showGuideRoute() {        return switch (current) {
            case ZH_TW -> "顯示路線 快捷鍵";
            case ZH_CN -> "显示路线 快捷键";
            case JA    -> "ルート表示 ショートカット";
            case KO    -> "경로 표시 단축키";
            case DE    -> "Route Taste";
            case FR    -> "Touche itinéraire";
            case ES    -> "Tecla de ruta";
            case PT    -> "Tecla de rota";
            case PL    -> "Klawisz trasy";
            default    -> "Show Guide Route";
        };
    }

    /** Chat: route from teammate's verified share. {sender} 由呼叫端代入。 */
    public static String routeFromTeammate(String sender) {
        return switch (current) {
            case ZH_TW -> "依 " + sender + " 提供的座標顯示路線 \u2192 ";
            case ZH_CN -> "依 " + sender + " 提供的坐标显示路线 \u2192 ";
            case JA    -> sender + " さんの座標でルート表示 \u2192 ";
            case KO    -> sender + " 님 좌표로 경로 표시 \u2192 ";
            case DE    -> "Route nach " + sender + " \u2192 ";
            case FR    -> "Itinéraire d'après " + sender + " \u2192 ";
            case ES    -> "Ruta según " + sender + " \u2192 ";
            case PT    -> "Rota de " + sender + " \u2192 ";
            case PL    -> "Trasa od " + sender + " \u2192 ";
            default    -> "Route from " + sender + " \u2192 ";
        };
    }

    /** Chat: a new version is available. {tag} 由呼叫端代入。 */
    public static String updateAvailable(String tag) {
        return switch (current) {
            case ZH_TW -> "有新版本可用：" + tag;
            case ZH_CN -> "有新版本可用：" + tag;
            case JA    -> "新しいバージョン: " + tag;
            case KO    -> "새 버전 있음: " + tag;
            case DE    -> "Neue Version verfügbar: " + tag;
            case FR    -> "Nouvelle version : " + tag;
            case ES    -> "Nueva versión: " + tag;
            case PT    -> "Nova versão: " + tag;
            case PL    -> "Nowa wersja: " + tag;
            default    -> "New version available: " + tag;
        };
    }

    /** Chat: open release page. */
    public static String openReleasePage() {
        return switch (current) {
            case ZH_TW -> "開啟下載頁面";
            case ZH_CN -> "打开下载页面";
            case JA    -> "ダウンロードページを開く";
            case KO    -> "다운로드 페이지 열기";
            case DE    -> "Downloadseite öffnen";
            case FR    -> "Ouvrir la page";
            case ES    -> "Abrir página";
            case PT    -> "Abrir página";
            case PL    -> "Otwórz stronę";
            default    -> "Open release page";
        };
    }
}
