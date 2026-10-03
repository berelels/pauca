package app.pauca.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import app.pauca.MainActivity
import app.pauca.R
import app.pauca.data.Constants
import app.pauca.data.Palette
import app.pauca.data.Prefs
import app.pauca.helper.Language
import app.pauca.helper.dpToPx
import app.pauca.helper.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Editor do tema "Fundo": papel de parede do sistema ou uma imagem, com desfoque e brilho.
 * Mostra a prévia ao vivo atrás do painel; nada é salvo até tocar em "Aplicar".
 */
class WallpaperFragment : BaseFragment() {

    private lateinit var prefs: Prefs
    private lateinit var screen: Palette
    private lateinit var ui: SettingsBuilder
    private var applied = false

    // O que está sendo editado (começa no que está salvo)
    private var source = Constants.WallpaperSource.SYSTEM
    private var blur = false
    private var blurRadius = 12
    private var brightness = 100

    // Tela de bloqueio: ajustes próprios
    private var editingLock = false
    private var lockEnabled = false
    private var lockBlur = false
    private var lockBlurRadius = 12
    private var lockBrightness = 100
    private val mockLabels = mutableListOf<View>()

    private val picker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { Wallpaper.import(context, uri) }
            if (!ok) {
                context.showToast(R.string.wallpaper_import_failed)
                return@launch
            }
            // Pela aba da tela de bloqueio, a imagem nova vale só para ela
            if (editingLock) lockEnabled = true else source = Constants.WallpaperSource.IMAGE
            build()
            preview()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        prefs = Prefs(requireContext())
        screen = prefs.palette.forScreens
        source = prefs.wallpaperSource
        blur = prefs.wallpaperBlur
        blurRadius = prefs.wallpaperBlurRadius
        brightness = prefs.wallpaperBrightness
        lockEnabled = prefs.lockWallpaper
        lockBlur = prefs.lockBlur
        lockBlurRadius = prefs.lockBlurRadius
        lockBrightness = prefs.lockBrightness

        val context = requireContext()
        val root = FrameLayout(context)
        // Toques no fundo não passam para a tela de trás
        root.isClickable = true

