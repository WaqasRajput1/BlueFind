package com.example.bluefind

import java.util.UUID

/**
 * Shared constants for the tiny BlueFind Bluetooth protocol.
 *
 * Both phones run the same app. One side starts [service.FinderService], which
 * opens an RFCOMM server socket and waits. The other side looks up a paired
 * device, opens a client RFCOMM socket to the same UUID, and writes the
 * [RING_COMMAND] byte. When the listening phone reads that byte it triggers
 * an alarm sound + vibration + full-screen [ui.RingActivity] so it can be found.
 */
object BlueFindProtocol {
    // Fixed app-specific UUID. Must match on both the server and client socket.
    val SERVICE_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    const val SERVICE_NAME = "BlueFindService"

    // Single-byte command sent over the RFCOMM stream to trigger the alarm.
    const val RING_COMMAND: Int = 0x52 // ASCII 'R'

    const val NOTIFICATION_CHANNEL_ID = "bluefind_listening"
    const val NOTIFICATION_ID = 1001

    const val ACTION_RING_RECEIVED = "com.example.bluefind.ACTION_RING_RECEIVED"
    const val ACTION_STOP_LISTENING = "com.example.bluefind.ACTION_STOP_LISTENING"
}
