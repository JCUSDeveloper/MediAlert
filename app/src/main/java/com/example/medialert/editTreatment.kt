package com.example.medialert

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.util.Calendar

class editTreatment : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var treatmentId: String? = null
    private var selectedCompartment: String? = null
    private var previousCompartment: String? = null
    private lateinit var treatmentNumberSpinner: Spinner // Declara aquí


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

        treatmentNumberSpinner = findViewById<Spinner>(R.id.treatment_number_spinner)
        val medicineNameInput = findViewById<EditText>(R.id.medicine_name_input)
        val timeEditText = findViewById<EditText>(R.id.timeEditText)
        val frequencyInput = findViewById<EditText>(R.id.frequency_input)
        val doseInput = findViewById<EditText>(R.id.dose_input)
        val compartmentQuantityInput = findViewById<EditText>(R.id.compartment_quantity_input)

        // Listener para el spinner de número de compartimiento
        treatmentNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                previousCompartment = selectedCompartment  // Guarda el valor actual antes de cambiarlo
                selectedCompartment = parent?.getItemAtPosition(position).toString()
                loadTreatmentData(selectedCompartment ?: "", medicineNameInput, timeEditText, frequencyInput, doseInput, compartmentQuantityInput)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No hacer nada
            }
        }


        // Asignar TimePickerDialog a los campos de texto de tiempo
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

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val medicineName = medicineNameInput.text.toString().trim().replace("\\s+".toRegex(), " ")
            val firstDoseTime = timeEditText.text.toString()
            val frequency = frequencyInput.text.toString()
            val dose = doseInput.text.toString()
            val compartmentQuantity = compartmentQuantityInput.text.toString()

            // Validar que dose y compartmentQuantity no sean ceros
            if (dose == "0" || compartmentQuantity == "0" || dose == "00" || compartmentQuantity == "00") {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Valor inválido")
                    .setContentText("La dosis y la cantidad de compartimentos no pueden ser cero.")
                    .show()
                return@setOnClickListener
            }

            if (medicineName.isNotEmpty() && firstDoseTime.isNotEmpty() && dose.isNotEmpty() && compartmentQuantity.isNotEmpty() && frequency.isNotEmpty()) {
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
                        .setConfirmClickListener { dialog ->
                            treatmentNumberSpinner.onItemSelectedListener = null // Deshabilitar el listener
                            treatmentNumberSpinner.setSelection(
                                (treatmentNumberSpinner.adapter as ArrayAdapter<String>)
                                    .getPosition(previousCompartment)
                            )
                            treatmentNumberSpinner.post { // Rehabilitar el listener después de un breve retraso
                                treatmentNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                        previousCompartment = selectedCompartment  // Guarda el valor actual antes de cambiarlo
                                        selectedCompartment = parent?.getItemAtPosition(position).toString()
                                        loadTreatmentData(selectedCompartment ?: "", medicineNameInput, timeEditText, frequencyInput, doseInput, compartmentQuantityInput)
                                    }

                                    override fun onNothingSelected(parent: AdapterView<*>?) {
                                        // No hacer nada
                                    }
                                }
                            }
                            dialog.dismissWithAnimation() // Cerrar el diálogo
                        }
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

                    val bluetoothMessage = "$firstDoseTime,$frequency,$medicineName,$selectedCompartment,$dose"
                    try {
                        BluetoothManager.sendData(bluetoothMessage)
                        Toast.makeText(this, "Se Envio Correctamente por Bluetooth", Toast.LENGTH_SHORT).show()
                    } catch (e: IOException) {
                        Toast.makeText(this, "Error al enviar datos por Bluetooth", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()

                    }
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

    private fun showHourPickerDialog(context: Context, onHourPicked: (Int) -> Unit) {
        val numberPicker = NumberPicker(context)
        numberPicker.minValue = 0
        numberPicker.maxValue = 23
        numberPicker.value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Selecciona la hora")
        builder.setView(numberPicker)
        builder.setPositiveButton("OK") { _, _ ->
            onHourPicked(numberPicker.value)
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }
}
