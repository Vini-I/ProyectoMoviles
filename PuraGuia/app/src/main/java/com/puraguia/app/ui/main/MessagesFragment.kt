package com.puraguia.app.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.MensajeriaDao
import com.puraguia.app.data.Usuario
import com.puraguia.app.data.UsuarioDao
import com.puraguia.app.ui.chat.ChatActivity
import com.puraguia.app.util.SessionManager
import com.puraguia.app.util.iniciales
import com.puraguia.app.util.toast

/**
 * Pestaña "Mensajes": lista de conversaciones reales persistidas en SQLite,
 * con búsqueda en tiempo real, creación de nuevo chat y eliminación de conversaciones.
 */
class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private lateinit var mensajeriaDao: MensajeriaDao
    private lateinit var usuarioDao: UsuarioDao
    private lateinit var session: SessionManager
    private lateinit var contenedor: LinearLayout
    private lateinit var txtSinChats: TextView
    private lateinit var editBuscar: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mensajeriaDao = MensajeriaDao(requireContext())
        usuarioDao = UsuarioDao(requireContext())
        session = SessionManager(requireContext())

        contenedor = view.findViewById(R.id.container_chats)
        txtSinChats = view.findViewById(R.id.txt_sin_chats)
        editBuscar = view.findViewById(R.id.edit_buscar_chat)

        editBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cargarConversaciones()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        view.findViewById<View>(R.id.btn_nuevo_chat).setOnClickListener {
            mostrarSelectorContacto()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarConversaciones()
    }

    private fun cargarConversaciones() {
        if (!isAdded || !session.isLoggedIn()) return
        val userId = session.getUserId()
        val filtro = editBuscar.text.toString()
        val chats = mensajeriaDao.obtenerConversaciones(userId, filtro)

        contenedor.removeAllViews()
        txtSinChats.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
        if (chats.isEmpty()) {
            txtSinChats.text = if (filtro.isBlank()) {
                "No tienes conversaciones todavía.\nToca + para escribirle a un guía."
            } else {
                "No se encontraron conversaciones para \"$filtro\"."
            }
        }

        chats.forEach { chat ->
            val fila = layoutInflater.inflate(R.layout.item_chat, contenedor, false)
            fila.findViewById<TextView>(R.id.txt_iniciales).text = iniciales(chat.nombre)
            fila.findViewById<TextView>(R.id.txt_nombre).text = chat.nombre
            fila.findViewById<TextView>(R.id.txt_ultimo).text = chat.ultimoMensaje
            fila.findViewById<TextView>(R.id.txt_hora).text = chat.hora
            fila.findViewById<View>(R.id.dot_online).visibility =
                if (chat.enLinea) View.VISIBLE else View.GONE
            fila.findViewById<View>(R.id.dot_unread).visibility =
                if (chat.sinLeer) View.VISIBLE else View.INVISIBLE

            fila.setOnClickListener {
                startActivity(
                    ChatActivity.createIntent(
                        context = requireContext(),
                        nombre = chat.nombre,
                        enLinea = chat.enLinea,
                        contactoId = chat.contactoId,
                        conversacionId = chat.id
                    )
                )
            }

            fila.setOnLongClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar conversación")
                    .setMessage("¿Deseas eliminar la conversación con ${chat.nombre}?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
                        if (mensajeriaDao.eliminarConversacion(chat.id)) {
                            requireContext().toast("Conversación eliminada")
                            cargarConversaciones()
                            (activity as? MainActivity)?.refrescarBadgeMensajes()
                        }
                    }
                    .show()
                true
            }

            contenedor.addView(fila)
        }

        (activity as? MainActivity)?.refrescarBadgeMensajes()
    }

    private fun mostrarSelectorContacto() {
        val contactos = usuarioDao.obtenerContactosDisponibles(session.getUserId())
        if (contactos.isEmpty()) {
            requireContext().toast("No hay otros usuarios registrados aún")
            return
        }
        val etiquetas = contactos.map { usuario ->
            val tipo = if (usuario.rol == Usuario.ROL_GUIA) "Guía" else "Turista"
            "${usuario.nombre} ($tipo)"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Nueva conversación")
            .setItems(etiquetas) { _, which ->
                val elegido = contactos[which]
                startActivity(
                    ChatActivity.createIntent(
                        context = requireContext(),
                        nombre = elegido.nombre,
                        enLinea = true,
                        contactoId = elegido.id
                    )
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
