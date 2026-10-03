package app.pauca.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnLayout
import app.pauca.R
import app.pauca.data.Constants
import app.pauca.helper.Language
import app.pauca.helper.dpToPx
import java.text.SimpleDateFormat
import java.util.Locale

/** Fontes empacotadas e utilidades de estilo usadas pelas telas desenhadas em código. */
object Look {

    private val cache = mutableMapOf<String, Typeface?>()

    fun typeface(context: Context, font: String): Typeface = cache.getOrPut(font) {
        when (font) {
            Constants.Font.JAKARTA -> ResourcesCompat.getFont(context, R.font.jakarta)
            Constants.Font.NEWSREADER -> ResourcesCompat.getFont(context, R.font.newsreader)
            Constants.Font.MONO -> Typeface.MONOSPACE
            Constants.Font.SYSTEM_SERIF -> Typeface.SERIF
            else -> Typeface.SANS_SERIF
        }
    } ?: Typeface.DEFAULT

    /**
     * Aplica fonte e peso (100–900). As fontes empacotadas são variáveis, então o peso
     * vai pelo eixo 'wght'; a do sistema usa o peso mais próximo disponível.
     */
    fun applyFont(view: TextView, font: String, weight: Int, opticalSize: Float? = null) {
        try {
            setFont(view, font, weight, opticalSize)
        } catch (e: Exception) {
            // Alguns celulares recusam uma combinação de fonte e peso; melhor a fonte sem
            // peso do que o app fechando a cada abertura
            e.printStackTrace()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) runCatching { view.fontVariationSettings = null }
            view.typeface = runCatching { typeface(view.context, font) }.getOrDefault(Typeface.DEFAULT)
        }
    }

    private fun setFont(view: TextView, font: String, weight: Int, opticalSize: Float?) {
        val base = typeface(view.context, font)
        val variable = font == Constants.Font.JAKARTA || font == Constants.Font.NEWSREADER
        val weight = weight.coerceIn(100, 900)
        // Trocar a fonte descarta o peso já aplicado, mas o TextView ignora o mesmo peso de
        // novo (acha que nada mudou). Limpar antes garante que o peso volte a cada redesenho.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) view.fontVariationSettings = null
        if (variable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.typeface = base
            val axes = buildList {
                add("'wght' $weight")
                if (font == Constants.Font.NEWSREADER && opticalSize != null)
                    add("'opsz' ${opticalSize.coerceIn(6f, 72f).toInt()}")
            }
            view.fontVariationSettings = axes.joinToString(", ")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.typeface = Typeface.create(base, weight, false)
        } else {
            view.typeface = Typeface.create(base, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    /**
     * Sem o espaço extra da fonte (includeFontPadding falso), números grossos ou grandes passam
     * da altura que a fonte declara e saem cortados em cima. Mede o desenho de verdade e dá
     * só a folga que falta. Chamar depois de definir fonte, peso e tamanho.
     */
    fun fitGlyphs(view: TextView, sample: String = "0123456789:APM") {
        val bounds = Rect()
        view.paint.getTextBounds(sample, 0, sample.length, bounds)
        val metrics = view.paint.fontMetricsInt
        val top = (metrics.ascent - bounds.top).coerceAtLeast(0)
        val bottom = (bounds.bottom - metrics.descent).coerceAtLeast(0)
        val extra = 2.dpToPx()
        view.setPadding(view.paddingLeft, if (top > 0) top + extra else 0, view.paddingRight, if (bottom > 0) bottom + extra else 0)
    }

    /**
     * Relógio sempre numa linha: se no tamanho escolhido a hora não couber em [container],
     * encolhe só o bastante (antes, "10:00" grande e grosso quebrava em "10:0" e "0").
     * Depois acerta a folga de cima com [fitGlyphs].
     */
    fun fitClock(clock: TextView, container: View) {
        clock.maxLines = 1
        container.doOnLayout {
            val available = container.width - container.paddingLeft - container.paddingRight -
                    clock.paddingLeft - clock.paddingRight
            // Todos os dígitos como "0", um dos mais largos: a largura não muda a cada minuto
            val sample = clock.text.toString().replace(Regex("\\d"), "0")
            val width = clock.paint.measureText(sample)
            if (available > 0 && width > available) {
                clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, clock.textSize * available / width * 0.97f)
            }
            fitGlyphs(clock)
        }
    }

    /**
     * Cursor piscando, alças de seleção e realce na cor de destaque. Sem isso o Android
     * usa a cor fixa do tema, que não acompanha a cor escolhida nos ajustes.
     */
    fun tintTextInput(view: TextView, @ColorInt accent: Int) {
        view.highlightColor = ColorUtils.setAlphaComponent(accent, 0x55)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        view.textCursorDrawable = GradientDrawable().apply {
            setColor(accent)
            setSize(2.dpToPx(), 1)
        }
        view.textSelectHandle?.mutate()?.let { it.setTint(accent); view.setTextSelectHandle(it) }
        view.textSelectHandleLeft?.mutate()?.let { it.setTint(accent); view.setTextSelectHandleLeft(it) }
        view.textSelectHandleRight?.mutate()?.let { it.setTint(accent); view.setTextSelectHandleRight(it) }
    }

    fun applyCase(text: String, textCase: Int): String = when (textCase) {
        Constants.TextCase.LOWER -> text.lowercase(Locale.getDefault())
        Constants.TextCase.UPPER -> text.uppercase(Locale.getDefault())
        else -> text
    }

    fun rounded(@ColorInt color: Int, radiusDp: Int, @ColorInt stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp.dpToPx().toFloat()
            setColor(color)
            stroke?.let { setStroke(1.dpToPx(), it) }
        }

    /** Fundo arredondado com o "toque" do Android, na cor do texto. */
    fun pressable(@ColorInt color: Int, radiusDp: Int, @ColorInt ripple: Int): RippleDrawable {
        val shape = rounded(color, radiusDp)
        val mask = rounded(0xFFFFFFFF.toInt(), radiusDp)
        return RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(ripple, 0x33)), shape, mask)
    }

    /**
     * Padrão do relógio. Vazio é o automático: segue o 12/24h do celular, e não o do idioma
     * do app (um celular em 24h com o Pauca em inglês continua em 24h).
     */
    fun timePattern(context: Context, custom: String): String =
        custom.ifEmpty { if (systemUses24h(context)) "HH:mm" else "h:mm" }

    /**
     * Hora curta para textos como "desde 09:42". No formato 24h usa sempre dois dígitos, como o
     * relógio (o padrão de alguns idiomas, como o espanhol, tiraria o zero); no 12h mantém o AM/PM.
     */
    fun shortTime(context: Context, millis: Long): String =
        if (systemUses24h(context)) SimpleDateFormat("HH:mm", Language.locale(context)).format(millis)
        else DateFormat.getTimeFormat(context).format(millis)

    private fun systemUses24h(context: Context): Boolean {
        Settings.System.getString(context.contentResolver, Settings.System.TIME_12_24)?.let { return it == "24" }
        // Sem escolha explícita, o Android decide pelo idioma do sistema
        val locale = Language.systemLocale(context)
        val pattern = DateFormat.getBestDateTimePattern(locale, "jm")
        return pattern.contains('H') || pattern.contains('k')
    }

    fun spToPx(context: Context, sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
}
