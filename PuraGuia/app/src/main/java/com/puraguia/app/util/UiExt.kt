package com.puraguia.app.util

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.puraguia.app.R

/** Convierte dp a píxeles. */
fun Context.dp(valor: Int): Int = (valor * resources.displayMetrics.density).toInt()

fun Context.toast(mensaje: String) {
    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
}

/** Iniciales para los avatares: "Carlos Rodríguez" -> "CR". */
fun iniciales(nombre: String): String {
    val partes = nombre.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes[0].take(1).uppercase()
        else -> (partes[0].take(1) + partes[1].take(1)).uppercase()
    }
}

/**
 * Configura el encabezado común (include_header.xml): botón volver + título + subtítulo.
 */
fun AppCompatActivity.setupHeader(titulo: String, subtitulo: String? = null) {
    findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
    findViewById<TextView>(R.id.header_title).text = titulo
    val sub = findViewById<TextView>(R.id.header_subtitle)
    if (subtitulo != null) {
        sub.text = subtitulo
        sub.visibility = View.VISIBLE
    }
}

/**
 * Cuadrícula de "chips" seleccionables (intereses, idiomas, ubicaciones, método de pago...).
 * Se dibuja dentro de un GridLayout del XML. Uso:
 *
 *   val chips = ChipGrid(this, findViewById(R.id.grid_idiomas), listOf("Español", "Inglés"))
 *   val elegidos: List<String> = chips.seleccionados
 */
class ChipGrid(
    private val context: Context,
    private val grid: GridLayout,
    opciones: List<String>,
    private val seleccionMultiple: Boolean = true,
    columnas: Int = 2,
    alturaDp: Int = 52,
    iconos: List<Int>? = null,
    private val alCambiar: ((List<String>) -> Unit)? = null
) {
    private val chips = mutableListOf<TextView>()

    val seleccionados: List<String>
        get() = chips.filter { it.isSelected }.map { it.text.toString() }

    fun limpiar() {
        chips.forEach { it.isSelected = false }
    }

    fun deseleccionar(texto: String) {
        chips.filter { it.text.toString() == texto }.forEach { it.isSelected = false }
    }

    init {
        grid.removeAllViews()
        grid.columnCount = columnas
        opciones.forEachIndexed { indice, texto ->
            agregarChip(texto, iconos?.getOrNull(indice), alturaDp)
        }
    }

    private fun agregarChip(texto: String, icono: Int?, alturaDp: Int) {
        val chip = TextView(context)
        chip.text = texto
        chip.gravity = Gravity.CENTER
        chip.textSize = 14f
        chip.isClickable = true
        chip.isFocusable = true
        chip.isSelected = false
        chip.setBackgroundResource(R.drawable.bg_chip)
        chip.setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text))
        chip.setPadding(context.dp(4), 0, context.dp(4), 0)

        if (icono != null) {
            val drawable = ContextCompat.getDrawable(context, icono)?.mutate()
            if (drawable != null) {
                val envuelto = DrawableCompat.wrap(drawable)
                DrawableCompat.setTint(envuelto, ContextCompat.getColor(context, R.color.pg_blue))
                chip.setCompoundDrawablesWithIntrinsicBounds(null, envuelto, null, null)
                chip.compoundDrawablePadding = context.dp(6)
            }
        }

        chip.setOnClickListener {
            if (seleccionMultiple) {
                chip.isSelected = !chip.isSelected
            } else {
                val yaSeleccionado = chip.isSelected
                chips.forEach { it.isSelected = false }
                chip.isSelected = !yaSeleccionado
            }
            alCambiar?.invoke(seleccionados)
        }

        val parametros = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED),
            GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f)
        )
        parametros.width = 0
        parametros.height = context.dp(alturaDp)
        val margen = context.dp(4)
        parametros.setMargins(margen, margen, margen, margen)

        chips.add(chip)
        grid.addView(chip, parametros)
    }
}
