package com.puraguia.app.model

import androidx.annotation.DrawableRes

/**
 * MODELOS DE EJEMPLO para las pantallas base.
 *
 * TODO(equipo): cuando cada módulo tenga su tabla en SQLite, estos modelos se
 * pueden mover/ajustar (por ejemplo agregar el campo `id` de la BD, claves foráneas, etc.).
 * Por ahora solo sirven para que las pantallas muestren algo parecido al wireframe.
 */

data class Tour(
    val id: Int,
    val titulo: String,
    val lugar: String,
    val precio: Int,
    val rating: Double,
    val resenas: Int,
    val guia: String,
    val idiomas: List<String>,
    val duracion: String,
    val cuidados: String,
    @DrawableRes val imagen: Int,
    val precioOriginal: Int? = null,
    val descuento: Int? = null
)

data class Chat(
    val id: Int,
    val nombre: String,
    val ultimoMensaje: String,
    val hora: String,
    val enLinea: Boolean,
    val sinLeer: Boolean
)

data class Mensaje(
    val texto: String,
    val hora: String,
    val esMio: Boolean
)

data class Resena(
    val autor: String,
    val fecha: String,
    val estrellas: Int,
    val comentario: String
)

data class Tarjeta(
    val marca: String,
    val ultimos4: String,
    val vence: String,
    val predeterminada: Boolean
)

enum class TipoActividad { RESERVA, FAVORITO, RESENA, BUSQUEDA, PAGO, SESION }

data class Actividad(
    val tipo: TipoActividad,
    val titulo: String,
    val detalle: String,
    val fecha: String
)

data class ReservaResumen(
    val tour: String,
    val fecha: String,
    val estado: String,
    val total: Int
)
