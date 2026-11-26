package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import com.puraguia.app.R
import com.puraguia.app.model.Tour
import java.util.Locale

/**
 * DAO de tours: gestiona las operaciones CRUD y consultas filtradas sobre la tabla `tours`.
 */
class TourDAO(context: Context) {

    private val helper = DatabaseHelper.getInstance(context)

    fun insertar(
        guiaId: Long,
        titulo: String,
        lugar: String,
        precio: Int,
        categoria: String,
        duracion: String,
        cuidados: String,
        idiomas: List<String> = listOf("Español", "Inglés"),
        imagenPath: String? = null,
        imagenResName: String = imagenPorCategoria(categoria),
        latitud: Double? = null,
        longitud: Double? = null
    ): Long {
        val valores = ContentValues().apply {
            put(DatabaseHelper.COL_TOUR_GUIA_ID, guiaId)
            put(DatabaseHelper.COL_TOUR_TITULO, titulo.trim())
            put(DatabaseHelper.COL_TOUR_LUGAR, lugar.trim())
            put(DatabaseHelper.COL_TOUR_PRECIO, precio)
            put(DatabaseHelper.COL_TOUR_CATEGORIA, categoria.trim())
            put(DatabaseHelper.COL_TOUR_DURACION, duracion.trim())
            put(DatabaseHelper.COL_TOUR_CUIDADOS, cuidados.trim())
            put(DatabaseHelper.COL_TOUR_IDIOMAS, idiomas.joinToString("|"))
            put(DatabaseHelper.COL_TOUR_RATING, 5.0)
            put(DatabaseHelper.COL_TOUR_RESENAS_COUNT, 0)
            put(DatabaseHelper.COL_TOUR_IMAGEN_RES, imagenResName)
            if (imagenPath != null) put(DatabaseHelper.COL_TOUR_IMAGEN_PATH, imagenPath) else putNull(DatabaseHelper.COL_TOUR_IMAGEN_PATH)
            put(DatabaseHelper.COL_TOUR_FECHA_CREACION, System.currentTimeMillis())
            if (latitud != null && longitud != null) {
                put(DatabaseHelper.COL_TOUR_LATITUD, latitud)
                put(DatabaseHelper.COL_TOUR_LONGITUD, longitud)
            } else {
                putNull(DatabaseHelper.COL_TOUR_LATITUD)
                putNull(DatabaseHelper.COL_TOUR_LONGITUD)
            }
        }
        return try {
            helper.writableDatabase.insertOrThrow(DatabaseHelper.TABLA_TOURS, null, valores)
        } catch (e: SQLiteException) {
            -1L
        }
    }

    fun obtenerTodos(): List<Tour> = consultarTours(orderBy = "t.${DatabaseHelper.COL_TOUR_ID} DESC")

    fun obtenerOfertas(): List<Tour> = consultarTours(
        where = "t.${DatabaseHelper.COL_TOUR_DESCUENTO} IS NOT NULL AND t.${DatabaseHelper.COL_TOUR_DESCUENTO} > 0",
        orderBy = "t.${DatabaseHelper.COL_TOUR_DESCUENTO} DESC"
    )

    fun obtenerPopulares(limite: Int = 6): List<Tour> = consultarTours(
        orderBy = "t.${DatabaseHelper.COL_TOUR_RESENAS_COUNT} DESC, t.${DatabaseHelper.COL_TOUR_ID} DESC",
        limit = limite.toString()
    )

    fun obtenerPorId(id: Int): Tour? = consultarTours(
        where = "t.${DatabaseHelper.COL_TOUR_ID} = ?",
        args = arrayOf(id.toString())
    ).firstOrNull()

    fun obtenerPorGuia(guiaId: Long): List<Tour> = consultarTours(
        where = "t.${DatabaseHelper.COL_TOUR_GUIA_ID} = ?",
        args = arrayOf(guiaId.toString()),
        orderBy = "t.${DatabaseHelper.COL_TOUR_ID} DESC"
    )

    fun obtenerPorNombreGuia(nombreGuia: String): List<Tour> = consultarTours(
        where = "LOWER(u.${DatabaseHelper.COL_NOMBRE}) = LOWER(?)",
        args = arrayOf(nombreGuia.trim()),
        orderBy = "t.${DatabaseHelper.COL_TOUR_ID} DESC"
    )

