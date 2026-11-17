package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import com.puraguia.app.model.ReservaResumen

/**
 * DAO de reservas: guarda y consulta las reservas de tours en SQLite.
 */
class ReservaDao(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)

    fun insertar(
        usuarioId: Long,
        tourId: Int,
        fechaTour: String,
        horaTour: String,
        metodoPago: String,
        total: Int
    ): Long {
        val cv = ContentValues().apply {
            put(DatabaseHelper.COL_RES_USUARIO_ID, usuarioId)
            put(DatabaseHelper.COL_RES_TOUR_ID, tourId)
            put(DatabaseHelper.COL_RES_FECHA_TOUR, fechaTour)
            put(DatabaseHelper.COL_RES_HORA_TOUR, horaTour)
            put(DatabaseHelper.COL_RES_METODO_PAGO, metodoPago)
            put(DatabaseHelper.COL_RES_TOTAL, total)
            put(DatabaseHelper.COL_RES_ESTADO, "Confirmado")
            put(DatabaseHelper.COL_RES_FECHA_CREACION, System.currentTimeMillis())
        }
        return try {
            helper.writableDatabase.insertOrThrow(DatabaseHelper.TABLA_RESERVAS, null, cv)
        } catch (e: SQLiteException) {
            -1L
        }
    }

    fun obtenerPorUsuario(usuarioId: Long): List<ReservaResumen> {
        val sql = """
            SELECT
                r.${DatabaseHelper.COL_RES_ID},
                r.${DatabaseHelper.COL_RES_TOUR_ID},
                t.${DatabaseHelper.COL_TOUR_TITULO},
                r.${DatabaseHelper.COL_RES_FECHA_TOUR},
                r.${DatabaseHelper.COL_RES_HORA_TOUR},
                r.${DatabaseHelper.COL_RES_METODO_PAGO},
                r.${DatabaseHelper.COL_RES_ESTADO},
                r.${DatabaseHelper.COL_RES_TOTAL}
            FROM ${DatabaseHelper.TABLA_RESERVAS} r
            INNER JOIN ${DatabaseHelper.TABLA_TOURS} t ON r.${DatabaseHelper.COL_RES_TOUR_ID} = t.${DatabaseHelper.COL_TOUR_ID}
            WHERE r.${DatabaseHelper.COL_RES_USUARIO_ID} = ?
            ORDER BY r.${DatabaseHelper.COL_RES_FECHA_CREACION} DESC
        """.trimIndent()

        val lista = mutableListOf<ReservaResumen>()
        helper.readableDatabase.rawQuery(sql, arrayOf(usuarioId.toString())).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    ReservaResumen(
                        id = c.getLong(0),
                        tourId = c.getInt(1),
                        tour = c.getString(2),
                        fecha = c.getString(3),
                        hora = c.getString(4),
                        metodoPago = c.getString(5),
                        estado = c.getString(6),
                        total = c.getInt(7)
                    )
                )
            }
        }
        return lista
    }

    fun contarPorUsuario(usuarioId: Long): Int {
        return helper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLA_RESERVAS} WHERE ${DatabaseHelper.COL_RES_USUARIO_ID} = ?",
            arrayOf(usuarioId.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }
}
