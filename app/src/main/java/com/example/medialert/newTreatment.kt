package com.example.medialert

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
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

            timePickerDialog.setOnShowListener {
                val positiveButton = timePickerDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = timePickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                positiveButton.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                negativeButton.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }

            timePickerDialog.show()
        }


        frequencyInput.setOnClickListener {
            val hour = 0
            val minute = 0

            val timePickerDialog = TimePickerDialog(
                this, R.style.CustomTimePickerDialog,
                { _, hourOfDay, minuteOfHour ->
                    frequencyInput.setText(String.format("%02d:%02d", hourOfDay, minuteOfHour))
                }, hour, minute, true
            )

            timePickerDialog.setOnShowListener {
                val positiveButton = timePickerDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = timePickerDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                positiveButton.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                negativeButton.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }

            timePickerDialog.show()
        }



        // Save button click listener
        findViewById<Button>(R.id.save_button).setOnClickListener {
            val treatmentNumber = treatmentNumberSpinner.selectedItem.toString()
            val medicineName = medicineNameInput.text.toString().trim().replace("\\s+".toRegex(), " ")
            val firstDoseTime = timeEditText.text.toString()
            val frequency = frequencyInput.text.toString()
            val dose = doseInput.text.toString()
            val compartmentQuantity = compartmentQuantityInput.text.toString()

            // Validar que dose y compartmentQuantity no sean ceros
            if (dose == "0" || compartmentQuantity == "0") {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Valor inválido")
                    .setContentText("La dosis y la cantidad de compartimentos no pueden ser cero.")
                    .show()
                return@setOnClickListener
            }

            if (medicineName.isNotEmpty() && firstDoseTime.isNotEmpty() && dose.isNotEmpty() && compartmentQuantity.isNotEmpty() && frequency.isNotEmpty()) {
                val userId = auth.currentUser?.uid ?: return@setOnClickListener
                val treatmentData = hashMapOf(
                    "treatmentNumber" to treatmentNumber,
                    "medicineName" to medicineName,
                    "firstDoseTime" to firstDoseTime,
                    "frequency" to frequency,
                    "dose" to dose,
                    "compartmentQuantity" to compartmentQuantity
                )

                // Verificar si el tratamiento ya existe
                db.collection("users").document(userId).collection("treatments")
                    .whereEqualTo("treatmentNumber", treatmentNumber)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            // Si el tratamiento ya existe, actualiza el documento
                            val documentId = documents.documents[0].id
                            db.collection("users").document(userId).collection("treatments")
                                .document(documentId)
                                .set(treatmentData)
                                .addOnSuccessListener {
                                    showSuccessAlert()
                                    scheduleNotification(treatmentNumber, medicineName, firstDoseTime, dose, frequency)
                                    sendBluetoothData(firstDoseTime, frequency, medicineName, treatmentNumber, dose)
                                }
                                .addOnFailureListener { e ->
                                    showErrorAlert(e.message)
                                }
                        } else {
                            // Si el tratamiento no existe, crea un nuevo documento
                            db.collection("users").document(userId).collection("treatments")
                                .add(treatmentData)
                                .addOnSuccessListener {
                                    showSuccessAlert()
                                    scheduleNotification(treatmentNumber, medicineName, firstDoseTime, dose, frequency)
                                    sendBluetoothData(firstDoseTime, frequency, medicineName, treatmentNumber, dose)
                                }
                                .addOnFailureListener { e ->
                                    showErrorAlert(e.message)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        showErrorAlert(e.message)
                    }
            } else {
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

    private fun showSuccessAlert() {
        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("Guardado exitosamente")
            .setConfirmClickListener {
                it.dismissWithAnimation()
                val intent = Intent(this, menu::class.java)
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun showErrorAlert(message: String?) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("Error")
            .setContentText(message)
            .show()
    }

    private fun sendBluetoothData(firstDoseTime: String, frequency: String, medicineName: String, treatmentNumber: String, dose: String) {
        val bluetoothMessage = "$firstDoseTime,$frequency,$medicineName,$treatmentNumber,$dose\n"
        try {
            BluetoothManager.sendData(bluetoothMessage)
            Toast.makeText(this, "Se Envio Correctamente por Bluetooth", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error al enviar datos por Bluetooth", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun scheduleNotification(treatmentNumber: String, medicineName: String, firstDoseTime: String, dose: String, frequency: String) {
        // Verificar si la frecuencia es "00:00" o la primera dosis es "00:00", en ese caso no programar la alarma
        if (frequency == "00:00" || firstDoseTime == "00:00") {
            // Cancelar cualquier alarma previa asociada a este tratamiento
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                treatmentNumber.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            // No programar ninguna nueva alarma
            return
        }

        val currentTime = Calendar.getInstance()
        val firstDoseParts = firstDoseTime.split(":").map { it.toInt() }
        val firstDoseCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, firstDoseParts[0])
            set(Calendar.MINUTE, firstDoseParts[1])
            set(Calendar.SECOND, 0)
        }

        val frequencyParts = frequency.split(":").map { it.toInt() }
        val totalFrequencyMinutes = frequencyParts[0] * 60 + frequencyParts[1]

        // Verifica si la primera toma es en el pasado y ajusta la hora de la primera alarma
        if (firstDoseCalendar.before(currentTime)) {
            // Calcula el tiempo en minutos desde la hora configurada hasta la hora actual
            val minutesSinceFirstDose = (currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)) -
                    (firstDoseCalendar.get(Calendar.HOUR_OF_DAY) * 60 + firstDoseCalendar.get(Calendar.MINUTE))

            // Calcula cuántos intervalos de frecuencia han pasado
            val intervalsPassed = minutesSinceFirstDose / totalFrequencyMinutes

            // Ajusta la siguiente alarma al próximo intervalo futuro
            firstDoseCalendar.add(Calendar.MINUTE, (intervalsPassed + 1) * totalFrequencyMinutes)
        }

        // Configurar el intent y el pending intent para la alarma
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("treatmentNumber", treatmentNumber)
            putExtra("medicineName", medicineName)
            putExtra("firstDoseTime", String.format("%02d:%02d", firstDoseCalendar.get(Calendar.HOUR_OF_DAY), firstDoseCalendar.get(Calendar.MINUTE)))
            putExtra("dose", dose)
            putExtra("frequency", frequency)  // Pasa la frecuencia para reprogramar
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            treatmentNumber.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar la alarma para la primera dosis ajustada
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            firstDoseCalendar.timeInMillis,
            pendingIntent
        )
    }




}
