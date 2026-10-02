package app.pauca.helper

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.pauca.R
import app.pauca.data.Prefs
import app.pauca.focus.FocusManager

/**
 * Duas funções:
 *  - toque duplo para bloquear a tela (ouve só cliques no próprio Pauca);
 *  - no modo foco com "bloquear apps", ouve a troca de janelas de todos os apps e volta
 *    para o início quando um app fora da lista abre. Fora do foco, volta a ouvir só o Pauca.
 */
class MyAccessibilityService : AccessibilityService() {

    private var lastBlockToast = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onServiceConnected() {
        instance = this
        Prefs(applicationContext).lockModeOn = true
        configure()
        super.onServiceConnected()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    /** Amplia ou reduz o que o serviço ouve conforme o foco. */
    fun configure() {
        val info = serviceInfo ?: return
        val prefs = Prefs(applicationContext)
        val blocking = prefs.focusActive && prefs.focusBlockApps
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                (if (blocking) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED else 0)
        info.packageNames = if (blocking) null else arrayOf(packageName)
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                blockIfRestricted(event.packageName?.toString() ?: return)
                return
            }
            val source: AccessibilityNodeInfo = event.source ?: return
            if (source.className != "android.widget.FrameLayout") return

            when (source.contentDescription) {
                getString(R.string.lock_layout_description) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
        } catch (e: Exception) {
            return
        }
    }

    private fun blockIfRestricted(pkg: String) {
        val prefs = Prefs(applicationContext)
        if (!prefs.focusActive || !prefs.focusBlockApps) return
        if (!FocusManager.isRestricted(applicationContext, pkg)) return
        performGlobalAction(GLOBAL_ACTION_HOME)
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockToast > 3000) {
            lastBlockToast = now
            applicationContext.showToast(Language.wrap(this).getString(R.string.focus_app_blocked))
        }
    }

    override fun onInterrupt() {
    }

    companion object {
        @Volatile
        var instance: MyAccessibilityService? = null
            private set
    }
}
