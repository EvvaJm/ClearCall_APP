package com.example.utmapp.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val TAG = "UTM_WEAR"
    private val appUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val vegasGold = Color(0xFFC5B358)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setContent {
            val scope = rememberCoroutineScope()
            var textState by remember { mutableStateOf("") }
            val alphaState = remember { Animatable(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(vegasGold)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = textState,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(alphaState.value)
                )
            }

            // Simple "server" that starts when the UI is ready
            LaunchedEffect(Unit) {
                Log.d(TAG, "LaunchedEffect started")
                startBluetoothServer { msg ->
                    Log.d(TAG, "Message received: $msg")
                    scope.launch {
                        textState = msg
                        vibrate()
                        alphaState.snapTo(1f)
                        delay(5000)
                        alphaState.animateTo(0f, animationSpec = tween(1000))
                        textState = "" // Clear after fade
                    }
                }
            }
        }

        // Check permissions at startup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            }
        }
    }

    private fun startBluetoothServer(onMessage: (String) -> Unit) {
        Thread {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                Log.e(TAG, "Bluetooth Adapter is null")
                return@Thread
            }

            var serverSocket: BluetoothServerSocket? = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permission not granted for server")
                    return@Thread
                }

                Log.d(TAG, "Listening for connections...")
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("UTMApp", appUUID)
                
                while (true) {
                    var socket: BluetoothSocket? = null
                    try {
                        socket = serverSocket?.accept()
                        Log.d(TAG, "Connection accepted!")
                        
                        val input = socket?.inputStream
                        val buffer = ByteArray(1024)

                        while (true) {
                            val bytes = input?.read(buffer) ?: -1
                            if (bytes == -1) {
                                Log.d(TAG, "Stream ended")
                                break
                            }
                            val msg = String(buffer, 0, bytes).trim()
                            if (msg.isNotEmpty()) {
                                Log.d(TAG, "Dispatching message: $msg")
                                runOnUiThread { onMessage(msg) }
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Socket error: ${e.message}")
                    } finally {
                        socket?.close()
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Server error: ${e.message}")
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }.start()
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
