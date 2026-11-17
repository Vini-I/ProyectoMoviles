package com.puraguia.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.puraguia.app.util.PasswordHasher

/**
 * Base de datos SQLite de Pura Guía ("pura_guia.db").
 * Gestiona todas las tablas de la aplicación con claves foráneas e integridad referencial.
 */
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_USUARIOS)
        db.execSQL(CREATE_TOURS)
        db.execSQL(CREATE_CONVERSACIONES)
        db.execSQL(CREATE_MENSAJES)
        db.execSQL(CREATE_RESERVAS)
        db.execSQL(CREATE_FAVORITOS)
        db.execSQL(CREATE_RESENAS)
        db.execSQL(CREATE_METODOS_PAGO)
        db.execSQL(CREATE_ACTIVIDAD_RECIENTE)
        sembrarDatosIniciales(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TOURS)
        }
        if (oldVersion < 3) {
            migrarTablaToursV3(db)
            db.execSQL(CREATE_CONVERSACIONES)
            db.execSQL(CREATE_MENSAJES)
            db.execSQL(CREATE_RESERVAS)
            db.execSQL(CREATE_FAVORITOS)
            db.execSQL(CREATE_RESENAS)
            db.execSQL(CREATE_METODOS_PAGO)
            db.execSQL(CREATE_ACTIVIDAD_RECIENTE)
            sembrarDatosIniciales(db)
        }
    }

    private fun migrarTablaToursV3(db: SQLiteDatabase) {
        val columnasExistentes = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info($TABLA_TOURS)", null).use { cursor ->
            val idx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (idx >= 0) columnasExistentes.add(cursor.getString(idx).lowercase())
            }
        }
        val nuevasColumnas = listOf(
            "idiomas" to "TEXT NOT NULL DEFAULT 'Español|Inglés'",
            "rating" to "REAL NOT NULL DEFAULT 5.0",
            "resenas_count" to "INTEGER NOT NULL DEFAULT 0",
            "precio_original" to "INTEGER",
            "descuento" to "INTEGER",
            "imagen_res_name" to "TEXT NOT NULL DEFAULT 'tour_volcan'",
            "imagen_path" to "TEXT",
            "fecha_creacion" to "INTEGER NOT NULL DEFAULT 0"
        )
        nuevasColumnas.forEach { (col, def) ->
            if (!columnasExistentes.contains(col)) {
                db.execSQL("ALTER TABLE $TABLA_TOURS ADD COLUMN $col $def")
            }
        }
    }

    /**
     * Inserta datos iniciales de demostración si la tabla de tours está vacía,
     * para que la aplicación muestre tours, guías, reseñas y conversaciones reales desde SQLite.
     */
    private fun sembrarDatosIniciales(db: SQLiteDatabase) {
        val hayTours = db.rawQuery("SELECT COUNT(*) FROM $TABLA_TOURS", null).use { c ->
            c.moveToFirst() && c.getInt(0) > 0
        }
        if (hayTours) return

        val ahora = System.currentTimeMillis()
        db.beginTransaction()
        try {
            // 1. Guías iniciales
            val idCarlos = obtenerOInsertarUsuario(
                db,
                rol = Usuario.ROL_GUIA,
                nombre = "Carlos Rodríguez",
                email = "carlos@puraguia.cr",
                telefono = "88881111",
                idiomas = "Español|Inglés|Francés",
                ubicaciones = "Alajuela|Limón",
                certificaciones = "Guía oficial certificado por el ICT|Primeros auxilios (Cruz Roja)|Guía de naturaleza y avistamiento de aves",
                password = "Guia1234",
                fecha = ahora - 86_400_000L * 30
            )
            val idAna = obtenerOInsertarUsuario(
                db,
                rol = Usuario.ROL_GUIA,
                nombre = "Ana García",
                email = "ana@puraguia.cr",
                telefono = "88882222",
                idiomas = "Español|Inglés",
                ubicaciones = "Puntarenas|Alajuela",
                certificaciones = "Guía oficial certificado por el ICT|Seguridad en Canopy y Aventura",
                password = "Guia1234",
                fecha = ahora - 86_400_000L * 25
            )
            val idMaria = obtenerOInsertarUsuario(
                db,
                rol = Usuario.ROL_GUIA,
                nombre = "María López",
                email = "maria@puraguia.cr",
                telefono = "88883333",
                idiomas = "Español|Inglés|Portugués",
                ubicaciones = "Limón",
                certificaciones = "Guía oficial certificado por el ICT|Primeros auxilios",
                password = "Guia1234",
                fecha = ahora - 86_400_000L * 20
            )
            val idPuraVida = obtenerOInsertarUsuario(
                db,
                rol = Usuario.ROL_GUIA,
                nombre = "Pura Vida Tours",
                email = "puravida@puraguia.cr",
                telefono = "88884444",
                idiomas = "Español|Inglés|Italiano",
                ubicaciones = "Guanacaste",
                certificaciones = "Operador Turístico Certificado ICT",
                password = "Guia1234",
                fecha = ahora - 86_400_000L * 15
            )

            // Turista demo
            val idTuristaDemo = obtenerOInsertarUsuario(
                db,
                rol = Usuario.ROL_TURISTA,
                nombre = "Sofía Vargas",
                email = "demo@puraguia.cr",
                telefono = "88889999",
                pais = "Costa Rica",
                intereses = "Aventura|Naturaleza|Playa",
                password = "Demo1234",
                fecha = ahora - 86_400_000L * 10
            )

            // 2. Tours iniciales
            val t1 = insertarTourSeed(
                db, idCarlos,
                titulo = "Tour Volcán Arenal & Aguas Termales",
                lugar = "La Fortuna de San Carlos, Alajuela",
                precio = 60, categoria = "Aventura",
                duracion = "8 horas (9:00 AM - 5:00 PM)",
                cuidados = "Llevar ropa cómoda, protector solar, repelente de insectos",
                idiomas = "Español|Inglés|Francés",
                rating = 4.9, resenasCount = 342,
                precioOriginal = 75, descuento = 20,
                imagenResName = "tour_volcan",
                fecha = ahora - 86_400_000L * 7
            )
            val t2 = insertarTourSeed(
                db, idAna,
                titulo = "Canopy Tour Monteverde",
                lugar = "Monteverde, Puntarenas",
                precio = 65, categoria = "Aventura",
                duracion = "3 horas (8:00 AM - 11:00 AM)",
                cuidados = "Zapatos cerrados, ropa ligera, no apto con problemas cardíacos",
                idiomas = "Español|Inglés",
                rating = 4.9, resenasCount = 218,
                precioOriginal = null, descuento = null,
                imagenResName = "tour_canopy",
                fecha = ahora - 86_400_000L * 6
            )
            val t3 = insertarTourSeed(
                db, idCarlos,
                titulo = "Safari Wildlife Tortuguero",
                lugar = "Tortuguero, Limón",
                precio = 85, categoria = "Avistamiento",
                duracion = "6 horas (6:00 AM - 12:00 PM)",
                cuidados = "Repelente, binoculares, impermeable ligero",
                idiomas = "Español|Inglés|Alemán",
                rating = 4.8, resenasCount = 156,
                precioOriginal = null, descuento = null,
                imagenResName = "bg_tour_selva",
                fecha = ahora - 86_400_000L * 5
            )
            val t4 = insertarTourSeed(
                db, idMaria,
                titulo = "Playas del Caribe",
                lugar = "Puerto Viejo, Limón",
                precio = 55, categoria = "Familiar",
                duracion = "5 horas (10:00 AM - 3:00 PM)",
                cuidados = "Traje de baño, protector solar, toalla",
                idiomas = "Español|Inglés|Portugués",
                rating = 4.7, resenasCount = 98,
                precioOriginal = null, descuento = null,
                imagenResName = "bg_tour_playa",
                fecha = ahora - 86_400_000L * 4
            )
            insertarTourSeed(
                db, idAna,
                titulo = "Caminata Volcán Poás",
                lugar = "Poás, Alajuela",
                precio = 45, categoria = "Naturaleza",
                duracion = "4 horas (7:00 AM - 11:00 AM)",
                cuidados = "Abrigo, zapatos de caminata, agua",
                idiomas = "Español|Inglés",
                rating = 4.6, resenasCount = 120,
                precioOriginal = 55, descuento = 18,
                imagenResName = "tour_volcan",
                fecha = ahora - 86_400_000L * 3
            )
            insertarTourSeed(
                db, idPuraVida,
                titulo = "Aventura en Tirolesa Guanacaste",
                lugar = "Rincón de la Vieja, Guanacaste",
                precio = 70, categoria = "Aventura",
                duracion = "4 horas (9:00 AM - 1:00 PM)",
                cuidados = "Ropa deportiva, zapatos cerrados",
                idiomas = "Español|Inglés|Italiano",
                rating = 4.8, resenasCount = 187,
                precioOriginal = null, descuento = null,
                imagenResName = "tour_canopy",
                fecha = ahora - 86_400_000L * 2
            )
            insertarTourSeed(
                db, idCarlos,
                titulo = "Tour del Café & Plantaciones",
                lugar = "Naranjo, Alajuela",
                precio = 35, categoria = "Familiar",
                duracion = "3 horas (9:00 AM - 12:00 PM)",
                cuidados = "Zapatos cómodos, gorra, bloqueador solar",
                idiomas = "Español|Inglés",
                rating = 4.7, resenasCount = 189,
                precioOriginal = null, descuento = null,
                imagenResName = "bg_tour_selva",
                fecha = ahora - 86_400_000L
            )

            // 3. Reseñas iniciales
            insertarResenaSeed(db, t1, idTuristaDemo, "María González", 5,
                "¡Experiencia inolvidable! El volcán es impresionante y las aguas termales muy relajantes.",
                "15 Nov 2026", ahora - 86_400_000L * 2)
            insertarResenaSeed(db, t1, idTuristaDemo, "John Smith", 5,
                "Amazing tour! Carlos was very knowledgeable and the hot springs were perfect.",
                "10 Nov 2026", ahora - 86_400_000L * 7)
            insertarResenaSeed(db, t2, idTuristaDemo, "Ana Pérez", 4,
                "Muy buen tour, las vistas desde las tirolesas son espectaculares.",
                "05 Nov 2026", ahora - 86_400_000L * 12)
            insertarResenaSeed(db, t4, idTuristaDemo, "Sofía Vargas", 5,
                "Playas hermosas y excelente atención de María.",
                "02 Nov 2026", ahora - 86_400_000L * 15)

            // 4. Datos demo para el turista demo
            insertarFavoritoSeed(db, idTuristaDemo, t1, ahora - 86_400_000L * 2)
            insertarFavoritoSeed(db, idTuristaDemo, t2, ahora - 86_400_000L * 5)

            insertarReservaSeed(db, idTuristaDemo, t1, "Viernes 20 de Noviembre", "9:00 AM", "Tarjeta", 60, "Completado", ahora - 86_400_000L * 4)
            insertarReservaSeed(db, idTuristaDemo, t2, "Sábado 21 de Noviembre", "8:00 AM", "Sinpe", 65, "Confirmado", ahora - 86_400_000L)

            insertarTarjetaSeed(db, idTuristaDemo, "VISA", "4242", "12/27", true)
            insertarTarjetaSeed(db, idTuristaDemo, "MC", "8888", "08/28", false)

            insertarActividadSeed(db, idTuristaDemo, "RESERVA", "Reserva confirmada", "Canopy Tour Monteverde", "16 Nov 2026, 2:30 PM", ahora - 86_400_000L)
            insertarActividadSeed(db, idTuristaDemo, "FAVORITO", "Tour agregado a favoritos", "Tour Volcán Arenal", "15 Nov 2026, 5:15 PM", ahora - 86_400_000L * 2)

            // 5. Conversaciones y mensajes iniciales para demo
            val conv1 = insertarConversacionSeed(db, idTuristaDemo, idCarlos, "Perfecto! ¿A qué hora nos vemos?", ahora - 300_000L, noLeidosU1 = 1, noLeidosU2 = 0)
            insertarMensajeSeed(db, conv1, idTuristaDemo, "Hola! Tengo una consulta sobre el tour al volcán Arenal", ahora - 900_000L, leido = true)
            insertarMensajeSeed(db, conv1, idCarlos, "¡Claro! ¿En qué te puedo ayudar?", ahora - 600_000L, leido = true)
            insertarMensajeSeed(db, conv1, idCarlos, "Perfecto! ¿A qué hora nos vemos?", ahora - 300_000L, leido = false)

            val conv2 = insertarConversacionSeed(db, idTuristaDemo, idAna, "El tour incluye transporte y almuerzo", ahora - 3_600_000L, noLeidosU1 = 1, noLeidosU2 = 0)
            insertarMensajeSeed(db, conv2, idAna, "Hola Sofía, el tour incluye transporte y almuerzo", ahora - 3_600_000L, leido = false)

            val conv3 = insertarConversacionSeed(db, idTuristaDemo, idMaria, "Gracias por tu interés en Puerto Viejo!", ahora - 86_400_000L, noLeidosU1 = 0, noLeidosU2 = 0)
            insertarMensajeSeed(db, conv3, idMaria, "Gracias por tu interés en Puerto Viejo!", ahora - 86_400_000L, leido = true)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun obtenerOInsertarUsuario(
        db: SQLiteDatabase,
        rol: String,
        nombre: String,
        email: String,
        telefono: String,
        pais: String = "Costa Rica",
        intereses: String = "",
        idiomas: String = "",
        ubicaciones: String = "",
        certificaciones: String = "",
        password: String,
        fecha: Long
    ): Long {
        db.query(
            TABLA_USUARIOS,
            arrayOf(COL_ID),
            "$COL_EMAIL = ?",
            arrayOf(email.lowercase()),
            null, null, null
        ).use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        val salt = PasswordHasher.generarSalt()
        val cv = ContentValues().apply {
            put(COL_ROL, rol)
            put(COL_NOMBRE, nombre)
            put(COL_EMAIL, email.lowercase())
            put(COL_TELEFONO, telefono)
            put(COL_PAIS, pais)
            put(COL_INTERESES, intereses)
            put(COL_IDIOMAS, idiomas)
            put(COL_UBICACIONES, ubicaciones)
            put(COL_CERTIFICACIONES, certificaciones)
            putNull(COL_FOTO_PATH)
            put(COL_PASSWORD_HASH, PasswordHasher.hash(password, salt))
            put(COL_SALT, salt)
            put(COL_FECHA_REGISTRO, fecha)
        }
        return db.insert(TABLA_USUARIOS, null, cv)
    }

    private fun insertarTourSeed(
        db: SQLiteDatabase,
        guiaId: Long,
        titulo: String,
        lugar: String,
        precio: Int,
        categoria: String,
        duracion: String,
        cuidados: String,
        idiomas: String,
        rating: Double,
        resenasCount: Int,
        precioOriginal: Int?,
        descuento: Int?,
        imagenResName: String,
        fecha: Long
    ): Int {
        val cv = ContentValues().apply {
            put(COL_TOUR_GUIA_ID, guiaId)
            put(COL_TOUR_TITULO, titulo)
            put(COL_TOUR_LUGAR, lugar)
            put(COL_TOUR_PRECIO, precio)
            put(COL_TOUR_CATEGORIA, categoria)
            put(COL_TOUR_DURACION, duracion)
            put(COL_TOUR_CUIDADOS, cuidados)
            put(COL_TOUR_IDIOMAS, idiomas)
            put(COL_TOUR_RATING, rating)
            put(COL_TOUR_RESENAS_COUNT, resenasCount)
            if (precioOriginal != null) put(COL_TOUR_PRECIO_ORIGINAL, precioOriginal) else putNull(COL_TOUR_PRECIO_ORIGINAL)
            if (descuento != null) put(COL_TOUR_DESCUENTO, descuento) else putNull(COL_TOUR_DESCUENTO)
            put(COL_TOUR_IMAGEN_RES, imagenResName)
            putNull(COL_TOUR_IMAGEN_PATH)
            put(COL_TOUR_FECHA_CREACION, fecha)
        }
        return db.insert(TABLA_TOURS, null, cv).toInt()
    }

    private fun insertarResenaSeed(
        db: SQLiteDatabase, tourId: Int, usuarioId: Long, autor: String, estrellas: Int,
        comentario: String, fechaTexto: String, fecha: Long
    ) {
        val cv = ContentValues().apply {
            put(COL_RESENA_TOUR_ID, tourId)
            put(COL_RESENA_USUARIO_ID, usuarioId)
            put(COL_RESENA_AUTOR, autor)
            put(COL_RESENA_ESTRELLAS, estrellas)
            put(COL_RESENA_COMENTARIO, comentario)
            put(COL_RESENA_FECHA_TEXTO, fechaTexto)
            put(COL_RESENA_FECHA_CREACION, fecha)
        }
        db.insert(TABLA_RESENAS, null, cv)
    }

    private fun insertarFavoritoSeed(db: SQLiteDatabase, usuarioId: Long, tourId: Int, fecha: Long) {
        val cv = ContentValues().apply {
            put(COL_FAV_USUARIO_ID, usuarioId)
            put(COL_FAV_TOUR_ID, tourId)
            put(COL_FAV_FECHA, fecha)
        }
        db.insert(TABLA_FAVORITOS, null, cv)
    }

    private fun insertarReservaSeed(
        db: SQLiteDatabase, usuarioId: Long, tourId: Int, fechaTour: String,
        horaTour: String, metodoPago: String, total: Int, estado: String, fecha: Long
    ) {
        val cv = ContentValues().apply {
            put(COL_RES_USUARIO_ID, usuarioId)
            put(COL_RES_TOUR_ID, tourId)
            put(COL_RES_FECHA_TOUR, fechaTour)
            put(COL_RES_HORA_TOUR, horaTour)
            put(COL_RES_METODO_PAGO, metodoPago)
            put(COL_RES_TOTAL, total)
            put(COL_RES_ESTADO, estado)
            put(COL_RES_FECHA_CREACION, fecha)
        }
        db.insert(TABLA_RESERVAS, null, cv)
    }

    private fun insertarTarjetaSeed(
        db: SQLiteDatabase, usuarioId: Long, marca: String, ultimos4: String, vence: String, pred: Boolean
    ) {
        val cv = ContentValues().apply {
            put(COL_MP_USUARIO_ID, usuarioId)
            put(COL_MP_MARCA, marca)
            put(COL_MP_ULTIMOS4, ultimos4)
            put(COL_MP_VENCE, vence)
            put(COL_MP_PREDETERMINADA, if (pred) 1 else 0)
        }
        db.insert(TABLA_METODOS_PAGO, null, cv)
    }

    private fun insertarActividadSeed(
        db: SQLiteDatabase, usuarioId: Long, tipo: String, titulo: String,
        detalle: String, fechaTexto: String, fecha: Long
    ) {
        val cv = ContentValues().apply {
            put(COL_ACT_USUARIO_ID, usuarioId)
            put(COL_ACT_TIPO, tipo)
            put(COL_ACT_TITULO, titulo)
            put(COL_ACT_DETALLE, detalle)
            put(COL_ACT_FECHA_TEXTO, fechaTexto)
            put(COL_ACT_FECHA_CREACION, fecha)
        }
        db.insert(TABLA_ACTIVIDAD_RECIENTE, null, cv)
    }

    private fun insertarConversacionSeed(
        db: SQLiteDatabase, uA: Long, uB: Long, ultimoMensaje: String,
        ultimaFecha: Long, noLeidosU1: Int, noLeidosU2: Int
    ): Long {
        val u1 = minOf(uA, uB)
        val u2 = maxOf(uA, uB)
        val n1 = if (uA == u1) noLeidosU1 else noLeidosU2
        val n2 = if (uA == u1) noLeidosU2 else noLeidosU1
        val cv = ContentValues().apply {
            put(COL_CONV_U1, u1)
            put(COL_CONV_U2, u2)
            put(COL_CONV_ULTIMO_MENSAJE, ultimoMensaje)
            put(COL_CONV_ULTIMA_FECHA, ultimaFecha)
            put(COL_CONV_NO_LEIDOS_U1, n1)
            put(COL_CONV_NO_LEIDOS_U2, n2)
        }
        return db.insert(TABLA_CONVERSACIONES, null, cv)
    }

    private fun insertarMensajeSeed(
        db: SQLiteDatabase, convId: Long, remitenteId: Long, contenido: String,
        fecha: Long, leido: Boolean
    ) {
        val cv = ContentValues().apply {
            put(COL_MSG_CONV_ID, convId)
            put(COL_MSG_REMITENTE_ID, remitenteId)
            put(COL_MSG_CONTENIDO, contenido)
            put(COL_MSG_TIPO, "TEXTO")
            putNull(COL_MSG_ADJUNTO_PATH)
            put(COL_MSG_FECHA_ENVIO, fecha)
            put(COL_MSG_LEIDO, if (leido) 1 else 0)
        }
        db.insert(TABLA_MENSAJES, null, cv)
    }

    companion object {
        const val DB_NAME = "pura_guia.db"
        const val DB_VERSION = 3

        // ---------- Tabla: usuarios ----------
        const val TABLA_USUARIOS = "usuarios"
        const val COL_ID = "id"
        const val COL_ROL = "rol"
        const val COL_NOMBRE = "nombre"
        const val COL_EMAIL = "email"
        const val COL_TELEFONO = "telefono"
        const val COL_PAIS = "pais"
        const val COL_INTERESES = "intereses"
        const val COL_IDIOMAS = "idiomas"
        const val COL_UBICACIONES = "ubicaciones"
        const val COL_CERTIFICACIONES = "certificaciones"
        const val COL_FOTO_PATH = "foto_path"
        const val COL_PASSWORD_HASH = "password_hash"
        const val COL_SALT = "salt"
        const val COL_FECHA_REGISTRO = "fecha_registro"

        // ---------- Tabla: tours ----------
        const val TABLA_TOURS = "tours"
        const val COL_TOUR_ID = "id"
        const val COL_TOUR_GUIA_ID = "guia_id"
        const val COL_TOUR_TITULO = "titulo"
        const val COL_TOUR_LUGAR = "lugar"
        const val COL_TOUR_PRECIO = "precio"
        const val COL_TOUR_CATEGORIA = "categoria"
        const val COL_TOUR_DURACION = "duracion"
        const val COL_TOUR_CUIDADOS = "cuidados"
        const val COL_TOUR_IDIOMAS = "idiomas"
        const val COL_TOUR_RATING = "rating"
        const val COL_TOUR_RESENAS_COUNT = "resenas_count"
        const val COL_TOUR_PRECIO_ORIGINAL = "precio_original"
        const val COL_TOUR_DESCUENTO = "descuento"
        const val COL_TOUR_IMAGEN_RES = "imagen_res_name"
        const val COL_TOUR_IMAGEN_PATH = "imagen_path"
        const val COL_TOUR_FECHA_CREACION = "fecha_creacion"

        // ---------- Tabla: conversaciones ----------
        const val TABLA_CONVERSACIONES = "conversaciones"
        const val COL_CONV_ID = "id"
        const val COL_CONV_U1 = "usuario1_id"
        const val COL_CONV_U2 = "usuario2_id"
        const val COL_CONV_ULTIMO_MENSAJE = "ultimo_mensaje"
        const val COL_CONV_ULTIMA_FECHA = "ultima_fecha"
        const val COL_CONV_NO_LEIDOS_U1 = "no_leidos_u1"
        const val COL_CONV_NO_LEIDOS_U2 = "no_leidos_u2"

        // ---------- Tabla: mensajes ----------
        const val TABLA_MENSAJES = "mensajes"
        const val COL_MSG_ID = "id"
        const val COL_MSG_CONV_ID = "conversacion_id"
        const val COL_MSG_REMITENTE_ID = "remitente_id"
        const val COL_MSG_CONTENIDO = "contenido"
        const val COL_MSG_TIPO = "tipo"
        const val COL_MSG_ADJUNTO_PATH = "adjunto_path"
        const val COL_MSG_FECHA_ENVIO = "fecha_envio"
        const val COL_MSG_LEIDO = "leido"

        // ---------- Tabla: reservas ----------
        const val TABLA_RESERVAS = "reservas"
        const val COL_RES_ID = "id"
        const val COL_RES_USUARIO_ID = "usuario_id"
        const val COL_RES_TOUR_ID = "tour_id"
        const val COL_RES_FECHA_TOUR = "fecha_tour"
        const val COL_RES_HORA_TOUR = "hora_tour"
        const val COL_RES_METODO_PAGO = "metodo_pago"
        const val COL_RES_TOTAL = "total"
        const val COL_RES_ESTADO = "estado"
        const val COL_RES_FECHA_CREACION = "fecha_creacion"

        // ---------- Tabla: favoritos ----------
        const val TABLA_FAVORITOS = "favoritos"
        const val COL_FAV_ID = "id"
        const val COL_FAV_USUARIO_ID = "usuario_id"
        const val COL_FAV_TOUR_ID = "tour_id"
        const val COL_FAV_FECHA = "fecha_guardado"

        // ---------- Tabla: resenas ----------
        const val TABLA_RESENAS = "resenas"
        const val COL_RESENA_ID = "id"
        const val COL_RESENA_TOUR_ID = "tour_id"
        const val COL_RESENA_USUARIO_ID = "usuario_id"
        const val COL_RESENA_AUTOR = "autor_nombre"
        const val COL_RESENA_ESTRELLAS = "estrellas"
        const val COL_RESENA_COMENTARIO = "comentario"
        const val COL_RESENA_FECHA_TEXTO = "fecha_texto"
        const val COL_RESENA_FECHA_CREACION = "fecha_creacion"

        // ---------- Tabla: metodos_pago ----------
        const val TABLA_METODOS_PAGO = "metodos_pago"
        const val COL_MP_ID = "id"
        const val COL_MP_USUARIO_ID = "usuario_id"
        const val COL_MP_MARCA = "marca"
        const val COL_MP_ULTIMOS4 = "ultimos4"
        const val COL_MP_VENCE = "vence"
        const val COL_MP_PREDETERMINADA = "predeterminada"

        // ---------- Tabla: actividad_reciente ----------
        const val TABLA_ACTIVIDAD_RECIENTE = "actividad_reciente"
        const val COL_ACT_ID = "id"
        const val COL_ACT_USUARIO_ID = "usuario_id"
        const val COL_ACT_TIPO = "tipo"
        const val COL_ACT_TITULO = "titulo"
        const val COL_ACT_DETALLE = "detalle"
        const val COL_ACT_FECHA_TEXTO = "fecha_texto"
        const val COL_ACT_FECHA_CREACION = "fecha_creacion"

        private const val CREATE_USUARIOS =
            "CREATE TABLE IF NOT EXISTS $TABLA_USUARIOS (" +
                "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_ROL TEXT NOT NULL CHECK ($COL_ROL IN ('TURISTA','GUIA')), " +
                "$COL_NOMBRE TEXT NOT NULL, " +
                "$COL_EMAIL TEXT NOT NULL UNIQUE, " +
                "$COL_TELEFONO TEXT NOT NULL, " +
                "$COL_PAIS TEXT, " +
                "$COL_INTERESES TEXT, " +
                "$COL_IDIOMAS TEXT, " +
                "$COL_UBICACIONES TEXT, " +
                "$COL_CERTIFICACIONES TEXT, " +
                "$COL_FOTO_PATH TEXT, " +
                "$COL_PASSWORD_HASH TEXT NOT NULL, " +
                "$COL_SALT TEXT NOT NULL, " +
                "$COL_FECHA_REGISTRO INTEGER NOT NULL" +
                ")"

        private const val CREATE_TOURS =
            "CREATE TABLE IF NOT EXISTS $TABLA_TOURS (" +
                "$COL_TOUR_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_TOUR_GUIA_ID INTEGER NOT NULL, " +
                "$COL_TOUR_TITULO TEXT NOT NULL, " +
                "$COL_TOUR_LUGAR TEXT NOT NULL, " +
                "$COL_TOUR_PRECIO INTEGER NOT NULL, " +
                "$COL_TOUR_CATEGORIA TEXT NOT NULL, " +
                "$COL_TOUR_DURACION TEXT NOT NULL, " +
                "$COL_TOUR_CUIDADOS TEXT NOT NULL, " +
                "$COL_TOUR_IDIOMAS TEXT NOT NULL DEFAULT 'Español|Inglés', " +
                "$COL_TOUR_RATING REAL NOT NULL DEFAULT 5.0, " +
                "$COL_TOUR_RESENAS_COUNT INTEGER NOT NULL DEFAULT 0, " +
                "$COL_TOUR_PRECIO_ORIGINAL INTEGER, " +
                "$COL_TOUR_DESCUENTO INTEGER, " +
                "$COL_TOUR_IMAGEN_RES TEXT NOT NULL DEFAULT 'tour_volcan', " +
                "$COL_TOUR_IMAGEN_PATH TEXT, " +
                "$COL_TOUR_FECHA_CREACION INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY ($COL_TOUR_GUIA_ID) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE" +
                ")"

        private const val CREATE_CONVERSACIONES =
            "CREATE TABLE IF NOT EXISTS $TABLA_CONVERSACIONES (" +
                "$COL_CONV_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_CONV_U1 INTEGER NOT NULL, " +
                "$COL_CONV_U2 INTEGER NOT NULL, " +
                "$COL_CONV_ULTIMO_MENSAJE TEXT NOT NULL DEFAULT '', " +
                "$COL_CONV_ULTIMA_FECHA INTEGER NOT NULL, " +
                "$COL_CONV_NO_LEIDOS_U1 INTEGER NOT NULL DEFAULT 0, " +
                "$COL_CONV_NO_LEIDOS_U2 INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE($COL_CONV_U1, $COL_CONV_U2), " +
                "FOREIGN KEY ($COL_CONV_U1) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE, " +
                "FOREIGN KEY ($COL_CONV_U2) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE" +
                ")"

        private const val CREATE_MENSAJES =
            "CREATE TABLE IF NOT EXISTS $TABLA_MENSAJES (" +
                "$COL_MSG_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_MSG_CONV_ID INTEGER NOT NULL, " +
                "$COL_MSG_REMITENTE_ID INTEGER NOT NULL, " +
                "$COL_MSG_CONTENIDO TEXT NOT NULL, " +
                "$COL_MSG_TIPO TEXT NOT NULL DEFAULT 'TEXTO', " +
                "$COL_MSG_ADJUNTO_PATH TEXT, " +
                "$COL_MSG_FECHA_ENVIO INTEGER NOT NULL, " +
                "$COL_MSG_LEIDO INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY ($COL_MSG_CONV_ID) REFERENCES $TABLA_CONVERSACIONES($COL_CONV_ID) ON DELETE CASCADE, " +
                "FOREIGN KEY ($COL_MSG_REMITENTE_ID) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE" +
                ")"

        private const val CREATE_RESERVAS =
            "CREATE TABLE IF NOT EXISTS $TABLA_RESERVAS (" +
                "$COL_RES_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_RES_USUARIO_ID INTEGER NOT NULL, " +
                "$COL_RES_TOUR_ID INTEGER NOT NULL, " +
                "$COL_RES_FECHA_TOUR TEXT NOT NULL, " +
                "$COL_RES_HORA_TOUR TEXT NOT NULL, " +
                "$COL_RES_METODO_PAGO TEXT NOT NULL, " +
                "$COL_RES_TOTAL INTEGER NOT NULL, " +
                "$COL_RES_ESTADO TEXT NOT NULL DEFAULT 'Confirmado', " +
                "$COL_RES_FECHA_CREACION INTEGER NOT NULL, " +
                "FOREIGN KEY ($COL_RES_USUARIO_ID) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE, " +
                "FOREIGN KEY ($COL_RES_TOUR_ID) REFERENCES $TABLA_TOURS($COL_TOUR_ID) ON DELETE CASCADE" +
                ")"

        private const val CREATE_FAVORITOS =
            "CREATE TABLE IF NOT EXISTS $TABLA_FAVORITOS (" +
                "$COL_FAV_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_FAV_USUARIO_ID INTEGER NOT NULL, " +
                "$COL_FAV_TOUR_ID INTEGER NOT NULL, " +
                "$COL_FAV_FECHA INTEGER NOT NULL, " +
                "UNIQUE($COL_FAV_USUARIO_ID, $COL_FAV_TOUR_ID), " +
                "FOREIGN KEY ($COL_FAV_USUARIO_ID) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE, " +
                "FOREIGN KEY ($COL_FAV_TOUR_ID) REFERENCES $TABLA_TOURS($COL_TOUR_ID) ON DELETE CASCADE" +
                ")"

        private const val CREATE_RESENAS =
            "CREATE TABLE IF NOT EXISTS $TABLA_RESENAS (" +
                "$COL_RESENA_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_RESENA_TOUR_ID INTEGER NOT NULL, " +
                "$COL_RESENA_USUARIO_ID INTEGER NOT NULL, " +
                "$COL_RESENA_AUTOR TEXT NOT NULL, " +
                "$COL_RESENA_ESTRELLAS INTEGER NOT NULL, " +
                "$COL_RESENA_COMENTARIO TEXT NOT NULL, " +
                "$COL_RESENA_FECHA_TEXTO TEXT NOT NULL, " +
                "$COL_RESENA_FECHA_CREACION INTEGER NOT NULL, " +
                "FOREIGN KEY ($COL_RESENA_TOUR_ID) REFERENCES $TABLA_TOURS($COL_TOUR_ID) ON DELETE CASCADE, " +
                "FOREIGN KEY ($COL_RESENA_USUARIO_ID) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE" +
                ")"

        private const val CREATE_METODOS_PAGO =
            "CREATE TABLE IF NOT EXISTS $TABLA_METODOS_PAGO (" +
                "$COL_MP_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_MP_USUARIO_ID INTEGER NOT NULL, " +
                "$COL_MP_MARCA TEXT NOT NULL, " +
                "$COL_MP_ULTIMOS4 TEXT NOT NULL, " +
                "$COL_MP_VENCE TEXT NOT NULL, " +
                "$COL_MP_PREDETERMINADA INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY ($COL_MP_USUARIO_ID) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE" +
                ")"

        private const val CREATE_ACTIVIDAD_RECIENTE =
            "CREATE TABLE IF NOT EXISTS $TABLA_ACTIVIDAD_RECIENTE (" +
                "$COL_ACT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_ACT_USUARIO_ID INTEGER NOT NULL, " +
                "$COL_ACT_TIPO TEXT NOT NULL, " +
                "$COL_ACT_TITULO TEXT NOT NULL, " +
                "$COL_ACT_DETALLE TEXT NOT NULL, " +
                "$COL_ACT_FECHA_TEXTO TEXT NOT NULL, " +
                "$COL_ACT_FECHA_CREACION INTEGER NOT NULL, " +
                "FOREIGN KEY ($COL_ACT_USUARIO_ID) REFERENCES $TABLA_USUARIOS($COL_ID) ON DELETE CASCADE" +
                ")"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
    }
}
