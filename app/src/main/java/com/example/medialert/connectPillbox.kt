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
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class connectPillbox : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 101
    private val REQUEST_ENABLE_BT = 1
    private lateinit var deviceList: ListView
    private lateinit var enviar: Button
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val deviceListAdapter by lazy {
        DeviceListAdapter(this, mutableListOf())
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore


    private val handler = Handler(Looper.getMainLooper()) { message ->
        // PRUEBA ENVIO DE DATOS
        val receivedData = message.obj as String
        true
    }

    // BroadcastReceiver para recibir dispositivos encontrados durante la búsqueda
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

    // BroadcastReceiver para detectar cambios en el estado del Bluetooth
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_OFF) {
                        // Cambiar el ícono de la luz de estado a la luz roja
                        val statusLight = findViewById<ImageView>(R.id.connection_status_light)
                        val connectionStatus = findViewById<TextView>(R.id.connection_status)
                        statusLight.setImageResource(R.drawable.red_light)
                        connectionStatus.text = "Estado: Desconectado"

                        // Cerrar la conexión Bluetooth
                        BluetoothManager.closeConnection()
                    }
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

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val statusLight = findViewById<ImageView>(R.id.connection_status_light)
        val connectionStatus = findViewById<TextView>(R.id.connection_status)

        if (BluetoothManager.isConnected()) {
            statusLight.setImageResource(R.drawable.successfull_bluetooth)
            connectionStatus.text = "Estado: Conectado"
        } else {
            statusLight.setImageResource(R.drawable.red_light)
            connectionStatus.text = "Estado: Desconectado"
        }

        checkPermissions()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.connectPillbox)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.back_button).setOnClickListener {
            val intent = Intent(this, menu::class.java)
            startActivity(intent)
        }

        // Registrar el BroadcastReceiver para cambios en el estado del Bluetooth
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

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
                Toast.makeText(this, "No se han concedido los permisos necesarios", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startBluetoothDiscovery() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_LONG).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos necesarios no concedidos", Toast.LENGTH_LONG).show()
                return
            }

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
            Toast.makeText(this, "Iniciando búsqueda de dispositivos", Toast.LENGTH_SHORT).show()

            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            registerReceiver(receiver, filter)
        }
    }

    private fun connectToDevice(deviceAddress: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de conexión Bluetooth no concedido", Toast.LENGTH_LONG).show()
            return
        }

        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        device?.let {
            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()

                Toast.makeText(this, "Conectado a $deviceAddress", Toast.LENGTH_SHORT).show()

                val outputStream = socket.outputStream
                val inputStream = socket.inputStream

                // Guardar el outputStream y socket en el singleton
                BluetoothManager.setOutputStream(outputStream, socket)
                BluetoothManager.setInputStream(inputStream)

                // Cambiar el ícono de la luz de estado a la palomita
                val statusLight = findViewById<ImageView>(R.id.connection_status_light)
                val connectionStatus = findViewById<TextView>(R.id.connection_status)
                statusLight.setImageResource(R.drawable.successfull_bluetooth)
                connectionStatus.text = "Estado: Conectado"

                // Iniciar un hilo para monitorear la conexión
                Thread {
                    while (BluetoothManager.isConnected()) {
                        if (!BluetoothManager.isConnected()) {
                            runOnUiThread {
                                statusLight.setImageResource(R.drawable.red_light)
                                connectionStatus.text = "Estado: Desconectado"
                                BluetoothManager.closeConnection()
                            }
                            break
                        }
                        Thread.sleep(1000)  // Verificación cada segundo
                    }
                }.start()

                val readThread = Thread {
                    receiveData(inputStream)
                }
                readThread.start()

            } catch (e: IOException) {
                Toast.makeText(this, "No se pudo conectar a $deviceAddress", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        } ?: run {
            Toast.makeText(this, "No se encontró el dispositivo $deviceAddress", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendData(outputStream: OutputStream, data: String) {
        try {
            outputStream.write(data.toByteArray())
        } catch (e: IOException) {
            Toast.makeText(this, "Error al enviar datos", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun receiveData(inputStream: InputStream) {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (true) {
            try {
                bytes = inputStream.read(buffer)
                val readMessage = String(buffer, 0, bytes)
                handler.obtainMessage(0, readMessage).sendToTarget()
            } catch (e: IOException) {
                e.printStackTrace()
                break
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBluetoothDiscovery()
            } else {
                Toast.makeText(this, "Bluetooth no habilitado", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        unregisterReceiver(bluetoothStateReceiver) // Asegúrate de anular el registro del nuevo BroadcastReceiver
    }
}
