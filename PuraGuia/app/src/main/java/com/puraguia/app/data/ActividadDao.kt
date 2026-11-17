package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import com.puraguia.app.model.Actividad
import com.puraguia.app.model.TipoActividad
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DAO de actividad reciente: registra y consulta acciones del usuario en SQLite.
 */
class ActividadDao(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)

    fun registrar(usuarioId: Long, tipo: TipoActividad, titulo: String, detalle: String): Long {
        if (usuarioId <= 0L) return -1L
        val ahora = System.currentTimeMillis()
        val fechaTexto = SimpleDateFormat("d MMM yyyy, h:mm a", Locale("es", "CR")).format(Date(ahora))
        val cv = ContentValues().apply {
            put(DatabaseHelper.COL_ACT_USUARIO_ID, usuarioId)
            put(DatabaseHelper.COL_ACT_TIPO, tipo.name)
            put(DatabaseHelper.COL_ACT_TITULO, titulo)
            put(DatabaseHelper.COL_ACT_DETALLE, detalle)
            put(DatabaseHelper.COL_ACT_FECHA_TEXTO, fechaTexto)
            put(DatabaseHelper.COL_ACT_FECHA_CREACION, ahora)
        }
        return try {
            helper.writableDatabase.insertOrThrow(DatabaseHelper.TABLA_ACTIVIDAD_RECIENTE, null, cv)
        } catch (e: SQLiteException) {
            -1L
        }
    }

    fun obtenerPorUsuario(usuarioId: Long, filtro: TipoActividad? = null): List<Actividad> {
        val where = if (filtro == null) {
            "${DatabaseHelper.COL_ACT_USUARIO_ID} = ?"
        } else {
            "${DatabaseHelper.COL_ACT_USUARIO_ID} = ? AND ${DatabaseHelper.COL_ACT_TIPO} = ?"
        }
        val args = if (filtro == null) {
            arrayOf(usuarioId.toString())
        } else {
            arrayOf(usuarioId.toString(), filtro.name)
        }
        val lista = mutableListOf<Actividad>()
        helper.readableDatabase.query(
            DatabaseHelper.TABLA_ACTIVIDAD_RECIENTE,
            null,
            where,
            args,
            null, null,
            "${DatabaseHelper.COL_ACT_FECHA_CREACION} DESC"
        ).use { c ->
            val idxId = c.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_ID)
            val idxTipo = c.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_TIPO)
            val idxTitulo = c.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_TITULO)
            val idxDetalle = c.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_DETALLE)
            val idxFecha = c.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_FECHA_TEXTO)

            while (c.moveToNext()) {
                val tipo = runCatching { TipoActividad.valueOf(c.getString(idxTipo)) }
                    .getOrDefault(TipoActividad.SESION)
                lista.add(
                    Actividad(
                        id = c.getLong(idxId),
                        tipo = tipo,
                        titulo = c.getString(idxTitulo),
                        detalle = c.getString(idxDetalle),
                        fecha = c.getString(idxFecha)
                    )
                )
            }
        }
        return lista
    }
}
