package com.puraguia.app.ui.profile

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
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
import com.puraguia.app.util.Validators
import com.puraguia.app.util.dp
import com.puraguia.app.util.setupHeader
import com.puraguia.app.util.toast

/**
 * Pantalla "Métodos de Pago": permite configurar, editar y eliminar cuentas SINPE Móvil y tarjetas en SQLite.
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
        setupHeader("Métodos de Pago", "SINPE Móvil y tarjetas guardadas")

        metodoPagoDao = MetodoPagoDao(this)
        actividadDao = ActividadDao(this)
        session = SessionManager(this)

        contenedor = findViewById(R.id.container_tarjetas)
        txtSinTarjetas = findViewById(R.id.txt_sin_tarjetas)

        findViewById<View>(R.id.btn_agregar_sinpe).setOnClickListener {
            mostrarDialogoSinpe(null)
        }
        findViewById<View>(R.id.btn_agregar_tarjeta).setOnClickListener {
            mostrarDialogoTarjeta(null)
        }

        mostrarMetodosPago()
    }

    private fun mostrarMetodosPago() {
        val metodos = metodoPagoDao.obtenerPorUsuario(session.getUserId())
        contenedor.removeAllViews()
        txtSinTarjetas.visibility = if (metodos.isEmpty()) View.VISIBLE else View.GONE

        metodos.forEach { metodo ->
            val vista = layoutInflater.inflate(R.layout.item_tarjeta, contenedor, false)
            val txtMarca = vista.findViewById<TextView>(R.id.txt_marca)
            val txtNumero = vista.findViewById<TextView>(R.id.txt_numero)
            val txtVence = vista.findViewById<TextView>(R.id.txt_vence)

            if (metodo.esSinpe) {
                txtMarca.text = "SINPE MÓVIL"
                txtMarca.setBackgroundResource(R.drawable.bg_brand_sinpe)
                txtNumero.text = metodo.telefono.ifBlank { "•••• ${metodo.ultimos4}" }
                txtVence.text = if (metodo.titular.isNotBlank()) {
                    "Titular: ${metodo.titular}"
                } else {
                    "Transferencia SINPE Móvil"
                }
            } else {
                txtMarca.text = metodo.marca
                txtMarca.setBackgroundResource(
                    if (metodo.marca.equals("MC", ignoreCase = true)) {
                        R.drawable.bg_brand_mc
                    } else {
                        R.drawable.bg_brand_tag
                    }
                )
                txtNumero.text = "•••• •••• •••• ${metodo.ultimos4}"
                txtVence.text = "Vence ${metodo.vence}"
            }

            vista.findViewById<View>(R.id.txt_predeterminada).visibility =
                if (metodo.predeterminada) View.VISIBLE else View.GONE

            vista.setOnClickListener {
                if (!metodo.predeterminada) {
                    confirmarPredeterminado(metodo)
                }
            }

            vista.findViewById<View>(R.id.btn_editar).setOnClickListener {
                if (metodo.esSinpe) {
                    mostrarDialogoSinpe(metodo)
                } else {
                    mostrarDialogoTarjeta(metodo)
                }
            }

            vista.findViewById<View>(R.id.btn_eliminar).setOnClickListener {
                confirmarEliminar(metodo)
            }
            contenedor.addView(vista)
        }
    }

    private fun mostrarDialogoSinpe(existente: Tarjeta?) {
        val esEdicion = existente != null
        val editTelefono = EditText(this).apply {
            hint = "Ej. 8888-8888"
            inputType = InputType.TYPE_CLASS_PHONE
            filters = arrayOf(InputFilter.LengthFilter(12))
            if (esEdicion) setText(existente!!.telefono)
        }
        val editTitular = EditText(this).apply {
            hint = "Nombre del titular"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(if (esEdicion) existente!!.titular else session.getNombre())
        }
        val checkPredeterminada = CheckBox(this).apply {
            text = "Usar como método de pago predeterminado"
            isChecked = existente?.predeterminada ?: false
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
            addView(TextView(this@MetodosPagoActivity).apply {
                text = "Número SINPE Móvil (8 dígitos)"
                textSize = 13f
            })
            addView(editTelefono)
            addView(TextView(this@MetodosPagoActivity).apply {
                text = "Nombre del titular"
                textSize = 13f
                setPadding(0, dp(10), 0, 0)
            })
            addView(editTitular)
            addView(checkPredeterminada)
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle(if (esEdicion) "Editar SINPE Móvil" else "Configurar SINPE Móvil")
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(if (esEdicion) "Actualizar" else "Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val telefono = editTelefono.text.toString().trim()
                val titular = editTitular.text.toString().trim()

                var valido = true
                if (!Validators.esSinpeValido(telefono)) {
                    editTelefono.error = "Ingresa un número SINPE válido de 8 dígitos"
                    valido = false
                }
                if (!Validators.esNombreValido(titular)) {
                    editTitular.error = "Ingresa el nombre completo del titular"
                    valido = false
                }
                if (!valido) return@setOnClickListener

                val userId = session.getUserId()
                val telFormateado = MetodoPagoDao.formatearTelefonoSinpe(telefono)
                if (esEdicion) {
                    val ok = metodoPagoDao.actualizarSinpe(
                        id = existente!!.id,
                        usuarioId = userId,
                        telefono = telFormateado,
                        titular = titular,
                        predeterminada = checkPredeterminada.isChecked
                    )
                    if (ok) {
                        toast("SINPE Móvil actualizado")
                        dialogo.dismiss()
                        mostrarMetodosPago()
                    } else {
                        toast("No se pudo actualizar el SINPE Móvil")
                    }
                } else {
                    val id = metodoPagoDao.insertarSinpe(
                        usuarioId = userId,
                        telefono = telFormateado,
                        titular = titular,
                        predeterminada = checkPredeterminada.isChecked
                    )
                    if (id > 0L) {
                        actividadDao.registrar(
                            userId,
                            TipoActividad.PAGO,
                            "SINPE Móvil configurado",
                            "$telFormateado ($titular)"
                        )
                        toast("SINPE Móvil guardado")
                        dialogo.dismiss()
                        mostrarMetodosPago()
                    } else {
                        toast("No se pudo guardar el SINPE Móvil")
                    }
                }
            }
        }
        dialogo.show()
    }

    private fun mostrarDialogoTarjeta(existente: Tarjeta?) {
        val esEdicion = existente != null
        val marcas = listOf("VISA", "MC", "AMEX")
        val spinnerMarca = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MetodosPagoActivity,
                android.R.layout.simple_spinner_item,
                marcas
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            if (esEdicion) {
                val idx = marcas.indexOfFirst { it.equals(existente!!.marca, ignoreCase = true) }
                if (idx >= 0) setSelection(idx)
            }
        }
        val editUltimos4 = EditText(this).apply {
            hint = "Últimos 4 dígitos (ej. 4242)"
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(4))
            if (esEdicion) setText(existente!!.ultimos4)
        }
        val editVence = EditText(this).apply {
            hint = "Vencimiento (MM/AA)"
            inputType = InputType.TYPE_CLASS_NUMBER
            keyListener = DigitsKeyListener.getInstance("0123456789/")
            filters = arrayOf(InputFilter.LengthFilter(5))
            if (esEdicion) setText(existente!!.vence)
            configurarFormatoVencimiento(this)
        }
        val checkPredeterminada = CheckBox(this).apply {
            text = "Usar como método de pago predeterminado"
            isChecked = existente?.predeterminada ?: false
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
            .setTitle(if (esEdicion) "Editar tarjeta" else "Agregar tarjeta")
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton(if (esEdicion) "Actualizar" else "Guardar", null)
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

                val userId = session.getUserId()
                if (esEdicion) {
                    val ok = metodoPagoDao.actualizarTarjeta(
                        id = existente!!.id,
                        usuarioId = userId,
                        marca = marca,
                        ultimos4 = ultimos4,
                        vence = vence,
                        predeterminada = checkPredeterminada.isChecked
                    )
                    if (ok) {
                        toast("Tarjeta actualizada")
                        dialogo.dismiss()
                        mostrarMetodosPago()
                    } else {
                        toast("No se pudo actualizar la tarjeta")
                    }
                } else {
                    val id = metodoPagoDao.insertar(
                        usuarioId = userId,
                        marca = marca,
                        ultimos4 = ultimos4,
                        vence = vence,
                        predeterminada = checkPredeterminada.isChecked
                    )
                    if (id > 0L) {
                        actividadDao.registrar(
                            userId,
                            TipoActividad.PAGO,
                            "Método de pago agregado",
                            "$marca •••• $ultimos4"
                        )
                        toast("Tarjeta guardada")
                        dialogo.dismiss()
                        mostrarMetodosPago()
                    } else {
                        toast("No se pudo guardar la tarjeta")
                    }
                }
            }
        }
        dialogo.show()
    }

    /**
     * Formatea automáticamente el vencimiento como MM/AA al escribir números,
     * evitando que el usuario tenga que buscar el carácter "/" en el teclado numérico.
     */
    private fun configurarFormatoVencimiento(editText: EditText) {
        var actualizando = false
        var borrando = false
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!actualizando) {
                    borrando = after < count
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (actualizando || s == null) return
                val textoActual = s.toString()
                val digitos = textoActual.filter { it.isDigit() }.take(4)
                val formateado = when {
                    digitos.length > 2 -> "${digitos.substring(0, 2)}/${digitos.substring(2)}"
                    digitos.length == 2 && !borrando -> "$digitos/"
                    else -> digitos
                }
                if (textoActual != formateado) {
                    actualizando = true
                    s.replace(0, s.length, formateado)
                    actualizando = false
                }
            }
        })
    }

    private fun confirmarPredeterminado(metodo: Tarjeta) {
        val descripcion = if (metodo.esSinpe) "SINPE Móvil ${metodo.telefono}" else "${metodo.marca} •••• ${metodo.ultimos4}"
        AlertDialog.Builder(this)
            .setTitle("Método predeterminado")
            .setMessage("¿Deseas usar \"$descripcion\" como método de pago predeterminado?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                if (metodoPagoDao.establecerPredeterminado(metodo.id, session.getUserId())) {
                    toast("Método predeterminado actualizado")
                    mostrarMetodosPago()
                }
            }
            .show()
    }

    private fun confirmarEliminar(metodo: Tarjeta) {
        val titulo = if (metodo.esSinpe) "Eliminar SINPE Móvil" else "Eliminar tarjeta"
        val mensaje = if (metodo.esSinpe) {
            "¿Deseas eliminar la cuenta SINPE Móvil ${metodo.telefono}?"
        } else {
            "¿Deseas eliminar la tarjeta terminada en ${metodo.ultimos4}?"
        }
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                if (metodoPagoDao.eliminar(metodo.id)) {
                    toast(if (metodo.esSinpe) "SINPE Móvil eliminado" else "Tarjeta eliminada")
                    mostrarMetodosPago()
                } else {
                    toast("No se pudo eliminar el método de pago")
                }
            }
            .show()
    }
}
