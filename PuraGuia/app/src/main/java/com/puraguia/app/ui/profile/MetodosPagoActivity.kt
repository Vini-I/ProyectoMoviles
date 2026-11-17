package com.puraguia.app.ui.profile

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.ActividadDao
import com.puraguia.app.data.MetodoPagoDao
import com.puraguia.app.model.Tarjeta
import com.puraguia.app.model.TipoActividad
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.dp
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast

/**
 * Pantalla "Métodos de Pago": muestra las tarjetas guardadas en SQLite y permite agregar o eliminar.
 */
class MetodosPagoActivity : AppCompatActivity() {

    private lateinit var metodoPagoDao: MetodoPagoDao
    private lateinit var actividadDao: ActividadDao
    private lateinit var session: SessionManager
    private lateinit var contenedor: LinearLayout
    private lateinit var txtSinTarjetas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metodos_pago)
        setupHeader("Métodos de Pago")

        metodoPagoDao = MetodoPagoDao(this)
        actividadDao = ActividadDao(this)
        session = SessionManager(this)

        contenedor = findViewById(R.id.container_tarjetas)
        txtSinTarjetas = findViewById(R.id.txt_sin_tarjetas)

        findViewById<View>(R.id.btn_agregar_tarjeta).setOnClickListener {
            mostrarDialogoAgregarTarjeta()
        }

        mostrarTarjetas()
    }

    private fun mostrarTarjetas() {
        val tarjetas = metodoPagoDao.obtenerPorUsuario(session.getUserId())
        contenedor.removeAllViews()
        txtSinTarjetas.visibility = if (tarjetas.isEmpty()) View.VISIBLE else View.GONE

        tarjetas.forEach { tarjeta ->
            val vista = layoutInflater.inflate(R.layout.item_tarjeta, contenedor, false)
            val txtMarca = vista.findViewById<TextView>(R.id.txt_marca)
            txtMarca.text = tarjeta.marca
            txtMarca.setBackgroundResource(
                if (tarjeta.marca.equals("MC", ignoreCase = true)) {
                    R.drawable.bg_brand_mc
                } else {
                    R.drawable.bg_brand_tag
                }
            )
            vista.findViewById<View>(R.id.txt_predeterminada).visibility =
                if (tarjeta.predeterminada) View.VISIBLE else View.GONE
            vista.findViewById<TextView>(R.id.txt_numero).text =
                "•••• •••• •••• ${tarjeta.ultimos4}"
            vista.findViewById<TextView>(R.id.txt_vence).text = "Vence ${tarjeta.vence}"

            vista.findViewById<View>(R.id.btn_eliminar).setOnClickListener {
                confirmarEliminar(tarjeta)
            }
            contenedor.addView(vista)
        }
    }

    private fun mostrarDialogoAgregarTarjeta() {
        val marcas = listOf("VISA", "MC", "AMEX")
        val spinnerMarca = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MetodosPagoActivity,
                android.R.layout.simple_spinner_item,
                marcas
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        val editUltimos4 = EditText(this).apply {
            hint = "Últimos 4 dígitos (ej. 4242)"
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(4))
        }
        val editVence = EditText(this).apply {
            hint = "Vencimiento (MM/AA)"
            inputType = InputType.TYPE_CLASS_DATETIME
            filters = arrayOf(InputFilter.LengthFilter(5))
        }
        val checkPredeterminada = CheckBox(this).apply {
            text = "Usar como tarjeta predeterminada"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
            addView(TextView(this@MetodosPagoActivity).apply { text = "Marca"; textSize = 13f })
            addView(spinnerMarca)
            addView(TextView(this@MetodosPagoActivity).apply {
                text = "Últimos 4 dígitos"
                textSize = 13f
                setPadding(0, dp(10), 0, 0)
            })
            addView(editUltimos4)
            addView(TextView(this@MetodosPagoActivity).apply {
                text = "Fecha de vencimiento"
                textSize = 13f
                setPadding(0, dp(10), 0, 0)
            })
            addView(editVence)
            addView(checkPredeterminada)
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Agregar tarjeta")
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val ultimos4 = editUltimos4.text.toString().trim()
                val vence = editVence.text.toString().trim()
                val marca = spinnerMarca.selectedItem.toString()

                var valido = true
                if (ultimos4.length != 4 || ultimos4.any { !it.isDigit() }) {
                    editUltimos4.error = "Ingresa exactamente 4 dígitos"
                    valido = false
                }
                if (!Regex("^(0[1-9]|1[0-2])/\\d{2}$").matches(vence)) {
                    editVence.error = "Formato válido: MM/AA"
                    valido = false
                }
                if (!valido) return@setOnClickListener

                val id = metodoPagoDao.insertar(
                    usuarioId = session.getUserId(),
                    marca = marca,
                    ultimos4 = ultimos4,
                    vence = vence,
                    predeterminada = checkPredeterminada.isChecked
                )
                if (id > 0L) {
                    actividadDao.registrar(
                        session.getUserId(),
                        TipoActividad.PAGO,
                        "Método de pago agregado",
                        "$marca •••• $ultimos4"
                    )
                    toast("Tarjeta guardada")
                    dialogo.dismiss()
                    mostrarTarjetas()
                } else {
                    toast("No se pudo guardar la tarjeta")
                }
            }
        }
        dialogo.show()
    }

    private fun confirmarEliminar(tarjeta: Tarjeta) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarjeta")
            .setMessage("¿Deseas eliminar la tarjeta terminada en ${tarjeta.ultimos4}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                if (metodoPagoDao.eliminar(tarjeta.id)) {
                    toast("Tarjeta eliminada")
                    mostrarTarjetas()
                } else {
                    toast("No se pudo eliminar la tarjeta")
                }
            }
            .show()
    }
}
