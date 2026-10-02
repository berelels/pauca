package app.pauca.ui

import android.content.res.ColorStateList
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import app.pauca.data.Constants
import app.pauca.data.Palette
import app.pauca.databinding.ViewTopBarBinding

/** Barra de topo das telas internas: voltar, título em serifa e "Concluído". */
fun ViewTopBarBinding.style(
    palette: Palette,
    title: CharSequence,
    onBack: () -> Unit,
    onDone: (() -> Unit)? = null,
) {
    this.title.text = title
    this.title.setTextColor(palette.text)
    Look.applyFont(this.title, Constants.Font.NEWSREADER, 500, 22f)
    back.imageTintList = ColorStateList.valueOf(palette.text)
    back.background = Look.pressable(0, 22, palette.text)
    back.setOnClickListener { onBack() }
    done.isVisible = onDone != null
    done.setTextColor(palette.accent)
    Look.applyFont(done, Constants.Font.JAKARTA, 700)
    done.background = Look.pressable(0, 22, palette.accent)
    done.setOnClickListener { onDone?.invoke() }
}

/** Fundo da paleta e espaço para as barras do sistema. */
fun View.applyScreenStyle(palette: Palette) {
    setBackgroundColor(if (palette.showsWallpaper) Palette.PAUCA.bg else palette.bg)
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsetsIgnoringVisibility(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        v.updatePadding(top = bars.top, bottom = maxOf(bars.bottom, ime.bottom))
        insets
    }
}

/** As telas internas não usam o fundo transparente do papel de parede: ficariam ilegíveis. */
val Palette.forScreens: Palette
    get() = if (showsWallpaper) Palette.PAUCA.copy(accent = accent, accentInk = accentInk) else this
