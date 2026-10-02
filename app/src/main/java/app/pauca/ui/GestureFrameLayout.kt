package app.pauca.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import kotlin.math.abs

/**
 * Raiz da tela inicial: vê todos os toques sem roubá-los dos filhos (a rolagem e os apps
 * continuam funcionando) e reconhece os gestos do launcher por cima.
 *
 * Deslizar para cima só abre a gaveta quando a lista já está no fim, e para baixo só abre
 * as notificações quando está no topo — assim a rolagem dos cartões não dispara nada.
 */
class GestureFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    interface Listener {
        fun onSwipeUp() {}
        fun onSwipeDown() {}
        fun onSwipeLeft() {}
        fun onSwipeRight() {}
        fun onDoubleTapEmpty() {}
        fun onLongPressEmpty() {}

        /** Toque simples no fundo; [rawY] é a altura na tela, para saber em que faixa foi. */
        fun onTapEmpty(rawY: Float) {}
    }

    var listener: Listener? = null
    var scrollView: ScrollView? = null

    private var downOnInteractive = false
    private var atTopOnDown = true
    private var atBottomOnDown = true
    private val hitRect = Rect()
    private val location = IntArray(2)

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (!downOnInteractive) listener?.onTapEmpty(e.rawY)
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!downOnInteractive) listener?.onDoubleTapEmpty()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            if (!downOnInteractive) listener?.onLongPressEmpty()
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val start = e1 ?: return false
            val dx = e2.x - start.x
            val dy = e2.y - start.y
            if (abs(dx) > abs(dy)) {
                if (abs(dx) > SWIPE_DISTANCE && abs(velocityX) > SWIPE_VELOCITY) {
                    if (dx > 0) listener?.onSwipeRight() else listener?.onSwipeLeft()
                }
            } else if (abs(dy) > SWIPE_DISTANCE && abs(velocityY) > SWIPE_VELOCITY) {
                if (dy < 0 && atBottomOnDown) listener?.onSwipeUp()
                if (dy > 0 && atTopOnDown) listener?.onSwipeDown()
            }
            return false
        }
    })

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            downOnInteractive = findInteractive(this, ev.rawX.toInt(), ev.rawY.toInt())
            atTopOnDown = scrollView?.canScrollVertically(-1) != true
            atBottomOnDown = scrollView?.canScrollVertically(1) != true
        }
        detector.onTouchEvent(ev)
        super.dispatchTouchEvent(ev)
        // Sempre "aceita" o toque para continuar recebendo o resto do gesto
        return true
    }

    /** Há um app ou botão debaixo do dedo? Aí o gesto de fundo não vale. */
    private fun findInteractive(view: View, x: Int, y: Int): Boolean {
        if (!view.isShown) return false
        view.getLocationOnScreen(location)
        hitRect.set(location[0], location[1], location[0] + view.width, location[1] + view.height)
        if (!hitRect.contains(x, y)) return false
        if (view !== this && (view.isClickable || view.isLongClickable) && view !is ScrollView) return true
        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0)
                if (findInteractive(view.getChildAt(i), x, y)) return true
        }
        return false
    }

    private companion object {
        const val SWIPE_DISTANCE = 100
        const val SWIPE_VELOCITY = 100
    }
}
