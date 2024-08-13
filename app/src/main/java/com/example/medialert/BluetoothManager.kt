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
            it.write(data.toByteArray())
        } ?: throw IOException("OutputStream not initialized")
    }

    fun receiveData(): String {
        val buffer = ByteArray(1024)
        return try {
            val bytes = inputStream?.read(buffer) ?: 0
            String(buffer, 0, bytes)
        } catch (e: IOException) {
            e.printStackTrace()
            closeConnection()  // Handle disconnection
            ""
        }
    }


    fun isConnected(): Boolean {
        return isConnected && socket?.isConnected == true
    }
}
