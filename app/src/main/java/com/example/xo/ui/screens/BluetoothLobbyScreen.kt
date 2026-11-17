package com.example.xo.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
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
    onConnected: (BluetoothSocketChannel, Boolean, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val adapter = remember(manager) { manager.adapter }
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selected by remember { mutableStateOf<BluetoothDevice?>(null) }
    var message by remember { mutableStateOf("Pair the two phones in Android Bluetooth settings first.") }
    var busy by remember { mutableStateOf(false) }

    fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun refreshDevices() {
        if (adapter == null || !hasConnectPermission()) return
        devices = runCatching { adapter.bondedDevices.orEmpty().toList().sortedBy { it.name ?: it.address } }
            .getOrDefault(emptyList())
        if (selected !in devices) selected = devices.firstOrNull()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) refreshDevices() else message = "Bluetooth permission is required to continue."
    }

    LaunchedEffect(adapter) {
        if (adapter == null) message = "This device does not support Bluetooth."
        else if (hasConnectPermission()) refreshDevices()
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun connectAsHost() {
        if (adapter == null || !hasConnectPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        if (!adapter.isEnabled) {
            message = "Turn on Bluetooth from Android quick settings, then try again."
            return
        }
        scope.launch {
            busy = true
            message = "Waiting for the other player to connect…"
            val host = BluetoothHostManager(adapter)
            try {
                val socket = host.awaitPlayer("XO3D Multiplayer", XO_SERVICE_ID)
                val channel = BluetoothSocketChannel(socket)
                val matchId = UUID.randomUUID().toString()
                channel.send(BluetoothSignal(matchId = matchId, messageId = UUID.randomUUID().toString(), type = SignalType.CONNECT))
                withTimeout(20_000) {
                    val reply = channel.receive()
                    check(reply.type == SignalType.CONNECTED && reply.matchId == matchId) { "Invalid player handshake." }
                }
                onConnected(channel, true, matchId)
            } catch (error: Exception) {
                host.close()
                message = error.message ?: "Could not create the Bluetooth game."
                busy = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun joinDevice(device: BluetoothDevice) {
        if (adapter == null || !hasConnectPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        if (!adapter.isEnabled) {
            message = "Turn on Bluetooth from Android quick settings, then try again."
            return
        }
        scope.launch {
            busy = true
            message = "Connecting to ${device.name ?: device.address}…"
            val client = BluetoothClientManager(adapter)
            try {
                val socket = client.connect(device, XO_SERVICE_ID)
                val channel = BluetoothSocketChannel(socket)
                val hello = withTimeout(20_000) { channel.receive() }
                check(hello.type == SignalType.CONNECT && hello.matchId.isNotBlank()) { "The other device did not start an XO3D game." }
                channel.send(BluetoothSignal(matchId = hello.matchId, messageId = UUID.randomUUID().toString(), type = SignalType.CONNECTED))
                onConnected(channel, false, hello.matchId)
            } catch (error: Exception) {
                client.close()
                message = error.message ?: "Could not connect to the selected device."
                busy = false
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Text("Bluetooth multiplayer", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Button(onClick = { connectAsHost() }, enabled = !busy && adapter != null, modifier = Modifier.fillMaxWidth()) {
                Text("Create game  •  Wait for player")
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Paired devices", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { refreshDevices() }, enabled = !busy) { Text("Refresh") }
            }
            if (devices.isEmpty()) {
                Text("No paired devices found. Pair the phones in Android Bluetooth settings.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(devices, key = { it.address }) { device ->
                        val isSelected = selected?.address == device.address
                        ListItem(
                            headlineContent = { Text(device.name ?: "Bluetooth device") },
                            supportingContent = { Text(device.address) },
                            trailingContent = { if (isSelected) Text("Selected", color = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable { selected = device }
                        )
                        HorizontalDivider()
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { selected?.let { joinDevice(it) } },
                    enabled = !busy && selected != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Join selected device") }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onBack, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}
