package com.sid.smartmoisture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sid.smartmoisture.R
import com.sid.smartmoisture.core.ScannedDevice
import com.sid.smartmoisture.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(vm: MainViewModel, onConnected: () -> Unit) {
    val devices by vm.devices.collectAsState()
    val connected by vm.connected.collectAsState()
    var busy by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(connected) { if (connected == busy || connected == null) busy = null }
    LaunchedEffect(Unit) { vm.startScan() }
    LaunchedEffect(busy) {
        val addr = busy ?: return@LaunchedEffect

        delay(10000)
        if (busy == addr && connected != addr) busy = null
    }
    DisposableEffect(Unit) { onDispose { vm.stopScan() } }

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text("Select Device")
                if (connected != null) Text(
                    "Connected: $connected", style = MaterialTheme.typography.bodySmall
                )
            }
        }, navigationIcon = {
            IconButton(onClick = onConnected) {
                Icon(
                    painter = painterResource(id = R.drawable.nav_arrow),
                    contentDescription = "Back",
                    modifier = Modifier.height(24.dp)
                )
            }
        }, actions = {
            IconButton(onClick = { vm.startScan(true) }) {
                Icon(
                    painter = painterResource(id = R.drawable.refresh),
                    contentDescription = "Rescan",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(18.dp)
                )
            }
        })
    }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
        ) {
            ManualConnectRow(
                isConnected = connected != null,
                onConnect = { address -> vm.connectTo(address) },
                onManualDisconnect = { vm.disconnect() })
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices, key = { it.address }) { dev ->
                    DeviceRow(
                        dev = dev,
                        isConnected = dev.address == connected,
                        isBusy = dev.address == busy,
                        onConnect = {
                            busy = dev.address
                            vm.connectTo(dev.address)
                        },
                        onDisconnect = {
                            busy = dev.address
                            vm.disconnect()
                        })
                }
            }
            if (devices.isEmpty()) Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Scanning for devices...", style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ManualConnectRow(
    isConnected: Boolean, onConnect: (String) -> Unit, onManualDisconnect: () -> Unit
) {
    var manual by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue("")
        )
    }
    val isValid by remember(manual) {
        mutableStateOf(
            manual.text.trim().matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$"))
        )
    }

    OutlinedTextField(
        value = manual,
        onValueChange = { it ->
            val grouped =
                it.text.filter { it.toString().matches(Regex("[0-9A-Fa-f]")) }.take(12).uppercase()
                    .chunked(2).joinToString(":")
            manual = TextFieldValue(
                grouped, selection = TextRange(grouped.length.coerceAtMost(grouped.length))
            )
        },
        label = { Text("Enter MAC Address") },
        isError = manual.text.isNotBlank() && !isValid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = {
            val address = manual.text.trim()
            if (isValid) onConnect(address)
        }),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        trailingIcon = {
            if (isConnected) IconButton(onClick = onManualDisconnect) {
                Icon(
                    painter = painterResource(id = R.drawable.xmark),
                    contentDescription = "Disconnect",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.height(24.dp)
                )
            } else IconButton(
                onClick = {
                    val address = manual.text.trim()
                    if (isValid) onConnect(address)
                }, enabled = isValid
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.check),
                    contentDescription = "Connect",
                    tint = if (isValid) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.height(24.dp)
                )
            }
        })
}

@Composable
private fun DeviceRow(
    dev: ScannedDevice,
    isConnected: Boolean,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dev.name ?: "Unnamed", style = MaterialTheme.typography.titleMedium
                    )
                    if (isConnected) Box(
                        Modifier
                            .padding(start = 8.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    )
                }
                Text(
                    "${dev.address} | ${dev.rssi} dBm", style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(12.dp))
            if (isBusy) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else if (isConnected) OutlinedButton(onClick = onDisconnect) {
                Text("Disconnect", style = MaterialTheme.typography.labelLarge)
            } else FilledTonalButton(onClick = onConnect) {
                Text("Connect", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview
@Composable
private fun ScanScreenPreview() {
    ScanScreen(vm = MainViewModel.preview(), onConnected = {})
}