        val mock = mockHome()
        root.addView(mock, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))

        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ColorUtils.setAlphaComponent(screen.bg, 0xF2))
                val r = 28.dpToPx().toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            setPadding(16.dpToPx(), 20.dpToPx(), 16.dpToPx(), 12.dpToPx())
            isClickable = true
        }
        panel.addView(TextView(context).apply {
            text = getString(R.string.wallpaper_title)
            textSize = 26f
            setTextColor(screen.text)
            Look.applyFont(this, Constants.Font.NEWSREADER, 500, 26f)
            setPadding(6.dpToPx(), 0, 6.dpToPx(), 4.dpToPx())
        })
        val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            addView(content)
        }
        panel.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        panel.addView(buttons())
        val maxPanel = (resources.displayMetrics.heightPixels * 0.56f).toInt()
        root.addView(panel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxPanel, Gravity.BOTTOM))

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            mock.updatePadding(top = bars.top + 36.dpToPx())
            panel.updatePadding(bottom = bars.bottom + 12.dpToPx())
            insets
        }

        ui = SettingsBuilder(context, content, screen)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        build()
        preview()
    }

    /** Relógio e dois apps de mentira, com as escolhas atuais, para ver se dá para ler. */
    private fun mockHome(): View {
        val context = requireContext()
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28.dpToPx(), 0, 28.dpToPx(), 0)
        }
        val shadow = 0x80000000.toInt()
        box.addView(TextClock(context).apply {
            val pattern = Look.timePattern(context, prefs.clockPattern)
            format12Hour = pattern
            format24Hour = pattern
            textSize = prefs.clockSize.coerceAtMost(88).toFloat()
            includeFontPadding = false
            setTextColor(0xFFFFFFFF.toInt())
            setShadowLayer(6f, 0f, 1.5f, shadow)
            Look.applyFont(this, prefs.clockFont, prefs.clockWeight, prefs.clockSize.toFloat())
            Look.fitClock(this, box)
        })
        listOf(R.string.preview_app_1, R.string.preview_app_2).forEach {
            box.addView(TextView(context).also { label -> mockLabels.add(label) }.apply {
                text = Look.applyCase(getString(it), prefs.textCase)
                textSize = prefs.appTextSize.toFloat()
                setTextColor(0xFFFFFFFF.toInt())
                setShadowLayer(6f, 0f, 1.5f, shadow)
                setPadding(0, 10.dpToPx(), 0, 0)
                Look.applyFont(this, prefs.appFont, prefs.appWeight, prefs.appTextSize.toFloat())
            })
        }
        return box
    }

    private fun buttons(): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setPadding(0, 12.dpToPx(), 0, 0)
        }
        row.addView(TextView(context).apply {
            text = getString(R.string.cancel)
            textSize = 15f
            setTextColor(screen.muted)
            setPadding(20.dpToPx(), 12.dpToPx(), 20.dpToPx(), 12.dpToPx())
            background = Look.pressable(0, 22, screen.text)
            Look.applyFont(this, Constants.Font.JAKARTA, 600)
            setOnClickListener { findNavController().popBackStack() }
        })
        row.addView(TextView(context).apply {
            text = getString(R.string.wallpaper_apply)
            textSize = 15f
            setTextColor(screen.accentInk)
            setPadding(26.dpToPx(), 12.dpToPx(), 26.dpToPx(), 12.dpToPx())
            background = Look.pressable(screen.accent, 22, screen.accentInk)
            Look.applyFont(this, Constants.Font.JAKARTA, 700)
            setOnClickListener { apply() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = 8.dpToPx()
        })
        return row
    }

    private fun build() {
        ui.clear()
        ui.section(null)
        ui.custom(segmented(
            listOf(HOME to getString(R.string.wallpaper_target_home), LOCK to getString(R.string.wallpaper_target_lock)),
            selected = if (editingLock) LOCK else HOME,
        ) {
            editingLock = it == LOCK
            // Na tela de bloqueio não há apps: a prévia mostra só o relógio
            mockLabels.forEach { label -> label.visibility = if (editingLock) View.GONE else View.VISIBLE }
            build()
            preview()
        })
        if (editingLock) buildLock() else buildHome()
    }

    /** A tela de bloqueio usa a imagem escolhida, com o seu próprio desfoque e brilho. */
    private fun buildLock() {
        val hasImage = Wallpaper.hasImage(requireContext())
        if (!hasImage) {
            ui.row(getString(R.string.wallpaper_choose_image), accentValue = true, chevron = true) { pickImage() }
            ui.note(getString(R.string.wallpaper_lock_needs_image))
            return
        }
        ui.toggle(
            getString(R.string.wallpaper_lock_apply),
            subtitle = { getString(R.string.wallpaper_lock_summary) },
            get = { lockEnabled },
            set = {
                lockEnabled = it
                build()
                preview()
                true
            },
        )
        if (!lockEnabled) return
        ui.section(null)
        ui.toggle(getString(R.string.wallpaper_blur), get = { lockBlur }, set = {
            lockBlur = it
            build()
            preview()
            true
        })
        if (lockBlur) ui.slider(
            getString(R.string.wallpaper_blur_intensity),
            get = { lockBlurRadius }, set = { lockBlurRadius = it; preview() },
            min = 1, max = 25,
        )
        ui.slider(
            getString(R.string.wallpaper_brightness),
            get = { lockBrightness }, set = { lockBrightness = it; preview() },
            min = 20, max = 130, format = { "$it%" },
        )
    }

    private fun buildHome() {
        val context = requireContext()
        val hasImage = Wallpaper.hasImage(context)
        ui.section(null)
        ui.custom(segmented(
            listOf(
                Constants.WallpaperSource.SYSTEM to getString(R.string.wallpaper_source_system),
                Constants.WallpaperSource.IMAGE to getString(R.string.wallpaper_source_image),
            ),
            selected = source,
        ) { id ->
            if (id == Constants.WallpaperSource.IMAGE && !Wallpaper.hasImage(context)) {
                pickImage()
                return@segmented
            }
            source = id
            build()
            preview()
        })
        if (source == Constants.WallpaperSource.IMAGE || hasImage) {
            ui.row(
                getString(if (hasImage) R.string.wallpaper_change_image else R.string.wallpaper_choose_image),
                accentValue = true,
                chevron = true,
            ) { pickImage() }
        }

        ui.section(null)
        val systemNoBlur = source == Constants.WallpaperSource.SYSTEM && !Wallpaper.canBlurSystem(context)
        if (!systemNoBlur) {
            ui.toggle(getString(R.string.wallpaper_blur), get = { blur }, set = {
                blur = it
                build()
                preview()
                true
            })
            if (blur) ui.slider(
                getString(R.string.wallpaper_blur_intensity),
                get = { blurRadius }, set = { blurRadius = it; preview() },
                min = 1, max = 25,
            )
        }
        ui.slider(
            getString(R.string.wallpaper_brightness),
            get = { brightness }, set = { brightness = it; preview() },
            min = 20, max = 130, format = { "$it%" },
        )
        if (systemNoBlur) ui.note(getString(R.string.wallpaper_blur_unavailable))
    }

    /** Botões lado a lado, um escolhido (ex.: "Do sistema" | "Minha imagem"). */
    private fun segmented(options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit): View {
        val context = requireContext()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10.dpToPx(), 10.dpToPx(), 10.dpToPx(), 10.dpToPx())
        }
        options.forEach { (id, label) ->
            val on = id == selected
            row.addView(TextView(context).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                maxLines = 1
                setTextColor(if (on) screen.bg else screen.text)
                setPadding(12.dpToPx(), 11.dpToPx(), 12.dpToPx(), 11.dpToPx())
                background = if (on) Look.pressable(screen.text, 20, screen.bg) else Look.pressable(0, 20, screen.text)
                Look.applyFont(this, Constants.Font.JAKARTA, 600)
                setOnClickListener { if (!on) onPick(id) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 6.dpToPx()
            })
        }
        return row
    }

    private fun pickImage() {
        try {
            (activity as? MainActivity)?.keepCurrentScreen = true
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            requireContext().showToast(R.string.wallpaper_import_failed)
        }
    }

    private fun look() = Wallpaper.Look(source, blur, blurRadius, brightness)

    private fun lockLook() = Wallpaper.Look(Constants.WallpaperSource.IMAGE, lockBlur, lockBlurRadius, lockBrightness)

    private fun preview() {
        val showLock = editingLock && lockEnabled && Wallpaper.hasImage(requireContext())
        (activity as? MainActivity)?.applyBackground(Palette.WALLPAPER.withAccent(prefs.accent), if (showLock) lockLook() else look())
    }

    private fun apply() {
        // O tema "Fundo" é escuro: vindo de um tema claro, diálogos e menus precisam trocar
        val needsRecreate = !prefs.palette.isDark
        prefs.wallpaperSource = source
        prefs.wallpaperBlur = blur
        prefs.wallpaperBlurRadius = blurRadius
        prefs.wallpaperBrightness = brightness
        prefs.lockWallpaper = lockEnabled
        prefs.lockBlur = lockBlur
        prefs.lockBlurRadius = lockBlurRadius
        prefs.lockBrightness = lockBrightness
        if (lockEnabled && Wallpaper.hasImage(requireContext())) applyLockScreen()
        prefs.paletteId = Palette.WALLPAPER.id
        applied = true
        val activity = requireActivity()
        findNavController().popBackStack(R.id.mainFragment, false)
        if (needsRecreate) activity.recreate() else (activity as? MainActivity)?.applyBackground()
    }

    /**
     * Gera e põe a imagem na tela de bloqueio em segundo plano. Usa o contexto do app:
     * a tela pode ser recriada (troca de tema) antes de terminar.
     */
    private fun applyLockScreen() {
        val app = requireContext().applicationContext
        val look = lockLook()
        val main = Handler(Looper.getMainLooper())
        Thread {
            val ok = Wallpaper.setLockScreen(app, look)
            val text = Language.wrap(app).getString(if (ok) R.string.wallpaper_lock_done else R.string.wallpaper_lock_failed)
            main.post { app.showToast(text) }
        }.start()
    }

    override fun onDestroyView() {
        // Saiu sem aplicar: volta o fundo que estava
        if (!applied) (activity as? MainActivity)?.applyBackground()
        super.onDestroyView()
    }

    private companion object {
        const val HOME = "home"
        const val LOCK = "lock"
    }
}
