package com.sid.smartmoisture.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sid.smartmoisture.core.AppDatabase
import com.sid.smartmoisture.core.BleManager
import com.sid.smartmoisture.core.Equation
import com.sid.smartmoisture.core.EquationRepo
import com.sid.smartmoisture.core.ScannedDevice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import net.objecthunter.exp4j.ExpressionBuilder
import timber.log.Timber

data class LogLine(val id: Long, val text: String, val time: Long)
data class Reading(val t: Long, val rawLine: String, val rawValue: Double?)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(app: Application, previewMode: Boolean = false) : AndroidViewModel(app) {
    private val db by lazy { AppDatabase.get(getApplication()) }
    private val repo by lazy { EquationRepo(db.equationDao()) }
    private val ble by lazy { BleManager(getApplication()) }

    private val _log = MutableStateFlow<List<LogLine>>(emptyList())
    val log: StateFlow<List<LogLine>> = _log

    private val _equations = MutableStateFlow<List<Equation>>(emptyList())
    val equations: StateFlow<List<Equation>> = _equations

    private val _selected: MutableStateFlow<Equation?> = MutableStateFlow(null)
    val selected: StateFlow<Equation?> = _selected

    private val _sampleMs = MutableStateFlow(1000L)
    val sampleMs: StateFlow<Long> = _sampleMs

    private val _readings = MutableStateFlow<List<Reading>>(emptyList())
    val readings: StateFlow<List<Reading>> = _readings

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices

    private val _connected = MutableStateFlow<String?>(null)
    val connected: StateFlow<String?> = _connected

    private var prevRaw: Double? = null
    private var logCounter = 0L

    init {
        if (!previewMode) {
            viewModelScope.launch { loadEquations() }
            viewModelScope.launch {
                _sampleMs.flatMapLatest { periodMs -> ble.lines.sample(periodMs) }.collect { line ->
                    appendLog(line)
                    addReading(line)
                }
            }
            viewModelScope.launch { ble.devices.collect { list -> _devices.value = list } }
            viewModelScope.launch { ble.connected.collect { addr -> _connected.value = addr } }

            startScan()
        } else {
            _equations.value = listOf(
                Equation(id = 1, name = "Linear", formula = "2*x+1"),
                Equation(id = 2, name = "Quadratic", formula = "x^2")
            )
            _selected.value = _equations.value.first()
            _log.value = listOf(
                LogLine(1, "10.00 C Soil 1234.56", System.currentTimeMillis() - 60000L),
                LogLine(2, "10.50 C Soil 1235.00", System.currentTimeMillis() - 55000L),
                LogLine(3, "11.00 C Soil 1235.50", System.currentTimeMillis() - 50000L)
            )
            logCounter = _log.value.size.toLong()
            _readings.value = List(10) { i ->
                val v = Math.random() * i / 10.0
                Reading(System.currentTimeMillis() - (10 - i) * 1000L, "Value: $v", v)
            }.reversed()
            _devices.value = listOf(
                ScannedDevice("Sensor 1", "AA:BB:CC:DD:EE:01", -50),
                ScannedDevice("Sensor 2", "AA:BB:CC:DD:EE:02", -60),
                ScannedDevice("Sensor 3", "AA:BB:CC:DD:EE:03", -70),
            )
            _connected.value = "AA:BB:CC:DD:EE:01"
        }
    }

    constructor(app: Application) : this(app, false)

    fun startScan(force: Boolean = false) {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            appendLog("No BLUETOOTH_SCAN permission")
            return
        }
        ble.startScan(force)
    }

    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            appendLog("No BLUETOOTH_SCAN permission")
            return
        }
        ble.stopScan()
    }

    fun connectTo(address: String) {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            appendLog("No BLUETOOTH_CONNECT permission")
            return
        }
        ble.connect(address)
    }

    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            appendLog("No BLUETOOTH_CONNECT permission")
            return
        }
        ble.disconnect()
    }

    fun setSampleMs(ms: Long) {
        _sampleMs.value = ms
    }

    private fun appendLog(line: String) {
        _log.value = (_log.value + LogLine(
            id = ++logCounter, text = line, time = System.currentTimeMillis()
        )).takeLast(100)
    }

    private fun addReading(line: String) {
        _readings.value = (_readings.value + Reading(
            System.currentTimeMillis(), line, extractLastNumber(line)
        )).takeLast(1000)
    }

    private fun extractLastNumber(s: String): Double? =
        Regex("([-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)").findAll(s).toList()
            .lastOrNull()?.value?.toDoubleOrNull()

    fun validateFormula(text: String, x: Double, xp: Double, dx: Double): Pair<Double?, String?> =
        try {
            ExpressionBuilder(text).variables("x", "xp", "dx").build().setVariable("x", x)
                .setVariable("xp", xp).setVariable("dx", dx).evaluate() to null
        } catch (e: IllegalArgumentException) {
            null to (e.message ?: "Invalid formula.")
        } catch (e: ArithmeticException) {
            null to (e.message ?: "Arithmetic error in formula.")
        } catch (e: Exception) {
            null to (e.message ?: "Could not evaluate formula.")
        }

    fun computeInstant(v: Double): Double? {
        val eq = _selected.value ?: return null
        val xp = prevRaw
        val dx = if (xp != null) v - xp else 0.0

        return try {
            val result = validateFormula(eq.formula, v, xp ?: v, dx).first

            prevRaw = v
            result
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    fun setSelected(e: Equation?) {
        _selected.value = e
    }

    fun removeEquation(e: Equation) = viewModelScope.launch {
        repo.delete(e); loadEquations()
        if (_selected.value?.id == e.id) _selected.value = null
    }

    fun addEquation(name: String, formula: String, select: Boolean = true) = viewModelScope.launch {
        require(name.isNotBlank())
        validateFormula(
            formula, 1.5, 1.0, 0.5
        ).second?.let { error -> throw IllegalArgumentException(error) }

        repo.add(name, formula)
        loadEquations()

        if (select) _selected.value =
            _equations.value.find { it.name == name && it.formula == formula }
    }

    fun updateEquation(id: Long, name: String, formula: String, select: Boolean = true) =
        viewModelScope.launch {
            require(name.isNotBlank()) { "Name cannot be empty." }
            validateFormula(
                formula, 1.5, 1.0, 0.5
            ).second?.let { error -> throw IllegalArgumentException(error) }

            val existing = _equations.value.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Equation not found.")
            val updated = existing.copy(name = name, formula = formula)

            repo.update(updated)
            loadEquations()

            if (select) _selected.value = _equations.value.firstOrNull { it.id == id }
            else if (_selected.value?.id == id) _selected.value = updated
        }

    private fun loadEquations() = viewModelScope.launch { _equations.value = repo.list() }

    companion object {
        fun preview() = MainViewModel(Application(), true)
    }
}
