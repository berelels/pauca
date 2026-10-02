package app.pauca.helper

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import app.pauca.data.Prefs
import java.util.Locale

/**
 * Idioma do app, aplicado pelo próprio Pauca. A API de idioma por app do Android 13
 * derruba launchers em celulares Samsung ("Can't change activity type once set"),
 * então o idioma escolhido vai direto na configuração da activity.
 */
object Language {

    /** Idioma do celular, mesmo que o app esteja em outro. */
    fun systemLocale(context: Context): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            context.getSystemService(LocaleManager::class.java).systemLocales[0]
        else Resources.getSystem().configuration.locales[0]

    fun locale(context: Context): Locale {
        val tag = Prefs(context).language
        return if (tag.isEmpty()) systemLocale(context) else Locale.forLanguageTag(tag)
    }

    /** Um contexto com os textos no idioma do app (para serviços e o contexto da aplicação). */
    fun wrap(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale(context))
        return context.createConfigurationContext(config)
    }
}
