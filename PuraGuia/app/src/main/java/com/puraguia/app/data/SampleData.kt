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
 * DATOS DE EJEMPLO (solo para maquetar las pantallas).
 *
 * TODO(equipo): reemplazar cada lista por consultas a SQLite (un DAO por tabla,
 * igual que UsuarioDao). Cuando una pantalla ya use la base de datos, se puede
 * borrar la lista correspondiente de este archivo.
 */
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
        ),
        Tour(
            id = 3, titulo = "Safari Wildlife Tortuguero",
            lugar = "Tortuguero, Limón", precio = 85, rating = 4.8, resenas = 156,
            guia = "Carlos Rodríguez", idiomas = listOf("Español", "Inglés", "Alemán"),
            duracion = "6 horas (6:00 AM - 12:00 PM)",
            cuidados = "Repelente, binoculares, impermeable ligero",
            imagen = R.drawable.bg_tour_selva
        ),
        Tour(
            id = 4, titulo = "Playas del Caribe",
            lugar = "Puerto Viejo, Limón", precio = 55, rating = 4.7, resenas = 98,
            guia = "María López", idiomas = listOf("Español", "Inglés", "Portugués"),
            duracion = "5 horas (10:00 AM - 3:00 PM)",
            cuidados = "Traje de baño, protector solar, toalla",
            imagen = R.drawable.bg_tour_playa
        ),
        Tour(
            id = 5, titulo = "Caminata Volcán Poás",
            lugar = "Poás, Alajuela", precio = 45, rating = 4.6, resenas = 120,
            guia = "Ana García", idiomas = listOf("Español", "Inglés"),
            duracion = "4 horas (7:00 AM - 11:00 AM)",
            cuidados = "Abrigo, zapatos de caminata, agua",
            imagen = R.drawable.tour_volcan
        ),
        Tour(
            id = 6, titulo = "Aventura en Tirolesa Guanacaste",
            lugar = "Rincón de la Vieja, Guanacaste", precio = 70, rating = 4.8, resenas = 187,
            guia = "Pura Vida Tours", idiomas = listOf("Español", "Inglés", "Italiano"),
            duracion = "4 horas (9:00 AM - 1:00 PM)",
            cuidados = "Ropa deportiva, zapatos cerrados",
            imagen = R.drawable.tour_canopy
        ),
        Tour(
            id = 7, titulo = "Tour del Café & Plantaciones",
            lugar = "Naranjo, Alajuela", precio = 35, rating = 4.7, resenas = 189,
            guia = "Carlos Rodríguez", idiomas = listOf("Español", "Inglés"),
            duracion = "3 horas (9:00 AM - 12:00 PM)",
            cuidados = "Zapatos cómodos, gorra, bloqueador solar",
            imagen = R.drawable.bg_tour_selva
        )
    )

    fun tourPorId(id: Int): Tour? = tours.firstOrNull { it.id == id }

    val chats: List<Chat> = listOf(
        Chat(1, "Carlos Rodríguez", "Perfecto! ¿A qué hora nos vemos?", "Hace 5 min", enLinea = true, sinLeer = true),
        Chat(2, "Ana García - Guía", "El tour incluye transporte y almuerzo", "Hace 1 hora", enLinea = true, sinLeer = true),
        Chat(3, "Tour Monteverde", "Gracias por tu reserva!", "Ayer", enLinea = false, sinLeer = false),
        Chat(4, "María López", "¿Tienen descuentos para grupos?", "2 días", enLinea = false, sinLeer = false),
        Chat(5, "Pura Vida Tours", "Tu experiencia fue confirmada ✓", "3 días", enLinea = false, sinLeer = false)
    )

    val mensajes: List<Mensaje> = listOf(
        Mensaje("Hola! Tengo una consulta sobre el tour al volcán Arenal", "10:30 AM", esMio = false),
        Mensaje("¡Claro! ¿En qué te puedo ayudar?", "10:31 AM", esMio = true),
        Mensaje("Perfecto! ¿A qué hora nos vemos?", "10:35 AM", esMio = false)
    )

    val resenas: List<Resena> = listOf(
        Resena("María González", "15 Nov 2025", 5, "¡Experiencia inolvidable! El volcán es impresionante y las aguas termales muy relajantes."),
        Resena("John Smith", "10 Nov 2025", 5, "Amazing tour! Carlos was very knowledgeable and the hot springs were perfect."),
        Resena("Ana Pérez", "5 Nov 2025", 4, "Muy buen tour, solo que el almuerzo podría mejorar. El resto excelente.")
    )

    val tarjetas: List<Tarjeta> = listOf(
        Tarjeta("VISA", "4242", "12/25", predeterminada = true),
        Tarjeta("MC", "8888", "08/26", predeterminada = false)
    )

    val actividades: List<Actividad> = listOf(
        Actividad(TipoActividad.RESERVA, "Reserva confirmada", "Canopy Tour Monteverde", "19 Nov 2025, 2:30 PM"),
        Actividad(TipoActividad.FAVORITO, "Tour agregado a favoritos", "Tour Volcán Arenal", "18 Nov 2025, 5:15 PM"),
        Actividad(TipoActividad.RESENA, "Reseña publicada", "Valoraste \"Playas del Caribe\" con 5 estrellas", "17 Nov 2025, 10:20 AM"),
        Actividad(TipoActividad.BUSQUEDA, "Búsqueda realizada", "Tours en Guanacaste, 2-4 horas", "16 Nov 2025, 3:45 PM"),
        Actividad(TipoActividad.PAGO, "Método de pago agregado", "Tarjeta •••• 4242", "15 Nov 2025, 11:00 AM"),
        Actividad(TipoActividad.SESION, "Inicio de sesión", "Desde tu dispositivo móvil", "15 Nov 2025, 10:55 AM")
    )

    val historialReservas: List<ReservaResumen> = listOf(
        ReservaResumen("Tour Volcán Arenal", "15 Nov 2025", "Completado", 85),
        ReservaResumen("Canopy Tour Monteverde", "19 Nov 2025", "Confirmado", 65)
    )

    // Datos del guía de ejemplo (pantalla de perfil de guía)
    const val GUIA_NOMBRE = "Carlos Rodríguez"
    const val GUIA_BIO =
        "Guía local con más de 10 años de experiencia mostrando lo mejor de Costa Rica. " +
            "Apasionado por la naturaleza, la fauna y la historia de cada rincón."
    val guiaIdiomas = listOf("Español", "Inglés", "Francés")
    val guiaUbicaciones = listOf("Alajuela", "Limón")
    val guiaCertificaciones = listOf(
        "Guía oficial certificado por el ICT",
        "Primeros auxilios (Cruz Roja)",
        "Guía de naturaleza y avistamiento de aves"
    )
}
