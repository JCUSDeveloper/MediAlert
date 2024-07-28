package com.example.medialert

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class profile : AppCompatActivity() {
    private val editProfileRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Obtén los datos del Intent
        val email = intent.getStringExtra("email")
        val name = intent.getStringExtra("name")
        val birthdate = intent.getStringExtra("birthdate")

        // Muestra los datos en los TextViews correspondientes
        findViewById<TextView>(R.id.email_text_view).text = email
        findViewById<TextView>(R.id.name_text_view).text = name
        findViewById<TextView>(R.id.birthdate_text_view).text = birthdate

        // Configura el botón de editar
        findViewById<Button>(R.id.edit_button).setOnClickListener {
            val intent = Intent(this, editProfile::class.java)

            // Pasar solo los valores actuales en lugar de los pasados previamente
            val email = findViewById<TextView>(R.id.email_text_view).text.toString()
            val name = findViewById<TextView>(R.id.name_text_view).text.toString()
            val birthdate = findViewById<TextView>(R.id.birthdate_text_view).text.toString()

            intent.putExtra("email", email)
            intent.putExtra("name", name)
            intent.putExtra("birthdate", birthdate)

            startActivityForResult(intent, editProfileRequestCode)
        }


        // Configura el botón de regresar
        findViewById<Button>(R.id.back_button).setOnClickListener {
            finish() // Termina la actividad actual y regresa a la anterior
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == editProfileRequestCode && resultCode == RESULT_OK) {
            // Obtén los datos actualizados del Intent
            val updatedEmail = data?.getStringExtra("email")
            val updatedName = data?.getStringExtra("name")
            val updatedBirthdate = data?.getStringExtra("birthdate")

            // Actualiza los TextViews
            findViewById<TextView>(R.id.email_text_view).text = updatedEmail
            findViewById<TextView>(R.id.name_text_view).text = updatedName
            findViewById<TextView>(R.id.birthdate_text_view).text = updatedBirthdate
        }
    }
}