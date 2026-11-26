package com.puraguia.app.model

import androidx.annotation.DrawableRes

data class Tour(
    val id: Int,
    val guiaId: Long = 0L,
    val titulo: String,
    val lugar: String,
    val precio: Int,
    val categoria: String = "Aventura",
    val rating: Double = 5.0,
    val resenas: Int = 0,
    val guia: String = "",
    val idiomas: List<String> = listOf("Español", "Inglés"),
    val duracion: String,
    val cuidados: String,
    @DrawableRes val imagen: Int,
    val imagenPath: String? = null,
    val precioOriginal: Int? = null,
    val descuento: Int? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
) {
    val tieneUbicacionGps: Boolean get() = latitud != null && longitud != null
}

data class Chat(
    val id: Long,
    val contactoId: Long,
    val nombre: String,
    val ultimoMensaje: String,
    val hora: String,
    val ultimaFecha: Long = 0L,
    val enLinea: Boolean = true,
    val sinLeer: Boolean = false,
    val cantidadSinLeer: Int = 0
)

data class Mensaje(
    val id: Long = 0L,
    val conversacionId: Long = 0L,
    val remitenteId: Long = 0L,
    val texto: String,
    val hora: String,
    val esMio: Boolean,
    val tipo: String = TIPO_TEXTO,
    val adjuntoPath: String? = null,
    val fechaEnvio: Long = 0L
) {
    companion object {
        const val TIPO_TEXTO = "TEXTO"
        const val TIPO_ADJUNTO = "ADJUNTO"
    }
}

data class Resena(
    val id: Long = 0L,
    val tourId: Int = 0,
    val usuarioId: Long = 0L,
    val autor: String,
    val fecha: String,
    val estrellas: Int,
    val comentario: String
)

data class Tarjeta(
    val id: Long = 0L,
    val usuarioId: Long = 0L,
    val marca: String,
    val ultimos4: String,
    val vence: String,
    val predeterminada: Boolean,
    val tipo: String = TIPO_TARJETA,
    val telefono: String = "",
    val titular: String = ""
) {
    val esSinpe: Boolean get() = tipo == TIPO_SINPE

    companion object {
        const val TIPO_TARJETA = "TARJETA"
        const val TIPO_SINPE = "SINPE"
    }
}

enum class TipoActividad { RESERVA, FAVORITO, RESENA, BUSQUEDA, PAGO, SESION }

data class Actividad(
    val id: Long = 0L,
    val tipo: TipoActividad,
    val titulo: String,
    val detalle: String,
    val fecha: String
)

data class ReservaResumen(
    val id: Long = 0L,
    val tourId: Int = 0,
    val tour: String,
    val fecha: String,
    val hora: String = "",
    val metodoPago: String = "",
    val estado: String,
    val total: Int
)
