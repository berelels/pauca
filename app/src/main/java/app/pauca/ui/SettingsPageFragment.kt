package app.pauca.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import app.pauca.BuildConfig
import app.pauca.MainActivity
import app.pauca.MainViewModel
import app.pauca.R
import app.pauca.data.Accent
import app.pauca.data.Constants
import app.pauca.data.HomeGroup
import app.pauca.data.HomeStore
import app.pauca.data.Palette
import app.pauca.data.Prefs
import app.pauca.data.Profile
import app.pauca.databinding.FragmentSettingsBinding
import app.pauca.focus.FocusManager
import app.pauca.helper.Edition
import app.pauca.helper.OlDialog
import app.pauca.helper.appUsagePermissionGranted
import app.pauca.helper.copyToClipboard
import app.pauca.helper.createDialog
import app.pauca.helper.dpToPx
import app.pauca.helper.hideStatusBar
import app.pauca.helper.isAccessServiceEnabled
import app.pauca.helper.isDefaultHome
import app.pauca.helper.openAppInfo
import app.pauca.helper.openUrl
import app.pauca.helper.showInputDialog
import app.pauca.helper.showPopupMenu
import app.pauca.helper.showStatusBar
import app.pauca.helper.showToast
import app.pauca.listener.DeviceAdmin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Todas as telas de ajustes. A página vem no argumento [Constants.Key.PAGE];
 * sem argumento, é a tela principal.
 */
