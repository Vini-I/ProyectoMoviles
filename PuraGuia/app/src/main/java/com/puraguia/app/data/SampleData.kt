package com.puraguia.app.data

import com.puraguia.app.R
import com.puraguia.app.model.Actividad
import com.puraguia.app.model.Chat
import com.puraguia.app.model.Mensaje
import com.puraguia.app.model.ReservaResumen
import com.puraguia.app.model.Resena
import com.puraguia.app.model.Tarjeta
import com.puraguia.app.model.TipoActividad
import com.puraguia.app.model.Tour

/**
 * Datos de referencia originales del proyecto base.
 * Todas las pantallas ahora consultan y persisten directamente en SQLite (DatabaseHelper + DAOs).
 */
@Suppress("unused")
object SampleData {

    val tours: List<Tour> = listOf(
        Tour(
            id = 1, titulo = "Tour Volcán Arenal & Aguas Termales",
            lugar = "La Fortuna de San Carlos, Alajuela", precio = 60, rating = 4.9, resenas = 342,
            guia = "Carlos Rodríguez", idiomas = listOf("Español", "Inglés", "Francés"),
            duracion = "8 horas (9:00 AM - 5:00 PM)",
            cuidados = "Llevar ropa cómoda, protector solar, repelente de insectos",
            imagen = R.drawable.tour_volcan, precioOriginal = 75, descuento = 20
        ),
        Tour(
            id = 2, titulo = "Canopy Tour Monteverde",
            lugar = "Monteverde, Puntarenas", precio = 65, rating = 4.9, resenas = 218,
            guia = "Ana García", idiomas = listOf("Español", "Inglés"),
            duracion = "3 horas (8:00 AM - 11:00 AM)",
            cuidados = "Zapatos cerrados, ropa ligera, no apto con problemas cardíacos",
            imagen = R.drawable.tour_canopy
        )
    )

    val chats: List<Chat> = listOf(
        Chat(id = 1, contactoId = 1, nombre = "Carlos Rodríguez", ultimoMensaje = "Perfecto! ¿A qué hora nos vemos?", hora = "Hace 5 min", enLinea = true, sinLeer = true)
    )

    val mensajes: List<Mensaje> = listOf(
        Mensaje(texto = "Hola! Tengo una consulta sobre el tour al volcán Arenal", hora = "10:30 AM", esMio = false),
        Mensaje(texto = "¡Claro! ¿En qué te puedo ayudar?", hora = "10:31 AM", esMio = true)
    )

    val resenas: List<Resena> = listOf(
        Resena(autor = "María González", fecha = "15 Nov 2025", estrellas = 5, comentario = "¡Experiencia inolvidable!")
    )

    val tarjetas: List<Tarjeta> = listOf(
        Tarjeta(marca = "VISA", ultimos4 = "4242", vence = "12/25", predeterminada = true)
    )

    val actividades: List<Actividad> = listOf(
        Actividad(tipo = TipoActividad.RESERVA, titulo = "Reserva confirmada", detalle = "Canopy Tour Monteverde", fecha = "19 Nov 2025, 2:30 PM")
    )

    val historialReservas: List<ReservaResumen> = listOf(
        ReservaResumen(tour = "Tour Volcán Arenal", fecha = "15 Nov 2025", estado = "Completado", total = 85)
    )
}
