package com.example.medialert

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val treatmentNumber = intent.getStringExtra("treatmentNumber")
        val medicineName = intent.getStringExtra("medicineName")
        val firstDoseTime = intent.getStringExtra("firstDoseTime")
        val dose = intent.getStringExtra("dose")
        val frequency = intent.getStringExtra("frequency")

        // Crear la notificación
        val builder = NotificationCompat.Builder(context, "treatment_success_channel")
            .setSmallIcon(R.drawable.logo) // Asegúrate de tener un ícono
            .setContentTitle("Es hora de tu tratamiento")
            .setContentText("Tratamiento No.: $treatmentNumber\nMedicamento: $medicineName\nDosis: $dose\n")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Mostrar la notificación
        with(NotificationManagerCompat.from(context)) {
            notify(treatmentNumber.hashCode(), builder.build())
        }

        // Verificar si la frecuencia es "00:00", en ese caso no reprogramar la alarma
        if (frequency == "00:00" || firstDoseTime == "00:00" ) {
            // Desactivar la alarma si la frecuencia es "00:00"
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                treatmentNumber.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            return
        }

        // Reprogramar la próxima notificación según la frecuencia
        val intervalMillis = parseFrequencyToMillis(frequency ?: "00:00")
        val nextAlarmTime = System.currentTimeMillis() + intervalMillis

        val newIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("treatmentNumber", treatmentNumber)
            putExtra("medicineName", medicineName)
            putExtra("firstDoseTime", firstDoseTime)
            putExtra("dose", dose)
            putExtra("frequency", frequency)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            treatmentNumber.hashCode(),
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextAlarmTime,
            pendingIntent
        )
    }

    private fun parseFrequencyToMillis(frequency: String): Long {
        val timeParts = frequency.split(":").map { it.toInt() }
        val hoursInMillis = timeParts[0].toLong() * 60 * 60 * 1000
        val minutesInMillis = timeParts[1].toLong() * 60 * 1000
        return hoursInMillis + minutesInMillis
    }
}

