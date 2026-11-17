package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import com.puraguia.app.model.Tarjeta

/**
 * DAO de métodos de pago: guarda, lista y elimina tarjetas por usuario en SQLite.
 */
class MetodoPagoDao(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)

    fun insertar(
        usuarioId: Long,
        marca: String,
        ultimos4: String,
        vence: String,
        predeterminada: Boolean
    ): Long {
        val db = helper.writableDatabase
        var nuevoId = -1L
        db.beginTransaction()
        try {
            if (predeterminada) {
                val reset = ContentValues().apply { put(DatabaseHelper.COL_MP_PREDETERMINADA, 0) }
                db.update(
                    DatabaseHelper.TABLA_METODOS_PAGO,
                    reset,
                    "${DatabaseHelper.COL_MP_USUARIO_ID} = ?",
                    arrayOf(usuarioId.toString())
                )
            }
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_MP_USUARIO_ID, usuarioId)
                put(DatabaseHelper.COL_MP_MARCA, marca.trim().uppercase())
                put(DatabaseHelper.COL_MP_ULTIMOS4, ultimos4.trim())
                put(DatabaseHelper.COL_MP_VENCE, vence.trim())
                put(DatabaseHelper.COL_MP_PREDETERMINADA, if (predeterminada) 1 else 0)
            }
            nuevoId = db.insertOrThrow(DatabaseHelper.TABLA_METODOS_PAGO, null, cv)
            db.setTransactionSuccessful()
        } catch (e: SQLiteException) {
            nuevoId = -1L
        } finally {
            db.endTransaction()
        }
        return nuevoId
    }

    fun obtenerPorUsuario(usuarioId: Long): List<Tarjeta> {
        val lista = mutableListOf<Tarjeta>()
        helper.readableDatabase.query(
            DatabaseHelper.TABLA_METODOS_PAGO,
            null,
            "${DatabaseHelper.COL_MP_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString()),
            null, null,
            "${DatabaseHelper.COL_MP_PREDETERMINADA} DESC, ${DatabaseHelper.COL_MP_ID} DESC"
        ).use { c ->
            val idxId = c.getColumnIndexOrThrow(DatabaseHelper.COL_MP_ID)
            val idxMarca = c.getColumnIndexOrThrow(DatabaseHelper.COL_MP_MARCA)
            val idxUltimos = c.getColumnIndexOrThrow(DatabaseHelper.COL_MP_ULTIMOS4)
            val idxVence = c.getColumnIndexOrThrow(DatabaseHelper.COL_MP_VENCE)
            val idxPred = c.getColumnIndexOrThrow(DatabaseHelper.COL_MP_PREDETERMINADA)
            while (c.moveToNext()) {
                lista.add(
                    Tarjeta(
                        id = c.getLong(idxId),
                        usuarioId = usuarioId,
                        marca = c.getString(idxMarca),
                        ultimos4 = c.getString(idxUltimos),
                        vence = c.getString(idxVence),
                        predeterminada = c.getInt(idxPred) == 1
                    )
                )
            }
        }
        return lista
    }

    fun eliminar(id: Long): Boolean {
        return try {
            helper.writableDatabase.delete(
                DatabaseHelper.TABLA_METODOS_PAGO,
                "${DatabaseHelper.COL_MP_ID} = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: SQLiteException) {
            false
        }
    }
}
