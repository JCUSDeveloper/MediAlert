package com.example.medialert

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class menu : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // Inicializa Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.new_treatment_button).setOnClickListener {
            if (BluetoothManager.isConnected()) {
                val intent = Intent(this, newTreatment::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Debes conectarte a un dispositivo Bluetooth primero", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.edit_treatment_button).setOnClickListener {
            if (BluetoothManager.isConnected()) {
                val intent = Intent(this, editTreatment::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Debes conectarte a un dispositivo Bluetooth primero", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.connect_pillbox_button).setOnClickListener {
            val intent = Intent(this, connectPillbox::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.close_button).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.user).setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Obtén los datos del usuario desde Firestore
                val userRef = firestore.collection("users").document(currentUser.uid)
                userRef.get().addOnSuccessListener { document ->
                    if (document != null) {
                        val email = document.getString("email")
                        val name = document.getString("name")
                        val birthdate = document.getString("birthdate")

                        // Pasa los datos a la actividad profile
                        val intent = Intent(this, profile::class.java)
                        intent.putExtra("email", email)
                        intent.putExtra("name", name)
                        intent.putExtra("birthdate", birthdate)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}

