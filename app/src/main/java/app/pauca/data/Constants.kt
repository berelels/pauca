package app.pauca.data

object Constants {

    object Key {
        const val FLAG = "flag"
        const val RENAME = "rename"
        const val PROFILE_ID = "profileId"
        const val GROUP_ID = "groupId"
        const val ITEM_ID = "itemId"
        const val MODE = "mode"
        const val PAGE = "page"
    }

    object Dialog {
        const val ABOUT = "ABOUT"
        const val HIDDEN = "HIDDEN"
        const val KEYBOARD = "KEYBOARD"
        const val DIGITAL_WELLBEING = "DIGITAL_WELLBEING"
    }

    object DateTime {
        const val OFF = 0
        const val ON = 1
        const val DATE_ONLY = 2
        const val TIME_ONLY = 3

        fun isTimeVisible(dateTimeVisibility: Int): Boolean =
            dateTimeVisibility == ON || dateTimeVisibility == TIME_ONLY

        fun isDateVisible(dateTimeVisibility: Int): Boolean =
            dateTimeVisibility == ON || dateTimeVisibility == DATE_ONLY
    }

    object HomeStyle {
        const val CARDS = 0
        const val LIST = 1
    }

    object Font {
        const val JAKARTA = "jakarta"
        const val NEWSREADER = "newsreader"
        const val SYSTEM = "system"
        const val SYSTEM_SERIF = "serif"
        const val MONO = "mono"
    }

    object WallpaperSource {
        const val SYSTEM = "system"
        const val IMAGE = "image"
    }

    /** Como as barras de botões da tela inicial aparecem. */
    object BarMode {
        const val HIDDEN = 0
        const val ALWAYS = 1
        /** Escondida; tocar na faixa dela mostra por alguns segundos. */
        const val AUTO = 2
    }

    object TextCase {
        const val LOWER = 0
        const val AS_TYPED = 1
        const val UPPER = 2
    }

    object Vertical {
        const val TOP = 0
        const val CENTER = 1
        const val BOTTOM = 2
    }

    object FocusSource {
        const val MANUAL = "manual"
        const val PROFILE = "profile"
    }

    /** Mesmos valores de ZenPolicy.PEOPLE_TYPE_*. */
    object Callers {
        const val ANYONE = 1
        const val CONTACTS = 2
        const val STARRED = 3
        const val NONE = 4
    }

    /** O que o seletor de apps está escolhendo. */
    object PickerMode {
        const val HOME = 0
        const val REPLACE = 1
        const val FOCUS_ALLOWED = 2
    }

    object Page {
        const val APPEARANCE = "appearance"
        const val CLOCK = "clock"
        const val FOCUS = "focus"
        const val PROFILES = "profiles"
        const val GESTURES = "gestures"
        const val ABOUT = "about"
    }

    object CharacterIndicator {
        const val SHOW = 102
        const val HIDE = 101
    }

    val CLOCK_APP_PACKAGES = arrayOf(
        "com.google.android.deskclock", //Google Clock
        "com.sec.android.app.clockpackage", //Samsung Clock
        "com.oneplus.deskclock", //OnePlus Clock
        "com.miui.clock", //Xiaomi Clock
    )

    const val FLAG_LAUNCH_APP = 100
    const val FLAG_HIDDEN_APPS = 101

    const val FLAG_SET_SWIPE_LEFT_APP = 11
    const val FLAG_SET_SWIPE_RIGHT_APP = 12
    const val FLAG_SET_CLOCK_APP = 13
    const val FLAG_SET_CALENDAR_APP = 14
    const val FLAG_SET_SCREEN_TIME_APP = 15

    const val REQUEST_CODE_ENABLE_ADMIN = 666
    const val REQUEST_CODE_LAUNCHER_SELECTOR = 678

    const val LONG_PRESS_DELAY_MS = 500L
    const val ONE_DAY_IN_MILLIS = 86400000L
    const val ONE_HOUR_IN_MILLIS = 3600000L
    const val ONE_MINUTE_IN_MILLIS = 60000L

    const val MIN_ANIM_REFRESH_RATE = 30f

    /** Quanto tempo as barras no modo automático ficam visíveis. */
    const val BARS_AUTO_HIDE_MS = 5000L

    const val DEVELOPER_NAME = "Gabriel Dias"
    const val URL_DEVELOPER_GITHUB = "https://github.com/berelels"
    const val URL_OLAUNCHER_GITHUB = "https://github.com/tanujnotes/Olauncher"
    const val URL_DOUBLE_TAP = "https://tanujnotes.notion.site/Double-tap-to-lock-Olauncher-0f7fb103ec1f47d7a90cdfdcd7fb86ef"
    const val URL_DUCK_SEARCH = "https://duck.co/?q="

    const val DIGITAL_WELLBEING_PACKAGE_NAME = "com.google.android.apps.wellbeing"
    const val DIGITAL_WELLBEING_ACTIVITY = "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity"
    const val DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME = "com.samsung.android.forest"
    const val DIGITAL_WELLBEING_SAMSUNG_ACTIVITY = "com.samsung.android.forest.launcher.LauncherActivity"
}
