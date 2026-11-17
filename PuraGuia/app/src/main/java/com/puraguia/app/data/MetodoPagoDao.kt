package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import com.puraguia.app.model.Tarjeta

/**
 * DAO de métodos de pago: guarda, consulta, actualiza y elimina tarjetas y cuentas SINPE Móvil por usuario en SQLite.
 */
class MetodoPagoDao(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)

    fun insertar(
        usuarioId: Long,
        marca: String,
        ultimos4: String,
        vence: String,
        predeterminada: Boolean,
        tipo: String = Tarjeta.TIPO_TARJETA,
        telefono: String = "",
        titular: String = ""
    ): Long {
        val db = helper.writableDatabase
        var nuevoId = -1L
        db.beginTransaction()
        try {
            if (predeterminada) {
                limpiarPredeterminada(db, usuarioId)
            }
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_MP_USUARIO_ID, usuarioId)
                put(DatabaseHelper.COL_MP_MARCA, marca.trim().uppercase())
                put(DatabaseHelper.COL_MP_ULTIMOS4, ultimos4.trim())
                put(DatabaseHelper.COL_MP_VENCE, vence.trim())
                put(DatabaseHelper.COL_MP_PREDETERMINADA, if (predeterminada) 1 else 0)
                put(DatabaseHelper.COL_MP_TIPO, tipo)
                put(DatabaseHelper.COL_MP_TELEFONO, telefono.trim())
                put(DatabaseHelper.COL_MP_TITULAR, titular.trim())
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

    fun insertarSinpe(
        usuarioId: Long,
        telefono: String,
        titular: String,
        predeterminada: Boolean
    ): Long {
        val telefonoFormateado = formatearTelefonoSinpe(telefono)
        val ultimos4 = telefonoFormateado.filter { it.isDigit() }.takeLast(4)
        return insertar(
            usuarioId = usuarioId,
            marca = "SINPE",
            ultimos4 = ultimos4,
            vence = "",
            predeterminada = predeterminada,
            tipo = Tarjeta.TIPO_SINPE,
            telefono = telefonoFormateado,
            titular = titular.trim()
        )
    }

    fun actualizarSinpe(
        id: Long,
        usuarioId: Long,
        telefono: String,
        titular: String,
        predeterminada: Boolean
    ): Boolean {
        val telefonoFormateado = formatearTelefonoSinpe(telefono)
        val ultimos4 = telefonoFormateado.filter { it.isDigit() }.takeLast(4)
        val db = helper.writableDatabase
        var actualizado = false
        db.beginTransaction()
        try {
            if (predeterminada) {
                limpiarPredeterminada(db, usuarioId)
            }
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_MP_ULTIMOS4, ultimos4)
                put(DatabaseHelper.COL_MP_TELEFONO, telefonoFormateado)
                put(DatabaseHelper.COL_MP_TITULAR, titular.trim())
                put(DatabaseHelper.COL_MP_PREDETERMINADA, if (predeterminada) 1 else 0)
            }
            actualizado = db.update(
                DatabaseHelper.TABLA_METODOS_PAGO,
                cv,
                "${DatabaseHelper.COL_MP_ID} = ? AND ${DatabaseHelper.COL_MP_USUARIO_ID} = ?",
                arrayOf(id.toString(), usuarioId.toString())
            ) > 0
            db.setTransactionSuccessful()
        } catch (e: SQLiteException) {
            actualizado = false
        } finally {
            db.endTransaction()
        }
        return actualizado
    }

    fun actualizarTarjeta(
        id: Long,
        usuarioId: Long,
        marca: String,
        ultimos4: String,
        vence: String,
        predeterminada: Boolean
    ): Boolean {
        val db = helper.writableDatabase
        var actualizado = false
        db.beginTransaction()
        try {
            if (predeterminada) {
                limpiarPredeterminada(db, usuarioId)
            }
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_MP_MARCA, marca.trim().uppercase())
                put(DatabaseHelper.COL_MP_ULTIMOS4, ultimos4.trim())
                put(DatabaseHelper.COL_MP_VENCE, vence.trim())
                put(DatabaseHelper.COL_MP_PREDETERMINADA, if (predeterminada) 1 else 0)
            }
            actualizado = db.update(
                DatabaseHelper.TABLA_METODOS_PAGO,
                cv,
                "${DatabaseHelper.COL_MP_ID} = ? AND ${DatabaseHelper.COL_MP_USUARIO_ID} = ?",
                arrayOf(id.toString(), usuarioId.toString())
            ) > 0
            db.setTransactionSuccessful()
        } catch (e: SQLiteException) {
            actualizado = false
        } finally {
            db.endTransaction()
        }
        return actualizado
    }

    fun establecerPredeterminado(id: Long, usuarioId: Long): Boolean {
        val db = helper.writableDatabase
        var ok = false
        db.beginTransaction()
        try {
            limpiarPredeterminada(db, usuarioId)
            val cv = ContentValues().apply { put(DatabaseHelper.COL_MP_PREDETERMINADA, 1) }
            ok = db.update(
                DatabaseHelper.TABLA_METODOS_PAGO,
                cv,
                "${DatabaseHelper.COL_MP_ID} = ? AND ${DatabaseHelper.COL_MP_USUARIO_ID} = ?",
                arrayOf(id.toString(), usuarioId.toString())
            ) > 0
            db.setTransactionSuccessful()
        } catch (e: SQLiteException) {
            ok = false
        } finally {
            db.endTransaction()
        }
        return ok
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
            val idxTipo = c.getColumnIndexOrThrow(DatabaseHelper.COL_MP_TIPO)
            val idxTelefono = c.getColumnIndexOrThrow(DatabaseHelper.COL_MP_TELEFONO)
            val idxTitular = c.getColumnIndexOrThrow(DatabaseHelper.COL_MP_TITULAR)
            while (c.moveToNext()) {
                lista.add(
                    Tarjeta(
                        id = c.getLong(idxId),
                        usuarioId = usuarioId,
                        marca = c.getString(idxMarca),
                        ultimos4 = c.getString(idxUltimos),
                        vence = c.getString(idxVence),
                        predeterminada = c.getInt(idxPred) == 1,
                        tipo = c.getString(idxTipo) ?: Tarjeta.TIPO_TARJETA,
                        telefono = c.getString(idxTelefono) ?: "",
                        titular = c.getString(idxTitular) ?: ""
                    )
                )
            }
        }
        return lista
    }

    fun obtenerSinpePorUsuario(usuarioId: Long): List<Tarjeta> =
        obtenerPorUsuario(usuarioId).filter { it.esSinpe }

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

    private fun limpiarPredeterminada(db: android.database.sqlite.SQLiteDatabase, usuarioId: Long) {
        val reset = ContentValues().apply { put(DatabaseHelper.COL_MP_PREDETERMINADA, 0) }
        db.update(
            DatabaseHelper.TABLA_METODOS_PAGO,
            reset,
            "${DatabaseHelper.COL_MP_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString())
        )
    }

    companion object {
        fun formatearTelefonoSinpe(telefono: String): String {
            val digitos = telefono.filter { it.isDigit() }
            return if (digitos.length == 8) {
                "${digitos.substring(0, 4)}-${digitos.substring(4)}"
            } else {
                telefono.trim()
            }
        }
    }
}
