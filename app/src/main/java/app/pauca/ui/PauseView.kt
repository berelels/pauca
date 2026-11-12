package app.pauca.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import app.pauca.R
import app.pauca.data.Constants
import app.pauca.data.Palette
import app.pauca.helper.dpToPx
import app.pauca.helper.isSystemAnimationsDisabled
import kotlin.math.acos

/**
 * Pausa para respirar, por cima de tudo: o fundo sobe na cor de destaque e o círculo só respira
 * (4 s para dentro, 6 s para fora) enquanto a pessoa o segura. "Abrir mesmo assim" é liberado
 * depois de 30 s de toque acumulados, não de relógio.
 */
@SuppressLint("ViewConstructor")
class PauseView(
    context: Context,
    palette: Palette,
    appLabel: String,
    count: Int,
    private val onBack: () -> Unit,
    private val onOpen: () -> Unit,
) : FrameLayout(context) {

    private val still = context.isSystemAnimationsDisabled()
    private val ink = palette.accentInk
    private val accent = palette.accent
    private val openAnyway = TextView(context)
    private val circle = View(context)
    private val label = TextView(context)

    // Respiração: segurando, a fase anda pelo ciclo de 10 s; soltando, o círculo assenta devagar
    private var holding = false
    private var unlocked = false
    private var heldMs = 0L
    private var phaseMs = 0f
    private var scale = REST
    private var lastFrame = 0L
    private val ease = AccelerateDecelerateInterpolator()

    private val frame = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val dt = (now - lastFrame).coerceIn(0, 100)
            lastFrame = now
            if (holding) {
                heldMs += dt
                phaseMs = (phaseMs + dt) % (IN_MS + OUT_MS)
                if (!still) {
                    val f = if (phaseMs < IN_MS) ease.getInterpolation(phaseMs / IN_MS)
                    else 1f - ease.getInterpolation((phaseMs - IN_MS) / OUT_MS)
                    scale = REST + (1f - REST) * f
                }
                if (heldMs >= HOLD_MS) unlock() else showRemaining()
            } else if (!still) {
                scale += (REST - scale) * (1f - Math.exp(-dt / 700.0).toFloat())
            }
            circle.scaleX = scale
            circle.scaleY = scale
            // Solto e já assentado: para o laço até o próximo toque
            if (holding || (!still && scale - REST > 0.002f)) postOnAnimation(this)
            else {
                scale = if (still) STILL else REST
                circle.scaleX = scale
                circle.scaleY = scale
            }
        }
    }

    init {
        setBackgroundColor(accent)
        isClickable = true
        isFocusable = true
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val bars = insets.getInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPaddingRelative(28.dpToPx(), 16.dpToPx(), 28.dpToPx(), 24.dpToPx())
        }
        addView(column, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // Círculo que respira, com a palavra parada no meio
        val circleBox = FrameLayout(context).apply { clipChildren = false }
        val size = 210.dpToPx()
        circle.apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ColorUtils.setAlphaComponent(ink, 0x26))
                setStroke(2.dpToPx(), ColorUtils.setAlphaComponent(ink, 0x99))
            }
        }
        circleBox.addView(circle, LayoutParams(size, size, Gravity.CENTER))
        circleBox.addView(label.apply {
            setText(R.string.pause_hold)
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(ink)
            Look.applyFont(this, Constants.Font.NEWSREADER, 500, 18f)
        }, LayoutParams(110.dpToPx(), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        circleBox.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    hold(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> hold(false)
            }
            true
        }
        column.addView(circleBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // O texto comum fica um pouco mais suave que o número, que fica na tinta cheia e maior
        val soft = ColorUtils.blendARGB(ink, accent, 0.15f)
            .takeIf { ColorUtils.calculateContrast(it, accent) >= 4.5 } ?: ink
        val number = SpannableString(count.toString()).apply {
            setSpan(ForegroundColorSpan(ink), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(RelativeSizeSpan(1.5f), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        column.addView(TextView(context).apply {
            text = TextUtils.expandTemplate(context.getText(R.string.pause_message), appLabel, number)
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(soft)
            setLineSpacing(4.dpToPx().toFloat(), 1f)
            Look.applyFont(this, Constants.Font.NEWSREADER, 400, 20f)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 28.dpToPx()
        })

        column.addView(TextView(context).apply {
            setText(R.string.pause_back)
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(accent)
            Look.applyFont(this, Constants.Font.JAKARTA, 700)
            background = Look.pressable(ink, 28, accent)
            setOnClickListener { onBack() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 56.dpToPx()))

        openAnyway.apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(soft)
            alpha = 0.7f
            isEnabled = false
            Look.applyFont(this, Constants.Font.JAKARTA, 500)
            background = Look.pressable(0, 22, ink)
            setOnClickListener { onOpen() }
        }
        column.addView(openAnyway, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 44.dpToPx()).apply {
            topMargin = 8.dpToPx()
        })

        contentDescription = TextUtils.expandTemplate(context.getText(R.string.pause_message), appLabel, count.toString())

        circle.scaleX = scale
        circle.scaleY = scale
        if (still) {
            scale = STILL
            circle.scaleX = scale
            circle.scaleY = scale
        } else doOnLayout { rise() }
        showRemaining()
    }

    private fun hold(on: Boolean) {
        if (holding == on) return
        holding = on
        label.setText(if (on) R.string.pause_breathe else R.string.pause_hold)
        if (on) {
            // Recomeça a inspirar do tamanho em que o círculo está
            phaseMs = (acos((1f - 2f * ((scale - REST) / (1f - REST)).coerceIn(0f, 1f)).toDouble()) / Math.PI).toFloat() * IN_MS
        }
        lastFrame = SystemClock.elapsedRealtime()
        removeCallbacks(frame)
        postOnAnimation(frame)
    }

    private fun showRemaining() {
        if (unlocked) return
        val left = ((HOLD_MS - heldMs + 999) / 1000).toInt()
        openAnyway.text = context.getString(R.string.pause_open_wait, left)
    }

    /** O fundo sobe de baixo, junto com o resto. */
    private fun rise() {
        translationY = height.toFloat()
        animate().translationY(0f).setDuration(400).setInterpolator(DecelerateInterpolator(2f)).start()
    }

    private fun unlock() {
        if (unlocked) return
        unlocked = true
        openAnyway.setText(R.string.pause_open_anyway)
        openAnyway.isEnabled = true
        openAnyway.animate().alpha(1f).setDuration(300).start()
    }

    /** Tira da tela; não decide nada sobre o app. */
    fun dismiss() {
        removeCallbacks(frame)
        animate().cancel()
        (parent as? ViewGroup)?.removeView(this)
    }

    private companion object {
        const val HOLD_MS = 30_000L
        const val IN_MS = 4000f
        const val OUT_MS = 6000f
        const val REST = 0.65f
        const val STILL = 0.8f
    }
}
