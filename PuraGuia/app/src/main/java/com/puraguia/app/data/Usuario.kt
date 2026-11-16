package com.puraguia.app.data

/**
 * Usuario de la app (turista o guía). La contraseña NUNCA se guarda aquí:
 * en la base de datos solo se guarda su hash + salt (ver PasswordHasher).
 */
data class Usuario(
    val id: Long = 0,
    val rol: String,
    val nombre: String,
    val email: String,
    val telefono: String,
    val pais: String = "",
    val intereses: List<String> = emptyList(),   // solo turista
    val idiomas: List<String> = emptyList(),     // solo guía
    val ubicaciones: List<String> = emptyList(), // solo guía
    val certificaciones: List<String> = emptyList(), // solo guía (rutas de archivos)
    val fotoPath: String? = null,                // solo guía
    val fechaRegistro: Long = System.currentTimeMillis()
) {
    val esGuia: Boolean get() = rol == ROL_GUIA
    val esTurista: Boolean get() = rol == ROL_TURISTA

    companion object {
        const val ROL_TURISTA = "TURISTA"
        const val ROL_GUIA = "GUIA"
    }
}
