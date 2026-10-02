package app.pauca.focus

import android.Manifest
import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.service.notification.Condition
import android.service.notification.NotificationListenerService
import android.service.notification.ZenPolicy
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import app.pauca.BuildConfig
import app.pauca.MainActivity
import app.pauca.R
import app.pauca.data.Constants
import app.pauca.data.HomeStore
import app.pauca.data.Prefs
import app.pauca.helper.Edition
import app.pauca.helper.Language
import app.pauca.helper.MyAccessibilityService

/**
 * Modo foco, em camadas independentes (cada uma só age se o usuário deu a permissão):
 *
 *  1. Não Perturbe — uma regra própria do Pauca no sistema (Android 10+), então não
 *     mexe no Não Perturbe que o usuário liga à mão.
 *  2. Filtro de notificações — [FocusListenerService] segura as notificações de apps fora
 *     da lista e mostra um resumo quando o foco acaba.
 *  3. Gaveta — só mostra os apps permitidos (feito no MainViewModel).
 *  4. Bloqueio — o serviço de acessibilidade volta para o início se um app fora da lista abrir.
 *  5. Tons de cinza — precisa de WRITE_SECURE_SETTINGS, concedida uma vez pelo adb.
 */
object FocusManager {

    private val ZEN_CONDITION: Uri = Uri.parse("condition://${BuildConfig.APPLICATION_ID}/focus")

    private val listeners = mutableSetOf<() -> Unit>()

    /** A tela inicial e os ajustes se inscrevem para redesenhar quando o foco muda. */
    fun addListener(listener: () -> Unit) = listeners.add(listener)
    fun removeListener(listener: () -> Unit) = listeners.remove(listener)

    fun isActive(context: Context) = Prefs(context).focusActive

    fun toggle(context: Context) {
        if (isActive(context)) deactivate(context) else activate(context, Constants.FocusSource.MANUAL)
    }

    fun activate(context: Context, source: String) {
        // O modo foco é só da versão completa
        if (Edition.isLite) return
        val prefs = Prefs(context)
        if (!prefs.focusActive) {
            prefs.focusSince = System.currentTimeMillis()
            prefs.focusDigest = emptyMap()
            FocusListenerService.instance?.resetCounts()
        }
        prefs.focusActive = true
        prefs.focusSource = source
        refreshLaunchable(context)
        applyAll(context, on = true)
        FocusListenerService.instance?.sweep() ?: ensureListener(context)
        notifyListeners()
    }

    fun deactivate(context: Context) {
        val prefs = Prefs(context)
        if (!prefs.focusActive) return
        prefs.focusActive = false
        applyAll(context, on = false)
        notifyListeners()
    }

    /** Reaplica as camadas depois de uma mudança nos ajustes do foco. */
    fun refresh(context: Context) {
        if (isActive(context)) applyAll(context, on = true)
        notifyListeners()
    }

    private fun applyAll(context: Context, on: Boolean) {
        val prefs = Prefs(context)
        applyDnd(context, on && prefs.focusDnd)
        applyGrayscale(context, on && prefs.focusGrayscale)
        MyAccessibilityService.instance?.configure()
    }

    private fun notifyListeners() = listeners.toList().forEach { it() }

    // Quais apps passam

    @Volatile
    private var launchable: Set<String> = emptySet()

