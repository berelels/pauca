package app.pauca.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import app.pauca.R
import app.pauca.data.Accent
import app.pauca.data.Constants
import app.pauca.data.Palette
import app.pauca.helper.dpToPx
import app.pauca.helper.showPopupMenu

/**
 * Monta as telas de ajustes em código: seções com título e cartões de linhas.
 * Cada linha guarda como se atualizar; [refresh] redesenha os valores sem reconstruir.
 */
class SettingsBuilder(
    private val context: Context,
    private val container: LinearLayout,
    private val palette: Palette,
) {
    private val refreshers = mutableListOf<() -> Unit>()
    private var card: LinearLayout? = null

    fun refresh() = refreshers.forEach { it() }

    fun clear() {
        container.removeAllViews()
        refreshers.clear()
        card = null
    }

    /** Título grande em serifa, como no Respiro. */
    fun header(title: String, subtitle: String? = null) {
        container.addView(TextView(context).apply {
            text = title
            textSize = 34f
            setTextColor(palette.text)
            Look.applyFont(this, Constants.Font.NEWSREADER, 400, 34f)
            setPadding(6.dpToPx(), 8.dpToPx(), 6.dpToPx(), 0)
        })
        if (subtitle != null) container.addView(TextView(context).apply {
            text = subtitle
            textSize = 15f
            setTextColor(palette.muted)
            Look.applyFont(this, Constants.Font.JAKARTA, 400)
            setPadding(6.dpToPx(), 4.dpToPx(), 6.dpToPx(), 0)
        })
    }

    fun section(title: String?) {
        if (title != null) container.addView(TextView(context).apply {
            text = title
            textSize = 12.5f
            letterSpacing = 0.08f
            isAllCaps = true
            setTextColor(palette.muted)
            Look.applyFont(this, Constants.Font.JAKARTA, 700)
            setPadding(10.dpToPx(), 26.dpToPx(), 10.dpToPx(), 8.dpToPx())
        })
        else container.addView(View(context), LinearLayout.LayoutParams(1, 18.dpToPx()))
        card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = Look.rounded(palette.card, 22)
            clipToOutline = true
        }
        container.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun addToCard(view: View) {
        val target = card ?: run {
            section(null)
            card!!
        }
        if (target.childCount > 0) target.addView(View(context).apply {
            setBackgroundColor(palette.divider)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply {
            marginStart = 18.dpToPx()
        })
        target.addView(view)
    }

    private fun rowShell(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = 58.dpToPx()
        setPadding(18.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
        background = Look.pressable(0, 0, palette.text)
    }

    private fun texts(title: String, subtitle: (() -> String?)?): Pair<LinearLayout, TextView> {
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        column.addView(TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(palette.text)
            Look.applyFont(this, Constants.Font.JAKARTA, 500)
        })
        val sub = TextView(context).apply {
            textSize = 13f
            setTextColor(palette.muted)
            Look.applyFont(this, Constants.Font.JAKARTA, 400)
            setPadding(0, 2.dpToPx(), 0, 0)
        }
        column.addView(sub)
        val update = {
            val s = subtitle?.invoke()
            sub.text = s
            sub.isVisible = !s.isNullOrEmpty()
        }
        update()
        refreshers.add(update)
        return column to sub
    }

    /** Linha com título, subtítulo opcional, valor à direita e/ou seta. */
    fun row(
        title: String,
        subtitle: (() -> String?)? = null,
        value: (() -> String?)? = null,
        chevron: Boolean = false,
        accentValue: Boolean = false,
        onClick: (View) -> Unit,
    ): View {
        val row = rowShell()
        row.addView(texts(title, subtitle).first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (value != null) {
            val valueView = TextView(context).apply {
                textSize = 15f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                maxWidth = 170.dpToPx()
                setTextColor(if (accentValue) palette.accent else palette.muted)
                Look.applyFont(this, Constants.Font.JAKARTA, if (accentValue) 600 else 500)
                setPadding(12.dpToPx(), 0, 4.dpToPx(), 0)
            }
            row.addView(valueView)
            val update = { valueView.text = value() }
            update()
            refreshers.add(update)
        }
        if (chevron) row.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_chevron)
            imageTintList = ColorStateList.valueOf(palette.muted)
        }, LinearLayout.LayoutParams(20.dpToPx(), 20.dpToPx()))
        row.setOnClickListener { onClick(it) }
        addToCard(row)
        return row
    }

    /** Linha com interruptor. [set] devolve false para recusar (ex.: falta permissão). */
    fun toggle(
        title: String,
        subtitle: (() -> String?)? = null,
        get: () -> Boolean,
        set: (Boolean) -> Boolean,
    ): View {
        val row = rowShell()
        row.addView(texts(title, subtitle).first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val switch = SwitchCompat(context).apply {
            isClickable = false
            isFocusable = false
            thumbTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(0xFFFFFAF3.toInt(), ColorUtils.blendARGB(palette.text, palette.card, 0.35f))
            )
            trackTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(palette.accent, ColorUtils.setAlphaComponent(palette.text, 0x33))
            )
            setPadding(12.dpToPx(), 0, 0, 0)
        }
        row.addView(switch)
        val update = { switch.isChecked = get() }
        update()
        refreshers.add(update)
        row.setOnClickListener {
            if (set(!get())) refresh() else update()
        }
        addToCard(row)
        return row
    }

    /** − valor + */
    fun stepper(
        title: String,
        get: () -> Int,
        set: (Int) -> Unit,
        min: Int,
        max: Int,
        step: Int,
        format: (Int) -> String = { it.toString() },
    ): View {
        val row = rowShell()
        row.background = null
        row.addView(texts(title, null).first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val valueView = TextView(context).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            minWidth = 52.dpToPx()
            setTextColor(palette.text)
            Look.applyFont(this, Constants.Font.JAKARTA, 600)
        }
        fun button(label: String, delta: Int) = TextView(context).apply {
            text = label
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(palette.accent)
            Look.applyFont(this, Constants.Font.JAKARTA, 600)
            background = Look.pressable(ColorUtils.setAlphaComponent(palette.accent, 0x1F), 18, palette.accent)
            layoutParams = LinearLayout.LayoutParams(40.dpToPx(), 36.dpToPx())
            setOnClickListener {
                val next = (get() + delta).coerceIn(min, max)
                if (next != get()) {
                    set(next)
                    refresh()
                }
            }
        }
        row.addView(button("−", -step))
        row.addView(valueView)
        row.addView(button("+", step))
        val update = { valueView.text = format(get()) }
        update()
        refreshers.add(update)
        addToCard(row)
        return row
    }

    /**
     * Título e valor em cima, barra deslizante embaixo. [onChange] é chamado enquanto
     * arrasta (para a prévia); o valor fica salvo pelo próprio [set].
     */
    fun slider(
        title: String,
        get: () -> Int,
        set: (Int) -> Unit,
        min: Int,
        max: Int,
        format: (Int) -> String = { it.toString() },
    ): View {
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18.dpToPx(), 14.dpToPx(), 14.dpToPx(), 8.dpToPx())
        }
        val top = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(palette.text)
            Look.applyFont(this, Constants.Font.JAKARTA, 500)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val valueView = TextView(context).apply {
            textSize = 15f
            setTextColor(palette.muted)
            Look.applyFont(this, Constants.Font.JAKARTA, 600)
        }
        top.addView(valueView)
        column.addView(top)
        val bar = SeekBar(context).apply {
            this.max = max - min
            progress = get() - min
            progressTintList = ColorStateList.valueOf(palette.accent)
            thumbTintList = ColorStateList.valueOf(palette.accent)
            progressBackgroundTintList = ColorStateList.valueOf(palette.text)
            setPadding(6.dpToPx(), 10.dpToPx(), 6.dpToPx(), 6.dpToPx())
        }
        valueView.text = format(get())
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                set(progress + min)
                valueView.text = format(progress + min)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
        column.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        refreshers.add {
            bar.progress = get() - min
            valueView.text = format(get())
        }
        addToCard(column)
        return column
    }

    /** Uma view qualquer dentro de um cartão (ex.: prévia do relógio). */
    fun custom(view: View, onRefresh: (() -> Unit)? = null) {
        addToCard(view)
        onRefresh?.let {
            it()
            refreshers.add(it)
        }
    }

    /** Texto pequeno fora dos cartões, para explicar algo. */
    fun note(text: String) {
        container.addView(TextView(context).apply {
            this.text = text
            textSize = 13f
            setTextColor(palette.muted)
            Look.applyFont(this, Constants.Font.JAKARTA, 400)
            setLineSpacing(2.dpToPx().toFloat(), 1f)
            setPadding(10.dpToPx(), 10.dpToPx(), 10.dpToPx(), 0)
        })
        card = null
    }

    fun space(dp: Int) {
        container.addView(View(context), LinearLayout.LayoutParams(1, dp.dpToPx()))
    }

    /** Linha de amostras de cor, uma para cada paleta. */
    fun palettes(current: () -> String, onPick: (Palette) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8.dpToPx(), 16.dpToPx(), 8.dpToPx(), 12.dpToPx())
        }
        val swatches = Palette.ALL.map { p ->
            val swatch = View(context)
            val label = TextView(context).apply {
                setText(p.label)
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 1
                Look.applyFont(this, Constants.Font.JAKARTA, 500)
                setPadding(0, 6.dpToPx(), 0, 0)
            }
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(swatch, LinearLayout.LayoutParams(46.dpToPx(), 46.dpToPx()))
                addView(label)
                setOnClickListener { onPick(p) }
            }
            row.addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            Triple(p, swatch, label)
        }
        val update = {
            swatches.forEach { (p, swatch, label) ->
                val selected = p.id == current()
                swatch.background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    if (p.showsWallpaper) intArrayOf(0xFF8A6A55.toInt(), 0xFF2E3B44.toInt())
                    else intArrayOf(p.bg, p.card)
                ).apply {
                    shape = GradientDrawable.OVAL
                    setStroke(
                        if (selected) 3.dpToPx() else 1.dpToPx(),
                        if (selected) palette.accent else palette.divider
                    )
                }
                label.setTextColor(if (selected) palette.text else palette.muted)
            }
        }
        update()
        refreshers.add(update)
        addToCard(row)
    }

    /**
     * Grade de cores de destaque, quatro por linha. [value] null é "sem cor" (desenhada
     * na cor do texto); a última amostra abre a cor personalizada.
     */
    fun accents(
        options: List<Triple<String, String, Int?>>,
        current: () -> String,
        onPick: (String) -> Unit,
        customLabel: String,
        onCustom: () -> Unit,
    ) {
        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8.dpToPx(), 14.dpToPx(), 8.dpToPx(), 10.dpToPx())
        }
        // (id, amostra, nome); id null é a personalizada
        val cells = mutableListOf<Triple<String?, TextView, TextView>>()
        val all: List<Pair<String?, String>> = options.map { it.first to it.second } + (null to customLabel)
        all.chunked(4).forEach { chunk ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4.dpToPx(), 0, 6.dpToPx())
            }
            chunk.forEach { (id, label) ->
                val swatch = TextView(context).apply {
                    gravity = Gravity.CENTER
                    textSize = 20f
                }
                val name = TextView(context).apply {
                    text = label
                    textSize = 12f
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    Look.applyFont(this, Constants.Font.JAKARTA, 500)
                    setPadding(2.dpToPx(), 6.dpToPx(), 2.dpToPx(), 0)
                }
                val column = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    addView(swatch, LinearLayout.LayoutParams(42.dpToPx(), 42.dpToPx()))
                    addView(name)
                    setOnClickListener { if (id == null) onCustom() else onPick(id) }
                }
                row.addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                cells.add(Triple(id, swatch, name))
            }
            repeat(4 - chunk.size) { row.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f)) }
            grid.addView(row)
        }
        val presetIds = options.map { it.first }.toSet()
        val update = {
            val selected = current()
            val isCustom = selected !in presetIds
            cells.forEach { (id, swatch, name) ->
                val on = if (id == null) isCustom else id == selected
                val fill = when {
                    id == null -> if (isCustom) Accent.parse(selected) ?: palette.text else 0
                    else -> options.first { it.first == id }.third ?: palette.text
                }
                swatch.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(fill)
                    setStroke(if (on) 3.dpToPx() else 1.dpToPx(), if (on) palette.text else palette.divider)
                }
                swatch.text = if (id == null && !isCustom) "+" else ""
                swatch.setTextColor(palette.muted)
                name.setTextColor(if (on) palette.text else palette.muted)
            }
        }
        update()
        refreshers.add(update)
        addToCard(grid)
    }

    companion object {
        /** Menu de opções preso à linha, com um ✓ na atual. */
        fun <T> choose(anchor: View, options: List<Pair<String, T>>, current: T, onPick: (T) -> Unit) {
            anchor.showPopupMenu(configure = { menu ->
                options.forEachIndexed { i, (label, value) ->
                    menu.add(0, i, i, if (value == current) "✓  $label" else label)
                }
            }) { onPick(options[it.itemId].second) }
        }
    }
}
