package com.sid.smartmoisture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sid.smartmoisture.R
import com.sid.smartmoisture.ui.components.Sparkline
import com.sid.smartmoisture.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel, onOpenLog: () -> Unit, onOpenEquations: () -> Unit, onOpenScan: () -> Unit
) {
    val log by vm.log.collectAsState()
    val selected by vm.selected.collectAsState()
    val currentMs by vm.sampleMs.collectAsState()
    val readings by vm.readings.collectAsState()
    val devices by vm.devices.collectAsState()
    val connected by vm.connected.collectAsState()

    val raw = readings.lastOrNull()?.rawValue
    val computed = raw?.let { vm.computeInstant(it) }
    val spark = readings.mapNotNull { it.rawValue }.takeLast(20)
    val temp = log.lastOrNull()?.text?.split(" ")?.getOrNull(0)?.toFloatOrNull()

    var inputText by remember { mutableStateOf((currentMs / 1000).toString()) }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val versionName = remember {
        if (isPreview) "0.0.0" else context.packageManager.getPackageInfo(
            context.packageName, 0
        ).versionName
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "SmartMoisture",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Indriya Sensotech Private Limited",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }, actions = {
                if (temp != null) Column(
                    horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Temp.",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        "${"%.1f".format(temp)} °C",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            })
        }) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp, Alignment.CenterHorizontally
                    )
                ) {
                    FilledTonalButton(onClick = onOpenScan) { Text(if (connected != null) "Change Device" else "Connect") }
                    if (connected != null) OutlinedButton(onClick = { vm.disconnect() }) { Text("Disconnect") }
                }
                if (connected != null) Text(
                    "Connected: $connected", style = MaterialTheme.typography.bodyMedium
                )
                if (devices.isNotEmpty()) Text(
                    "${devices.size} device${if (devices.size == 1) "" else "s"} found",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Data", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Raw", style = MaterialTheme.typography.labelMedium)
                            Text(raw?.let { "%.2f".format(it) } ?: "--",
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                selected?.name ?: "No equation",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(computed?.let { "%.2f".format(it) } ?: "--",
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (spark.isNotEmpty()) Sparkline(
                        values = spark, modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp, Alignment.CenterHorizontally
                        )
                    ) {
                        FilledTonalButton(onClick = onOpenLog) {
                            Icon(
                                painter = painterResource(R.drawable.notes),
                                contentDescription = "Open Log",
                                modifier = Modifier
                                    .height(18.dp)
                                    .padding(end = 8.dp)
                            )
                            Text("Open Log")
                        }
                        FilledTonalButton(onClick = onOpenEquations) {
                            Icon(
                                painter = painterResource(R.drawable.calculator),
                                contentDescription = "Open Equations",
                                modifier = Modifier
                                    .height(18.dp)
                                    .padding(end = 8.dp)
                            )
                            Text("Equations")
                        }
                    }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                                inputText = newValue
                                isError = false
                            } else isError = true

                            newValue.toLongOrNull()?.let { seconds ->
                                vm.setSampleMs(seconds * 1000)
                            }
                        },
                        label = { Text("Sampling Interval (s)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Column(
                        Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Version v$versionName", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Developed by Siddharth Praveen Bharadwaj",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(vm = MainViewModel.preview(), onOpenLog = {}, onOpenEquations = {}, onOpenScan = {})
}
