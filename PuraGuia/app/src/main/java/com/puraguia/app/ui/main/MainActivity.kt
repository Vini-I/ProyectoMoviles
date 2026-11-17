package com.puraguia.app.ui.main

import android.graphics.Color
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.puraguia.app.R
import com.puraguia.app.ui.auth.LoginActivity
import com.puraguia.app.util.SessionManager

/**
 * Contenedor principal con la barra inferior: Inicio | Buscar | Mensajes | Perfil.
 *
 * Regla: Inicio y Buscar son públicos. Mensajes y Perfil requieren sesión; si no la hay,
 * se abre el login y, si inicia sesión, se entra a la pestaña que quería abrir.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var session: SessionManager

    // Pestaña a la que se quiere ir después de iniciar sesión
    private var pestanaPendiente: Int = R.id.nav_home

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            bottomNav.selectedItemId = pestanaPendiente
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = SessionManager(this)
        bottomNav = findViewById(R.id.bottom_nav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    mostrarFragmento(HomeFragment())
                    true
                }
                R.id.nav_search -> {
                    mostrarFragmento(SearchFragment())
                    true
                }
                R.id.nav_messages, R.id.nav_profile -> {
                    if (session.isLoggedIn()) {
                        mostrarFragmento(
                            if (item.itemId == R.id.nav_messages) MessagesFragment() else ProfileFragment()
                        )
                        true
                    } else {
                        pestanaPendiente = item.itemId
                        loginLauncher.launch(LoginActivity.createIntent(this))
                        false // se queda en la pestaña actual hasta que inicie sesión
                    }
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarBadgeMensajes()
        // Si cerró sesión (por ejemplo desde Configuración) y estaba en una pestaña privada, vuelve a Inicio
        val enPestanaPrivada = bottomNav.selectedItemId == R.id.nav_messages ||
            bottomNav.selectedItemId == R.id.nav_profile
        if (!session.isLoggedIn() && enPestanaPrivada) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    /** Punto rojo con la cantidad real de mensajes sin leer desde SQLite. */
    fun refrescarBadgeMensajes() {
        actualizarBadgeMensajes()
    }

    private fun actualizarBadgeMensajes() {
        if (session.isLoggedIn()) {
            val sinLeer = com.puraguia.app.data.MensajeriaDao(this)
                .contarTotalNoLeidos(session.getUserId())
            if (sinLeer > 0) {
                val badge = bottomNav.getOrCreateBadge(R.id.nav_messages)
                badge.backgroundColor = Color.parseColor("#EF4444")
                badge.number = sinLeer
                badge.isVisible = true
            } else {
                bottomNav.removeBadge(R.id.nav_messages)
            }
        } else {
            bottomNav.removeBadge(R.id.nav_messages)
        }
    }

    private fun mostrarFragmento(fragmento: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragmento)
            .commit()
    }

    fun irAInicio() {
        bottomNav.selectedItemId = R.id.nav_home
    }

    fun irABuscar() {
        bottomNav.selectedItemId = R.id.nav_search
    }

    fun irAPerfil() {
        bottomNav.selectedItemId = R.id.nav_profile
    }
}
