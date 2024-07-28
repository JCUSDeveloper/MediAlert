package com.example.medialert

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.auth.FirebaseAuth

class login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Configura los insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa Firebase Auth
        auth = FirebaseAuth.getInstance()

        val registerLink = findViewById<TextView>(R.id.register_link)
        val loginButton = findViewById<Button>(R.id.login_button)
        val emailEditText = findViewById<EditText>(R.id.email_input)
        val passwordEditText = findViewById<EditText>(R.id.password_input)

        registerLink.setOnClickListener {
            val intent = Intent(this, createAccount::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Inicia sesión con Firebase
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Muestra el diálogo de carga después de iniciar sesión
                            val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
                            loadingDialog.titleText = "Iniciando Sesión"
                            loadingDialog.contentText = "Por favor, espere..."
                            loadingDialog.setCancelable(false)
                            loadingDialog.show()

                            // Utiliza un Handler para cerrar el diálogo después de 3 segundos y luego iniciar la nueva actividad
                            Handler().postDelayed({
                                loadingDialog.dismiss()
                                val intent = Intent(this, menu::class.java)
                                startActivity(intent)
                                finish() // Opcional, cierra la actividad actual
                            }, 3000) // 3000 milisegundos = 3 segundos
                        } else {
                            // Si el inicio de sesión falla, muestra un SweetAlertDialog
                            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Autenticación Fallida")
                                .setContentText("Correo o Contraseña Inválidos")
                                .setConfirmText("OK")
                                .show()
                        }
                    }
            } else {
                // Si los campos están vacíos, muestra un SweetAlertDialog
                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Campos Requeridos")
                    .setContentText("Por favor, completa todos los campos.")
                    .setConfirmText("OK")
                    .show()
            }
        }
    }
}
