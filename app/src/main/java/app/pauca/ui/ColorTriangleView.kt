package app.pauca.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SweepGradient
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import app.pauca.helper.dpToPx
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Seletor de cor: um anel com todos os matizes e, dentro dele, um triângulo que vai da cor
 * pura ao branco e ao preto. O triângulo gira junto com o matiz, a ponta colorida sempre
 * virada para o marcador do anel.
 */
class ColorTriangleView(context: Context) : View(context) {

    /** Chamado a cada mudança feita pelo dedo. */
    var onColorChanged: ((Int) -> Unit)? = null

    private var hue = 0f
    private var sat = 1f
    private var value = 1f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val vertices = FloatArray(6)
    private val vertexColors = IntArray(3)

    private var cx = 0f
    private var cy = 0f
    private var outer = 0f
    private var ring = 0f
    private var inner = 0f

    private enum class Target { NONE, RING, TRIANGLE }
    private var dragging = Target.NONE

    init {
        // Os vértices coloridos (drawVertices) só são desenhados pela GPU a partir do Android 10
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    @get:ColorInt
    var color: Int
        get() = Color.HSVToColor(floatArrayOf(hue, sat, value))
        set(c) {
            val hsv = FloatArray(3)
            Color.colorToHSV(c, hsv)
            // Cinzas não têm matiz: mantém o atual para o triângulo não girar à toa
            if (hsv[1] > 0f && hsv[2] > 0f) hue = hsv[0]
            sat = hsv[1]
            value = hsv[2]
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val side = min(width, 300.dpToPx())
        setMeasuredDimension(width, side)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        outer = min(w, h) / 2f - 4.dpToPx()
        ring = outer * 0.16f
        inner = outer - ring - 8.dpToPx()
        ringPaint.strokeWidth = ring
        ringPaint.shader = SweepGradient(
            cx, cy,
            intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED),
            null,
        )
    }

    /** Pontas do triângulo: cor pura (no ângulo do matiz), branco e preto. */
    private fun corners(): Array<FloatArray> {
        val a = Math.toRadians(hue.toDouble())
        fun at(offset: Double) = floatArrayOf(
            cx + inner * cos(a + offset).toFloat(),
            cy + inner * sin(a + offset).toFloat(),
        )
        return arrayOf(at(0.0), at(2 * Math.PI / 3), at(4 * Math.PI / 3))
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(cx, cy, outer - ring / 2, ringPaint)

        val (h, w, b) = corners()
        floatArrayOf(h[0], h[1], w[0], w[1], b[0], b[1]).copyInto(vertices)
        vertexColors[0] = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        vertexColors[1] = Color.WHITE
        vertexColors[2] = Color.BLACK
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES, 6, vertices, 0, null, 0, vertexColors, 0, null, 0, 0, trianglePaint,
        )

        // Marcador do anel
        val a = Math.toRadians(hue.toDouble())
        val r = outer - ring / 2
        marker(canvas, cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat(), Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

        // Marcador do triângulo: pesos da cor pura, do branco e do preto
        val wh = sat * value
        val ww = value - wh
        val wb = 1f - value
        marker(canvas, wh * h[0] + ww * w[0] + wb * b[0], wh * h[1] + ww * w[1] + wb * b[1], color)
    }

    private fun marker(canvas: Canvas, x: Float, y: Float, @ColorInt under: Int) {
        val light = ColorUtils.calculateLuminance(under) > 0.5
        markerPaint.strokeWidth = 2.5f.dpToPxF()
        markerPaint.color = if (light) Color.BLACK else Color.WHITE
        canvas.drawCircle(x, y, 9.dpToPx().toFloat(), markerPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val d = hypot(x - cx, y - cy)
                dragging = when {
                    d > inner + 4.dpToPx() && d <= outer + 12.dpToPx() -> Target.RING
                    d <= inner -> Target.TRIANGLE
                    else -> Target.NONE
                }
                if (dragging == Target.NONE) return false
                parent?.requestDisallowInterceptTouchEvent(true)
                move(x, y)
            }
            MotionEvent.ACTION_MOVE -> move(x, y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = Target.NONE
        }
        return true
    }

    private fun move(x: Float, y: Float) {
        when (dragging) {
            Target.RING -> {
                hue = ((Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())) + 360) % 360).toFloat()
            }
            Target.TRIANGLE -> pickInTriangle(x, y)
            Target.NONE -> return
        }
        invalidate()
        onColorChanged?.invoke(color)
    }

    /** Converte o ponto em pesos das três pontas; fora do triângulo, prende na borda. */
    private fun pickInTriangle(x: Float, y: Float) {
        val (h, w, b) = corners()
        val det = (w[1] - b[1]) * (h[0] - b[0]) + (b[0] - w[0]) * (h[1] - b[1])
        var wh = ((w[1] - b[1]) * (x - b[0]) + (b[0] - w[0]) * (y - b[1])) / det
        var ww = ((b[1] - h[1]) * (x - b[0]) + (h[0] - b[0]) * (y - b[1])) / det
        var wb = 1f - wh - ww
        wh = wh.coerceAtLeast(0f); ww = ww.coerceAtLeast(0f); wb = wb.coerceAtLeast(0f)
        val sum = wh + ww + wb
        wh /= sum; ww /= sum
        value = (wh + ww).coerceIn(0f, 1f)
        sat = if (value > 0f) (wh / value).coerceIn(0f, 1f) else 0f
    }

    private fun Float.dpToPxF() = this * resources.displayMetrics.density
}