class SettingsPageFragment : BaseFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var prefs: Prefs
    private lateinit var palette: Palette
    private lateinit var ui: SettingsBuilder
    private var dialog: OlDialog? = null

    private val page: String get() = arguments?.getString(Constants.Key.PAGE) ?: MAIN

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        restyle()
        viewModel.updateSwipeApps.observe(viewLifecycleOwner) { ui.refresh() }
    }

    /** Cores da página; chamado de novo quando a cor de destaque muda. */
    private fun restyle() {
        palette = prefs.palette.forScreens
        binding.root.applyScreenStyle(palette)
        ui = SettingsBuilder(requireContext(), binding.content, palette)
        ui.onLocked = { Edition.showUpsell(requireContext()) { showDialog(it) } }

        val title = when (page) {
            Constants.Page.APPEARANCE -> R.string.appearance
            Constants.Page.CLOCK -> R.string.clock_and_date
            Constants.Page.FOCUS -> R.string.focus_mode
            Constants.Page.PROFILES -> R.string.profiles
            Constants.Page.GESTURES -> R.string.gestures
            Constants.Page.ABOUT -> R.string.about
            else -> R.string.settings
        }
        binding.topBar.style(palette, getString(title), onBack = { findNavController().popBackStack() })
    }

    override fun onResume() {
        super.onResume()
        // Reconstrói ao voltar: permissões concedidas nos ajustes do sistema mudam a página
        build()
    }

    private fun build() {
        val scroll = binding.scroll.scrollY
        ui.clear()
        when (page) {
            Constants.Page.APPEARANCE -> buildAppearance()
            Constants.Page.CLOCK -> buildClock()
            Constants.Page.FOCUS -> buildFocus()
            Constants.Page.PROFILES -> buildProfiles()
            Constants.Page.GESTURES -> buildGestures()
            Constants.Page.ABOUT -> buildAbout()
            else -> buildMain()
        }
        binding.scroll.post { binding.scroll.scrollTo(0, scroll) }
    }

    // Principal

    private fun buildMain() {
        val context = requireContext()
        ui.header(getString(R.string.app_name), getString(R.string.settings_tagline))
        if (Edition.isLite) {
            ui.section(null)
            ui.row(getString(R.string.full_row), subtitle = { getString(R.string.full_row_summary) }, chevron = true) { ui.onLocked() }
        }

        ui.section(getString(R.string.section_home))
        ui.row(getString(R.string.edit_apps), chevron = true) { go(R.id.editHomeFragment) }
        ui.row(getString(R.string.profiles), value = { HomeStore.load(context).active.name }, chevron = true) { openPage(Constants.Page.PROFILES) }
        ui.row(getString(R.string.appearance), value = { getString(prefs.palette.label) }, chevron = true) { openPage(Constants.Page.APPEARANCE) }
        ui.row(getString(R.string.clock_and_date), chevron = true) { openPage(Constants.Page.CLOCK) }

        ui.section(getString(R.string.focus_mode))
        if (Edition.isLite) ui.locked {
            ui.row(getString(R.string.focus_mode), subtitle = { getString(R.string.focus_lite_summary) }) {}
        } else {
            ui.toggle(
                getString(R.string.focus_mode),
                subtitle = { focusStatus() },
                get = { prefs.focusActive },
                set = { toggleFocus() },
            )
            ui.row(getString(R.string.focus_configure), chevron = true) { openPage(Constants.Page.FOCUS) }
        }

        ui.section(getString(R.string.section_drawer))
        ui.row(getString(R.string.gestures), subtitle = { getString(R.string.gestures_summary) }, chevron = true) { openPage(Constants.Page.GESTURES) }
        ui.row(getString(R.string.hidden_apps), chevron = true) { showHiddenApps() }
        ui.toggle(
            getString(R.string.auto_show_keyboard),
            get = { prefs.autoShowKeyboard },
            set = { prefs.autoShowKeyboard = it; true },
        )

        ui.section(getString(R.string.section_system))
        ui.row(getString(R.string.language), value = { languageLabel(currentLanguage()) }) { anchor ->
            SettingsBuilder.choose(anchor, languageOptions(), currentLanguage()) { tag ->
                if (tag == prefs.language) return@choose
                prefs.language = tag
                // Recria a tela já no idioma novo
                requireActivity().recreate()
            }
        }
        ui.row(
            getString(R.string.default_launcher),
            value = { getString(if (isDefaultHome(context)) R.string.yes else R.string.no) },
            accentValue = !isDefaultHome(context),
        ) { viewModel.resetLauncherLiveData.call() }
        ui.row(getString(R.string.app_info)) { openAppInfo(context, Process.myUserHandle(), BuildConfig.APPLICATION_ID) }
        ui.row(getString(R.string.tour_replay)) {
            HomeFragment.replayTour = true
            findNavController().popBackStack(R.id.mainFragment, false)
        }
        ui.row(getString(R.string.about), chevron = true) { openPage(Constants.Page.ABOUT) }

        ui.section(getString(R.string.section_developer))
        developerRow()
        ui.note(getString(R.string.settings_hint))
    }

    private fun developerRow() {
        ui.row(
            Constants.DEVELOPER_NAME,
            subtitle = { getString(R.string.developer_on_github, "@" + Constants.URL_DEVELOPER_GITHUB.substringAfterLast('/')) },
            chevron = true,
        ) { requireContext().openUrl(Constants.URL_DEVELOPER_GITHUB) }
    }

    private fun currentLanguage(): String = prefs.language

    private fun languageOptions() = listOf(
        "English" to "en",
        "Português (Brasil)" to "pt-BR",
        "Español" to "es",
        getString(R.string.language_system) to "",
    )

    private fun languageLabel(tag: String): String =
        languageOptions().firstOrNull { it.second.equals(tag, ignoreCase = true) }?.first
            ?: languageOptions().firstOrNull { tag.isNotEmpty() && it.second.substringBefore('-') == tag.substringBefore('-') }?.first
            ?: getString(R.string.language_system)

    // Aparência

    private fun buildAppearance() {
        ui.section(getString(R.string.theme))
        ui.palettes(current = { prefs.paletteId }, locked = { !Edition.hasPalette(it.id) }) { picked ->
            // "Fundo" abre o editor (imagem, desfoque, brilho) antes de aplicar
            if (picked.showsWallpaper) return@palettes go(R.id.wallpaperFragment)
            if (picked.id == prefs.paletteId) return@palettes
            val themeChanges = picked.isDark != prefs.palette.isDark
            prefs.paletteId = picked.id
            // Claro/escuro muda o tema do AppCompat inteiro: recria a activity
            if (themeChanges) requireActivity().recreate()
            else {
                (requireActivity() as MainActivity).applyBackground()
                restyle()
                build()
            }
        }
        if (prefs.palette.showsWallpaper) ui.row(
            getString(R.string.wallpaper_title),
            value = {
                getString(
                    if (prefs.wallpaperSource == Constants.WallpaperSource.IMAGE) R.string.wallpaper_source_image
                    else R.string.wallpaper_source_system
                )
            },
            chevron = true,
        ) { go(R.id.wallpaperFragment) }

        ui.section(getString(R.string.accent_color))
        ui.accents(
            options = Accent.PRESETS.map { (hex, label) -> Triple(hex, getString(label), Accent.parse(hex)) } +
                    Triple(Accent.NONE, getString(R.string.accent_none), null),
            current = { prefs.accent },
            onPick = { setAccent(it) },
            customLabel = getString(R.string.accent_custom),
            onCustom = { askCustomAccent() },
            // null é a cor personalizada, que também é só da versão completa
            locked = { id -> if (id == null) Edition.isLite else !Edition.hasAccent(id) },
        )
        ui.note(getString(R.string.accent_hint))

        ui.section(getString(R.string.section_apps_text))
        val preview = homePreview()
        ui.custom(preview.first, preview.second)
        ui.locked {
            ui.row(getString(R.string.home_style), value = { styleLabel(prefs.homeStyle) }) { anchor ->
                SettingsBuilder.choose(anchor, listOf(
                    getString(R.string.style_cards) to Constants.HomeStyle.CARDS,
                    getString(R.string.style_list) to Constants.HomeStyle.LIST,
                ), prefs.homeStyle) { prefs.homeStyle = it; ui.refresh() }
            }
            ui.row(getString(R.string.font), value = { fontLabel(prefs.appFont) }) { anchor ->
                SettingsBuilder.choose(anchor, fontOptions(), prefs.appFont) { prefs.appFont = it; ui.refresh() }
            }
        }
        ui.stepper(getString(R.string.text_size), get = { prefs.appTextSize }, set = { prefs.appTextSize = it }, min = 16, max = 56, step = 2)
        ui.locked {
            ui.row(getString(R.string.weight), value = { weightLabel(prefs.appWeight) }) { anchor ->
                SettingsBuilder.choose(anchor, weightOptions(), prefs.appWeight) { prefs.appWeight = it; ui.refresh() }
            }
            ui.row(getString(R.string.letters), value = { caseLabel(prefs.textCase) }) { anchor ->
                SettingsBuilder.choose(anchor, listOf(
                    getString(R.string.case_lower) to Constants.TextCase.LOWER,
                    getString(R.string.case_as_typed) to Constants.TextCase.AS_TYPED,
                    getString(R.string.case_upper) to Constants.TextCase.UPPER,
                ), prefs.textCase) { prefs.textCase = it; ui.refresh() }
            }
            ui.row(getString(R.string.alignment), value = { alignmentLabel(prefs.homeAlignment) }) { anchor ->
                SettingsBuilder.choose(anchor, alignmentOptions(), prefs.homeAlignment) { prefs.homeAlignment = it; ui.refresh() }
            }
            ui.row(getString(R.string.position), value = { verticalLabel(prefs.homeVertical) }) { anchor ->
                SettingsBuilder.choose(anchor, listOf(
                    getString(R.string.top) to Constants.Vertical.TOP,
                    getString(R.string.center) to Constants.Vertical.CENTER,
                    getString(R.string.bottom) to Constants.Vertical.BOTTOM,
                ), prefs.homeVertical) { prefs.homeVertical = it; ui.refresh() }
            }
        }
        ui.locked {
            ui.section(getString(R.string.section_buttons))
            ui.row(getString(R.string.top_bar), value = { barModeLabel(prefs.topBarMode) }) { anchor ->
                SettingsBuilder.choose(anchor, barModeOptions(), prefs.topBarMode) { prefs.topBarMode = it; ui.refresh() }
            }
            ui.toggle(getString(R.string.button_settings), get = { prefs.showSettingsButton }, set = { prefs.showSettingsButton = it; true })
            ui.toggle(getString(R.string.button_edit), get = { prefs.showEditButton }, set = { prefs.showEditButton = it; true })
            ui.section(null)
            ui.row(getString(R.string.bottom_bar), value = { barModeLabel(prefs.bottomBarMode) }) { anchor ->
                SettingsBuilder.choose(anchor, barModeOptions(), prefs.bottomBarMode) { prefs.bottomBarMode = it; ui.refresh() }
            }
            ui.toggle(getString(R.string.button_theme), get = { prefs.showThemeButton }, set = { prefs.showThemeButton = it; true })
            ui.toggle(getString(R.string.button_profile), get = { prefs.showProfileButton }, set = { prefs.showProfileButton = it; true })
            if (!Edition.isLite) ui.toggle(getString(R.string.button_focus), get = { prefs.showFocusButton }, set = { prefs.showFocusButton = it; true })
        }
        if (!Edition.isLite) ui.note(getString(R.string.buttons_hint))

        ui.locked {
            ui.section(getString(R.string.section_screen))
            ui.toggle(
                getString(R.string.status_bar_show),
                subtitle = { getString(R.string.status_bar_summary) },
                get = { prefs.showStatusBar },
                set = {
                    prefs.showStatusBar = it
                    if (it) requireActivity().window.showStatusBar() else requireActivity().window.hideStatusBar()
                    true
                },
            )
        }
    }

    private fun setAccent(accent: String) {
        if (accent == prefs.accent) return
        prefs.accent = accent
        // As cores da própria página mudam junto
        restyle()
        build()
    }

    private fun askCustomAccent() {
        val initial = prefs.accent.takeIf { it != Accent.NONE && Accent.PRESETS.none { p -> p.first == it } } ?: ""
        requireContext().showInputDialog(getString(R.string.accent_custom), initial, hint = Accent.DEFAULT) { input ->
            val hex = Accent.normalize(input)
            if (hex == null) requireContext().showToast(R.string.invalid_color)
            else setAccent(hex)
        }
    }

    private fun barModeOptions() = listOf(
        getString(R.string.bar_always) to Constants.BarMode.ALWAYS,
        getString(R.string.bar_auto) to Constants.BarMode.AUTO,
        getString(R.string.bar_hidden) to Constants.BarMode.HIDDEN,
    )

    private fun barModeLabel(mode: Int) = barModeOptions().firstOrNull { it.second == mode }?.first ?: getString(R.string.bar_always)

    /** Um cartão com dois apps de exemplo, desenhado com as escolhas atuais. */
    private fun homePreview(): Pair<View, () -> Unit> {
        val home = prefs.palette
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dpToPx(), 14.dpToPx(), 14.dpToPx(), 14.dpToPx())
        }
        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
        }
        box.addView(inner)
        val labels = listOf(getString(R.string.preview_app_1), getString(R.string.preview_app_2)).map {
            TextView(requireContext()).apply {
                tag = it
                setTextColor(home.text)
                setPadding(0, 4.dpToPx(), 0, 4.dpToPx())
                inner.addView(this, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
        val update = {
            box.background = Look.rounded(if (home.showsWallpaper) 0xFF3B3631.toInt() else home.bg, 18)
            inner.background = if (prefs.homeStyle == Constants.HomeStyle.CARDS) Look.rounded(home.card, 18) else null
            labels.forEach {
                it.text = Look.applyCase(it.tag as String, prefs.textCase)
                it.textSize = prefs.appTextSize.toFloat()
                it.gravity = prefs.homeAlignment
                Look.applyFont(it, prefs.appFont, prefs.appWeight, prefs.appTextSize.toFloat())
            }
        }
        return box to update
    }

    // Relógio

    private fun buildClock() {
        ui.section(null)
        val preview = clockPreview()
        ui.custom(preview.first, preview.second)

        ui.section(getString(R.string.section_show))
        ui.row(getString(R.string.show), value = { dateTimeLabel(prefs.dateTimeVisibility) }) { anchor ->
            SettingsBuilder.choose(anchor, listOf(
                getString(R.string.clock_and_date) to Constants.DateTime.ON,
                getString(R.string.clock_only) to Constants.DateTime.TIME_ONLY,
                getString(R.string.date_only) to Constants.DateTime.DATE_ONLY,
                getString(R.string.nothing) to Constants.DateTime.OFF,
            ), prefs.dateTimeVisibility) { prefs.dateTimeVisibility = it; ui.refresh() }
        }

        ui.section(getString(R.string.section_clock))
        ui.locked {
            ui.row(getString(R.string.time_format), value = { timeFormatLabel(prefs.clockPattern) }) { anchor ->
                val options = listOf(
                    getString(R.string.format_auto) to "",
                    "24h · 19:42" to "HH:mm",
                    "12h · 7:42" to "h:mm",
                    "12h · 7:42 PM" to "h:mm a",
                    getString(R.string.format_seconds) to "HH:mm:ss",
                    getString(R.string.format_custom) to CUSTOM,
                )
                SettingsBuilder.choose(anchor, options, prefs.clockPattern.takeIf { p -> options.any { it.second == p } } ?: CUSTOM) {
                    if (it == CUSTOM) askPattern(getString(R.string.time_format), prefs.clockPattern.ifEmpty { "HH:mm" }) { p -> prefs.clockPattern = p }
                    else prefs.clockPattern = it
                    ui.refresh()
                }
            }
            ui.row(getString(R.string.font), value = { fontLabel(prefs.clockFont) }) { anchor ->
                SettingsBuilder.choose(anchor, fontOptions(), prefs.clockFont) { prefs.clockFont = it; ui.refresh() }
            }
        }
        ui.stepper(getString(R.string.size), get = { prefs.clockSize }, set = { prefs.clockSize = it }, min = 28, max = 160, step = 4)
        ui.locked {
            ui.row(getString(R.string.weight), value = { weightLabel(prefs.clockWeight) }) { anchor ->
                SettingsBuilder.choose(anchor, weightOptions(), prefs.clockWeight) { prefs.clockWeight = it; ui.refresh() }
            }
            ui.row(getString(R.string.alignment), value = { alignmentLabel(prefs.clockAlignment) }) { anchor ->
                SettingsBuilder.choose(anchor, alignmentOptions(), prefs.clockAlignment) { prefs.clockAlignment = it; ui.refresh() }
            }
            ui.toggle(getString(R.string.clock_accent), get = { prefs.clockAccent }, set = { prefs.clockAccent = it; true })
        }

        ui.section(getString(R.string.section_date))
        ui.locked {
            ui.row(getString(R.string.date_format), value = { formatDate(prefs.datePattern.ifEmpty { defaultDatePattern() }) }) { anchor ->
                val patterns = listOf("", "EEE, d MMM", "EEEE", "d MMM yyyy", "dd/MM/yyyy", "dd/MM")
                val options = patterns.map { formatDate(it.ifEmpty { defaultDatePattern() }) to it } +
                        (getString(R.string.format_custom) to CUSTOM)
                SettingsBuilder.choose(anchor, options, prefs.datePattern.takeIf { it in patterns } ?: CUSTOM) {
                    if (it == CUSTOM) askPattern(getString(R.string.date_format), prefs.datePattern.ifEmpty { defaultDatePattern() }) { p -> prefs.datePattern = p }
                    else prefs.datePattern = it
                    ui.refresh()
                }
            }
        }
        ui.toggle(getString(R.string.show_battery), get = { prefs.showBattery }, set = { prefs.showBattery = it; true })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ui.toggle(
            getString(R.string.show_screen_time),
            subtitle = { getString(R.string.show_screen_time_summary) },
            get = { prefs.showScreenTime && requireContext().appUsagePermissionGranted() },
            set = { on ->
                if (on && !requireContext().appUsagePermissionGranted()) {
                    prefs.showScreenTime = true
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    false
                } else {
                    prefs.showScreenTime = on
                    true
                }
            },
        )

        ui.section(getString(R.string.section_on_tap))
        ui.row(getString(R.string.clock_tap_app), value = { appLabelFor(prefs.clockAppPackage) }) { pickApp(Constants.FLAG_SET_CLOCK_APP) }
        ui.row(getString(R.string.date_tap_app), value = { appLabelFor(prefs.calendarAppPackage) }) { pickApp(Constants.FLAG_SET_CALENDAR_APP) }
        ui.note(getString(R.string.clock_hint))
    }

    private fun clockPreview(): Pair<View, () -> Unit> {
        val home = prefs.palette
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22.dpToPx(), 18.dpToPx(), 22.dpToPx(), 20.dpToPx())
        }
        val clock = TextClock(requireContext()).apply { includeFontPadding = false }
        val date = TextClock(requireContext()).apply { textSize = 15f }
        box.addView(clock)
        box.addView(date)
        val update = {
            box.background = Look.rounded(if (home.showsWallpaper) 0xFF3B3631.toInt() else home.bg, 18)
            val pattern = prefs.clockPattern
            val timePattern = Look.timePattern(requireContext(), pattern)
            clock.format12Hour = timePattern
            clock.format24Hour = timePattern
            clock.textSize = prefs.clockSize.coerceAtMost(96).toFloat()
            clock.setTextColor(if (prefs.clockAccent) home.accent else home.text)
            Look.applyFont(clock, prefs.clockFont, prefs.clockWeight, prefs.clockSize.toFloat())
            val datePattern = prefs.datePattern.ifEmpty { defaultDatePattern() }
            date.format12Hour = datePattern
            date.format24Hour = datePattern
            date.setTextColor(home.muted)
            Look.applyFont(date, Constants.Font.JAKARTA, 400)
            box.gravity = prefs.clockAlignment
            clock.gravity = prefs.clockAlignment
            date.gravity = prefs.clockAlignment
            clock.setShown(Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility))
            date.setShown(Constants.DateTime.isDateVisible(prefs.dateTimeVisibility))
        }
        return box to update
    }

    private fun View.setShown(visible: Boolean) {
        visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun askPattern(title: String, initial: String, onValid: (String) -> Unit) {
        requireContext().showInputDialog(title, initial, hint = "HH:mm") { pattern ->
            try {
                SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
                onValid(pattern)
                ui.refresh()
            } catch (_: Exception) {
                requireContext().showToast(R.string.invalid_pattern)
            }
        }
    }

    private fun defaultDatePattern() = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")

    private fun formatDate(pattern: String): String = try {
        DateFormat.format(pattern, Date()).toString()
    } catch (_: Exception) {
        pattern
    }

    // Modo foco

    private fun buildFocus() {
        val context = requireContext()
        ui.section(null)
        ui.toggle(
            getString(R.string.focus_mode),
            subtitle = { focusStatus() },
            get = { prefs.focusActive },
            set = { toggleFocus() },
        )
        ui.note(getString(R.string.focus_intro))

        ui.section(getString(R.string.section_permissions))
        ui.row(
            getString(R.string.perm_notifications),
            subtitle = { getString(R.string.perm_notifications_summary) },
            value = { permissionLabel(FocusManager.hasListenerAccess(context)) },
            accentValue = !FocusManager.hasListenerAccess(context),
        ) { openSystem(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
        ui.row(
            getString(R.string.perm_dnd),
            subtitle = { getString(R.string.perm_dnd_summary) },
            value = { permissionLabel(FocusManager.hasDndAccess(context)) },
            accentValue = !FocusManager.hasDndAccess(context),
        ) { openSystem(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) }
        ui.row(
            getString(R.string.perm_accessibility),
            subtitle = { getString(R.string.perm_accessibility_summary) },
            value = { permissionLabel(isAccessServiceEnabled(context)) },
            accentValue = false,
        ) { showAccessibilityDialog() }

        ui.section(getString(R.string.section_silence))
        ui.toggle(
            getString(R.string.focus_dnd),
            subtitle = { getString(R.string.focus_dnd_summary) },
            get = { prefs.focusDnd },
            set = { on ->
                prefs.focusDnd = on
                FocusManager.refresh(context)
                true
            },
        )
        ui.row(getString(R.string.focus_calls), value = { callersLabel(prefs.focusCalls) }) { anchor ->
            SettingsBuilder.choose(anchor, listOf(
                getString(R.string.callers_none) to Constants.Callers.NONE,
                getString(R.string.callers_starred) to Constants.Callers.STARRED,
                getString(R.string.callers_contacts) to Constants.Callers.CONTACTS,
                getString(R.string.callers_anyone) to Constants.Callers.ANYONE,
            ), prefs.focusCalls) {
                prefs.focusCalls = it
                FocusManager.refresh(context)
                ui.refresh()
            }
        }
        ui.toggle(
            getString(R.string.focus_repeat_callers),
            subtitle = { getString(R.string.focus_repeat_callers_summary) },
            get = { prefs.focusRepeatCallers },
            set = { prefs.focusRepeatCallers = it; FocusManager.refresh(context); true },
        )
        ui.toggle(
            getString(R.string.focus_hide_silenced),
            subtitle = { getString(R.string.focus_hide_silenced_summary) },
            get = { prefs.focusHideSilenced },
            set = { prefs.focusHideSilenced = it; FocusManager.refresh(context); true },
        )

        ui.section(getString(R.string.section_apps))
        ui.row(
            getString(R.string.focus_allowed_apps),
            subtitle = { getString(R.string.focus_allowed_summary) },
            value = { prefs.focusAllowed.size.takeIf { it > 0 }?.let { "+$it" } },
            chevron = true,
        ) {
            viewModel.getAppList(includeHiddenApps = true)
            go(R.id.appPickerFragment, bundleOf(Constants.Key.MODE to Constants.PickerMode.FOCUS_ALLOWED))
        }
        ui.toggle(
            getString(R.string.focus_filter),
            subtitle = { getString(R.string.focus_filter_summary) },
            get = { prefs.focusFilter },
            set = { on ->
                prefs.focusFilter = on
                if (on && !FocusManager.hasListenerAccess(context)) openSystem(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                true
            },
        )
        ui.toggle(
            getString(R.string.focus_hide_apps),
            subtitle = { getString(R.string.focus_hide_apps_summary) },
            get = { prefs.focusHideApps },
            set = { prefs.focusHideApps = it; true },
        )
        ui.toggle(
            getString(R.string.focus_block_apps),
            subtitle = { getString(R.string.focus_block_apps_summary) },
            get = { prefs.focusBlockApps },
            set = { on ->
                prefs.focusBlockApps = on
                FocusManager.refresh(context)
                if (on && !isAccessServiceEnabled(context)) showAccessibilityDialog()
                true
            },
        )
        ui.toggle(
            getString(R.string.focus_grayscale),
            subtitle = { getString(R.string.focus_grayscale_summary) },
            get = { prefs.focusGrayscale && FocusManager.canWriteSecureSettings(context) },
            set = { on ->
                if (on && !FocusManager.canWriteSecureSettings(context)) {
                    showGrayscaleHelp()
                    false
                } else {
                    prefs.focusGrayscale = on
                    FocusManager.refresh(context)
                    true
                }
            },
        )
        ui.note(getString(R.string.focus_profiles_hint))
    }

    private fun focusStatus(): String =
        if (prefs.focusActive) getString(R.string.focus_on_since, Look.shortTime(requireContext(), prefs.focusSince))
        else getString(R.string.focus_off)

    private fun toggleFocus(): Boolean {
        val context = requireContext()
        if (!prefs.focusActive && !FocusManager.hasDndAccess(context) && !FocusManager.hasListenerAccess(context)) {
            context.showToast(getString(R.string.focus_needs_permission), Toast.LENGTH_LONG)
            if (page != Constants.Page.FOCUS) openPage(Constants.Page.FOCUS)
            return false
        }
        FocusManager.toggle(context)
        return true
    }

    private fun showGrayscaleHelp() {
        val command = "adb shell pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"
        showDialog(requireContext().createDialog(
            title = getString(R.string.focus_grayscale),
            action = getString(R.string.copy_command),
            message = getString(R.string.grayscale_help, command),
            onAction = { requireContext().copyToClipboard(command) },
        ))
    }

    // Perfis

    private fun buildProfiles() {
        val context = requireContext()
        val data = HomeStore.load(context)
        ui.section(getString(R.string.profiles))
        data.profiles.forEach { profile ->
            ui.row(
                profile.name,
                subtitle = { profileSummary(profile) },
                value = { getString(R.string.in_use).takeIf { HomeStore.load(context).activeId == profile.id } },
                accentValue = true,
            ) { anchor -> showProfileMenu(anchor, profile) }
        }
        // No Lite: o perfil padrão e mais um
        ui.locked(!Edition.canAddProfile(data.profiles.size)) {
            ui.row("+  " + getString(R.string.new_profile), accentValue = true) {
                context.showInputDialog(getString(R.string.new_profile), "", hint = getString(R.string.profile_name_hint)) { name ->
                    if (name.isEmpty()) return@showInputDialog
                    val profile = Profile(name = name, groups = mutableListOf(HomeGroup()))
                    HomeStore.update(context) { it.profiles.add(profile) }
                    go(R.id.editHomeFragment, bundleOf(Constants.Key.PROFILE_ID to profile.id))
                }
            }
        }
        ui.note(getString(if (Edition.isLite) R.string.profiles_hint_lite else R.string.profiles_hint))
    }

    private fun profileSummary(profile: Profile): String {
        val count = resources.getQuantityString(R.plurals.apps_count, profile.allItems.size, profile.allItems.size)
        return if (profile.focusOnSwitch) "$count · ${getString(R.string.turns_on_focus)}" else count
    }

    private fun showProfileMenu(anchor: View, profile: Profile) {
        val context = requireContext()
        val data = HomeStore.load(context)
        anchor.showPopupMenu(configure = { menu ->
            if (data.activeId != profile.id) menu.add(0, 1, 0, R.string.use_profile)
            menu.add(0, 2, 1, R.string.edit_apps)
            menu.add(0, 3, 2, R.string.rename)
            if (!Edition.isLite) menu.add(0, 4, 3, if (profile.focusOnSwitch) R.string.focus_on_switch_off else R.string.focus_on_switch_on)
            if (data.profiles.size > 1) menu.add(0, 5, 4, R.string.delete)
        }) {
            when (it.itemId) {
                1 -> {
                    HomeStore.update(context) { d -> d.activeId = profile.id }
                    if (profile.focusOnSwitch) FocusManager.activate(context, Constants.FocusSource.PROFILE)
                    else if (prefs.focusActive && prefs.focusSource == Constants.FocusSource.PROFILE) FocusManager.deactivate(context)
                    build()
                }

                2 -> go(R.id.editHomeFragment, bundleOf(Constants.Key.PROFILE_ID to profile.id))
                3 -> context.showInputDialog(getString(R.string.rename), profile.name) { name ->
                    if (name.isNotEmpty()) {
                        HomeStore.update(context) { profile.name = name }
                        build()
                    }
                }

                4 -> {
                    HomeStore.update(context) { profile.focusOnSwitch = !profile.focusOnSwitch }
                    build()
                }

                5 -> showDialog(context.createDialog(
                    title = getString(R.string.delete_profile),
                    action = getString(R.string.delete),
                    message = getString(R.string.delete_profile_message, profile.name),
                    onAction = {
                        HomeStore.update(context) { d ->
                            d.profiles.remove(profile)
                            if (d.activeId == profile.id) d.activeId = d.profiles.first().id
                        }
                        build()
                    },
                ))
            }
        }
    }

    // Gestos

    private fun buildGestures() {
        ui.section(getString(R.string.gestures))
        ui.row(
            getString(R.string.swipe_left),
            value = { swipeLabel(left = true) },
        ) { anchor -> showSwipeMenu(anchor, left = true) }
        ui.row(
            getString(R.string.swipe_right),
            value = { swipeLabel(left = false) },
        ) { anchor -> showSwipeMenu(anchor, left = false) }
        ui.toggle(
            getString(R.string.double_tap_to_lock),
            subtitle = { getString(R.string.double_tap_summary) },
            get = { lockEnabled() },
            set = { toggleLockMode() },
        )
        ui.note(getString(R.string.gestures_hint))
    }

    private fun swipeLabel(left: Boolean): String {
        val enabled = if (left) prefs.swipeLeftEnabled else prefs.swipeRightEnabled
        if (!enabled) return getString(R.string.off)
        val name = if (left) prefs.appNameSwipeLeft else prefs.appNameSwipeRight
        return name.ifEmpty { getString(if (left) R.string.camera else R.string.phone) }
    }

    private fun showSwipeMenu(anchor: View, left: Boolean) {
        val enabled = if (left) prefs.swipeLeftEnabled else prefs.swipeRightEnabled
        anchor.showPopupMenu(configure = { menu ->
            menu.add(0, 1, 0, R.string.choose_app)
            menu.add(0, 2, 1, if (enabled) R.string.turn_off else R.string.turn_on)
        }) {
            when (it.itemId) {
                1 -> {
                    if (left) prefs.swipeLeftEnabled = true else prefs.swipeRightEnabled = true
                    pickApp(if (left) Constants.FLAG_SET_SWIPE_LEFT_APP else Constants.FLAG_SET_SWIPE_RIGHT_APP)
                }

                2 -> {
                    if (left) prefs.swipeLeftEnabled = !enabled else prefs.swipeRightEnabled = !enabled
                    ui.refresh()
                }
            }
        }
    }

    private fun lockEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) prefs.lockModeOn && isAccessServiceEnabled(requireContext())
        else prefs.lockModeOn

    private fun toggleLockMode(): Boolean {
        val context = requireContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!prefs.lockModeOn && !isAccessServiceEnabled(context)) {
                showAccessibilityDialog()
                return false
            }
            prefs.lockModeOn = !prefs.lockModeOn
            return true
        }
        val deviceManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, DeviceAdmin::class.java)
        if (deviceManager.isAdminActive(componentName)) {
            deviceManager.removeActiveAdmin(componentName)
            prefs.lockModeOn = false
        } else {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_permission_message))
            @Suppress("DEPRECATION")
            requireActivity().startActivityForResult(intent, Constants.REQUEST_CODE_ENABLE_ADMIN)
        }
        return true
    }

    /** Aviso claro antes de mandar para a acessibilidade (exigência do Google Play). */
    private fun showAccessibilityDialog() {
        val enabled = isAccessServiceEnabled(requireContext())
        showDialog(requireContext().createDialog(
            title = R.string.perm_accessibility,
            action = if (enabled) R.string.disable else R.string.enable,
            message = R.string.accessibility_disclosure,
            onAction = { openSystem(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
        ))
    }

    // Sobre

    private fun buildAbout() {
        ui.header(getString(R.string.app_name), getString(R.string.version, BuildConfig.VERSION_NAME))
        ui.section(getString(R.string.section_developer))
        developerRow()
        ui.section(null)
        ui.row(getString(R.string.about_olauncher), subtitle = { getString(R.string.about_olauncher_summary) }, chevron = true) {
            requireContext().openUrl(Constants.URL_OLAUNCHER_GITHUB)
        }
        ui.row(getString(R.string.about_license), subtitle = { getString(R.string.about_license_summary) }) {}
        ui.row(getString(R.string.about_fonts), subtitle = { getString(R.string.about_fonts_summary) }) {}
        ui.note(getString(R.string.about_privacy))
    }

    // Utilidades

    private fun showHiddenApps() {
        if (prefs.hiddenApps.isEmpty()) {
            requireContext().showToast(getString(R.string.no_hidden_apps))
            return
        }
        viewModel.getHiddenApps()
        go(R.id.appListFragment, bundleOf(Constants.Key.FLAG to Constants.FLAG_HIDDEN_APPS))
    }

    private fun pickApp(flag: Int) {
        viewModel.getAppList(true)
        go(R.id.appListFragment, bundleOf(Constants.Key.FLAG to flag))
    }

    private fun appLabelFor(pkg: String): String {
        if (pkg.isBlank()) return getString(R.string.system_default)
        val pm = requireContext().packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    private fun openPage(page: String) = go(R.id.settingsPageFragment, bundleOf(Constants.Key.PAGE to page))

    private fun go(destination: Int, args: Bundle? = null) {
        val options = NavOptions.Builder()
            .setEnterAnim(R.anim.fade_enter)
            .setExitAnim(R.anim.fade_exit)
            .setPopEnterAnim(R.anim.fade_enter)
            .setPopExitAnim(R.anim.fade_exit)
            .build()
        try {
            findNavController().navigate(destination, args, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSystem(action: String) {
        try {
            startActivity(Intent(action))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun showDialog(newDialog: OlDialog) {
        dialog?.dismiss()
        dialog = newDialog
        newDialog.showRespectingStatusBar()
    }

    private fun permissionLabel(granted: Boolean) = getString(if (granted) R.string.granted else R.string.grant)

    private fun styleLabel(v: Int) = getString(if (v == Constants.HomeStyle.LIST) R.string.style_list else R.string.style_cards)

    private fun fontOptions() = listOf(
        Constants.Font.JAKARTA,
        Constants.Font.NEWSREADER,
        Constants.Font.SYSTEM,
        Constants.Font.SYSTEM_SERIF,
        Constants.Font.MONO,
    ).map { fontLabel(it) to it }

    private fun fontLabel(font: String) = getString(
        when (font) {
            Constants.Font.NEWSREADER -> R.string.font_serif
            Constants.Font.SYSTEM -> R.string.font_system
            Constants.Font.SYSTEM_SERIF -> R.string.font_system_serif
            Constants.Font.MONO -> R.string.font_mono
            else -> R.string.font_sans
        }
    )

    private fun weightOptions() = listOf(
        getString(R.string.weight_thin) to 200,
        getString(R.string.weight_light) to 300,
        getString(R.string.weight_regular) to 400,
        getString(R.string.weight_medium) to 500,
        getString(R.string.weight_bold) to 700,
        getString(R.string.weight_black) to 800,
    )

    private fun weightLabel(w: Int) = weightOptions().minByOrNull { kotlin.math.abs(it.second - w) }!!.first

    private fun caseLabel(v: Int) = getString(
        when (v) {
            Constants.TextCase.UPPER -> R.string.case_upper
            Constants.TextCase.AS_TYPED -> R.string.case_as_typed
            else -> R.string.case_lower
        }
    )

    private fun alignmentOptions() = listOf(
        getString(R.string.left) to Gravity.START,
        getString(R.string.center) to Gravity.CENTER_HORIZONTAL,
        getString(R.string.right) to Gravity.END,
    )

    private fun alignmentLabel(g: Int) = alignmentOptions().firstOrNull { it.second == g }?.first ?: getString(R.string.left)

    private fun verticalLabel(v: Int) = getString(
        when (v) {
            Constants.Vertical.CENTER -> R.string.center
            Constants.Vertical.BOTTOM -> R.string.bottom
            else -> R.string.top
        }
    )

    private fun dateTimeLabel(v: Int) = getString(
        when (v) {
            Constants.DateTime.TIME_ONLY -> R.string.clock_only
            Constants.DateTime.DATE_ONLY -> R.string.date_only
            Constants.DateTime.OFF -> R.string.nothing
            else -> R.string.clock_and_date
        }
    )

    private fun timeFormatLabel(pattern: String): String = when (pattern) {
        "" -> getString(R.string.format_auto)
        else -> formatDate(pattern)
    }

    private fun callersLabel(v: Int) = getString(
        when (v) {
            Constants.Callers.ANYONE -> R.string.callers_anyone
            Constants.Callers.CONTACTS -> R.string.callers_contacts
            Constants.Callers.NONE -> R.string.callers_none
            else -> R.string.callers_starred
        }
    )

    override fun onDestroyView() {
        dialog?.dismiss()
        dialog = null
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MAIN = "main"
        const val CUSTOM = "\u0000custom"
    }
}
