package app.pauca.ui

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import app.pauca.MainViewModel
import app.pauca.R
import app.pauca.data.AppModel
import app.pauca.data.Constants
import app.pauca.data.HomeItem
import app.pauca.data.HomeStore
import app.pauca.data.Palette
import app.pauca.data.Prefs
import app.pauca.databinding.FragmentHomeBinding
import app.pauca.focus.FocusManager
import app.pauca.helper.appUsagePermissionGranted
import app.pauca.helper.dpToPx
import app.pauca.helper.expandNotificationDrawer
import app.pauca.helper.getUserHandleFromString
import app.pauca.helper.hideStatusBar
import app.pauca.helper.isDefaultHome
import app.pauca.helper.isPackageInstalled
import app.pauca.helper.openAlarmApp
import app.pauca.helper.openAppInfo
import app.pauca.helper.openCalendar
import app.pauca.helper.openCameraApp
import app.pauca.helper.openDialerApp
import app.pauca.helper.showInputDialog
import app.pauca.helper.showListDialog
import app.pauca.helper.showPopupMenu
import app.pauca.helper.showStatusBar
import app.pauca.helper.showToast
import java.util.Locale

class HomeFragment : BaseFragment() {

    private lateinit var prefs: Prefs
    private lateinit var deviceManager: DevicePolicyManager
    private val viewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var batteryLevel = -1
    private var screenTime: String? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            batteryLevel = if (level >= 0 && scale > 0) level * 100 / scale else -1
            if (_binding != null) renderDateExtra()
        }
    }

    private val focusListener: () -> Unit = { if (_binding != null) render() }

    // Barras no modo automático: aparecem ao tocar na faixa delas e somem sozinhas
    private val handler = Handler(Looper.getMainLooper())
    private var barsRevealed = false
    private val hideBars = Runnable {
        barsRevealed = false
        if (_binding != null) applyBarVisibility(animate = true)
    }

    private var tour: TourView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        deviceManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        applyInsets()
        initGestures()
        initClicks()

        viewModel.refreshHome.observe(viewLifecycleOwner) { render() }
        viewModel.screenTimeValue.observe(viewLifecycleOwner) {
            screenTime = it
            renderDateExtra()
        }
        viewModel.isDefaultHome.observe(viewLifecycleOwner) { isDefault ->
            binding.setDefaultLauncher.isVisible = !isDefault && !prefs.hideSetDefaultLauncher
        }
    }

    override fun onResume() {
        super.onResume()
        if (prefs.showStatusBar) requireActivity().window.showStatusBar()
        else requireActivity().window.hideStatusBar()
        viewModel.isDefaultHome()
        render()
        maybeStartTour()
        FocusManager.addListener(focusListener)
        requireContext().registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (prefs.showScreenTime && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && requireContext().appUsagePermissionGranted())
            viewModel.getTodaysScreenTime()
    }

    override fun onPause() {
        FocusManager.removeListener(focusListener)
        handler.removeCallbacks(hideBars)
        barsRevealed = false
        // Saiu no meio do tour: some sem marcar como visto, e volta na próxima vez
        tour?.dismiss()
        tour = null
        try {
            requireContext().unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        super.onPause()
    }

    // Desenho

    private fun render() {
        val palette = prefs.palette
        binding.mainLayout.setBackgroundColor(palette.bg)
        renderBars(palette)
        renderClock(palette)
        renderDigest(palette)
        renderGroups(palette)
        binding.setDefaultLauncher.setTextColor(palette.accent)
    }

    private fun renderBars(palette: Palette) {
        val iconTint = ColorStateList.valueOf(palette.muted)
        listOf(binding.btnSettings, binding.btnEdit, binding.btnAppearance).forEach {
            it.imageTintList = iconTint
            it.background = Look.pressable(0, 22, palette.text)
        }
        val focusOn = prefs.focusActive
        binding.btnFocus.setImageResource(if (focusOn) R.drawable.ic_moon_filled else R.drawable.ic_moon)
        binding.btnFocus.imageTintList = ColorStateList.valueOf(if (focusOn) palette.accent else palette.muted)
        binding.btnFocus.background = Look.pressable(0, 24, palette.text)

        binding.focusChip.isVisible = focusOn
        if (focusOn) {
            val since = DateFormat.getTimeFormat(requireContext()).format(prefs.focusSince)
            binding.focusChip.text = getString(R.string.focus_chip, since)
            binding.focusChip.setTextColor(palette.accent)
            binding.focusChip.background = Look.rounded(ColorUtils.setAlphaComponent(palette.accent, 0x26), 16)
        }

        val profile = HomeStore.load(requireContext()).active
        binding.profilePill.text = profile.name
        binding.profilePill.setTextColor(palette.text)
        binding.profilePill.background = Look.pressable(palette.card, 22, palette.text)
        Look.applyFont(binding.profilePill, Constants.Font.JAKARTA, 600)
        applyBarVisibility(animate = false)
    }

    /**
     * Cada barra tem um modo (oculta, sempre visível, automática) e cada botão pode ser
     * desligado. No modo automático os botões ficam invisíveis mas ocupando o lugar, para
     * a tela não pular quando aparecem. Durante o tour, tudo que não está oculto aparece.
     */
    private fun applyBarVisibility(animate: Boolean) {
        val top = listOf(binding.btnSettings to prefs.showSettingsButton, binding.btnEdit to prefs.showEditButton)
        val bottom = listOf(
            binding.btnAppearance to prefs.showThemeButton,
            binding.profilePill to prefs.showProfileButton,
            binding.btnFocus to prefs.showFocusButton,
        )
        // A barra de cima também segura o aviso do foco, que aparece mesmo com os botões ocultos
        applyBar(binding.topBar, prefs.topBarMode, top, animate, keep = binding.focusChip.isVisible)
        applyBar(binding.bottomBar, prefs.bottomBarMode, bottom, animate, keep = false)
    }

    private fun applyBar(bar: View, mode: Int, items: List<Pair<View, Boolean>>, animate: Boolean, keep: Boolean) {
        val hidden = mode == Constants.BarMode.HIDDEN
        bar.isVisible = keep || (!hidden && items.any { it.second })
        val shown = mode == Constants.BarMode.ALWAYS || (mode == Constants.BarMode.AUTO && (barsRevealed || tour != null))
        items.forEach { (view, enabled) ->
            view.animate().cancel()
            when {
                hidden || !enabled -> view.visibility = View.GONE
                shown -> {
                    view.visibility = View.VISIBLE
                    if (animate) view.animate().alpha(1f).setDuration(180).start() else view.alpha = 1f
                }

                animate && view.visibility == View.VISIBLE ->
                    view.animate().alpha(0f).setDuration(240).withEndAction { view.visibility = View.INVISIBLE }.start()

                else -> {
                    view.alpha = 0f
                    view.visibility = View.INVISIBLE
                }
            }
        }
    }

    /** Toque no fundo: se foi na faixa de uma barra automática, mostra as barras por 5 s. */
    private fun onBackgroundTap(rawY: Float) {
        val slack = 32.dpToPx()
        val location = IntArray(2)
        val topBar = binding.topBar
        val bottomBar = binding.bottomBar
        topBar.getLocationOnScreen(location)
        val inTop = prefs.topBarMode == Constants.BarMode.AUTO && topBar.isVisible &&
                rawY <= location[1] + topBar.height + slack
        bottomBar.getLocationOnScreen(location)
        val inBottom = prefs.bottomBarMode == Constants.BarMode.AUTO && bottomBar.isVisible &&
                rawY >= location[1] - slack
        if (inTop || inBottom) revealBars()
    }

    private fun revealBars() {
        barsRevealed = true
        applyBarVisibility(animate = true)
        keepBarsRevealed()
    }

    /** Reinicia a contagem para esconder (ex.: depois de usar um botão da barra). */
    private fun keepBarsRevealed() {
        handler.removeCallbacks(hideBars)
        if (barsRevealed) handler.postDelayed(hideBars, Constants.BARS_AUTO_HIDE_MS)
    }

    // Tour

    private fun maybeStartTour() {
        if (tour != null) return
        val requested = replayTour
        if (!requested && (prefs.tutorialDone || !isDefaultHome(requireContext()))) return
        replayTour = false
        // Depois do desenho: o tour mede onde cada elemento está
        binding.root.post { startTour() }
    }

    private fun startTour() {
        if (_binding == null || tour != null || !isResumed) return
        val content = requireActivity().findViewById<ViewGroup>(android.R.id.content) ?: return
        binding.scroll.scrollTo(0, 0)
        val newTour = TourView(requireContext(), prefs.palette, tourSteps()) {
            prefs.tutorialDone = true
            tour = null
            if (_binding != null) applyBarVisibility(animate = true)
        }
        tour = newTour
        // O tour precisa dos botões das barras automáticas visíveis antes de filtrar os passos
        applyBarVisibility(animate = false)
        content.addView(newTour, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        newTour.start()
    }

    private fun tourSteps(): List<TourView.Step> = listOf(
        TourView.Step(getString(R.string.tour_welcome_title), getString(R.string.tour_welcome_body)),
        TourView.Step(getString(R.string.tour_clock_title), getString(R.string.tour_clock_body)) { binding.dateTimeLayout },
        TourView.Step(getString(R.string.tour_apps_title), getString(R.string.tour_apps_body)) { binding.groups.getChildAt(0) },
        TourView.Step(getString(R.string.tour_edit_title), getString(R.string.tour_edit_body)) { binding.btnEdit },
        TourView.Step(getString(R.string.tour_settings_title), getString(R.string.tour_settings_body)) { binding.btnSettings },
        TourView.Step(getString(R.string.tour_theme_title), getString(R.string.tour_theme_body)) { binding.btnAppearance },
        TourView.Step(getString(R.string.tour_profile_title), getString(R.string.tour_profile_body)) { binding.profilePill },
        TourView.Step(getString(R.string.tour_focus_title), getString(R.string.tour_focus_body)) { binding.btnFocus },
        TourView.Step(getString(R.string.tour_drawer_title), getString(R.string.tour_drawer_body), TourView.Gesture.SWIPE_UP),
        TourView.Step(getString(R.string.tour_gestures_title), getString(R.string.tour_gestures_body), TourView.Gesture.SWIPE_DOWN),
        TourView.Step(getString(R.string.tour_buttons_title), getString(R.string.tour_buttons_body)),
    )

    private fun renderClock(palette: Palette) {
        val visibility = prefs.dateTimeVisibility
        binding.dateTimeLayout.isVisible = visibility != Constants.DateTime.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(visibility)
        binding.dateRow.isVisible = Constants.DateTime.isDateVisible(visibility)

        val clockPattern = prefs.clockPattern
        val timePattern = Look.timePattern(requireContext(), clockPattern)
        binding.clock.format12Hour = timePattern
        binding.clock.format24Hour = timePattern
        binding.clock.textSize = prefs.clockSize.toFloat()
        binding.clock.setTextColor(if (prefs.clockAccent) palette.accent else palette.text)
        Look.applyFont(binding.clock, prefs.clockFont, prefs.clockWeight, prefs.clockSize.toFloat())

        val datePattern = prefs.datePattern.ifEmpty { defaultDatePattern() }
        binding.date.format12Hour = datePattern
        binding.date.format24Hour = datePattern
        binding.date.setTextColor(palette.muted)
        binding.dateExtra.setTextColor(palette.muted)

        val gravity = prefs.clockAlignment
        binding.dateTimeLayout.gravity = gravity
        binding.clock.updateLayoutParams<LinearLayout.LayoutParams> { this.gravity = gravity }
        binding.dateRow.updateLayoutParams<LinearLayout.LayoutParams> { this.gravity = gravity }
        renderDateExtra()
        applyTextShadow(palette, binding.clock, binding.date, binding.dateExtra)
    }

    private fun renderDateExtra() {
        val parts = mutableListOf<String>()
        if (prefs.showBattery && batteryLevel >= 0) parts.add("$batteryLevel%")
        if (prefs.showScreenTime && !screenTime.isNullOrBlank()) parts.add(screenTime!!)
        binding.dateExtra.text = parts.joinToString(prefix = " · ", separator = " · ").takeIf { parts.isNotEmpty() } ?: ""
    }

    private fun renderDigest(palette: Palette) {
        val digest = prefs.focusDigest.filterValues { it > 0 }
        val show = !prefs.focusActive && digest.isNotEmpty()
        binding.digest.isVisible = show
        if (!show) return
        val total = digest.values.sum()
        binding.digest.text = resources.getQuantityString(R.plurals.focus_digest, total, total, digest.size)
        binding.digest.setTextColor(palette.text)
        binding.digest.background = Look.pressable(ColorUtils.setAlphaComponent(palette.accent, 0x2E), 18, palette.text)
    }

    private fun renderGroups(palette: Palette) {
        val container = binding.groups
        container.removeAllViews()
        val context = requireContext()
        val profile = HomeStore.load(context).active
        val cards = prefs.homeStyle == Constants.HomeStyle.CARDS
        val alignment = prefs.homeAlignment

        binding.scrollContent.gravity = when (prefs.homeVertical) {
            Constants.Vertical.CENTER -> Gravity.CENTER_VERTICAL
            Constants.Vertical.BOTTOM -> Gravity.BOTTOM
            else -> Gravity.TOP
        }

        val groups = profile.groups.map { group ->
            group.items.filter { it.isShortcut || isPackageInstalled(context, it.pkg, it.user) }
        }.filter { it.isNotEmpty() }

        if (groups.isEmpty()) {
            container.addView(emptyCard(palette, cards))
            return
        }

        groups.forEachIndexed { index, items ->
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = alignment
                if (cards) {
                    background = Look.rounded(palette.card, 24)
                    setPadding(14.dpToPx(), 14.dpToPx(), 14.dpToPx(), 14.dpToPx())
                } else {
                    setPadding(4.dpToPx(), 0, 4.dpToPx(), 0)
                }
            }
            items.forEach { card.addView(appLabel(it, palette, alignment)) }
            val spacing = if (cards) 12 else 28
            container.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { if (index < groups.lastIndex) bottomMargin = spacing.dpToPx() })
        }
    }

    private fun appLabel(item: HomeItem, palette: Palette, alignment: Int): TextView =
        TextView(requireContext()).apply {
            text = Look.applyCase(item.label, prefs.textCase)
            textSize = prefs.appTextSize.toFloat()
            setTextColor(palette.text)
            gravity = alignment
            includeFontPadding = false
            setPadding(8.dpToPx(), 7.dpToPx(), 8.dpToPx(), 7.dpToPx())
            Look.applyFont(this, prefs.appFont, prefs.appWeight, prefs.appTextSize.toFloat())
            background = Look.pressable(0, 14, palette.text)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            applyTextShadow(palette, this)
            setOnClickListener { launch(item) }
            setOnLongClickListener {
                showItemMenu(it, item)
                true
            }
        }

    private fun emptyCard(palette: Palette, cards: Boolean): View =
        TextView(requireContext()).apply {
            text = getString(R.string.home_empty)
            textSize = 18f
            setTextColor(palette.muted)
            Look.applyFont(this, Constants.Font.JAKARTA, 500)
            setPadding(22.dpToPx(), 22.dpToPx(), 22.dpToPx(), 22.dpToPx())
            background = if (cards) Look.pressable(palette.card, 24, palette.text) else Look.pressable(0, 24, palette.text)
            setOnClickListener { openEditor() }
        }

    private fun applyTextShadow(palette: Palette, vararg views: TextView) {
        views.forEach {
            if (palette.showsWallpaper) it.setShadowLayer(6f, 0f, 1.5f, 0x80000000.toInt())
            else it.setShadowLayer(0f, 0f, 0f, 0)
        }
    }

    private fun defaultDatePattern(): String =
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.content) { v, insets ->
            val bars = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    // Ações

    private fun initClicks() {
        binding.btnSettings.setOnClickListener { navigate(R.id.action_mainFragment_to_settingsFragment) }
        binding.btnEdit.setOnClickListener { openEditor() }
        binding.btnAppearance.setOnClickListener { openSettingsPage(Constants.Page.APPEARANCE) }
        binding.btnFocus.setOnClickListener { toggleFocus() }
        binding.btnFocus.setOnLongClickListener {
            openSettingsPage(Constants.Page.FOCUS)
            true
        }
        binding.focusChip.setOnClickListener { openSettingsPage(Constants.Page.FOCUS) }
        binding.profilePill.setOnClickListener { showProfileMenu() }
        binding.profilePill.setOnLongClickListener {
            openSettingsPage(Constants.Page.PROFILES)
            true
        }
        binding.clock.setOnClickListener { openClockApp() }
        binding.clock.setOnLongClickListener {
            openSettingsPage(Constants.Page.CLOCK)
            true
        }
        binding.date.setOnClickListener { openCalendarApp() }
        binding.date.setOnLongClickListener {
            openSettingsPage(Constants.Page.CLOCK)
            true
        }
        binding.digest.setOnClickListener { showDigest() }
        binding.setDefaultLauncher.setOnClickListener { viewModel.resetLauncherLiveData.call() }
        binding.setDefaultLauncher.setOnLongClickListener {
            prefs.hideSetDefaultLauncher = true
            binding.setDefaultLauncher.isVisible = false
            true
        }
    }

    private fun initGestures() {
        binding.mainLayout.scrollView = binding.scroll
        binding.mainLayout.listener = object : GestureFrameLayout.Listener {
            override fun onSwipeUp() = showAppDrawer()
            override fun onSwipeDown() = expandNotificationDrawer(requireContext())
            override fun onSwipeLeft() = openSwipeApp(left = true)
            override fun onSwipeRight() = openSwipeApp(left = false)
            override fun onDoubleTapEmpty() {
                if (!prefs.lockModeOn) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) binding.lock.performClick()
                else lockPhone()
            }

            override fun onLongPressEmpty() = navigate(R.id.action_mainFragment_to_settingsFragment)
            override fun onTapEmpty(rawY: Float) = onBackgroundTap(rawY)
        }
        // O clique do toque duplo precisa existir para o serviço de acessibilidade vê-lo
        binding.lock.setOnClickListener { }
    }

    private fun launch(item: HomeItem) {
        val user = getUserHandleFromString(requireContext(), item.user)
        val model = if (item.isShortcut) AppModel.PinnedShortcut(
            appLabel = item.label, key = null, appPackage = item.pkg,
            shortcutId = item.shortcutId!!, user = user,
        ) else AppModel.App(
            appLabel = item.label, key = null, appPackage = item.pkg,
            activityClassName = item.activity, user = user,
        )
        viewModel.selectedApp(model, Constants.FLAG_LAUNCH_APP)
    }

    private fun showItemMenu(anchor: View, item: HomeItem) {
        anchor.showPopupMenu(configure = { menu ->
            menu.add(0, 1, 0, R.string.rename)
            menu.add(0, 2, 1, R.string.replace_app)
            menu.add(0, 3, 2, R.string.remove_from_home)
            if (!item.isShortcut) menu.add(0, 4, 3, R.string.app_info)
            menu.add(0, 5, 4, R.string.edit_home)
        }) { menuItem ->
            when (menuItem.itemId) {
                1 -> requireContext().showInputDialog(getString(R.string.rename), item.label) { name ->
                    if (name.isNotEmpty()) {
                        HomeStore.update(requireContext()) { data ->
                            data.active.allItems.firstOrNull { it.id == item.id }?.label = name
                        }
                        render()
                    }
                }

                2 -> openPicker(Constants.PickerMode.REPLACE, itemId = item.id)
                3 -> {
                    HomeStore.update(requireContext()) { data ->
                        data.active.groups.forEach { g -> g.items.removeAll { it.id == item.id } }
                    }
                    render()
                }

                4 -> openAppInfo(requireContext(), getUserHandleFromString(requireContext(), item.user), item.pkg)
                5 -> openEditor()
            }
        }
    }

    private fun showProfileMenu() {
        val data = HomeStore.load(requireContext())
        val options = data.profiles.map { profile ->
            val title = if (profile.id == data.activeId) "✓  ${profile.name}" else "     ${profile.name}"
            title as CharSequence to { switchProfile(profile.id) }
        }
        requireContext().showListDialog(
            title = getString(R.string.profiles),
            options = options,
            action = getString(R.string.manage_profiles),
            onAction = { openSettingsPage(Constants.Page.PROFILES) },
        )
    }

    private fun switchProfile(id: String) {
        val context = requireContext()
        val profile = HomeStore.update(context) { it.activeId = id }.active
        if (profile.focusOnSwitch) FocusManager.activate(context, Constants.FocusSource.PROFILE)
        else if (prefs.focusActive && prefs.focusSource == Constants.FocusSource.PROFILE) FocusManager.deactivate(context)
        render()
        keepBarsRevealed()
    }

    private fun toggleFocus() {
        val context = requireContext()
        if (!prefs.focusActive && !FocusManager.hasDndAccess(context) && !FocusManager.hasListenerAccess(context)) {
            context.showToast(getString(R.string.focus_needs_permission), Toast.LENGTH_LONG)
            openSettingsPage(Constants.Page.FOCUS)
            return
        }
        FocusManager.toggle(context)
        render()
        keepBarsRevealed()
    }

    private fun showDigest() {
        val context = requireContext()
        val pm = context.packageManager
        val digest = prefs.focusDigest.filterValues { it > 0 }.toList().sortedByDescending { it.second }
        val options = digest.map { (pkg, count) ->
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg
            }
            "$label — $count" as CharSequence to {
                HomeStore.launchItem(context, pkg)?.let { launch(it) }
                Unit
            }
        }
        context.showListDialog(
            title = getString(R.string.focus_digest_title),
            message = getString(R.string.focus_digest_message),
            options = options,
            action = getString(R.string.clear),
            onAction = {
                prefs.focusDigest = emptyMap()
                render()
            },
        )
    }

    private fun openSwipeApp(left: Boolean) {
        val enabled = if (left) prefs.swipeLeftEnabled else prefs.swipeRightEnabled
        if (!enabled) return
        val pkg = if (left) prefs.appPackageSwipeLeft else prefs.appPackageSwipeRight
        if (pkg.isEmpty()) {
            if (left) openCameraApp(requireContext()) else openDialerApp(requireContext())
            return
        }
        val user = getUserHandleFromString(requireContext(), if (left) prefs.appUserSwipeLeft else prefs.appUserSwipeRight)
        val isShortcut = if (left) prefs.isShortcutSwipeLeft else prefs.isShortcutSwipeRight
        val label = if (left) prefs.appNameSwipeLeft else prefs.appNameSwipeRight
        val model = if (isShortcut) AppModel.PinnedShortcut(
            appLabel = label, key = null, appPackage = pkg, user = user,
            shortcutId = if (left) prefs.shortcutIdSwipeLeft else prefs.shortcutIdSwipeRight,
        ) else AppModel.App(
            appLabel = label, key = null, appPackage = pkg, user = user,
            activityClassName = if (left) prefs.appActivityClassNameSwipeLeft else prefs.appActivityClassNameRight,
        )
        viewModel.selectedApp(model, Constants.FLAG_LAUNCH_APP)
    }

    private fun openClockApp() {
        if (prefs.clockAppPackage.isBlank()) openAlarmApp(requireContext())
        else viewModel.selectedApp(
            AppModel.App(
                appLabel = "Clock", key = null, appPackage = prefs.clockAppPackage,
                activityClassName = prefs.clockAppClassName,
                user = getUserHandleFromString(requireContext(), prefs.clockAppUser),
            ), Constants.FLAG_LAUNCH_APP
        )
    }

    private fun openCalendarApp() {
        if (prefs.calendarAppPackage.isBlank()) openCalendar(requireContext())
        else viewModel.selectedApp(
            AppModel.App(
                appLabel = "Calendar", key = null, appPackage = prefs.calendarAppPackage,
                activityClassName = prefs.calendarAppClassName,
                user = getUserHandleFromString(requireContext(), prefs.calendarAppUser),
            ), Constants.FLAG_LAUNCH_APP
        )
    }

    private fun showAppDrawer() {
        viewModel.getAppList()
        navigate(R.id.action_mainFragment_to_appListFragment, bundleOf(Constants.Key.FLAG to Constants.FLAG_LAUNCH_APP))
    }

    private fun openEditor() = navigate(R.id.action_mainFragment_to_editHomeFragment)

    private fun openSettingsPage(page: String) =
        navigate(R.id.action_mainFragment_to_settingsPageFragment, bundleOf(Constants.Key.PAGE to page))

    private fun openPicker(mode: Int, itemId: String? = null) {
        viewModel.getAppList(includeHiddenApps = true)
        navigate(
            R.id.action_mainFragment_to_appPickerFragment,
            bundleOf(Constants.Key.MODE to mode, Constants.Key.ITEM_ID to itemId)
        )
    }

    private fun navigate(action: Int, args: Bundle? = null) {
        try {
            findNavController().navigate(action, args)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun lockPhone() {
        try {
            deviceManager.lockNow()
        } catch (e: SecurityException) {
            requireContext().showToast(getString(R.string.please_turn_on_double_tap_to_unlock), Toast.LENGTH_LONG)
            openSettingsPage(Constants.Page.GESTURES)
        } catch (e: Exception) {
            requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
            prefs.lockModeOn = false
        }
    }

    override fun onDestroyView() {
        handler.removeCallbacks(hideBars)
        tour?.dismiss()
        tour = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /** Pedido pelos ajustes ("ver o tour de novo"): vale mesmo sem ser o launcher padrão. */
        var replayTour = false
    }
}
