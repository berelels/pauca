package app.pauca.data

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import app.pauca.helper.Edition
import org.json.JSONObject

class Prefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    private fun bool(key: String, def: Boolean) = prefs.getBoolean(key, def)
    private fun int(key: String, def: Int) = prefs.getInt(key, def)
    private fun long(key: String, def: Long) = prefs.getLong(key, def)
    private fun string(key: String, def: String = "") = prefs.getString(key, def) ?: def
    private fun put(key: String, value: Any?) = prefs.edit {
        when (value) {
            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
            else -> putString(key, value?.toString())
        }
    }

    // Primeiro uso

    var firstOpen: Boolean
        get() = bool("FIRST_OPEN", true)
        set(value) = put("FIRST_OPEN", value)

    var firstOpenTime: Long
        get() = long("FIRST_OPEN_TIME", 0L)
        set(value) = put("FIRST_OPEN_TIME", value)

    var firstSettingsOpen: Boolean
        get() = bool("FIRST_SETTINGS_OPEN", true)
        set(value) = put("FIRST_SETTINGS_OPEN", value)

    var firstHide: Boolean
        get() = bool("FIRST_HIDE", true)
        set(value) = put("FIRST_HIDE", value)

    /** O tour da tela inicial já foi visto (ou pulado). */
    var tutorialDone: Boolean
        get() = bool("TUTORIAL_DONE", false)
        set(value) = put("TUTORIAL_DONE", value)

    /** Idioma do app ("en", "pt-BR", "es"); vazio segue o do sistema, que é o padrão. */
    var language: String
        get() = string("LANGUAGE")
        set(value) = put("LANGUAGE", value)

    var hideSetDefaultLauncher: Boolean
        get() = bool("HIDE_SET_DEFAULT_LAUNCHER", false)
        set(value) = put("HIDE_SET_DEFAULT_LAUNCHER", value)

    var launcherRestartTimestamp: Long
        get() = long("LAUNCHER_RECREATE_TIMESTAMP", 0L)
        set(value) = put("LAUNCHER_RECREATE_TIMESTAMP", value)

    // Aparência

    // No Lite, só os temas e cores dele (o que for de fora vira o padrão)
    var paletteId: String
        get() = string("PALETTE", Palette.PAUCA.id).takeIf { Edition.hasPalette(it) } ?: Palette.PAUCA.id
        set(value) = put("PALETTE", value)

    /** Cor de destaque: "#RRGGBB" ou [Accent.NONE]. */
    var accent: String
        get() = string("ACCENT", Accent.DEFAULT).takeIf { Edition.hasAccent(it) } ?: Accent.DEFAULT
        set(value) = put("ACCENT", value)

    val palette: Palette get() = Palette.byId(paletteId).withAccent(accent)

    /** O tema do AppCompat acompanha a paleta (claro/escuro). */
    val appTheme: Int
        get() = if (palette.isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO

    // Papel de parede (tema "Fundo")

    /** De onde vem o fundo: o papel de parede do sistema ou uma imagem escolhida. */
    var wallpaperSource: String
        get() = string("WALLPAPER_SOURCE", Constants.WallpaperSource.SYSTEM)
        set(value) = put("WALLPAPER_SOURCE", value)

    var wallpaperBlur: Boolean
        get() = bool("WALLPAPER_BLUR", false)
        set(value) = put("WALLPAPER_BLUR", value)

    /** Intensidade do desfoque, de 1 a 25. */
    var wallpaperBlurRadius: Int
        get() = int("WALLPAPER_BLUR_RADIUS", 12)
        set(value) = put("WALLPAPER_BLUR_RADIUS", value)

    /** Brilho em %: abaixo de 100 escurece, acima clareia. */
    var wallpaperBrightness: Int
        get() = int("WALLPAPER_BRIGHTNESS", 100)
        set(value) = put("WALLPAPER_BRIGHTNESS", value)

    /** A imagem também vai para a tela de bloqueio (com os ajustes próprios abaixo). */
    var lockWallpaper: Boolean
        get() = bool("LOCK_WALLPAPER", false)
        set(value) = put("LOCK_WALLPAPER", value)

    var lockBlur: Boolean
        get() = bool("LOCK_BLUR", false)
        set(value) = put("LOCK_BLUR", value)

    var lockBlurRadius: Int
        get() = int("LOCK_BLUR_RADIUS", 12)
        set(value) = put("LOCK_BLUR_RADIUS", value)

    var lockBrightness: Int
        get() = int("LOCK_BRIGHTNESS", 100)
        set(value) = put("LOCK_BRIGHTNESS", value)

    /** Categoria escolhida à mão para um app na gaveta: pacote -> categoria. */
    var appCategories: Map<String, String>
        get() = try {
            val o = JSONObject(string("APP_CATEGORIES", "{}"))
            o.keys().asSequence().associateWith { o.optString(it) }
        } catch (_: Exception) {
            emptyMap()
        }
        set(value) = put("APP_CATEGORIES", JSONObject(value).toString())

    var homeStyle: Int
        get() = int("HOME_STYLE", Constants.HomeStyle.CARDS)
        set(value) = put("HOME_STYLE", value)

    var appFont: String
        get() = string("APP_FONT", Constants.Font.JAKARTA)
        set(value) = put("APP_FONT", value)

    var appTextSize: Int
        get() = int("APP_TEXT_SIZE", 30)
        set(value) = put("APP_TEXT_SIZE", value)

    var appWeight: Int
        get() = int("APP_WEIGHT", 700)
        set(value) = put("APP_WEIGHT", value)

    var textCase: Int
        get() = int("TEXT_CASE", Constants.TextCase.LOWER)
        set(value) = put("TEXT_CASE", value)

    var homeAlignment: Int
        get() = int("HOME_ALIGNMENT", Gravity.START)
        set(value) = put("HOME_ALIGNMENT", value)

    var homeVertical: Int
        get() = int("HOME_VERTICAL", Constants.Vertical.TOP)
        set(value) = put("HOME_VERTICAL", value)

    /** Barra de cima (ajustes, editar): oculta, sempre visível ou aparece ao tocar. */
    var topBarMode: Int
        get() = int("TOP_BAR_MODE", legacyBarMode())
        set(value) = put("TOP_BAR_MODE", value)

    /** Barra de baixo (tema, perfil, foco). */
    var bottomBarMode: Int
        get() = int("BOTTOM_BAR_MODE", legacyBarMode())
        set(value) = put("BOTTOM_BAR_MODE", value)

    // A versão 0.1 tinha um interruptor só para as duas barras
    private fun legacyBarMode() =
        if (bool("SHOW_BUTTONS", true)) Constants.BarMode.ALWAYS else Constants.BarMode.HIDDEN

    var showSettingsButton: Boolean
        get() = bool("SHOW_SETTINGS_BUTTON", true)
        set(value) = put("SHOW_SETTINGS_BUTTON", value)

    var showEditButton: Boolean
        get() = bool("SHOW_EDIT_BUTTON", true)
        set(value) = put("SHOW_EDIT_BUTTON", value)

    var showThemeButton: Boolean
        get() = bool("SHOW_THEME_BUTTON", true)
        set(value) = put("SHOW_THEME_BUTTON", value)

    var showProfileButton: Boolean
        get() = bool("SHOW_PROFILE_BUTTON", true)
        set(value) = put("SHOW_PROFILE_BUTTON", value)

    var showFocusButton: Boolean
        get() = !Edition.isLite && bool("SHOW_FOCUS_BUTTON", true)
        set(value) = put("SHOW_FOCUS_BUTTON", value)

    var appLabelAlignment: Int
        get() = int("APP_LABEL_ALIGNMENT", Gravity.START)
        set(value) = put("APP_LABEL_ALIGNMENT", value)

    var showStatusBar: Boolean
        get() = bool("STATUS_BAR", true)
        set(value) = put("STATUS_BAR", value)

    var textSizeScale: Float
        get() = prefs.getFloat("TEXT_SIZE_SCALE", 1.0f)
        set(value) = put("TEXT_SIZE_SCALE", value)

    // Relógio e data

    var dateTimeVisibility: Int
        get() = int("DATE_TIME_VISIBILITY", Constants.DateTime.ON)
        set(value) = put("DATE_TIME_VISIBILITY", value)

    /** Padrão do relógio; vazio segue o 12/24h do sistema. */
    var clockPattern: String
        get() = string("CLOCK_PATTERN")
        set(value) = put("CLOCK_PATTERN", value)

    /** Padrão da data; vazio usa o padrão do idioma. */
    var datePattern: String
        get() = string("DATE_PATTERN")
        set(value) = put("DATE_PATTERN", value)

    var clockFont: String
        get() = string("CLOCK_FONT", Constants.Font.NEWSREADER)
        set(value) = put("CLOCK_FONT", value)

    var clockSize: Int
        get() = int("CLOCK_SIZE", 72)
        set(value) = put("CLOCK_SIZE", value)

    var clockWeight: Int
        get() = int("CLOCK_WEIGHT", 300)
        set(value) = put("CLOCK_WEIGHT", value)

    var clockAlignment: Int
        get() = int("CLOCK_ALIGNMENT", Gravity.START)
        set(value) = put("CLOCK_ALIGNMENT", value)

    var clockAccent: Boolean
        get() = bool("CLOCK_ACCENT", false)
        set(value) = put("CLOCK_ACCENT", value)

    /** Nomes dos apps da tela inicial na cor de destaque. */
    var appsAccent: Boolean
        get() = bool("APPS_ACCENT", false)
        set(value) = put("APPS_ACCENT", value)

    /** Volta fonte, peso e tamanho dos apps e do relógio ao padrão. */
    fun resetText() = prefs.edit(commit = true) {
        listOf("APP_FONT", "APP_WEIGHT", "APP_TEXT_SIZE", "CLOCK_FONT", "CLOCK_WEIGHT", "CLOCK_SIZE", "TEXT_SIZE_SCALE")
            .forEach { remove(it) }
    }

    var showBattery: Boolean
        get() = bool("SHOW_BATTERY", true)
        set(value) = put("SHOW_BATTERY", value)

    var showScreenTime: Boolean
        get() = bool("SHOW_SCREEN_TIME", false)
        set(value) = put("SHOW_SCREEN_TIME", value)

    var screenTimeLastUpdated: Long
        get() = long("SCREEN_TIME_LAST_UPDATED", 0L)
        set(value) = put("SCREEN_TIME_LAST_UPDATED", value)

    // Modo foco

    var focusActive: Boolean
        get() = !Edition.isLite && bool("FOCUS_ACTIVE", false)
        set(value) = put("FOCUS_ACTIVE", value)

    var focusSince: Long
        get() = long("FOCUS_SINCE", 0L)
        set(value) = put("FOCUS_SINCE", value)

    /** Quem ligou o foco: o botão (manual) ou a troca de perfil. */
    var focusSource: String
        get() = string("FOCUS_SOURCE", Constants.FocusSource.MANUAL)
        set(value) = put("FOCUS_SOURCE", value)

    var focusDnd: Boolean
        get() = bool("FOCUS_DND", true)
        set(value) = put("FOCUS_DND", value)

    var focusCalls: Int
        get() = int("FOCUS_CALLS", Constants.Callers.STARRED)
        set(value) = put("FOCUS_CALLS", value)

    var focusRepeatCallers: Boolean
        get() = bool("FOCUS_REPEAT_CALLERS", true)
        set(value) = put("FOCUS_REPEAT_CALLERS", value)

    var focusHideSilenced: Boolean
        get() = bool("FOCUS_HIDE_SILENCED", true)
        set(value) = put("FOCUS_HIDE_SILENCED", value)

    var focusFilter: Boolean
        get() = bool("FOCUS_FILTER", true)
        set(value) = put("FOCUS_FILTER", value)

    var focusHideApps: Boolean
        get() = bool("FOCUS_HIDE_APPS", true)
        set(value) = put("FOCUS_HIDE_APPS", value)

    var focusBlockApps: Boolean
        get() = bool("FOCUS_BLOCK_APPS", false)
        set(value) = put("FOCUS_BLOCK_APPS", value)

    var focusGrayscale: Boolean
        get() = bool("FOCUS_GRAYSCALE", false)
        set(value) = put("FOCUS_GRAYSCALE", value)

    /** Pacotes que podem notificar e abrir no foco, além dos que estão na tela inicial. */
    var focusAllowed: Set<String>
        get() = prefs.getStringSet("FOCUS_ALLOWED", emptySet())?.toSet() ?: emptySet()
        set(value) = put("FOCUS_ALLOWED", value)

    var zenRuleId: String
        get() = string("ZEN_RULE_ID")
        set(value) = put("ZEN_RULE_ID", value)

    var previousInterruptionFilter: Int
        get() = int("PREV_INTERRUPTION_FILTER", 0)
        set(value) = put("PREV_INTERRUPTION_FILTER", value)

    var previousDaltonizer: String
        get() = string("PREV_DALTONIZER")
        set(value) = put("PREV_DALTONIZER", value)

    /** Notificações seguradas durante o foco: pacote -> quantidade. */
    var focusDigest: Map<String, Int>
        get() = try {
            val o = JSONObject(string("FOCUS_DIGEST", "{}"))
            o.keys().asSequence().associateWith { o.optInt(it) }
        } catch (_: Exception) {
            emptyMap()
        }
        set(value) = put("FOCUS_DIGEST", JSONObject(value).toString())

    // Gestos

    var lockModeOn: Boolean
        get() = bool("LOCK_MODE", false)
        set(value) = put("LOCK_MODE", value)

    var autoShowKeyboard: Boolean
        get() = bool("AUTO_SHOW_KEYBOARD", true)
        set(value) = put("AUTO_SHOW_KEYBOARD", value)

    /** Na busca da gaveta, abrir o app sozinho quando sobra um só. */
    var autoLaunchSearch: Boolean
        get() = bool("AUTO_LAUNCH_SEARCH", true)
        set(value) = put("AUTO_LAUNCH_SEARCH", value)

    var keyboardMessageShown: Boolean
        get() = bool("KEYBOARD_MESSAGE", false)
        set(value) = put("KEYBOARD_MESSAGE", value)

    var swipeLeftEnabled: Boolean
        get() = bool("SWIPE_LEFT_ENABLED", true)
        set(value) = put("SWIPE_LEFT_ENABLED", value)

    var swipeRightEnabled: Boolean
        get() = bool("SWIPE_RIGHT_ENABLED", true)
        set(value) = put("SWIPE_RIGHT_ENABLED", value)

    var appNameSwipeLeft: String
        get() = string("APP_NAME_SWIPE_LEFT")
        set(value) = put("APP_NAME_SWIPE_LEFT", value)

    var appNameSwipeRight: String
        get() = string("APP_NAME_SWIPE_RIGHT")
        set(value) = put("APP_NAME_SWIPE_RIGHT", value)

    var appPackageSwipeLeft: String
        get() = string("APP_PACKAGE_SWIPE_LEFT")
        set(value) = put("APP_PACKAGE_SWIPE_LEFT", value)

    var appPackageSwipeRight: String
        get() = string("APP_PACKAGE_SWIPE_RIGHT")
        set(value) = put("APP_PACKAGE_SWIPE_RIGHT", value)

    var appActivityClassNameSwipeLeft: String?
        get() = string("APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT")
        set(value) = put("APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT", value)

    var appActivityClassNameRight: String?
        get() = string("APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT")
        set(value) = put("APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT", value)

    var appUserSwipeLeft: String
        get() = string("APP_USER_SWIPE_LEFT")
        set(value) = put("APP_USER_SWIPE_LEFT", value)

    var appUserSwipeRight: String
        get() = string("APP_USER_SWIPE_RIGHT")
        set(value) = put("APP_USER_SWIPE_RIGHT", value)

    var shortcutIdSwipeLeft: String
        get() = string("SHORTCUT_ID_SWIPE_LEFT")
        set(value) = put("SHORTCUT_ID_SWIPE_LEFT", value)

    var isShortcutSwipeLeft: Boolean
        get() = bool("IS_SHORTCUT_SWIPE_LEFT", false)
        set(value) = put("IS_SHORTCUT_SWIPE_LEFT", value)

    var shortcutIdSwipeRight: String
        get() = string("SHORTCUT_ID_SWIPE_RIGHT")
        set(value) = put("SHORTCUT_ID_SWIPE_RIGHT", value)

    var isShortcutSwipeRight: Boolean
        get() = bool("IS_SHORTCUT_SWIPE_RIGHT", false)
        set(value) = put("IS_SHORTCUT_SWIPE_RIGHT", value)

    var clockAppPackage: String
        get() = string("CLOCK_APP_PACKAGE")
        set(value) = put("CLOCK_APP_PACKAGE", value)

    var clockAppUser: String
        get() = string("CLOCK_APP_USER")
        set(value) = put("CLOCK_APP_USER", value)

    var clockAppClassName: String?
        get() = string("CLOCK_APP_CLASS_NAME")
        set(value) = put("CLOCK_APP_CLASS_NAME", value)

    var calendarAppPackage: String
        get() = string("CALENDAR_APP_PACKAGE")
        set(value) = put("CALENDAR_APP_PACKAGE", value)

    var calendarAppUser: String
        get() = string("CALENDAR_APP_USER")
        set(value) = put("CALENDAR_APP_USER", value)

    var calendarAppClassName: String?
        get() = string("CALENDAR_APP_CLASS_NAME")
        set(value) = put("CALENDAR_APP_CLASS_NAME", value)

    var screenTimeAppPackage: String
        get() = string("SCREEN_TIME_APP_PACKAGE")
        set(value) = put("SCREEN_TIME_APP_PACKAGE", value)

    var screenTimeAppUser: String
        get() = string("SCREEN_TIME_APP_USER")
        set(value) = put("SCREEN_TIME_APP_USER", value)

    var screenTimeAppClassName: String?
        get() = string("SCREEN_TIME_APP_CLASS_NAME")
        set(value) = put("SCREEN_TIME_APP_CLASS_NAME", value)

    // Gaveta de apps

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet("HIDDEN_APPS", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set(value) = put("HIDDEN_APPS", value)

    var hiddenAppsUpdated: Boolean
        get() = bool("HIDDEN_APPS_UPDATED", false)
        set(value) = put("HIDDEN_APPS_UPDATED", value)

    fun updateAppActivityClassName(packageName: String, activityClassName: String) {
        if (clockAppPackage == packageName) clockAppClassName = activityClassName
        if (calendarAppPackage == packageName) calendarAppClassName = activityClassName
        if (screenTimeAppPackage == packageName) screenTimeAppClassName = activityClassName
        if (appPackageSwipeLeft == packageName) appActivityClassNameSwipeLeft = activityClassName
        if (appPackageSwipeRight == packageName) appActivityClassNameRight = activityClassName
    }

    // Nome escolhido para um app na gaveta (a tela inicial guarda o seu próprio, por item)
    fun getAppRenameLabel(appPackage: String): String = string(RENAME_PREFIX + appPackage)

    fun setAppRenameLabel(appPackage: String, renameLabel: String) = put(RENAME_PREFIX + appPackage, renameLabel)

    companion object {
        private const val PREFS_FILENAME = "app.pauca"
        private const val RENAME_PREFIX = "RENAME:"
    }
}
