package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import com.puraguia.app.model.Resena
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DAO de reseñas: permite publicar y consultar reseñas de tours en SQLite.
 */
class ResenaDao(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)

    fun insertar(
        tourId: Int,
        usuarioId: Long,
        autorNombre: String,
        estrellas: Int,
        comentario: String
    ): Long {
        val ahora = System.currentTimeMillis()
        val fechaTexto = SimpleDateFormat("d MMM yyyy", Locale("es", "CR")).format(Date(ahora))
        val db = helper.writableDatabase
        var nuevoId = -1L
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_RESENA_TOUR_ID, tourId)
                put(DatabaseHelper.COL_RESENA_USUARIO_ID, usuarioId)
                put(DatabaseHelper.COL_RESENA_AUTOR, autorNombre.trim())
                put(DatabaseHelper.COL_RESENA_ESTRELLAS, estrellas.coerceIn(1, 5))
                put(DatabaseHelper.COL_RESENA_COMENTARIO, comentario.trim())
                put(DatabaseHelper.COL_RESENA_FECHA_TEXTO, fechaTexto)
                put(DatabaseHelper.COL_RESENA_FECHA_CREACION, ahora)
            }
            nuevoId = db.insertOrThrow(DatabaseHelper.TABLA_RESENAS, null, cv)

            db.rawQuery(
                "SELECT AVG(${DatabaseHelper.COL_RESENA_ESTRELLAS}), COUNT(*) " +
                    "FROM ${DatabaseHelper.TABLA_RESENAS} WHERE ${DatabaseHelper.COL_RESENA_TOUR_ID} = ?",
                arrayOf(tourId.toString())
            ).use { c ->
                if (c.moveToFirst()) {
                    val promedio = Math.round(c.getDouble(0) * 10.0) / 10.0
                    val cantidad = c.getInt(1)
                    val cvTour = ContentValues().apply {
                        put(DatabaseHelper.COL_TOUR_RATING, if (promedio > 0) promedio else 5.0)
                        put(DatabaseHelper.COL_TOUR_RESENAS_COUNT, cantidad)
                    }
                    db.update(
                        DatabaseHelper.TABLA_TOURS,
                        cvTour,
                        "${DatabaseHelper.COL_TOUR_ID} = ?",
                        arrayOf(tourId.toString())
                    )
                }
            }
            db.setTransactionSuccessful()
        } catch (e: SQLiteException) {
            nuevoId = -1L
        } finally {
            db.endTransaction()
        }
        return nuevoId
    }

    fun obtenerPorTour(tourId: Int): List<Resena> {
        val lista = mutableListOf<Resena>()
        helper.readableDatabase.query(
            DatabaseHelper.TABLA_RESENAS,
            null,
            "${DatabaseHelper.COL_RESENA_TOUR_ID} = ?",
            arrayOf(tourId.toString()),
            null, null,
            "${DatabaseHelper.COL_RESENA_FECHA_CREACION} DESC"
        ).use { c ->
            val idxId = c.getColumnIndexOrThrow(DatabaseHelper.COL_RESENA_ID)
            val idxUid = c.getColumnIndexOrThrow(DatabaseHelper.COL_RESENA_USUARIO_ID)
            val idxAutor = c.getColumnIndexOrThrow(DatabaseHelper.COL_RESENA_AUTOR)
            val idxEstrellas = c.getColumnIndexOrThrow(DatabaseHelper.COL_RESENA_ESTRELLAS)
            val idxComentario = c.getColumnIndexOrThrow(DatabaseHelper.COL_RESENA_COMENTARIO)
            val idxFecha = c.getColumnIndexOrThrow(DatabaseHelper.COL_RESENA_FECHA_TEXTO)

            while (c.moveToNext()) {
                lista.add(
                    Resena(
                        id = c.getLong(idxId),
                        tourId = tourId,
                        usuarioId = c.getLong(idxUid),
                        autor = c.getString(idxAutor),
                        fecha = c.getString(idxFecha),
                        estrellas = c.getInt(idxEstrellas),
                        comentario = c.getString(idxComentario)
                    )
                )
            }
        }
        return lista
    }

    fun contarPorUsuario(usuarioId: Long): Int {
        return helper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLA_RESENAS} WHERE ${DatabaseHelper.COL_RESENA_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }
}
