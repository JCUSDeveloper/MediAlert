package com.example.medialert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class editTreatment : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var treatmentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_treatment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editTreatment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val treatmentNumberSpinner = findViewById<Spinner>(R.id.treatment_number_spinner)
        val medicineNameInput = findViewById<EditText>(R.id.medicine_name_input)
        val timeEditText = findViewById<EditText>(R.id.timeEditText)
        val frequencyInput = findViewById<EditText>(R.id.frequency_input)
        val doseInput = findViewById<EditText>(R.id.dose_input)
        val compartmentQuantityInput = findViewById<EditText>(R.id.compartment_quantity_input)

        // Listener para el spinner de número de compartimiento
        treatmentNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCompartment = parent?.getItemAtPosition(position).toString()
                loadTreatmentData(selectedCompartment, medicineNameInput, timeEditText, frequencyInput, doseInput, compartmentQuantityInput)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val medicineName = medicineNameInput.text.toString()
            val firstDoseTime = timeEditText.text.toString()
            val frequency = frequencyInput.text.toString()
            val dose = doseInput.text.toString()
            val compartmentQuantity = compartmentQuantityInput.text.toString()

            if (medicineName.isNotEmpty() && firstDoseTime.isNotEmpty() && dose.isNotEmpty() && compartmentQuantity.isNotEmpty()) {
                updateTreatmentData(medicineName, firstDoseTime, frequency, dose, compartmentQuantity)
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            val intent = Intent(this, menu::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadTreatmentData(compartmentNumber: String, medicineNameInput: EditText, timeEditText: EditText, frequencyInput: EditText, doseInput: EditText, compartmentQuantityInput: EditText) {
        val userId = auth.currentUser?.uid ?: return

        val loadingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "Cargando datos"
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        db.collection("users").document(userId).collection("treatments")
            .whereEqualTo("treatmentNumber", compartmentNumber)
            .get()
            .addOnSuccessListener { documents ->
                loadingDialog.dismissWithAnimation()
                if (!documents.isEmpty) {
                    val treatment = documents.documents[0]
                    treatmentId = treatment.id
                    medicineNameInput.setText(treatment.getString("medicineName"))
                    timeEditText.setText(treatment.getString("firstDoseTime"))
                    frequencyInput.setText(treatment.getString("frequency"))
                    doseInput.setText(treatment.getString("dose"))
                    compartmentQuantityInput.setText(treatment.getString("compartmentQuantity"))
                } else {
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("No se encontró tratamiento para el compartimiento $compartmentNumber")
                        .show()
                }
            }
            .addOnFailureListener { exception ->
                loadingDialog.dismissWithAnimation()
                Log.w("editTreatment", "Error al obtener los tratamientos: ", exception)
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error")
                    .setContentText("Error al obtener los datos del tratamiento.")
                    .show()
            }
    }

    private fun updateTreatmentData(medicineName: String, firstDoseTime: String, frequency: String, dose: String, compartmentQuantity: String) {
        val userId = auth.currentUser?.uid ?: return

        val updatingDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        updatingDialog.titleText = "Actualizando datos"
        updatingDialog.setCancelable(false)
        updatingDialog.show()

        val treatmentData = hashMapOf(
            "medicineName" to medicineName,
            "firstDoseTime" to firstDoseTime,
            "frequency" to frequency,
            "dose" to dose,
            "compartmentQuantity" to compartmentQuantity
        )

        treatmentId?.let {
            db.collection("users").document(userId).collection("treatments").document(it)
                .update(treatmentData as Map<String, Any>)
                .addOnSuccessListener {
                    updatingDialog.dismissWithAnimation()
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Éxito")
                        .setContentText("Tratamiento actualizado exitosamente.")
                        .setConfirmClickListener {
                            val intent = Intent(this, menu::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .show()
                }
                .addOnFailureListener { e ->
                    updatingDialog.dismissWithAnimation()
                    Log.w("editTreatment", "Error al actualizar el tratamiento", e)
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("Error al actualizar el tratamiento.")
                        .show()
                }
        }
    }
}
