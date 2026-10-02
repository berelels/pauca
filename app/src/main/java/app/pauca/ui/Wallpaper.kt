package app.pauca.ui

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import androidx.core.view.isVisible
import app.pauca.data.Constants
import app.pauca.data.Palette
import app.pauca.data.Prefs
import app.pauca.helper.dpToPx
import java.io.File
import kotlin.math.max

/**
 * Fundo da tela, desenhado pela própria activity: cor sólida, papel de parede do sistema
 * (atrás da janela, que é sempre translúcida) ou uma imagem escolhida, com desfoque e brilho.
 *
 * A janela nunca muda de formato: trocar entre opaca e translúcida com ela aberta faz o
 * Android parar de limpar a tela entre os quadros, e as telas anteriores ficam "fantasmas".
 */
object Wallpaper {

    /** O que desenhar; os ajustes vêm das preferências ou da prévia do editor. */
    data class Look(
        val source: String,
        val blur: Boolean,
        val blurRadius: Int,
        val brightness: Int,
    ) {
        companion object {
            fun from(prefs: Prefs) = Look(
                prefs.wallpaperSource, prefs.wallpaperBlur, prefs.wallpaperBlurRadius, prefs.wallpaperBrightness,
            )
        }
    }

    private const val FILE_NAME = "wallpaper.jpg"
    private var cached: Bitmap? = null
    private var cachedStamp = 0L

    fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun hasImage(context: Context) = file(context).exists()

    /** O celular deixa desfocar o que está atrás da janela (o papel de parede do sistema)? */
    fun canBlurSystem(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                context.getSystemService(WindowManager::class.java).isCrossWindowBlurEnabled

    fun apply(
        window: Window,
        root: View,
        image: ImageView,
        shade: View,
        palette: Palette,
        look: Look,
    ) {
        if (!palette.showsWallpaper) {
            root.setBackgroundColor(palette.bg)
            image.isVisible = false
            image.setImageDrawable(null)
            shade.isVisible = false
            setWindowBlur(window, 0)
            return
        }
        root.background = null
        val useImage = look.source == Constants.WallpaperSource.IMAGE && hasImage(root.context)
        val radius = if (look.blur) (look.blurRadius * 2).dpToPx() else 0
        if (useImage) {
            image.isVisible = true
            setWindowBlur(window, 0)
            val bitmap = load(root.context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                image.setImageBitmap(bitmap)
                image.setRenderEffect(
                    if (radius > 0) RenderEffect.createBlurEffect(radius.toFloat(), radius.toFloat(), Shader.TileMode.CLAMP)
                    else null
                )
            } else {
                image.setImageBitmap(if (radius > 0 && bitmap != null) cheapBlur(bitmap, look.blurRadius) else bitmap)
            }
        } else {
            image.isVisible = false
            image.setImageDrawable(null)
            setWindowBlur(window, if (canBlurSystem(root.context)) radius else 0)
        }
        shade.isVisible = look.brightness != 100
        shade.setBackgroundColor(shadeColor(look.brightness))
    }

    private fun setWindowBlur(window: Window, radius: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window.setBackgroundBlurRadius(radius)
    }

    private fun load(context: Context): Bitmap? {
        val file = file(context)
        if (!file.exists()) return null
        if (cached == null || cachedStamp != file.lastModified()) {
            cached = BitmapFactory.decodeFile(file.path)
            cachedStamp = file.lastModified()
        }
        return cached
    }

    /**
     * A imagem com desfoque e brilho já aplicados, para a tela de bloqueio (lá o Pauca
     * não desenha nada: o Android mostra um bitmap pronto). Roda fora da thread principal.
     */
    fun bake(context: Context, look: Look): Bitmap? {
        val source = BitmapFactory.decodeFile(file(context).path) ?: return null
        var out = source
        if (look.blur) {
            // Desfoca numa cópia 4x menor (bem mais rápido) e amplia de volta, já suave
            val factor = 4
            val small = source.scale(max(1, source.width / factor), max(1, source.height / factor))
                .copy(Bitmap.Config.ARGB_8888, true)
            val radius = max(1, (look.blurRadius * 2).dpToPx() / factor)
            repeat(3) { boxBlur(small, radius) }
            out = small.scale(source.width, source.height)
        }
        if (look.brightness != 100) {
            out = out.copy(Bitmap.Config.ARGB_8888, true)
            Canvas(out).drawColor(shadeColor(look.brightness))
        }
        return out
    }

    /** Põe a imagem na tela de bloqueio do Android. */
    fun setLockScreen(context: Context, look: Look): Boolean = try {
        val bitmap = bake(context, look) ?: throw IllegalStateException("no image")
        WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    /** Média móvel numa direção e depois na outra; três passadas ficam perto de um desfoque gaussiano. */
    private fun boxBlur(bitmap: Bitmap, radius: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val buffer = IntArray(w * h)
        blurPass(pixels, buffer, w, h, radius, horizontal = true)
        blurPass(buffer, pixels, w, h, radius, horizontal = false)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun blurPass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val length = if (horizontal) w else h
        val window = 2 * r + 1
        for (line in 0 until lines) {
            fun at(i: Int): Int {
                val c = i.coerceIn(0, length - 1)
                return if (horizontal) src[line * w + c] else src[c * w + line]
            }
            var a = 0; var red = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val p = at(i)
                a += p ushr 24; red += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            for (i in 0 until length) {
                val index = if (horizontal) line * w + i else i * w + line
                dst[index] = ((a / window) shl 24) or ((red / window) shl 16) or ((g / window) shl 8) or (b / window)
                val outP = at(i - r)
                val inP = at(i + r + 1)
                a += (inP ushr 24) - (outP ushr 24)
                red += ((inP shr 16) and 0xFF) - ((outP shr 16) and 0xFF)
                g += ((inP shr 8) and 0xFF) - ((outP shr 8) and 0xFF)
                b += (inP and 0xFF) - (outP and 0xFF)
            }
        }
    }

    /** Abaixo de 100% escurece com preto; acima clareia com branco. */
    private fun shadeColor(brightness: Int): Int =
        if (brightness < 100) ColorUtils.setAlphaComponent(0xFF000000.toInt(), (100 - brightness) * 255 / 100)
        else ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), (brightness - 100) * 255 * 6 / 1000)

    /** Android 11 ou anterior: reduz e amplia de novo, o que borra sem custo. */
    private fun cheapBlur(bitmap: Bitmap, intensity: Int): Bitmap {
        val factor = 1 + intensity / 2
        val small = bitmap.scale(max(1, bitmap.width / factor / 4), max(1, bitmap.height / factor / 4))
        return small.scale(bitmap.width, bitmap.height)
    }

    /**
     * Copia a imagem escolhida para o app, já do tamanho da tela (a original pode ser enorme)
     * e girada certa. Roda fora da thread principal.
     */
    fun import(context: Context, uri: Uri): Boolean = try {
        val metrics = context.resources.displayMetrics
        val target = max(metrics.widthPixels, metrics.heightPixels)
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > target) {
                    val ratio = target.toFloat() / longest
                    decoder.setTargetSize((info.size.width * ratio).toInt(), (info.size.height * ratio).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        } ?: throw IllegalStateException("decode")
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        tmp.renameTo(file(context))
        cached = null
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
