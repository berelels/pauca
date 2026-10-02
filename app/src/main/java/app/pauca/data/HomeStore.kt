package app.pauca.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.MediaStore
import android.provider.Telephony
import androidx.core.content.edit
import app.pauca.R
import app.pauca.helper.Edition
import app.pauca.helper.Language

/**
 * Guarda perfis, cartões e apps da tela inicial como JSON nas SharedPreferences.
 * Mantém uma cópia em memória: o launcher roda num processo só.
 */
object HomeStore {
    private const val FILE = "pauca_home"
    private const val KEY = "HOME_DATA"

    private var cache: HomeData? = null

    fun load(context: Context): HomeData {
        cache?.let { return it }
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY, null)?.let { HomeData.fromJson(it) }
            ?: createDefault(context).also { save(context, it) }
        cache = data
        return data
    }

    fun save(context: Context, data: HomeData) {
        cache = data
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit { putString(KEY, data.toJson()) }
    }

    /** Altera e salva numa tacada só. */
    inline fun update(context: Context, block: (HomeData) -> Unit): HomeData {
        val data = load(context)
        block(data)
        save(context, data)
        return data
    }

    /**
     * Primeiro uso: monta "Pessoal" com os apps padrão do aparelho (telefone, mensagens,
     * câmera...) e um perfil "Foco" mínimo que liga o modo foco.
     */
    /** Textos no idioma do app, mesmo que venha o contexto da aplicação. */
    private fun localized(context: Context): Context = Language.wrap(context)

    private fun createDefault(context: Context): HomeData {
        val essentials = listOfNotNull(
            defaultApp(context, Intent(Intent.ACTION_DIAL)),
            smsApp(context),
            defaultApp(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
            defaultApp(context, Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://media/external/images/media"), "image/*")),
            defaultApp(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))),
        ).distinctBy { it.pkg }

        val tools = listOfNotNull(
            Constants.CLOCK_APP_PACKAGES.firstNotNullOfOrNull { launchItem(context, it) },
            defaultApp(context, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)),
            defaultApp(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))),
        ).filter { tool -> essentials.none { it.pkg == tool.pkg } }.distinctBy { it.pkg }

        val personal = Profile(
            name = localized(context).getString(R.string.profile_default_personal),
            groups = mutableListOf(
                HomeGroup(items = essentials.toMutableList()),
                HomeGroup(items = tools.toMutableList()),
            ).filter { it.items.isNotEmpty() }.toMutableList().ifEmpty { mutableListOf(HomeGroup()) },
        )
        val focus = Profile(
            name = localized(context).getString(R.string.profile_default_focus),
            groups = mutableListOf(HomeGroup(items = essentials.take(3).map { it.copy(id = newId()) }.toMutableList())),
            focusOnSwitch = true,
        )
        // O Lite não tem modo foco, então começa só com o perfil pessoal
        val profiles = if (Edition.isLite) mutableListOf(personal) else mutableListOf(personal, focus)
        return HomeData(profiles, personal.id)
    }

    private fun smsApp(context: Context): HomeItem? =
        try {
            Telephony.Sms.getDefaultSmsPackage(context)?.let { launchItem(context, it) }
        } catch (_: Exception) {
            null
        } ?: defaultApp(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))

    /** O app que o sistema abriria para [intent] — ignora a tela de "abrir com". */
    private fun defaultApp(context: Context, intent: Intent): HomeItem? = try {
        val pkg = context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        if (pkg == null || pkg == "android" || pkg.contains("resolver")) null
        else launchItem(context, pkg)
    } catch (_: Exception) {
        null
    }

    fun launchItem(context: Context, pkg: String): HomeItem? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val user = Process.myUserHandle()
        val activity = launcherApps.getActivityList(pkg, user).firstOrNull() ?: return null
        return HomeItem(
            label = activity.label.toString(),
            pkg = pkg,
            activity = activity.componentName.className,
            user = user.toString(),
        )
    }
}
