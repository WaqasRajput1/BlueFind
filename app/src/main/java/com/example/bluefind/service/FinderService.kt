package com.example.bluefind.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.bluefind.BlueFindProtocol
import com.example.bluefind.MainActivity
import com.example.bluefind.R
import com.example.bluefind.ui.RingActivity
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Runs as a foreground service so it keeps listening even while the app is
 * backgrounded. Opens one RFCOMM server socket and, in a background thread,
 * repeatedly accepts connections. Any connection that sends
 * [BlueFindProtocol.RING_COMMAND] triggers [RingActivity].
 */
class FinderService : Service() {

    private var serverSocket: BluetoothServerSocket? = null
    private val running = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == BlueFindProtocol.ACTION_STOP_LISTENING) {
            stopListening()
            return START_NOT_STICKY
        }

        startForeground(BlueFindProtocol.NOTIFICATION_ID, buildNotification())
        startListeningThread()
        return START_STICKY
    }

    private fun startListeningThread() {
        if (running.getAndSet(true)) return // already running

        thread(name = "BlueFindListener") {
            try {
                val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
                    ?: return@thread stopSelf()

                serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(
                    BlueFindProtocol.SERVICE_NAME,
                    BlueFindProtocol.SERVICE_UUID
                )

                while (running.get()) {
                    val socket: BluetoothSocket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: IOException) {
                        // Socket closed (stopListening) or adapter turned off.
                        break
                    }
                    handleClient(socket)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission", e)
                stopSelf()
            } catch (e: IOException) {
                Log.e(TAG, "Could not open server socket", e)
                stopSelf()
            }
        }
    }

    private fun handleClient(socket: BluetoothSocket) {
        try {
            socket.inputStream.use { input ->
                val command = input.read() // blocks until the client writes/closes
                if (command == BlueFindProtocol.RING_COMMAND) {
                    launchRingActivity()
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Client connection dropped", e)
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun launchRingActivity() {
        val ringIntent = Intent(this, RingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(ringIntent)
    }

    private fun stopListening() {
        running.set(false)
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification() = NotificationCompat.Builder(this, BlueFindProtocol.NOTIFICATION_CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(getString(R.string.status_listening))
        .setSmallIcon(android.R.drawable.ic_menu_search)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                BlueFindProtocol.NOTIFICATION_CHANNEL_ID,
                "BlueFind listening",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "FinderService"
    }
}
