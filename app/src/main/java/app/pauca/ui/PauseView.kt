package app.pauca.ui

import android.animation.ValueAnimator
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
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
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

/**
 * Pausa para respirar, por cima de tudo: o fundo sobe na cor de destaque, um círculo respira
 * por um minuto (4 s para dentro, 6 s para fora) e só depois "abrir mesmo assim" é liberado.
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
    private var breathing: ValueAnimator? = null
    private val openAnyway = TextView(context)
    private val endAt = SystemClock.elapsedRealtime() + WAIT_MS

    private val tick = object : Runnable {
        override fun run() {
            val left = ((endAt - SystemClock.elapsedRealtime() + 999) / 1000).toInt()
            if (left > 0) {
                openAnyway.text = context.getString(R.string.pause_open_wait, left)
                postDelayed(this, 1000)
            } else unlock()
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
        val size = 190.dpToPx()
        val circle = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ColorUtils.setAlphaComponent(ink, 0x26))
                setStroke(2.dpToPx(), ColorUtils.setAlphaComponent(ink, 0x99))
            }
        }
        circleBox.addView(circle, LayoutParams(size, size, Gravity.CENTER))
        circleBox.addView(TextView(context).apply {
            setText(R.string.pause_breathe)
            textSize = 24f
            setTextColor(ink)
            Look.applyFont(this, Constants.Font.NEWSREADER, 500, 24f)
        }, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
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

        if (still) {
            circle.scaleX = 0.8f
            circle.scaleY = 0.8f
        } else {
            doOnLayout { rise() }
            breathing = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = (IN_MS + OUT_MS).toLong()
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                val ease = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val t = it.animatedFraction * (IN_MS + OUT_MS)
                    val f = if (t < IN_MS) ease.getInterpolation(t / IN_MS) else 1f - ease.getInterpolation((t - IN_MS) / OUT_MS)
                    val s = 0.55f + 0.45f * f
                    circle.scaleX = s
                    circle.scaleY = s
                }
                start()
            }
        }
        tick.run()
    }

    /** O fundo sobe de baixo, junto com o resto. */
    private fun rise() {
        translationY = height.toFloat()
        animate().translationY(0f).setDuration(400).setInterpolator(DecelerateInterpolator(2f)).start()
    }

    private fun unlock() {
        openAnyway.setText(R.string.pause_open_anyway)
        openAnyway.isEnabled = true
        openAnyway.animate().alpha(1f).setDuration(300).start()
    }

    /** Tira da tela; não decide nada sobre o app. */
    fun dismiss() {
        removeCallbacks(tick)
        breathing?.cancel()
        animate().cancel()
        (parent as? ViewGroup)?.removeView(this)
    }

    private companion object {
        const val WAIT_MS = 60_000L
        const val IN_MS = 4000f
        const val OUT_MS = 6000f
    }
}
