package com.puraguia.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {

    /**
     * Copia un archivo elegido por el usuario (foto, certificado) al almacenamiento
     * interno de la app y devuelve la ruta; así el archivo no se pierde si el
     * original se mueve o se borra. Devuelve null si falla.
     */
    fun copiarAInterno(context: Context, uri: Uri, carpeta: String): String? {
        return try {
            val directorio = File(context.filesDir, carpeta)
            directorio.mkdirs()
            val nombreSeguro = (obtenerNombre(context, uri) ?: "archivo")
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val destino = File(directorio, "${System.currentTimeMillis()}_$nombreSeguro")
            val entrada = context.contentResolver.openInputStream(uri) ?: return null
            entrada.use { input ->
                destino.outputStream().use { salida -> input.copyTo(salida) }
            }
            destino.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Nombre legible de un archivo guardado con copiarAInterno (sin el prefijo de fecha). */
    fun nombreLegible(ruta: String): String = File(ruta).name.substringAfter('_')

    /**
     * Crea un archivo temporal en cacheDir para recibir capturas de la cámara
     * sin persistencia obligatoria en base de datos.
     */
    fun crearArchivoTemporal(
        context: Context,
        carpeta: String = "camara",
        prefijo: String = "tour_",
        extension: String = ".jpg"
    ): File {
        val directorio = File(context.cacheDir, carpeta)
        if (!directorio.exists()) directorio.mkdirs()
        return File.createTempFile(prefijo, extension, directorio)
    }

    /**
     * Devuelve una content:// URI segura mediante el FileProvider configurado en el Manifest.
     */
    fun obtenerUriFileProvider(context: Context, archivo: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, archivo)
    }

    private fun obtenerNombre(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (indice >= 0 && cursor.moveToFirst()) cursor.getString(indice) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