    /** Pacotes com ícone na gaveta: só esses podem ser filtrados ou bloqueados. */
    fun refreshLaunchable(context: Context) {
        launchable = try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            userManager.userProfiles.flatMap { user ->
                launcherApps.getActivityList(null, user).map { it.applicationInfo.packageName }
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun allowedPackages(context: Context): Set<String> {
        val prefs = Prefs(context)
        val home = HomeStore.load(context).active.allItems.map { it.pkg }
        return buildSet {
            addAll(home)
            addAll(prefs.focusAllowed)
            addAll(alwaysAllowed(context))
        }
    }

    /** Nunca bloqueados: o próprio Pauca, o telefone, os ajustes e a câmera de emergência. */
    private fun alwaysAllowed(context: Context): Set<String> = buildSet {
        add(context.packageName)
        add("com.android.settings")
        try {
            (context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage?.let { add(it) }
        } catch (_: Exception) {
        }
    }

    /** Este pacote deve ser segurado/bloqueado agora? */
    fun isRestricted(context: Context, pkg: String): Boolean {
        if (!isActive(context)) return false
        if (launchable.isEmpty()) refreshLaunchable(context)
        if (pkg !in launchable) return false
        return pkg !in allowedPackages(context)
    }

    // Permissões

    fun hasDndAccess(context: Context): Boolean =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted

    fun hasListenerAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /**
     * Se há acesso às notificações mas o ouvinte não está ligado (o Android pode desligá-lo para
     * poupar bateria, ou depois de uma atualização), pede para religar. Ao voltar, ele passa
     * pelas notificações da barra ([FocusListenerService.sweep]).
     */
    fun ensureListener(context: Context) {
        if (FocusListenerService.instance != null || !hasListenerAccess(context)) return
        try {
            NotificationListenerService.requestRebind(ComponentName(context, FocusListenerService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    // Não Perturbe

    private fun applyDnd(context: Context, on: Boolean) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                applyZenRule(context, nm, on)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        applyInterruptionFilter(context, nm, on)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun applyZenRule(context: Context, nm: NotificationManager, on: Boolean) {
        val prefs = Prefs(context)
        val rule = AutomaticZenRule(
            Language.wrap(context).getString(R.string.focus_rule_name),
            null,
            ComponentName(context, MainActivity::class.java),
            ZEN_CONDITION,
            zenPolicy(prefs),
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            true,
        )
        var id = prefs.zenRuleId
        if (id.isEmpty() || nm.getAutomaticZenRule(id) == null) {
            if (!on) return
            id = nm.addAutomaticZenRule(rule)
            prefs.zenRuleId = id
        } else {
            nm.updateAutomaticZenRule(id, rule)
        }
        val state = if (on) Condition.STATE_TRUE else Condition.STATE_FALSE
        nm.setAutomaticZenRuleState(id, Condition(ZEN_CONDITION, Language.wrap(context).getString(R.string.focus_rule_name), state))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun zenPolicy(prefs: Prefs): ZenPolicy = ZenPolicy.Builder()
        .allowAlarms(true)
        .allowMedia(true)
        .allowSystem(false)
        .allowEvents(false)
        .allowReminders(false)
        .allowCalls(prefs.focusCalls)
        .allowRepeatCallers(prefs.focusRepeatCallers)
        .allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                allowConversations(ZenPolicy.CONVERSATION_SENDERS_NONE)
            if (prefs.focusHideSilenced) hideAllVisualEffects() else showAllVisualEffects()
        }
        .build()

    /** Android 7–9 (ou se a regra falhar): liga o Não Perturbe global e devolve como estava. */
    private fun applyInterruptionFilter(context: Context, nm: NotificationManager, on: Boolean) {
        val prefs = Prefs(context)
        try {
            if (on) {
                if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    prefs.previousInterruptionFilter = nm.currentInterruptionFilter
                val calls = when (prefs.focusCalls) {
                    Constants.Callers.ANYONE -> NotificationManager.Policy.PRIORITY_SENDERS_ANY
                    Constants.Callers.CONTACTS -> NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS
                    else -> NotificationManager.Policy.PRIORITY_SENDERS_STARRED
                }
                var categories = NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                        NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA
                if (prefs.focusCalls != Constants.Callers.NONE)
                    categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
                if (prefs.focusRepeatCallers)
                    categories = categories or NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS
                nm.notificationPolicy = NotificationManager.Policy(categories, calls, calls)
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                val previous = prefs.previousInterruptionFilter
                nm.setInterruptionFilter(
                    if (previous == 0) NotificationManager.INTERRUPTION_FILTER_ALL else previous
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Tons de cinza

    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER = "accessibility_display_daltonizer"
    private const val DALTONIZER_MONOCHROME = 0

    private fun applyGrayscale(context: Context, on: Boolean) {
        if (!canWriteSecureSettings(context)) return
        val prefs = Prefs(context)
        val resolver = context.contentResolver
        try {
            if (on) {
                if (prefs.previousDaltonizer.isEmpty()) {
                    val enabled = Settings.Secure.getInt(resolver, DALTONIZER_ENABLED, 0)
                    val mode = Settings.Secure.getInt(resolver, DALTONIZER, -1)
                    prefs.previousDaltonizer = "$enabled,$mode"
                }
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
                Settings.Secure.putInt(resolver, DALTONIZER, DALTONIZER_MONOCHROME)
            } else if (prefs.previousDaltonizer.isNotEmpty()) {
                val (enabled, mode) = prefs.previousDaltonizer.split(",").map { it.toIntOrNull() ?: -1 }
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, enabled.coerceAtLeast(0))
                if (mode >= 0) Settings.Secure.putInt(resolver, DALTONIZER, mode)
                prefs.previousDaltonizer = ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
