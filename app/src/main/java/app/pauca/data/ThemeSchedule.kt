package app.pauca.data

import androidx.annotation.StringRes
import app.pauca.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Tema automático: o dia dividido em três partes, cada uma com o seu tema. Cada parte vale
 * da hora em que começa até a próxima começar (a noite atravessa a meia-noite).
 */
object ThemeSchedule {

    data class Slot(@StringRes val label: Int, val startMinute: Int, val paletteId: String)

    /** Rótulos fixos, na ordem do dia. */
    private val LABELS = listOf(R.string.auto_theme_day, R.string.auto_theme_evening, R.string.auto_theme_night)

    /** Dia claro, fim de tarde no escuro quente do Pauca, noite no preto. */
    val DEFAULT = listOf(
        Slot(LABELS[0], 7 * 60, Palette.PAPEL.id),
        Slot(LABELS[1], 18 * 60, Palette.PAUCA.id),
        Slot(LABELS[2], 22 * 60, Palette.PRETO.id),
    )

    fun parse(json: String): List<Slot> = try {
        val array = JSONArray(json)
        val slots = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Slot(LABELS[i], o.getInt("start").coerceIn(0, 24 * 60 - 1), o.getString("palette"))
        }
        if (slots.size == LABELS.size) slots else DEFAULT
    } catch (_: Exception) {
        DEFAULT
    }

    fun toJson(slots: List<Slot>): String = JSONArray(slots.map {
        JSONObject().put("start", it.startMinute).put("palette", it.paletteId)
    }).toString()

    private fun nowMinute(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /** A parte do dia em que estamos: a última que já começou (ou a última de ontem). */
    fun current(slots: List<Slot>, minute: Int = nowMinute()): Slot {
        val sorted = slots.sortedBy { it.startMinute }
        return sorted.lastOrNull { it.startMinute <= minute } ?: sorted.last()
    }

    /** Milissegundos até a próxima troca, para conferir o tema bem na hora. */
    fun millisToNextChange(slots: List<Slot>): Long {
        val c = Calendar.getInstance()
        val minute = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        val next = slots.map { it.startMinute }.filter { it > minute }.minOrNull()
            ?: (slots.minOf { it.startMinute } + 24 * 60)
        val seconds = c.get(Calendar.SECOND)
        return ((next - minute) * 60L - seconds) * 1000L + 500L
    }

    fun formatMinute(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
}
