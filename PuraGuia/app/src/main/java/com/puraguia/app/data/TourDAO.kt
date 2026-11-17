package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteException

class TourDAO (context: Context) {
    private val Helper = DatabaseHelper.getInstance(context)
    fun insertar(
        guiaId: Long,
        titulo: String,
        lugar: String,
        precio: Int,
        categoria: String,
        duracion: String,
        cuidados: String
    ): Long {
        val valores = ContentValues().apply {
            put("guia_id", guiaId)
            put("titulo", titulo)
            put("lugar", lugar)
            put("precio", precio)
            put("categoria", categoria)
            put("duracion", duracion)
            put("cuidados", cuidados)

        }
        return try {
            Helper.writableDatabase.insertOrThrow(DatabaseHelper.TABLA_TOURS, null, valores)

        } catch (e: SQLiteException) {
            -1L
        }
    }


}