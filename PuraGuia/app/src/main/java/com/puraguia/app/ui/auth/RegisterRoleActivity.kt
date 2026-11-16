package com.puraguia.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.puraguia.app.R

/** Pantalla "¿Cómo deseas registrarte?": Turista o Guía. */
class RegisterRoleActivity : AppCompatActivity() {

    private val registroLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_role)

        findViewById<View>(R.id.card_turista).setOnClickListener {
            registroLauncher.launch(Intent(this, RegisterTouristActivity::class.java))
        }
        findViewById<View>(R.id.card_guia).setOnClickListener {
            registroLauncher.launch(Intent(this, RegisterGuideActivity::class.java))
        }
        findViewById<View>(R.id.btn_volver_login).setOnClickListener { finish() }
    }
}
