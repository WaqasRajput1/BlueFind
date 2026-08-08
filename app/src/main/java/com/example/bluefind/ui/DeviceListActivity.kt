package com.example.bluefind.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluefind.BlueFindProtocol
import com.example.bluefind.R
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Shows the phone's already-paired Bluetooth devices. Tapping one opens a
 * client RFCOMM connection and writes [BlueFindProtocol.RING_COMMAND], which
 * makes the other phone (if it's running BlueFind and listening) ring.
 *
 * Devices must already be paired via Android's Bluetooth settings — this
 * screen does not do discovery/pairing itself.
 */
class DeviceListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        title = getString(R.string.select_device)

        recyclerView = findViewById(R.id.recyclerDevices)
        emptyView = findViewById(R.id.textEmpty)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val paired: Set<BluetoothDevice> = try {
            adapter?.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            emptySet()
        }

        if (paired.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            recyclerView.adapter = DeviceAdapter(paired.toList()) { device -> sendRing(device) }
        }
    }

    private fun sendRing(device: BluetoothDevice) {
        Toast.makeText(this, getString(R.string.connecting), Toast.LENGTH_SHORT).show()

        thread(name = "BlueFindClient") {
            var socket: BluetoothSocket? = null
            try {
                BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
                socket = device.createInsecureRfcommSocketToServiceRecord(BlueFindProtocol.SERVICE_UUID)
                socket.connect()
                socket.outputStream.write(BlueFindProtocol.RING_COMMAND)
                socket.outputStream.flush()
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.ring_sent), Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.ring_failed), Toast.LENGTH_LONG).show()
                }
            } catch (e: SecurityException) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.ring_failed), Toast.LENGTH_LONG).show()
                }
            } finally {
                try {
                    socket?.close()
                } catch (_: IOException) {
                }
            }
        }
    }

    private class DeviceAdapter(
        private val devices: List<BluetoothDevice>,
        private val onClick: (BluetoothDevice) -> Unit
    ) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.textDeviceName)
            val address: TextView = view.findViewById(R.id.textDeviceAddress)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            holder.name.text = try {
                device.name ?: device.address
            } catch (e: SecurityException) {
                device.address
            }
            holder.address.text = device.address
            holder.itemView.setOnClickListener { onClick(device) }
        }

        override fun getItemCount() = devices.size
    }
}
