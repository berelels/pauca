package app.pauca.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import app.pauca.BuildConfig
import app.pauca.R
import app.pauca.data.Prefs

/**
 * Se o Pauca fecha sozinho logo ao abrir, o Android avisa que o app "apresenta falhas
 * contínuas" e o celular fica sem tela inicial. Aqui se contam as aberturas que não
 * chegaram a ficar de pé: depois de duas seguidas, a próxima volta fonte, peso e tamanho
 * ao padrão (o que muda o desenho de tudo) e guarda o erro para a pessoa poder mandar.
 */
object CrashGuard {

    private const val FILE = "crash_guard"
    private const val PENDING = "PENDING_STARTS"
    private const val LAST_ERROR = "LAST_ERROR"
    private const val WAS_RESET = "WAS_RESET"
    private const val LIMIT = 2

    /** Tempo de tela inicial aberta para contar como uma abertura que deu certo. */
    const val STABLE_MS = 4000L

    private var installed = false

    private fun store(context: Context): SharedPreferences = context.getSharedPreferences(FILE, 0)

    /** Guarda o erro de qualquer fechamento, antes de o Android encerrar o app. */
    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                store(app).edit(commit = true) { putString(LAST_ERROR, error.stackTraceToString().take(6000)) }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** No começo da abertura, antes de ler qualquer ajuste. */
    fun onStart(context: Context) {
        val store = store(context)
        val pending = store.getInt(PENDING, 0)
        if (pending >= LIMIT) {
            Prefs(context).resetText()
            store.edit(commit = true) { putInt(PENDING, 1); putBoolean(WAS_RESET, true) }
        } else {
            store.edit(commit = true) { putInt(PENDING, pending + 1) }
        }
    }

    /** A tela inicial ficou aberta um tempo: a abertura deu certo. */
    fun onStable(context: Context) = store(context).edit { putInt(PENDING, 0) }

    /** Depois de uma volta ao padrão, o erro guardado (uma vez só); null se não houve. */
    fun takeReset(context: Context): String? {
        val store = store(context)
        if (!store.getBoolean(WAS_RESET, false)) return null
        val error = store.getString(LAST_ERROR, null).orEmpty()
        store.edit { remove(WAS_RESET); remove(LAST_ERROR) }
        return error
    }

    /** Avisa que os ajustes de texto voltaram ao padrão e oferece copiar o erro. */
    fun showResetNotice(context: Context, error: String) {
        val details = "Pauca ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · " +
                "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}\n\n$error"
        val dialog = context.createDialog(
            title = context.getString(R.string.crash_reset_title),
            action = context.getString(if (error.isNotEmpty()) R.string.crash_copy else R.string.okay),
            message = context.getString(R.string.crash_reset_message),
            onAction = {
                if (error.isNotEmpty()) {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("Pauca", details))
                    context.showToast(context.getString(R.string.crash_copied))
                }
            },
        )
        dialog.showRespectingStatusBar()
    }
}
