package app.pauca.helper

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Quem compra o Pauca depois de usar o Lite leva junto cartões, perfis e ajustes. O Lite
 * expõe as preferências num provider que só apps com a mesma assinatura podem ler; o Pauca
 * as copia na primeira vez que abre. Tudo fica no celular, sem internet.
 */
object LiteImport {
    /** Arquivos de SharedPreferences copiados (os mesmos nomes nos dois apps). */
    val FILES = listOf("app.pauca", "pauca_home")

    // Do próprio aparelho/instalação: o Pauca recém-instalado ainda precisa virar o launcher
    private val SKIP = setOf("FIRST_OPEN", "FIRST_OPEN_TIME", "HIDE_SET_DEFAULT_LAUNCHER", "LITE_IMPORT_CHECKED")

    private const val CHECKED = "LITE_IMPORT_CHECKED"

    /** Tenta uma única vez, na primeira abertura. Devolve true se importou algo. */
    fun runOnce(context: Context): Boolean {
        if (Edition.isLite) return false
        val prefs = context.getSharedPreferences(FILES[0], Context.MODE_PRIVATE)
        if (prefs.getBoolean(CHECKED, false)) return false
        prefs.edit(commit = true) { putBoolean(CHECKED, true) }
        // Só num Pauca novo: se já há tela inicial montada, não mexe
        if (context.getSharedPreferences(FILES[1], Context.MODE_PRIVATE).contains("HOME_DATA")) return false

        val uri = Uri.parse("content://${Edition.litePackage}.export/prefs")
        val rows = try {
            context.contentResolver.query(uri, null, null, null, null)
        } catch (_: Exception) {
            null // Lite não instalado, ou assinado com outra chave
        } ?: return false
        var imported = false
        rows.use { cursor ->
            while (cursor.moveToNext()) {
                val file = cursor.getString(0)
                if (file !in FILES) continue
                val target = context.getSharedPreferences(file, Context.MODE_PRIVATE)
                target.edit(commit = true) { decode(JSONObject(cursor.getString(1)), this) }
                imported = true
            }
        }
        return imported
    }

    /** Preferências → JSON com o tipo de cada valor, para voltar igual do outro lado. */
    fun encode(prefs: SharedPreferences): String {
        val out = JSONObject()
        prefs.all.forEach { (key, value) ->
            if (key in SKIP) return@forEach
            val (type, v) = when (value) {
                is Boolean -> "b" to value
                is Int -> "i" to value
                is Long -> "l" to value
                is Float -> "f" to value.toDouble()
                is String -> "s" to value
                is Set<*> -> "ss" to JSONArray(value.filterIsInstance<String>())
                else -> return@forEach
            }
            out.put(key, JSONObject().put("t", type).put("v", v))
        }
        return out.toString()
    }

    private fun decode(json: JSONObject, editor: SharedPreferences.Editor) {
        json.keys().forEach { key ->
            if (key in SKIP) return@forEach
            val entry = json.getJSONObject(key)
            when (entry.getString("t")) {
                "b" -> editor.putBoolean(key, entry.getBoolean("v"))
                "i" -> editor.putInt(key, entry.getInt("v"))
                "l" -> editor.putLong(key, entry.getLong("v"))
                "f" -> editor.putFloat(key, entry.getDouble("v").toFloat())
                "s" -> editor.putString(key, entry.getString("v"))
                "ss" -> {
                    val arr = entry.getJSONArray("v")
                    editor.putStringSet(key, (0 until arr.length()).map { arr.getString(it) }.toSet())
                }
            }
        }
    }
}
