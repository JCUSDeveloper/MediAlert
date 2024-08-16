package com.example.medialert

import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import android.bluetooth.BluetoothSocket

object BluetoothManager {
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var socket: BluetoothSocket? = null
    private var isConnected: Boolean = false

    fun setOutputStream(outputStream: OutputStream?, socket: BluetoothSocket?) {
        this.outputStream = outputStream
        this.socket = socket
        isConnected = outputStream != null && socket?.isConnected == true
    }

    fun setInputStream(inputStream: InputStream?) {
        this.inputStream = inputStream
    }

    fun closeConnection() {
        try {
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        isConnected = false
    }

    @Throws(IOException::class)
    fun sendData(data: String) {
        outputStream?.let {
            try {
                it.write(data.toByteArray())
                it.flush()  // Asegúrate de que los datos se envíen de inmediato
            } catch (e: IOException) {
                e.printStackTrace()
                closeConnection()  // Maneja la desconexión
                throw IOException("Connection lost")
            }
        } ?: throw IOException("OutputStream not initialized")
    }


    fun receiveData(): String {
        val buffer = StringBuilder()
        val timeout = 5000  // 5 segundos de timeout
        val startTime = System.currentTimeMillis()

        return try {
            while (System.currentTimeMillis() - startTime < timeout) {
                if (inputStream?.available() ?: 0 > 0) {
                    val byte = inputStream?.read()
                    if (byte != null && byte != -1) {
                        val char = byte.toChar()
                        buffer.append(char)
                        if (char == '\n') {
                            break  // Fin de línea, terminamos la lectura
                        }
                    }
                }
            }

            if (buffer.isNotEmpty()) {
                buffer.toString().trim()  // Devuelve la cadena recibida, sin espacios en blanco adicionales
            } else {
                "ERROR: No se recibió respuesta"  // Si no se recibe nada
            }
        } catch (e: IOException) {
            e.printStackTrace()
            closeConnection()  // Maneja la desconexión
            "ERROR: Fallo en la conexión"  // Mensaje de error si hay una excepción
        }
    }

    fun isConnected(): Boolean {
        return isConnected && socket?.isConnected == true && !checkDisconnection()
    }

    private fun checkDisconnection(): Boolean {
        // Si la conexión se pierde, esto capturará la excepción y cerrará la conexión
        try {
            socket?.inputStream?.available()
            return false
        } catch (e: IOException) {
            closeConnection()
            return true
        }
    }
}
