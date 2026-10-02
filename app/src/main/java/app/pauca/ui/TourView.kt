package app.pauca.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import app.pauca.R
import app.pauca.data.Constants
import app.pauca.data.Palette
import app.pauca.helper.dpToPx

/**
 * Tour guiado por cima da tela inicial: escurece tudo, recorta um buraco em volta do
 * elemento da vez e mostra um cartão explicando. Tocar em qualquer lugar avança.
 */
@SuppressLint("ViewConstructor")
class TourView(
    context: Context,
    palette: Palette,
    steps: List<Step>,
    private val onFinish: () -> Unit,
) : FrameLayout(context) {

    /**
     * Um passo. [target] devolve a view a destacar, ou null para um passo sem alvo
     * (boas-vindas, gestos). Se o alvo não está na tela quando o tour começa, o passo sai.
     */
    class Step(
        val title: String,
        val body: String,
        val gesture: Gesture = Gesture.NONE,
        val target: (() -> View?)? = null,
    )

    enum class Gesture { NONE, SWIPE_UP, SWIPE_DOWN }

    // Só os passos cujo alvo está na tela (ex.: um botão ocultado nos ajustes fica de fora)
    private val steps = steps.filter { it.target == null || it.target.invoke()?.isShown == true }
    private val screen = palette.forScreens
    private val accent = palette.accent
    private var index = -1

    private val hole = RectF()
    private var hasHole = false
    private var holeAnimator: ValueAnimator? = null
    private val scrimColor = ColorUtils.setAlphaComponent(0xFF0E0C09.toInt(), 0xD6)
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.dpToPx().toFloat()
        color = accent
    }
    private val location = IntArray(2)
    private val targetLocation = IntArray(2)

    private val card = LinearLayout(context)
    private val titleView = TextView(context)
    private val bodyView = TextView(context)
    private val counter = TextView(context)
    private val skip = TextView(context)
    private val next = TextView(context)
    private val finger = View(context)
    private var fingerAnimator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        // A camada própria deixa o modo CLEAR apagar só o escurecido, não o que está por trás
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(R.string.tour_title)
        setOnClickListener { advance() }

        finger.background = Look.rounded(ColorUtils.setAlphaComponent(accent, 0xE6), 22, ColorUtils.setAlphaComponent(screen.text, 0x99))
        finger.isVisible = false
        addView(finger, LayoutParams(44.dpToPx(), 44.dpToPx(), Gravity.TOP or Gravity.CENTER_HORIZONTAL))

        card.orientation = LinearLayout.VERTICAL
        card.background = Look.rounded(if (screen.isDark) screen.card else screen.bg, 24, screen.divider)
        card.setPadding(22.dpToPx(), 20.dpToPx(), 16.dpToPx(), 14.dpToPx())
        card.isClickable = true
        card.setOnClickListener { advance() }

        titleView.textSize = 24f
        titleView.setTextColor(screen.text)
        titleView.setPadding(0, 0, 6.dpToPx(), 0)
        Look.applyFont(titleView, Constants.Font.NEWSREADER, 500, 24f)
        card.addView(titleView)

        bodyView.textSize = 15f
        bodyView.setTextColor(ColorUtils.setAlphaComponent(screen.text, 0xD0))
        bodyView.setLineSpacing(3.dpToPx().toFloat(), 1f)
        bodyView.setPadding(0, 8.dpToPx(), 6.dpToPx(), 0)
        Look.applyFont(bodyView, Constants.Font.JAKARTA, 400)
        card.addView(bodyView)

        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16.dpToPx(), 0, 0)
        }
        counter.textSize = 13f
        counter.setTextColor(screen.muted)
        Look.applyFont(counter, Constants.Font.JAKARTA, 500)
        footer.addView(counter, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        skip.text = context.getString(R.string.tour_skip)
        skip.textSize = 14f
        skip.setTextColor(screen.muted)
        skip.setPadding(14.dpToPx(), 10.dpToPx(), 14.dpToPx(), 10.dpToPx())
        skip.background = Look.pressable(0, 20, screen.text)
        Look.applyFont(skip, Constants.Font.JAKARTA, 600)
        skip.setOnClickListener { finish() }
        footer.addView(skip)

        next.textSize = 14f
        next.setTextColor(palette.accentInk)
        next.setPadding(20.dpToPx(), 10.dpToPx(), 20.dpToPx(), 10.dpToPx())
        next.background = Look.pressable(accent, 20, palette.accentInk)
        Look.applyFont(next, Constants.Font.JAKARTA, 700)
        next.setOnClickListener { advance() }
        footer.addView(next, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = 6.dpToPx()
        })
        card.addView(footer)

        addView(card, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP).apply {
            marginStart = 18.dpToPx()
            marginEnd = 18.dpToPx()
        })
        card.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> placeCard() }
    }

    fun start() {
        alpha = 0f
        animate().alpha(1f).setDuration(220).start()
        advance()
    }

    private fun advance() {
        if (index + 1 >= steps.size) {
            finish()
            return
        }
        index++
        show(steps[index])
    }

    private fun show(step: Step) {
        titleView.text = step.title
        bodyView.text = step.body
        val remaining = index < steps.lastIndex
        next.text = context.getString(if (remaining) R.string.tour_next else R.string.tour_start)
        skip.isVisible = remaining
        counter.text = context.getString(R.string.tour_counter, index + 1, steps.size)
        card.announceForAccessibility("${step.title}. ${step.body}")

        val target = step.target?.invoke()
        if (target != null) moveHoleTo(rectOf(target)) else {
            holeAnimator?.cancel()
            hasHole = false
            invalidate()
        }
        showGesture(step.gesture)
        placeCard()
    }

    private fun rectOf(view: View): RectF {
        getLocationInWindow(location)
        view.getLocationInWindow(targetLocation)
        val pad = 8.dpToPx()
        val edge = 4.dpToPx().toFloat()
        val left = (targetLocation[0] - location[0] - pad).toFloat()
        val top = (targetLocation[1] - location[1] - pad).toFloat()
        // Sem encostar na borda da tela: o anel ficaria cortado
        return RectF(
            maxOf(left, edge),
            maxOf(top, edge),
            minOf(left + view.width + 2 * pad, width - edge),
            minOf(top + view.height + 2 * pad, height - edge),
        )
    }

    private fun moveHoleTo(to: RectF) {
        holeAnimator?.cancel()
        if (!hasHole) {
            hole.set(to)
            hasHole = true
            invalidate()
            return
        }
        val from = RectF(hole)
        holeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val t = it.animatedFraction
                hole.set(
                    from.left + (to.left - from.left) * t,
                    from.top + (to.top - from.top) * t,
                    from.right + (to.right - from.right) * t,
                    from.bottom + (to.bottom - from.bottom) * t,
                )
                invalidate()
                placeCard()
            }
            start()
        }
    }

    /** O cartão fica do lado oposto ao buraco; sem buraco, no meio da tela. */
    private fun placeCard() {
        if (height == 0 || card.height == 0) return
        val insets = ViewCompat.getRootWindowInsets(this)
            ?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val minTop = (insets?.top ?: 0) + 16.dpToPx()
        val maxTop = height - (insets?.bottom ?: 0) - 16.dpToPx() - card.height
        val gap = 18.dpToPx()
        val desired = when {
            !hasHole -> (height - card.height) / 2
            hole.centerY() < height / 2f -> (hole.bottom + gap).toInt()
            else -> (hole.top - gap - card.height).toInt()
        }
        card.translationY = desired.coerceIn(minTop, maxOf(minTop, maxTop)).toFloat()
    }

    private fun showGesture(gesture: Gesture) {
        fingerAnimator?.cancel()
        finger.isVisible = gesture != Gesture.NONE
        if (gesture == Gesture.NONE) return
        val travel = 160.dpToPx().toFloat()
        val ease = DecelerateInterpolator(1.6f)
        fingerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val t = it.animatedFraction
                val moved = travel * ease.getInterpolation((t / 0.8f).coerceAtMost(1f))
                // Sobe a partir de perto do fim da tela, ou desce a partir do topo
                finger.translationY = if (gesture == Gesture.SWIPE_UP) height * 0.84f - moved else height * 0.08f + moved
                finger.alpha = when {
                    t < 0.12f -> t / 0.12f
                    t > 0.7f -> ((0.9f - t) / 0.2f).coerceAtLeast(0f)
                    else -> 1f
                }
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val save = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawColor(scrimColor)
        if (hasHole) {
            val r = minOf(18.dpToPx().toFloat(), hole.height() / 2f)
            canvas.drawRoundRect(hole, r, r, clearPaint)
        }
        canvas.restoreToCount(save)
        if (hasHole) {
            val r = minOf(18.dpToPx().toFloat(), hole.height() / 2f)
            canvas.drawRoundRect(hole, r, r, ringPaint)
        }
    }

    private fun finish() {
        if (index == Int.MAX_VALUE) return
        index = Int.MAX_VALUE
        animate().alpha(0f).setDuration(180).withEndAction { dismiss() }.start()
        onFinish()
    }

    /** Tira da tela sem marcar como visto (ex.: o launcher saiu de cena no meio do tour). */
    fun dismiss() {
        holeAnimator?.cancel()
        fingerAnimator?.cancel()
        (parent as? ViewGroup)?.removeView(this)
    }
}