    fun buscar(
        query: String = "",
        provincia: String? = null,
        categorias: List<String> = emptyList(),
        duracionFiltro: String? = null,
        orden: Int = 0
    ): List<Tour> {
        val condiciones = mutableListOf<String>()
        val args = mutableListOf<String>()

        val q = query.trim()
        if (q.isNotEmpty()) {
            condiciones.add(
                "(LOWER(t.${DatabaseHelper.COL_TOUR_TITULO}) LIKE LOWER(?) " +
                    "OR LOWER(t.${DatabaseHelper.COL_TOUR_LUGAR}) LIKE LOWER(?) " +
                    "OR LOWER(t.${DatabaseHelper.COL_TOUR_CATEGORIA}) LIKE LOWER(?) " +
                    "OR LOWER(COALESCE(u.${DatabaseHelper.COL_NOMBRE}, '')) LIKE LOWER(?))"
            )
            val pattern = "%$q%"
            args.add(pattern)
            args.add(pattern)
            args.add(pattern)
            args.add(pattern)
        }

        if (!provincia.isNullOrBlank() && provincia != "Todas las provincias") {
            condiciones.add("LOWER(t.${DatabaseHelper.COL_TOUR_LUGAR}) LIKE LOWER(?)")
            args.add("%$provincia%")
        }

        val categoriasReales = categorias.filter { it != "Cerca" }
        if (categoriasReales.isNotEmpty()) {
            val placeholders = categoriasReales.joinToString(",") { "LOWER(?)" }
            condiciones.add("LOWER(t.${DatabaseHelper.COL_TOUR_CATEGORIA}) IN ($placeholders)")
            categoriasReales.forEach { args.add(it) }
        }

        val orderSql = when (orden) {
            1 -> "t.${DatabaseHelper.COL_TOUR_PRECIO} ASC"
            2 -> "t.${DatabaseHelper.COL_TOUR_PRECIO} DESC"
            3 -> "t.${DatabaseHelper.COL_TOUR_RATING} DESC, t.${DatabaseHelper.COL_TOUR_RESENAS_COUNT} DESC"
            else -> "t.${DatabaseHelper.COL_TOUR_RESENAS_COUNT} DESC, t.${DatabaseHelper.COL_TOUR_ID} DESC"
        }

        val whereSql = if (condiciones.isEmpty()) null else condiciones.joinToString(" AND ")
        val resultados = consultarTours(
            where = whereSql,
            args = if (args.isEmpty()) null else args.toTypedArray(),
            orderBy = orderSql
        )

        if (duracionFiltro.isNullOrBlank()) return resultados

        return resultados.filter { tour ->
            coincideDuracion(tour.duracion, duracionFiltro)
        }
    }

