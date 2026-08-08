package com.example.bluefind

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bluefind.service.FinderService
import com.example.bluefind.ui.DeviceListActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var findableButton: Button
    private var isListening = false

    // What we were trying to do when we had to ask for permission.
    private var pendingAction: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            pendingAction?.invoke()
        } else {
            Toast.makeText(this, "BlueFind needs Bluetooth permission to work", Toast.LENGTH_LONG).show()
        }
        pendingAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.textStatus)
        findableButton = findViewById(R.id.btnMakeFindable)
        val findOtherButton: Button = findViewById(R.id.btnFindOther)

        findableButton.setOnClickListener {
            withBluetoothPermission {
                if (isListening) stopListening() else startListening()
            }
        }

        findOtherButton.setOnClickListener {
            withBluetoothPermission {
                startActivity(Intent(this, DeviceListActivity::class.java))
            }
        }

        updateStatus()
    }

    private fun startListening() {
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Please turn on Bluetooth first", Toast.LENGTH_LONG).show()
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, FinderService::class.java))
        isListening = true
        updateStatus()
    }

    private fun stopListening() {
        val intent = Intent(this, FinderService::class.java).apply {
            action = BlueFindProtocol.ACTION_STOP_LISTENING
        }
        startService(intent)
        isListening = false
        updateStatus()
    }

    private fun updateStatus() {
        statusText.text = getString(if (isListening) R.string.status_listening else R.string.status_idle)
        findableButton.text = getString(if (isListening) R.string.stop_listening else R.string.btn_make_findable)
    }

    private fun withBluetoothPermission(action: () -> Unit) {
        val needed = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            action()
        } else {
            pendingAction = action
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun requiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
