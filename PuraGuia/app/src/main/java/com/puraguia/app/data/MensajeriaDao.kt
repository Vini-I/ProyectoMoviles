package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException
import com.puraguia.app.model.Chat
import com.puraguia.app.model.Mensaje
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * DAO de mensajería: gestiona conversaciones y mensajes persistidos en SQLite.
 */
class MensajeriaDao(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)

    fun obtenerOCrearConversacion(usuarioA: Long, usuarioB: Long): Long {
        val u1 = minOf(usuarioA, usuarioB)
        val u2 = maxOf(usuarioA, usuarioB)
        val db = helper.writableDatabase
        db.query(
            DatabaseHelper.TABLA_CONVERSACIONES,
            arrayOf(DatabaseHelper.COL_CONV_ID),
            "${DatabaseHelper.COL_CONV_U1} = ? AND ${DatabaseHelper.COL_CONV_U2} = ?",
            arrayOf(u1.toString(), u2.toString()),
            null, null, null
        ).use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        val cv = ContentValues().apply {
            put(DatabaseHelper.COL_CONV_U1, u1)
            put(DatabaseHelper.COL_CONV_U2, u2)
            put(DatabaseHelper.COL_CONV_ULTIMO_MENSAJE, "")
            put(DatabaseHelper.COL_CONV_ULTIMA_FECHA, System.currentTimeMillis())
            put(DatabaseHelper.COL_CONV_NO_LEIDOS_U1, 0)
            put(DatabaseHelper.COL_CONV_NO_LEIDOS_U2, 0)
        }
        return try {
            db.insertOrThrow(DatabaseHelper.TABLA_CONVERSACIONES, null, cv)
        } catch (e: SQLiteException) {
            -1L
        }
    }

    fun obtenerConversaciones(usuarioId: Long, filtro: String = ""): List<Chat> {
        val sql = """
            SELECT
                c.${DatabaseHelper.COL_CONV_ID},
                c.${DatabaseHelper.COL_CONV_U1},
                c.${DatabaseHelper.COL_CONV_U2},
                c.${DatabaseHelper.COL_CONV_ULTIMO_MENSAJE},
                c.${DatabaseHelper.COL_CONV_ULTIMA_FECHA},
                c.${DatabaseHelper.COL_CONV_NO_LEIDOS_U1},
                c.${DatabaseHelper.COL_CONV_NO_LEIDOS_U2},
                CASE WHEN c.${DatabaseHelper.COL_CONV_U1} = ? THEN u2.${DatabaseHelper.COL_ID} ELSE u1.${DatabaseHelper.COL_ID} END AS otro_id,
                CASE WHEN c.${DatabaseHelper.COL_CONV_U1} = ? THEN u2.${DatabaseHelper.COL_NOMBRE} ELSE u1.${DatabaseHelper.COL_NOMBRE} END AS otro_nombre
            FROM ${DatabaseHelper.TABLA_CONVERSACIONES} c
            INNER JOIN ${DatabaseHelper.TABLA_USUARIOS} u1 ON c.${DatabaseHelper.COL_CONV_U1} = u1.${DatabaseHelper.COL_ID}
            INNER JOIN ${DatabaseHelper.TABLA_USUARIOS} u2 ON c.${DatabaseHelper.COL_CONV_U2} = u2.${DatabaseHelper.COL_ID}
            WHERE (c.${DatabaseHelper.COL_CONV_U1} = ? OR c.${DatabaseHelper.COL_CONV_U2} = ?)
              AND c.${DatabaseHelper.COL_CONV_ULTIMO_MENSAJE} != ''
            ORDER BY c.${DatabaseHelper.COL_CONV_ULTIMA_FECHA} DESC
        """.trimIndent()

        val uStr = usuarioId.toString()
        val lista = mutableListOf<Chat>()
        val q = filtro.trim().lowercase(Locale.getDefault())

        helper.readableDatabase.rawQuery(sql, arrayOf(uStr, uStr, uStr, uStr)).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val u1 = c.getLong(1)
                val ultimoMsg = c.getString(3)
                val fecha = c.getLong(4)
                val n1 = c.getInt(5)
                val n2 = c.getInt(6)
                val otroId = c.getLong(7)
                val otroNombre = c.getString(8)

                val noLeidos = if (u1 == usuarioId) n1 else n2
                if (q.isEmpty() ||
                    otroNombre.lowercase(Locale.getDefault()).contains(q) ||
                    ultimoMsg.lowercase(Locale.getDefault()).contains(q)
                ) {
                    lista.add(
                        Chat(
                            id = id,
                            contactoId = otroId,
                            nombre = otroNombre,
                            ultimoMensaje = ultimoMsg,
                            hora = formatearHoraLista(fecha),
                            ultimaFecha = fecha,
                            enLinea = true,
                            sinLeer = noLeidos > 0,
                            cantidadSinLeer = noLeidos
                        )
                    )
                }
            }
        }
        return lista
    }

    fun obtenerMensajes(conversacionId: Long, usuarioActualId: Long): List<Mensaje> {
        val lista = mutableListOf<Mensaje>()
        val formatoHora = SimpleDateFormat("h:mm a", Locale.getDefault())
        helper.readableDatabase.query(
            DatabaseHelper.TABLA_MENSAJES,
            null,
            "${DatabaseHelper.COL_MSG_CONV_ID} = ?",
            arrayOf(conversacionId.toString()),
            null, null,
            "${DatabaseHelper.COL_MSG_FECHA_ENVIO} ASC, ${DatabaseHelper.COL_MSG_ID} ASC"
        ).use { c ->
            val idxId = c.getColumnIndexOrThrow(DatabaseHelper.COL_MSG_ID)
            val idxRem = c.getColumnIndexOrThrow(DatabaseHelper.COL_MSG_REMITENTE_ID)
            val idxCont = c.getColumnIndexOrThrow(DatabaseHelper.COL_MSG_CONTENIDO)
            val idxTipo = c.getColumnIndexOrThrow(DatabaseHelper.COL_MSG_TIPO)
            val idxAdj = c.getColumnIndexOrThrow(DatabaseHelper.COL_MSG_ADJUNTO_PATH)
            val idxFecha = c.getColumnIndexOrThrow(DatabaseHelper.COL_MSG_FECHA_ENVIO)

            while (c.moveToNext()) {
                val remId = c.getLong(idxRem)
                val fecha = c.getLong(idxFecha)
                lista.add(
                    Mensaje(
                        id = c.getLong(idxId),
                        conversacionId = conversacionId,
                        remitenteId = remId,
                        texto = c.getString(idxCont),
                        hora = formatoHora.format(Date(fecha)),
                        esMio = remId == usuarioActualId,
                        tipo = c.getString(idxTipo),
                        adjuntoPath = if (c.isNull(idxAdj)) null else c.getString(idxAdj),
                        fechaEnvio = fecha
                    )
                )
            }
        }
        return lista
    }

    fun enviarMensaje(
        conversacionId: Long,
        remitenteId: Long,
        contenido: String,
        tipo: String = Mensaje.TIPO_TEXTO,
        adjuntoPath: String? = null
    ): Long {
        val textoLimpio = contenido.trim()
        if (textoLimpio.isEmpty()) return -1L

        val ahora = System.currentTimeMillis()
        val db = helper.writableDatabase
        var nuevoId = -1L
        db.beginTransaction()
        try {
            val cvMsg = ContentValues().apply {
                put(DatabaseHelper.COL_MSG_CONV_ID, conversacionId)
                put(DatabaseHelper.COL_MSG_REMITENTE_ID, remitenteId)
                put(DatabaseHelper.COL_MSG_CONTENIDO, textoLimpio)
                put(DatabaseHelper.COL_MSG_TIPO, tipo)
                if (adjuntoPath != null) put(DatabaseHelper.COL_MSG_ADJUNTO_PATH, adjuntoPath)
                else putNull(DatabaseHelper.COL_MSG_ADJUNTO_PATH)
                put(DatabaseHelper.COL_MSG_FECHA_ENVIO, ahora)
                put(DatabaseHelper.COL_MSG_LEIDO, 0)
            }
            nuevoId = db.insertOrThrow(DatabaseHelper.TABLA_MENSAJES, null, cvMsg)

            var u1 = 0L
            var u2 = 0L
            var n1 = 0
            var n2 = 0
            db.query(
                DatabaseHelper.TABLA_CONVERSACIONES,
                arrayOf(
                    DatabaseHelper.COL_CONV_U1,
                    DatabaseHelper.COL_CONV_U2,
                    DatabaseHelper.COL_CONV_NO_LEIDOS_U1,
                    DatabaseHelper.COL_CONV_NO_LEIDOS_U2
                ),
                "${DatabaseHelper.COL_CONV_ID} = ?",
                arrayOf(conversacionId.toString()),
                null, null, null
            ).use { c ->
                if (c.moveToFirst()) {
                    u1 = c.getLong(0)
                    u2 = c.getLong(1)
                    n1 = c.getInt(2)
                    n2 = c.getInt(3)
                }
            }

            if (u1 != u2) {
                if (remitenteId == u1) n2 += 1 else n1 += 1
            }

            val cvConv = ContentValues().apply {
                put(DatabaseHelper.COL_CONV_ULTIMO_MENSAJE, textoLimpio)
                put(DatabaseHelper.COL_CONV_ULTIMA_FECHA, ahora)
                put(DatabaseHelper.COL_CONV_NO_LEIDOS_U1, n1)
                put(DatabaseHelper.COL_CONV_NO_LEIDOS_U2, n2)
            }
            db.update(
                DatabaseHelper.TABLA_CONVERSACIONES,
                cvConv,
                "${DatabaseHelper.COL_CONV_ID} = ?",
                arrayOf(conversacionId.toString())
            )
            db.setTransactionSuccessful()
        } catch (e: SQLiteException) {
            nuevoId = -1L
        } finally {
            db.endTransaction()
        }
        return nuevoId
    }

    fun marcarComoLeidos(conversacionId: Long, usuarioId: Long) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            var u1 = 0L
            db.query(
                DatabaseHelper.TABLA_CONVERSACIONES,
                arrayOf(DatabaseHelper.COL_CONV_U1),
                "${DatabaseHelper.COL_CONV_ID} = ?",
                arrayOf(conversacionId.toString()),
                null, null, null
            ).use { c ->
                if (c.moveToFirst()) u1 = c.getLong(0)
            }
            val cv = ContentValues().apply {
                if (u1 == usuarioId) put(DatabaseHelper.COL_CONV_NO_LEIDOS_U1, 0)
                else put(DatabaseHelper.COL_CONV_NO_LEIDOS_U2, 0)
            }
            db.update(
                DatabaseHelper.TABLA_CONVERSACIONES,
                cv,
                "${DatabaseHelper.COL_CONV_ID} = ?",
                arrayOf(conversacionId.toString())
            )
            val cvMsg = ContentValues().apply { put(DatabaseHelper.COL_MSG_LEIDO, 1) }
            db.update(
                DatabaseHelper.TABLA_MENSAJES,
                cvMsg,
                "${DatabaseHelper.COL_MSG_CONV_ID} = ? AND ${DatabaseHelper.COL_MSG_REMITENTE_ID} != ?",
                arrayOf(conversacionId.toString(), usuarioId.toString())
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun contarTotalNoLeidos(usuarioId: Long): Int {
        val sql = """
            SELECT COALESCE(SUM(
                CASE
                    WHEN ${DatabaseHelper.COL_CONV_U1} = ? THEN ${DatabaseHelper.COL_CONV_NO_LEIDOS_U1}
                    WHEN ${DatabaseHelper.COL_CONV_U2} = ? THEN ${DatabaseHelper.COL_CONV_NO_LEIDOS_U2}
                    ELSE 0
                END
            ), 0)
            FROM ${DatabaseHelper.TABLA_CONVERSACIONES}
            WHERE ${DatabaseHelper.COL_CONV_U1} = ? OR ${DatabaseHelper.COL_CONV_U2} = ?
        """.trimIndent()
        val u = usuarioId.toString()
        return helper.readableDatabase.rawQuery(sql, arrayOf(u, u, u, u)).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun eliminarMensaje(mensajeId: Long, conversacionId: Long): Boolean {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val filas = db.delete(
                DatabaseHelper.TABLA_MENSAJES,
                "${DatabaseHelper.COL_MSG_ID} = ?",
                arrayOf(mensajeId.toString())
            )
            if (filas <= 0) return false

            var ultimoTexto = ""
            var ultimaFecha = System.currentTimeMillis()
            db.query(
                DatabaseHelper.TABLA_MENSAJES,
                arrayOf(DatabaseHelper.COL_MSG_CONTENIDO, DatabaseHelper.COL_MSG_FECHA_ENVIO),
                "${DatabaseHelper.COL_MSG_CONV_ID} = ?",
                arrayOf(conversacionId.toString()),
                null, null,
                "${DatabaseHelper.COL_MSG_FECHA_ENVIO} DESC, ${DatabaseHelper.COL_MSG_ID} DESC",
                "1"
            ).use { c ->
                if (c.moveToFirst()) {
                    ultimoTexto = c.getString(0)
                    ultimaFecha = c.getLong(1)
                }
            }
            val cv = ContentValues().apply {
                put(DatabaseHelper.COL_CONV_ULTIMO_MENSAJE, ultimoTexto)
                put(DatabaseHelper.COL_CONV_ULTIMA_FECHA, ultimaFecha)
            }
            db.update(
                DatabaseHelper.TABLA_CONVERSACIONES,
                cv,
                "${DatabaseHelper.COL_CONV_ID} = ?",
                arrayOf(conversacionId.toString())
            )
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    fun eliminarConversacion(conversacionId: Long): Boolean {
        return try {
            helper.writableDatabase.delete(
                DatabaseHelper.TABLA_CONVERSACIONES,
                "${DatabaseHelper.COL_CONV_ID} = ?",
                arrayOf(conversacionId.toString())
            ) > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    /**
     * Crea una conversación inicial de bienvenida al registrar un nuevo usuario,
     * para que pueda probar la mensajería de inmediato.
     */
    fun crearMensajeBienvenidaSiNuevo(usuarioId: Long, esGuia: Boolean) {
        val sql = "SELECT ${DatabaseHelper.COL_ID} FROM ${DatabaseHelper.TABLA_USUARIOS} " +
            "WHERE ${DatabaseHelper.COL_ID} != ? AND ${DatabaseHelper.COL_ROL} = ? LIMIT 1"
        val rolBuscado = if (esGuia) Usuario.ROL_TURISTA else Usuario.ROL_GUIA
        val otroId = helper.readableDatabase.rawQuery(sql, arrayOf(usuarioId.toString(), rolBuscado)).use { c ->
            if (c.moveToFirst()) c.getLong(0) else -1L
        }
        if (otroId <= 0L) return

        val convId = obtenerOCrearConversacion(usuarioId, otroId)
        if (convId > 0L) {
            val texto = if (esGuia) {
                "¡Hola! Vi tus tours en Pura Guía, ¿tienes disponibilidad esta semana?"
            } else {
                "¡Pura vida! Bienvenido a Pura Guía. Escríbeme si tienes dudas sobre algún tour."
            }
            enviarMensaje(convId, otroId, texto)
        }
    }

    private fun formatearHoraLista(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        if (diff < 60_000L) return "Ahora"
        if (diff < 3_600_000L) return "Hace ${diff / 60_000L} min"

        val calMsg = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calHoy = Calendar.getInstance()
        if (calMsg.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
            calMsg.get(Calendar.DAY_OF_YEAR) == calHoy.get(Calendar.DAY_OF_YEAR)
        ) {
            return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        calHoy.add(Calendar.DAY_OF_YEAR, -1)
        if (calMsg.get(Calendar.YEAR) == calHoy.get(Calendar.YEAR) &&
            calMsg.get(Calendar.DAY_OF_YEAR) == calHoy.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Ayer"
        }
        return SimpleDateFormat("d MMM", Locale("es", "CR")).format(Date(timestamp))
    }
}
