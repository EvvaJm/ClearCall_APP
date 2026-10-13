package com.example.utmapp

import android.Manifest
import android.content.pm.PackageManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var statusText: TextView
    private lateinit var pitchingGrid: GridLayout
    private lateinit var battingGrid: GridLayout
    private lateinit var baseRunningGrid: GridLayout
    private lateinit var playsGrid: GridLayout
    
    private var selectedPitch: String? = null
    private var selectedPlay: String? = null

    private val appUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        pitchingGrid = findViewById(R.id.pitchingGrid)
        battingGrid = findViewById(R.id.battingGrid)
        baseRunningGrid = findViewById(R.id.baseRunningGrid)
        playsGrid = findViewById(R.id.playsGrid)

        showPitchTypeSelection()
        setupBattingButtons()
        setupBaseRunningButtons()
        showPlaysInitialSelection()
        
        ensureBluetoothPermission()
    }

    // --- PITCHING ---
    private fun showPitchTypeSelection() {
        selectedPitch = null
        pitchingGrid.removeAllViews()
        pitchingGrid.columnCount = 2
        val pitches = listOf("Fastball", "Changeup", "Screwball", "Curveball", "Riseball", "Dropball")
        pitches.forEach { pitch ->
            val btn = createGoldButton(pitch)
            btn.setOnClickListener {
                selectedPitch = pitch
                showPitchLocationSelection()
            }
            pitchingGrid.addView(btn)
        }
    }

    private fun showPitchLocationSelection() {
        pitchingGrid.removeAllViews()
        pitchingGrid.columnCount = 2
        val backBtn = createBackButton { showPitchTypeSelection() }
        pitchingGrid.addView(backBtn)

        val locations = listOf("High Inside", "Low Inside", "High Outside", "Low Outside")
        locations.forEach { location ->
            val btn = createGoldButton(location)
            btn.setOnClickListener {
                sendMessage("$location $selectedPitch")
                showPitchTypeSelection()
            }
            pitchingGrid.addView(btn)
        }
    }

    // --- BATTING ---
    private fun setupBattingButtons() {
        battingGrid.removeAllViews()
        battingGrid.columnCount = 2
        val options = listOf("Bunt", "Slap Bunt", "Sacrifice Bunt", "Fake Bunt", "Swing")
        options.forEach { option ->
            val btn = createGoldButton(option)
            btn.setOnClickListener { sendMessage(option) }
            battingGrid.addView(btn)
        }
    }

    // --- BASE RUNNING ---
    private fun setupBaseRunningButtons() {
        baseRunningGrid.removeAllViews()
        baseRunningGrid.columnCount = 2
        val options = listOf("Steal", "Delayed Steal", "Nothing")
        options.forEach { option ->
            val btn = createGoldButton(option)
            btn.setOnClickListener { sendMessage(option) }
            baseRunningGrid.addView(btn)
        }
    }

    // --- PLAYS (FIELDING) ---
    private fun showPlaysInitialSelection() {
        selectedPlay = null
        playsGrid.removeAllViews()
        playsGrid.columnCount = 1
        val btn = createGoldButton("Horns Play")
        btn.setOnClickListener {
            selectedPlay = "Horns Play"
            showBaseballDiamond()
        }
        playsGrid.addView(btn)
    }

    private fun showBaseballDiamond() {
        playsGrid.removeAllViews()
        
        val diamondLayout = FrameLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.MATCH_PARENT
                height = 700
                setMargins(0,0,0,0)
            }
            setBackgroundResource(R.drawable.diamond_background)
        }

        // Back Button (Top Left)
        val backBtn = createBackButton { showPlaysInitialSelection() }.apply {
            layoutParams = FrameLayout.LayoutParams(250, 100).apply { gravity = Gravity.TOP or Gravity.START }
        }
        diamondLayout.addView(backBtn)

        // Positions
        addDiamondPosition(diamondLayout, "P", 0.5f, 0.6f)
        addDiamondPosition(diamondLayout, "SS", 0.35f, 0.4f)
        addDiamondPosition(diamondLayout, "2", 0.65f, 0.4f)
        addDiamondPosition(diamondLayout, "1", 0.85f, 0.6f)
        addDiamondPosition(diamondLayout, "3", 0.15f, 0.6f)

        playsGrid.addView(diamondLayout)
    }

    private fun addDiamondPosition(container: FrameLayout, label: String, xPercent: Float, yPercent: Float) {
        val btn = createGoldButton(label).apply {
            layoutParams = FrameLayout.LayoutParams(130, 130)
            textSize = 12f
        }
        
        btn.setOnClickListener {
            val positionName = when(label) {
                "1" -> "1st"
                "2" -> "2nd"
                "3" -> "3rd"
                "SS" -> "Short Stop"
                "P" -> "Pitcher"
                else -> label
            }
            sendMessage("$selectedPlay $positionName")
            showPlaysInitialSelection()
        }

        container.post {
            val x = (container.width * xPercent) - 65
            val y = (container.height * yPercent) - 65
            btn.x = x
            btn.y = y
        }
        
        container.addView(btn)
    }

    // --- HELPERS ---
    private fun createGoldButton(label: String): Button {
        return Button(this).apply {
            text = label
            setBackgroundResource(R.drawable.button_gold_selector)
            setTextColor(Color.BLACK)
            transformationMethod = null
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 160
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(12, 12, 12, 12)
            }
        }
    }

    private fun createBackButton(onClick: () -> Unit): Button {
        return Button(this).apply {
            text = "← Back"
            setBackgroundColor(Color.LTGRAY)
            setTextColor(Color.BLACK)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(0, 2, 1f)
                setMargins(12, 12, 12, 12)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun updateStatus(status: String) {
        runOnUiThread { statusText.text = "Status: $status" }
    }

    private fun ensureBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            } else { connectToWatch() }
        } else { connectToWatch() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) connectToWatch()
    }

    private fun connectToWatch() {
        Thread {
            try {
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    updateStatus("BT Off")
                    return@Thread
                }
                val pairedDevices = if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) bluetoothAdapter.bondedDevices else null
                val device: BluetoothDevice? = pairedDevices?.firstOrNull { it.name == "Stefanie's Watch8" }
                        ?: pairedDevices?.firstOrNull { it.name?.contains("Watch", ignoreCase = true) == true }
                        ?: if (pairedDevices?.size == 1) pairedDevices.first() else null

                if (device == null) {
                    updateStatus("No Watch")
                    return@Thread
                }
                updateStatus("Connecting...")
                bluetoothSocket?.close()
                bluetoothSocket = device.createRfcommSocketToServiceRecord(appUUID)
                bluetoothSocket?.connect()
                updateStatus("Connected")
            } catch (e: IOException) {
                updateStatus("Retrying...")
                Thread.sleep(3000)
                connectToWatch()
            }
        }.start()
    }

    private fun sendMessage(message: String) {
        if (bluetoothSocket == null || !bluetoothSocket!!.isConnected) {
            updateStatus("Not Connected")
            connectToWatch()
            return
        }
        Thread {
            try {
                bluetoothSocket?.outputStream?.write(message.toByteArray())
            } catch (e: IOException) { updateStatus("Send Error") }
        }.start()
    }
}
