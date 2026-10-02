package app.pauca.helper

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Só no Lite: entrega as preferências para o Pauca importar (veja [LiteImport]). A leitura
 * exige uma permissão de assinatura, então só o Pauca, assinado com a mesma chave, consegue.
 */
class LiteExportProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val context = context!!
        return MatrixCursor(arrayOf("file", "json")).apply {
            LiteImport.FILES.forEach { file ->
                addRow(arrayOf(file, LiteImport.encode(context.getSharedPreferences(file, Context.MODE_PRIVATE))))
            }
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
