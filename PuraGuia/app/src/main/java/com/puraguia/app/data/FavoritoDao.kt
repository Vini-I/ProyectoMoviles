package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import com.puraguia.app.model.Tour

/**
 * DAO de favoritos: guarda y consulta los tours marcados como favoritos por usuario.
 */
class FavoritoDao(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)
    private val tourDao = TourDAO(context)

    fun esFavorito(usuarioId: Long, tourId: Int): Boolean {
        if (usuarioId <= 0L) return false
        return helper.readableDatabase.query(
            DatabaseHelper.TABLA_FAVORITOS,
            arrayOf(DatabaseHelper.COL_FAV_ID),
            "${DatabaseHelper.COL_FAV_USUARIO_ID} = ? AND ${DatabaseHelper.COL_FAV_TOUR_ID} = ?",
            arrayOf(usuarioId.toString(), tourId.toString()),
            null, null, null
        ).use { it.moveToFirst() }
    }

    fun agregar(usuarioId: Long, tourId: Int): Boolean {
        val cv = ContentValues().apply {
            put(DatabaseHelper.COL_FAV_USUARIO_ID, usuarioId)
            put(DatabaseHelper.COL_FAV_TOUR_ID, tourId)
            put(DatabaseHelper.COL_FAV_FECHA, System.currentTimeMillis())
        }
        return try {
            helper.writableDatabase.insertOrThrow(DatabaseHelper.TABLA_FAVORITOS, null, cv) > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    fun eliminar(usuarioId: Long, tourId: Int): Boolean {
        return try {
            helper.writableDatabase.delete(
                DatabaseHelper.TABLA_FAVORITOS,
                "${DatabaseHelper.COL_FAV_USUARIO_ID} = ? AND ${DatabaseHelper.COL_FAV_TOUR_ID} = ?",
                arrayOf(usuarioId.toString(), tourId.toString())
            ) > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    /** Alterna el estado de favorito y devuelve true si quedó guardado, false si se quitó. */
    fun alternar(usuarioId: Long, tourId: Int): Boolean {
        return if (esFavorito(usuarioId, tourId)) {
            eliminar(usuarioId, tourId)
            false
        } else {
            agregar(usuarioId, tourId)
            true
        }
    }

    fun obtenerToursFavoritos(usuarioId: Long): List<Pair<Tour, Long>> {
        val resultado = mutableListOf<Pair<Tour, Long>>()
        helper.readableDatabase.query(
            DatabaseHelper.TABLA_FAVORITOS,
            arrayOf(DatabaseHelper.COL_FAV_TOUR_ID, DatabaseHelper.COL_FAV_FECHA),
            "${DatabaseHelper.COL_FAV_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString()),
            null, null,
            "${DatabaseHelper.COL_FAV_FECHA} DESC"
        ).use { c ->
            while (c.moveToNext()) {
                val tourId = c.getInt(0)
                val fecha = c.getLong(1)
                tourDao.obtenerPorId(tourId)?.let { tour ->
                    resultado.add(tour to fecha)
                }
            }
        }
        return resultado
    }
}
