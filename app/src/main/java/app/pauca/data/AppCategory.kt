package app.pauca.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.annotation.StringRes
import app.pauca.R

/**
 * Categorias da gaveta. O Android só sabe a categoria de alguns apps (o desenvolvedor
 * declara, ou não), então completamos com apps conhecidos pelo pacote; e a pessoa pode
 * escolher à mão, o que vale acima de tudo.
 */
object AppCategory {
    const val SOCIAL = "social"
    const val PRODUCTIVITY = "productivity"
    const val FINANCE = "finance"
    const val MEDIA = "media"
    const val PHOTOS = "photos"
    const val GAMES = "games"
    const val NEWS = "news"
    const val MAPS = "maps"
    const val OTHER = "other"

    /** Ordem dos botões na gaveta. */
    val ORDER = listOf(SOCIAL, PRODUCTIVITY, FINANCE, MEDIA, PHOTOS, GAMES, NEWS, MAPS, OTHER)

    @StringRes
    fun label(id: String): Int = when (id) {
        SOCIAL -> R.string.category_social
        PRODUCTIVITY -> R.string.category_productivity
        FINANCE -> R.string.category_finance
        MEDIA -> R.string.category_media
        PHOTOS -> R.string.category_photos
        GAMES -> R.string.category_games
        NEWS -> R.string.category_news
        MAPS -> R.string.category_maps
        else -> R.string.category_other
    }

    // Trechos de nome de pacote de apps conhecidos que não declaram categoria
    private val HINTS = mapOf(
        FINANCE to listOf(
            "com.nu.production", "com.itau", "com.bradesco", "com.santander", "br.com.gabba.caixa",
            "br.com.bb.android", "br.com.intermedium", "com.picpay", "com.mercadopago", "com.c6bank",
            "btgpactual", "com.xp.", "br.com.xp", "com.binance", "com.paypal", "walletnfcrel", "banking",
        ),
        SOCIAL to listOf(
            "com.whatsapp", "org.telegram", "com.instagram", "com.facebook", "com.twitter", "com.zhiliaoapp",
            "com.discord", "com.reddit", "com.linkedin", "com.snapchat", "org.thoughtcrime.securesms",
        ),
        MEDIA to listOf(
            "com.spotify", "com.google.android.youtube", "com.netflix", "deezer", "com.amazon.avod",
            "com.disney", "com.google.android.apps.youtube.music", "tv.twitch", "com.globo",
        ),
        MAPS to listOf("com.waze", "com.ubercab", "com.taxis99", "com.google.android.apps.maps", "airbnb", "booking"),
        PRODUCTIVITY to listOf(
            "com.google.android.gm", "com.google.android.calendar", "com.google.android.apps.docs",
            "notion", "com.microsoft.office", "com.microsoft.teams", "com.slack", "com.todoist", "com.samsung.android.calendar",
        ),
        PHOTOS to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.android.camera", "com.sec.android.app.camera"),
    )

    fun of(context: Context, pkg: String, overrides: Map<String, String>): String {
        overrides[pkg]?.takeIf { it.isNotEmpty() }?.let { return it }
        HINTS.entries.firstOrNull { (_, hints) -> hints.any { pkg.contains(it, ignoreCase = true) } }?.let { return it.key }
        val info = try {
            context.packageManager.getApplicationInfo(pkg, 0)
        } catch (_: Exception) {
            return OTHER
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (info.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> return SOCIAL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return PRODUCTIVITY
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> return MEDIA
                ApplicationInfo.CATEGORY_IMAGE -> return PHOTOS
                ApplicationInfo.CATEGORY_GAME -> return GAMES
                ApplicationInfo.CATEGORY_NEWS -> return NEWS
                ApplicationInfo.CATEGORY_MAPS -> return MAPS
            }
        }
        @Suppress("DEPRECATION")
        if (info.flags and ApplicationInfo.FLAG_IS_GAME != 0) return GAMES
        return OTHER
    }
}
