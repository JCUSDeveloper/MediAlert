package com.example.medialert

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.Calendar

class newTreatment : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_treatment)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val treatmentNumberSpinner = findViewById<Spinner>(R.id.treatment_number_spinner)
        val medicineNameInput = findViewById<EditText>(R.id.medicine_name_input)
        val timeEditText = findViewById<EditText>(R.id.timeEditText)
        val frequencyInput = findViewById<EditText>(R.id.frequency_input)
        val doseInput = findViewById<EditText>(R.id.dose_input)
        val compartmentQuantityInput = findViewById<EditText>(R.id.compartment_quantity_input)

        // Set up TimePickerDialog for first dose time
        timeEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this, R.style.CustomTimePickerDialog,
                { _, hourOfDay, minuteOfHour ->
                    timeEditText.setText(String.format("%02d:%02d", hourOfDay, minuteOfHour))
                }, hour, minute, true
            )
            timePickerDialog.show()
        }

        // Set up TimePickerDialog for frequency input
        frequencyInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this, R.style.CustomTimePickerDialog,
                { _, hourOfDay, minuteOfHour ->
                    frequencyInput.setText(String.format("%02d:%02d", hourOfDay, minuteOfHour))
                }, hour, minute, true
            )
            timePickerDialog.show()
        }

        // Save button click listener
        findViewById<Button>(R.id.save_button).setOnClickListener {
            val treatmentNumber = treatmentNumberSpinner.selectedItem.toString()
            val medicineName = medicineNameInput.text.toString()
            val firstDoseTime = timeEditText.text.toString()
            val frequency = frequencyInput.text.toString()
            val dose = doseInput.text.toString()
            val compartmentQuantity = compartmentQuantityInput.text.toString()

            if (medicineName.isNotEmpty() && firstDoseTime.isNotEmpty() && dose.isNotEmpty() && compartmentQuantity.isNotEmpty()) {
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                val treatmentData = hashMapOf(
                    "treatmentNumber" to treatmentNumber,
                    "medicineName" to medicineName,
                    "firstDoseTime" to firstDoseTime,
                    "frequency" to frequency,
                    "dose" to dose,
                    "compartmentQuantity" to compartmentQuantity
                )

                db.collection("users").document(userId).collection("treatments").add(treatmentData)
                    .addOnSuccessListener {
                        // Show success alert
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Guardado exitosamente")
                            .setConfirmClickListener {
                                it.dismissWithAnimation()
                                val intent = Intent(this, menu::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .show()

                        // Enviar datos por Bluetooth
                        val bluetoothMessage = "$firstDoseTime,$frequency,$medicineName,$treatmentNumber,$dose"
                        try {
                            BluetoothManager.sendData(bluetoothMessage)
                            Toast.makeText(this, "Se Envio Correctamente por Bluetooth", Toast.LENGTH_SHORT).show()
                        } catch (e: IOException) {
                            Toast.makeText(this, "Error al enviar datos por Bluetooth", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                    .addOnFailureListener { e ->
                        // Show failure alert
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText(e.message)
                            .show()
                    }
            } else {
                // Show error alert
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Campos vacíos")
                    .setContentText("Por favor, complete todos los campos.")
                    .show()
            }
        }

        // Cancel button click listener
        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            val intent = Intent(this, menu::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setDividerColor(picker: NumberPicker, color: Int) {
        try {
            val fields = NumberPicker::class.java.declaredFields
            for (field in fields) {
                if (field.name == "mSelectionDivider") {
                    field.isAccessible = true
                    val colorDrawable = ColorDrawable(color)
                    field.set(picker, colorDrawable)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
