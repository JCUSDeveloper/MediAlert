package com.example.medialert

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import java.io.IOException
import java.util.UUID

class connectPillbox : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 101
    private val REQUEST_ENABLE_BT = 1
    private lateinit var deviceList: ListView
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val deviceListAdapter by lazy {
        DeviceListAdapter(this, mutableListOf())
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceName = if (ActivityCompat.checkSelfPermission(
                                this@connectPillbox,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            it.name ?: "Unknown Device"
                        } else {
                            "Unknown Device"
                        }
                        val deviceAddress = it.address
                        val deviceInfo = "$deviceName - $deviceAddress"
                        deviceListAdapter.add(deviceInfo)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Toast.makeText(this@connectPillbox, "Búsqueda de dispositivos finalizada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_connect_pillbox)
        deviceList = findViewById(R.id.device_list)
        deviceList.adapter = deviceListAdapter
        deviceList.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val deviceInfo = parent.getItemAtPosition(position) as String
            val deviceAddress = deviceInfo.split(" - ").last()
            connectToDevice(deviceAddress)
        }
        checkPermissions()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.connectPillbox)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSION_REQUEST_CODE)
        } else {
            startBluetoothDiscovery()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBluetoothDiscovery()
            } else {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Permisos necesarios")
                    .setContentText("No se han concedido los permisos necesarios")
                    .show()
            }
        }
    }

    private fun startBluetoothDiscovery() {
        if (bluetoothAdapter == null) {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Bluetooth no disponible")
                .setContentText("Bluetooth no está disponible en este dispositivo")
                .show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Permisos necesarios")
                    .setContentText("Permisos necesarios no concedidos")
                    .show()
                return
            }

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Iniciando búsqueda")
                .setContentText("Iniciando búsqueda de dispositivos")
                .show()

            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            registerReceiver(receiver, filter)
        }
    }

    private fun connectToDevice(deviceAddress: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Permiso denegado")
                .setContentText("Permiso de conexión Bluetooth no concedido")
                .show()
            return
        }

        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        device?.let {
            try {
                // Reemplaza con el UUID adecuado para tu dispositivo
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Ejemplo para SPP
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Conectado")
                    .setContentText("Conectado a $deviceAddress")
                    .show()

                // Cierra el socket después de usarlo
                socket.close()
            } catch (e: IOException) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error de conexión")
                    .setContentText("No se pudo conectar a $deviceAddress")
                    .show()
                e.printStackTrace()
            }
        } ?: run {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Dispositivo no encontrado")
                .setContentText("No se encontró el dispositivo $deviceAddress")
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBluetoothDiscovery()
            } else {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Bluetooth no habilitado")
                    .setContentText("Bluetooth no habilitado")
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
