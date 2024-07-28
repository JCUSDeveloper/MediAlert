package com.example.medialert

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class createAccount : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var birthdateEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordConfirmEditText: EditText // Añadido para confirmación de contraseña

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)

        // Configura los insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val registerButton = findViewById<Button>(R.id.register_button)
        val nameEditText = findViewById<EditText>(R.id.name_input)
        val emailEditText = findViewById<EditText>(R.id.email_input)
        passwordEditText = findViewById<EditText>(R.id.password_input)
        passwordConfirmEditText = findViewById<EditText>(R.id.password_confirm) // Inicialización del campo de confirmación
        birthdateEditText = findViewById(R.id.birthdate_input)

        birthdateEditText.setOnClickListener {
            showDatePickerDialog()
        }

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = passwordConfirmEditText.text.toString() // Obtener confirmación de contraseña
            val birthdate = birthdateEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && birthdate.isNotEmpty()) {
                if (isPasswordValid(password)) {
                    if (password == confirmPassword) { // Validar coincidencia de contraseñas
                        // Crear una cuenta con Firebase
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Almacena la información adicional en Firestore
                                    val user = hashMapOf(
                                        "name" to name,
                                        "email" to email,
                                        "birthdate" to birthdate
                                    )

                                    db.collection("users").document(auth.currentUser!!.uid)
                                        .set(user)
                                        .addOnSuccessListener {
                                            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                                                .setTitleText("Registro Exitoso")
                                                .setContentText("Cuenta creada exitosamente.")
                                                .setConfirmText("OK")
                                                .setConfirmClickListener {
                                                    it.dismissWithAnimation()
                                                    val intent = Intent(this, login::class.java)
                                                    startActivity(intent)
                                                    finish() // Opcional, cierra la actividad actual
                                                }
                                                .show()
                                        }
                                        .addOnFailureListener {
                                            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                                                .setTitleText("Error")
                                                .setContentText("Error al almacenar la información: ${it.message}")
                                                .setConfirmText("OK")
                                                .show()
                                        }
                                } else {
                                    // Si el registro falla, muestra un mensaje al usuario
                                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                                        .setTitleText("Registro Fallido")
                                        .setContentText("El registro falló: ${task.exception?.message}")
                                        .setConfirmText("OK")
                                        .setConfirmClickListener {
                                            it.dismissWithAnimation()
                                        }
                                        .show()
                                }
                            }
                    } else {
                        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("Contraseñas No Coinciden")
                            .setContentText("Las contraseñas no coinciden. Por favor, vuelve a intentarlo.")
                            .setConfirmText("OK")
                            .setConfirmClickListener {
                                it.dismissWithAnimation()
                            }
                            .show()
                    }
                } else {
                    SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Contraseña Inválida")
                        .setContentText("La contraseña debe tener al menos 8 caracteres, incluir al menos una letra mayúscula, una letra minúscula y un carácter especial.")
                        .setConfirmText("OK")
                        .setConfirmClickListener {
                            it.dismissWithAnimation()
                        }
                        .show()
                }
            } else {
                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Campos Vacíos")
                    .setContentText("Por favor, completa todos los campos.")
                    .setConfirmText("OK")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                    }
                    .show()
            }
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                birthdateEditText.setText(selectedDate)
            },
            year, month, day
        )
        datePickerDialog.show()
    }
}