    fun actualizar(
        id: Int,
        titulo: String,
        lugar: String,
        precio: Int,
        categoria: String,
        duracion: String,
        cuidados: String,
        imagenPath: String? = null,
        latitud: Double? = null,
        longitud: Double? = null
    ): Boolean {
        val valores = ContentValues().apply {
            put(DatabaseHelper.COL_TOUR_TITULO, titulo.trim())
            put(DatabaseHelper.COL_TOUR_LUGAR, lugar.trim())
            put(DatabaseHelper.COL_TOUR_PRECIO, precio)
            put(DatabaseHelper.COL_TOUR_CATEGORIA, categoria.trim())
            put(DatabaseHelper.COL_TOUR_DURACION, duracion.trim())
            put(DatabaseHelper.COL_TOUR_CUIDADOS, cuidados.trim())
            put(DatabaseHelper.COL_TOUR_IMAGEN_RES, imagenPorCategoria(categoria))
            if (imagenPath != null) {
                put(DatabaseHelper.COL_TOUR_IMAGEN_PATH, imagenPath)
            }
            if (latitud != null && longitud != null) {
                put(DatabaseHelper.COL_TOUR_LATITUD, latitud)
                put(DatabaseHelper.COL_TOUR_LONGITUD, longitud)
            }
        }
        return try {
            val filas = helper.writableDatabase.update(
                DatabaseHelper.TABLA_TOURS,
                valores,
                "${DatabaseHelper.COL_TOUR_ID} = ?",
                arrayOf(id.toString())
            )
            filas > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    /** Actualiza o limpia únicamente las coordenadas GPS de un tour existente. */
    fun actualizarUbicacion(id: Int, latitud: Double?, longitud: Double?): Boolean {
        val valores = ContentValues().apply {
            if (latitud != null && longitud != null) {
                put(DatabaseHelper.COL_TOUR_LATITUD, latitud)
                put(DatabaseHelper.COL_TOUR_LONGITUD, longitud)
            } else {
                putNull(DatabaseHelper.COL_TOUR_LATITUD)
                putNull(DatabaseHelper.COL_TOUR_LONGITUD)
            }
        }
        return try {
            helper.writableDatabase.update(
                DatabaseHelper.TABLA_TOURS,
                valores,
                "${DatabaseHelper.COL_TOUR_ID} = ?",
                arrayOf(id.toString())
            ) > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    fun eliminar(id: Int): Boolean {
        return try {
            val filas = helper.writableDatabase.delete(
                DatabaseHelper.TABLA_TOURS,
                "${DatabaseHelper.COL_TOUR_ID} = ?",
                arrayOf(id.toString())
            )
            filas > 0
        } catch (e: SQLiteException) {
            false
        }
    }

    private fun consultarTours(
        where: String? = null,
        args: Array<String>? = null,
        orderBy: String? = null,
        limit: String? = null
    ): List<Tour> {
        val sql = buildString {
            append(
                "SELECT t.*, COALESCE(u.${DatabaseHelper.COL_NOMBRE}, 'Guía Local') AS guia_nombre " +
                    "FROM ${DatabaseHelper.TABLA_TOURS} t " +
                    "LEFT JOIN ${DatabaseHelper.TABLA_USUARIOS} u " +
                    "ON t.${DatabaseHelper.COL_TOUR_GUIA_ID} = u.${DatabaseHelper.COL_ID}"
            )
            if (!where.isNullOrBlank()) append(" WHERE $where")
            if (!orderBy.isNullOrBlank()) append(" ORDER BY $orderBy")
            if (!limit.isNullOrBlank()) append(" LIMIT $limit")
        }
        val lista = mutableListOf<Tour>()
        helper.readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                lista.add(cursorATour(c))
            }
        }
        return lista
    }

    private fun cursorATour(c: Cursor): Tour {
        val idiomasRaw = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_IDIOMAS))
        val idiomas = if (idiomasRaw.isNullOrBlank()) listOf("Español", "Inglés") else idiomasRaw.split("|")
        val idxOrig = c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_PRECIO_ORIGINAL)
        val idxDesc = c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_DESCUENTO)
        val idxImgPath = c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_IMAGEN_PATH)
        val idxLat = c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_LATITUD)
        val idxLon = c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_LONGITUD)
        val imgResName = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_IMAGEN_RES))

        return Tour(
            id = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_ID)),
            guiaId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_GUIA_ID)),
            titulo = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_TITULO)),
            lugar = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_LUGAR)),
            precio = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_PRECIO)),
            categoria = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_CATEGORIA)),
            rating = c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_RATING)),
            resenas = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_RESENAS_COUNT)),
            guia = c.getString(c.getColumnIndexOrThrow("guia_nombre")),
            idiomas = idiomas,
            duracion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_DURACION)),
            cuidados = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TOUR_CUIDADOS)),
            imagen = resolverDrawable(imgResName),
            imagenPath = if (c.isNull(idxImgPath)) null else c.getString(idxImgPath),
            precioOriginal = if (c.isNull(idxOrig)) null else c.getInt(idxOrig),
            descuento = if (c.isNull(idxDesc)) null else c.getInt(idxDesc),
            latitud = if (c.isNull(idxLat)) null else c.getDouble(idxLat),
            longitud = if (c.isNull(idxLon)) null else c.getDouble(idxLon)
        )
    }

    private fun coincideDuracion(duracionTexto: String, filtro: String): Boolean {
        val texto = duracionTexto.lowercase(Locale.getDefault())
        if (filtro == "+1 día") return texto.contains("día") || texto.contains("dia")
        if (texto.contains("30 min") || texto.contains("1 hora")) return filtro == "2-3h"
        val numero = Regex("\\d+").find(texto)?.value?.toIntOrNull() ?: return true
        return when (filtro) {
            "2-3h" -> numero in 1..3
            "4-5h" -> numero in 4..5
            "6-7h" -> numero in 6..7
            "8-10h" -> numero in 8..10
            else -> true
        }
    }

    companion object {
        fun resolverDrawable(nombre: String?): Int = when (nombre) {
            "tour_canopy" -> R.drawable.tour_canopy
            "bg_tour_selva" -> R.drawable.bg_tour_selva
            "bg_tour_playa" -> R.drawable.bg_tour_playa
            else -> R.drawable.tour_volcan
        }

        fun imagenPorCategoria(categoria: String): String = when (categoria.lowercase()) {
            "playa" -> "bg_tour_playa"
            "naturaleza", "avistamiento", "cultura" -> "bg_tour_selva"
            "aventura" -> "tour_canopy"
            else -> "tour_volcan"
        }
    }
}
