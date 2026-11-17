package com.example.xo.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.xo.bluetooth.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

private val XO_SERVICE_ID: UUID = UUID.fromString("9f7c2d10-6b3a-4e8f-a1c5-3d2e7f904b61")

@Composable
fun BluetoothLobbyScreen(
    onConnected: (BluetoothSocketChannel, Boolean, String, suspend () -> BluetoothSocketChannel) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val adapter = remember(manager) { manager.adapter }
    val scope = rememberCoroutineScope()
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var nearbyDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Pairing is optional: nearby devices can now be discovered here.") }
    var busy by remember { mutableStateOf(false) }
    var discovering by remember { mutableStateOf(false) }

    fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun refreshPaired() {
        if (adapter == null || !hasPermissions()) return
        pairedDevices = runCatching { adapter.bondedDevices.orEmpty().toList().sortedBy { it.name ?: it.address } }
            .getOrDefault(emptyList())
        if (selectedAddress == null) selectedAddress = pairedDevices.firstOrNull()?.address
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            refreshPaired()
            message = "Permission granted. Search for a nearby device or create a game."
        } else {
            message = "Bluetooth permissions are required to discover and connect to devices."
        }
    }

    fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        } else emptyArray()
        if (permissions.isEmpty()) {
            refreshPaired()
            return
        }
        permissionLauncher.launch(permissions)
    }

    fun startHost() {
        if (adapter == null || !hasPermissions()) {
            requestPermissions()
            return
        }
        if (!adapter.isEnabled) {
            message = "Turn on Bluetooth, then try again."
            return
        }
        scope.launch {
            busy = true
            message = "Waiting for the other player to connect…"
            val host = BluetoothHostManager(adapter)
            val matchId = UUID.randomUUID().toString()
            suspend fun acceptPlayer(): BluetoothSocketChannel {
                val socket = host.awaitPlayer("XO3D Multiplayer", XO_SERVICE_ID)
                val channel = BluetoothSocketChannel(socket)
                channel.send(BluetoothSignal(matchId = matchId, messageId = UUID.randomUUID().toString(), type = SignalType.CONNECT))
                withTimeout(20_000) {
                    val reply = channel.receive()
                    check(reply.type == SignalType.CONNECTED && reply.matchId == matchId) { "Invalid player handshake." }
                }
                return channel
            }
            try {
                val channel = acceptPlayer()
                onConnected(channel, true, matchId) { acceptPlayer() }
            } catch (error: Exception) {
                host.close()
                message = error.message ?: "Could not create the Bluetooth game."
                busy = false
            }
        }
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode > 0) startHost()
        else message = "The phone must be discoverable so the other player can find it."
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (adapter == null) {
            message = "This device does not support Bluetooth."
            return
        }
        if (!hasPermissions()) {
            requestPermissions()
            return
        }
        if (!adapter.isEnabled) {
            message = "Turn on Bluetooth, then search again."
            return
        }
        nearbyDevices = emptyList()
        discovering = true
        message = "Searching for nearby Bluetooth devices…"
        if (!adapter.startDiscovery()) {
            discovering = false
            message = "Could not start discovery. Turn Bluetooth off and on, then retry."
        }
    }

    DisposableEffect(adapter) {
        if (adapter == null) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (device != null && nearbyDevices.none { it.address == device.address } &&
                            pairedDevices.none { it.address == device.address }) {
                            nearbyDevices = (nearbyDevices + device).sortedBy { it.name ?: it.address }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        discovering = false
                        message = if (nearbyDevices.isEmpty()) "Search finished. No new devices found; try again or choose a paired device."
                        else "Found ${nearbyDevices.size} nearby device(s). Select one to connect."
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            if (hasPermissions()) runCatching { adapter.cancelDiscovery() }
        }
    }

    LaunchedEffect(adapter) {
        if (adapter == null) message = "This device does not support Bluetooth."
        else if (hasPermissions()) refreshPaired()
        else requestPermissions()
    }

    @SuppressLint("MissingPermission")
    fun joinDevice(device: BluetoothDevice) {
        if (adapter == null || !hasPermissions()) {
            requestPermissions()
            return
        }
        if (!adapter.isEnabled) {
            message = "Turn on Bluetooth, then try again."
            return
        }
        runCatching { adapter.cancelDiscovery() }
        scope.launch {
            busy = true
            message = "Connecting to ${device.name ?: device.address}…"
            val client = BluetoothClientManager(adapter)
            suspend fun connectToHost(expectedMatchId: String? = null): Pair<BluetoothSocketChannel, String> {
                val socket = client.connect(device, XO_SERVICE_ID)
                val channel = BluetoothSocketChannel(socket)
                val hello = withTimeout(20_000) { channel.receive() }
                check(hello.type == SignalType.CONNECT && hello.matchId.isNotBlank()) { "The other device did not start an XO3D game." }
                if (expectedMatchId != null) check(hello.matchId == expectedMatchId) { "The host started a different game." }
                channel.send(BluetoothSignal(matchId = hello.matchId, messageId = UUID.randomUUID().toString(), type = SignalType.CONNECTED))
                return channel to hello.matchId
            }
            try {
                val (channel, matchId) = connectToHost()
                onConnected(channel, false, matchId) { connectToHost(matchId).first }
            } catch (error: Exception) {
                client.close()
                message = error.message ?: "Could not connect to the selected device."
                busy = false
            }
        }
    }

    val allDevices = (pairedDevices + nearbyDevices).distinctBy { it.address }
    val selectedDevice = allDevices.firstOrNull { it.address == selectedAddress }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Text("Bluetooth multiplayer", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    if (!hasPermissions()) requestPermissions()
                    else discoverableLauncher.launch(
                        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    )
                },
                enabled = !busy && adapter != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create game • Make this phone discoverable") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { startDiscovery() },
                enabled = !busy && !discovering && adapter != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (discovering) "Searching…" else "Search nearby devices") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Available devices", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { refreshPaired() }, enabled = !busy) { Text("Refresh") }
            }
            if (allDevices.isEmpty()) {
                Text("No devices yet. Make the other phone discoverable, then search.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(allDevices, key = { it.address }) { device ->
                        val isSelected = selectedAddress == device.address
                        val isPaired = pairedDevices.any { it.address == device.address }
                        ListItem(
                            headlineContent = { Text(device.name ?: "Bluetooth device") },
                            supportingContent = { Text(if (isPaired) "Paired • ${device.address}" else "Nearby • ${device.address}") },
                            trailingContent = { if (isSelected) Text("Selected", color = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { selectedAddress = device.address }
                        )
                        HorizontalDivider()
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { selectedDevice?.let { joinDevice(it) } },
                    enabled = !busy && selectedDevice != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Join selected device") }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBack, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}
