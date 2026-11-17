
package com.puraguia.app.ui.profile

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R
import com.puraguia.app.data.TourDAO
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.toast

class AgregarTourActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    private lateinit var editTitulo: EditText
    private lateinit var editLugar: EditText
    private lateinit var editPrecio: EditText

    private lateinit var spinnerDuracion: Spinner
    private lateinit var editCuidados: EditText
    private lateinit var spinnerCategoria: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_agregar_tour)
        supportActionBar?.hide()

        findViewById<Button>(R.id.btn_imagen).setOnClickListener {
            seleccionarImagen.launch("image/*")
        }
        // Obtener la sesión
        session = SessionManager(this)

        // Solo los guías pueden agregar tours
        if (!session.esGuia()) {
            toast("Solo los guías pueden agregar tours")
            finish()
            return
        }


        // Conectar los campos con el XML
        editTitulo = findViewById(R.id.et_tour_titulo)
        editLugar = findViewById(R.id.et_tour_lugar)
        editPrecio = findViewById(R.id.et_tour_precio)
        spinnerDuracion = findViewById(R.id.sp_tour_duracion)
        editCuidados = findViewById(R.id.et_tour_cuidados)
        spinnerCategoria = findViewById(R.id.sp_tour_categoria)

        // Lista de categorías
        val categorias = listOf(
            "Aventura",
            "Naturaleza",
            "Avistamiento",
            "Familiar",
            "Cultura",
            "Playa"
        )

        // Adaptador del Spinner
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categorias
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerCategoria.adapter = adapter

        val duraciones = listOf(
            "Seleccionar duración",
            "30 minutos",
            "1 hora",
            "2 horas",
            "3 horas",
            "4 horas",
            "5 horas",
            "6 horas",
            "Día completo"
        )

        val adapterDuracion = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            duraciones
        )

        adapterDuracion.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerDuracion.adapter = adapterDuracion

        // Botón guardar
        findViewById<Button>(R.id.btn_guardar_tour)
            .setOnClickListener {
                guardarTour()
            }

        // Botón cancelar
        findViewById<Button>(R.id.btn_cancelar_tour)
            .setOnClickListener {
                finish()
            }

        // Volver a Perfil
        findViewById<TextView>(R.id.btn_volver_tour)
            .setOnClickListener {
                finish()
            }

    }
    private val seleccionarImagen =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                findViewById<ImageView>(R.id.img_tour).setImageURI(uri)
            }
        }

    private fun guardarTour() {

        // Obtener los datos
        val titulo = editTitulo.text.toString().trim()
        val lugar = editLugar.text.toString().trim()
        val precio = editPrecio.text.toString().toIntOrNull()
        val duracion = spinnerDuracion.selectedItem.toString().trim()
        val cuidados = editCuidados.text.toString().trim()
        val categoria = spinnerCategoria.selectedItem.toString()

        // Validar nombre
        if (titulo.isEmpty()) {
            editTitulo.error = "Ingrese el nombre"
            editTitulo.requestFocus()
            return
        }

        // Validar lugar
        if (lugar.isEmpty()) {
            editLugar.error = "Ingrese el lugar"
            editLugar.requestFocus()
            return
        }

        // Validar precio
        if (precio == null || precio <= 0) {
            editPrecio.error = "Precio inválido"
            editPrecio.requestFocus()
            return
        }

        // Validar duración
        if (spinnerDuracion.selectedItemPosition == 0) {
            toast("Seleccione la duración del tour")
            return
        }

        // Validar recomendaciones
        if (cuidados.isEmpty()) {
            editCuidados.error = "Ingrese las recomendaciones"
            editCuidados.requestFocus()
            return
        }

        // Guardar en SQLite utilizando TourDAO
        val id = try {
            val tourDAO = TourDAO(this)

            tourDAO.insertar(
                guiaId = session.getUserId(),
                titulo = titulo,
                lugar = lugar,
                precio = precio,
                categoria = categoria,
                duracion = duracion,
                cuidados = cuidados
            )
        } catch (e: Exception) {
            Log.e("AgregarTourActivity", "Error al guardar tour", e)
            -1L
        }

        if (id > 0) {
            toast("Tour agregado correctamente")
            finish()
        } else {
            toast("Error al guardar el tour")
        }
    }
}