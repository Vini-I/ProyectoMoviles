package com.puraguia.app.ui.main

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.puraguia.app.R
import com.puraguia.app.data.SampleData
import com.puraguia.app.ui.chat.ChatActivity
import com.puraguia.app.util.iniciales

/**
 * Pestaña "Mensajes": lista de conversaciones. Solo se puede entrar con sesión iniciada
 * (MainActivity lo valida).
 *
 * TODO(equipo mensajería): reemplazar SampleData.chats por las conversaciones guardadas en SQLite
 * y agregar la búsqueda de conversaciones (edit_buscar_chat).
 */
class MessagesFragment : Fragment(R.layout.fragment_messages) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val contenedor = view.findViewById<LinearLayout>(R.id.container_chats)
        contenedor.removeAllViews()

        SampleData.chats.forEach { chat ->
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
                startActivity(ChatActivity.createIntent(requireContext(), chat.nombre, chat.enLinea))
            }
            contenedor.addView(fila)
        }
    }
}
