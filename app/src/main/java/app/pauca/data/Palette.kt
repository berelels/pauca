package app.pauca.data

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.graphics.ColorUtils
import app.pauca.R

/**
 * Cores de uma tela inteira: fundo, cartões, texto e destaque.
 * Mesma família do Respiro: papel quente e tinta escura. O destaque é escolhido à parte
 * (oliva por padrão) e vale para todos os temas; veja [withAccent].
 */
data class Palette(
    val id: String,
    @StringRes val label: Int,
    @ColorInt val bg: Int,
    @ColorInt val card: Int,
    @ColorInt val text: Int,
    @ColorInt val accent: Int,
    @ColorInt val accentInk: Int,
    val isDark: Boolean,
    /** Fundo transparente: mostra o papel de parede do sistema. */
    val showsWallpaper: Boolean = false,
) {
    @get:ColorInt
    val muted: Int get() = ColorUtils.setAlphaComponent(text, if (showsWallpaper) 0xB3 else 0x80)

    @get:ColorInt
    val faint: Int get() = ColorUtils.setAlphaComponent(text, 0x14)

    @get:ColorInt
    val divider: Int get() = ColorUtils.setAlphaComponent(text, 0x1A)

    /**
     * Troca o destaque. [Accent.NONE] usa a própria cor do texto (sem cor nenhuma).
     * A tinta sobre o destaque (botões cheios) fica clara ou escura, a que tiver mais contraste.
     */
    fun withAccent(accent: String): Palette {
        val color = Accent.parse(accent) ?: return copy(
            accent = text,
            accentInk = if (showsWallpaper) Color.BLACK else bg,
        )
        val light = Color.parseColor("#FFFAF3")
        val dark = Color.parseColor("#1D1A14")
        val ink = if (ColorUtils.calculateContrast(light, color) >= ColorUtils.calculateContrast(dark, color)) light else dark
        return copy(accent = color, accentInk = ink)
    }

    companion object {
        val PAUCA = Palette(
            id = "pauca",
            label = R.string.palette_pauca,
            bg = Color.parseColor("#1A1713"),
            card = Color.parseColor("#26211B"),
            text = Color.parseColor("#EFE7DA"),
            accent = Color.parseColor("#7E8C54"),
            accentInk = Color.parseColor("#1D130D"),
            isDark = true,
        )
        val PAPEL = Palette(
            id = "papel",
            label = R.string.palette_papel,
            bg = Color.parseColor("#F6F1E7"),
            card = Color.parseColor("#EAE2D2"),
            text = Color.parseColor("#26211B"),
            accent = Color.parseColor("#7E8C54"),
            accentInk = Color.parseColor("#FFFAF3"),
            isDark = false,
        )
        val GRAFITE = Palette(
            id = "grafite",
            label = R.string.palette_grafite,
            bg = Color.parseColor("#1C1C1E"),
            card = Color.parseColor("#2C2C2E"),
            text = Color.parseColor("#FFFFFF"),
            accent = Color.parseColor("#7E8C54"),
            accentInk = Color.parseColor("#1D130D"),
            isDark = true,
        )
        val PRETO = Palette(
            id = "preto",
            label = R.string.palette_preto,
            bg = Color.BLACK,
            card = Color.parseColor("#151412"),
            text = Color.parseColor("#F3EEE6"),
            accent = Color.parseColor("#7E8C54"),
            accentInk = Color.parseColor("#1D130D"),
            isDark = true,
        )
        val WALLPAPER = Palette(
            id = "wallpaper",
            label = R.string.palette_wallpaper,
            bg = Color.TRANSPARENT,
            card = Color.parseColor("#59000000"),
            text = Color.WHITE,
            accent = Color.parseColor("#7E8C54"),
            accentInk = Color.parseColor("#1D130D"),
            isDark = true,
            showsWallpaper = true,
        )

        val ALL = listOf(PAUCA, PAPEL, GRAFITE, PRETO, WALLPAPER)

        fun byId(id: String): Palette = ALL.firstOrNull { it.id == id } ?: PAUCA
    }
}

/** Cores de destaque prontas. Guardadas como texto: "#RRGGBB" ou [NONE]. */
object Accent {
    const val NONE = "none"
    const val DEFAULT = "#7E8C54"
    const val BLUE = "#5E7FA3"

    /** Cor e nome (recurso de texto). */
    val PRESETS = listOf(
        DEFAULT to R.string.accent_olive,
        "#C4673F" to R.string.accent_terracotta,
        "#B8893A" to R.string.accent_ochre,
        BLUE to R.string.accent_blue,
        "#8E5E7E" to R.string.accent_plum,
        "#C2707A" to R.string.accent_rose,
    )

    @ColorInt
    fun parse(value: String): Int? = if (value == NONE) null else try {
        Color.parseColor(value) or 0xFF000000.toInt()
    } catch (_: Exception) {
        Color.parseColor(DEFAULT)
    }

    /** Aceita "7e8c54" ou "#7E8C54" e devolve "#7E8C54"; null se não for uma cor. */
    fun normalize(input: String): String? {
        val hex = input.trim().removePrefix("#")
        if (hex.length != 6 || hex.any { it !in "0123456789abcdefABCDEF" }) return null
        return "#" + hex.uppercase()
    }
}
