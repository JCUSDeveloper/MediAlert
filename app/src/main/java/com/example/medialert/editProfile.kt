package com.example.medialert

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import cn.pedant.SweetAlert.SweetAlertDialog
import java.util.Calendar

class editProfile : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var birthdateEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Inicializa Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Obtén los datos del Intent
        val email = intent.getStringExtra("email")
        val name = intent.getStringExtra("name")
        val birthdate = intent.getStringExtra("birthdate")

        // Encuentra los EditTexts
        val emailEditText = findViewById<EditText>(R.id.email_input)
        val nameEditText = findViewById<EditText>(R.id.name_input)
        birthdateEditText = findViewById(R.id.birthdate_input)

        // Muestra los datos en los EditTexts correspondientes
        emailEditText.setText(email)
        nameEditText.setText(name)
        birthdateEditText.setText(birthdate)

        // Configura el botón de guardar
        findViewById<Button>(R.id.save_button).setOnClickListener {
            val updatedEmail = emailEditText.text.toString()
            val updatedName = nameEditText.text.toString()
            val updatedBirthdate = birthdateEditText.text.toString()

            if (updatedEmail.isNotEmpty() && updatedName.isNotEmpty() && updatedBirthdate.isNotEmpty()) {
                // Actualiza los datos en Firestore
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                val user = hashMapOf(
                    "name" to updatedName,
                    "email" to updatedEmail,
                    "birthdate" to updatedBirthdate
                )

                db.collection("users").document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        // Muestra alerta de éxito con SweetAlert
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Éxito")
                            .setContentText("Perfil editado exitosamente")
                            .setConfirmClickListener {
                                // Devuelve los datos actualizados a la actividad anterior
                                val resultIntent = Intent()
                                resultIntent.putExtra("email", updatedEmail)
                                resultIntent.putExtra("name", updatedName)
                                resultIntent.putExtra("birthdate", updatedBirthdate)
                                setResult(RESULT_OK, resultIntent)
                                finish() // Cierra la actividad y regresa a la anterior
                            }
                            .show()
                    }
                    .addOnFailureListener {
                        // Muestra alerta de error con SweetAlert
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("No se pudo actualizar el perfil. Intenta nuevamente.")
                            .setConfirmClickListener { sDialog -> sDialog.dismissWithAnimation() }
                            .show()
                    }
            }
        }

        // Configura el botón de cancelar
        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish() // Cierra la actividad y regresa a la anterior
        }

        // Configura el OnClickListener para birthdateEditText
        birthdateEditText.setOnClickListener {
            showDatePickerDialog()
        }
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
