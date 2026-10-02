package app.pauca.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import app.pauca.R
import app.pauca.data.Constants
import app.pauca.helper.Language
import app.pauca.helper.dpToPx
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
        val base = typeface(view.context, font)
        val variable = font == Constants.Font.JAKARTA || font == Constants.Font.NEWSREADER
        // Trocar a fonte descarta o peso já aplicado, mas o TextView ignora o mesmo peso de
        // novo (acha que nada mudou). Limpar antes garante que o peso volte a cada redesenho.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) view.fontVariationSettings = null
        if (variable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.typeface = base
            val axes = buildList {
                add("'wght' $weight")
                if (font == Constants.Font.NEWSREADER && opticalSize != null)
                    add("'opsz' ${opticalSize.coerceIn(6f, 72f)}")
            }
            view.fontVariationSettings = axes.joinToString(", ")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.typeface = Typeface.create(base, weight.coerceIn(1, 1000), false)
        } else {
            view.typeface = Typeface.create(base, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
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
