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
import kotlinx.coroutines.launch
import net.objecthunter.exp4j.ExpressionBuilder
import timber.log.Timber

data class Reading(
    val t: Long,
    val seq: Long?,
    val tempC: Double?,
    val moistureRaw: Int?,
    val checksumOk: Boolean,
    val rawLine: String
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(app: Application, previewMode: Boolean = false) : AndroidViewModel(app) {
    private val db by lazy { AppDatabase.get(getApplication()) }
    private val repo by lazy { EquationRepo(db.equationDao()) }
    private val ble by lazy { BleManager(getApplication()) }

    private val _equations = MutableStateFlow<List<Equation>>(emptyList())
    val equations: StateFlow<List<Equation>> = _equations

    private val _selected: MutableStateFlow<Equation?> = MutableStateFlow(null)
    val selected: StateFlow<Equation?> = _selected

    private val _latestTempC = MutableStateFlow<Double?>(null)
    val latestTempC: StateFlow<Double?> = _latestTempC

    private val _deviceRateSec = MutableStateFlow(1L)
    val deviceRateSec: StateFlow<Long> = _deviceRateSec

    private val _deviceSampling = MutableStateFlow(true)
    val deviceSampling: StateFlow<Boolean> = _deviceSampling

    private val _readings = MutableStateFlow<List<Reading>>(emptyList())
    val readings: StateFlow<List<Reading>> = _readings

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices

    private val _connected = MutableStateFlow<String?>(null)
    val connected: StateFlow<String?> = _connected

    private var prevRaw: Double? = null

    init {
        if (!previewMode) {
            viewModelScope.launch { loadEquations() }
            viewModelScope.launch {
                ble.lines.collect { line ->
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
            _readings.value = List(10) { i ->
                Reading(
                    t = System.currentTimeMillis() - (10 - i) * 1000L,
                    seq = i.toLong(),
                    tempC = 10.0 + i,
                    moistureRaw = 1200 + i * 10,
                    checksumOk = true,
                    rawLine = """{"s":$i,"t":${10.0 + i},"m":${1200 + i * 10}}*${checksum("""{"s":$i,"t":${10.0 + i},"m":${1200 + i * 10}}""")}"""
                )
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
        ) return
        ble.startScan(force)
    }

    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        ble.stopScan()
    }

    fun connectTo(address: String) {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        ble.connect(address)
    }

    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        ble.disconnect()
    }

    fun cmdStart() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        ble.sendCommand("START")
        _deviceSampling.value = true
    }

    fun cmdStop() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        ble.sendCommand("STOP")
        _deviceSampling.value = false
    }

    fun cmdRate(seconds: Long) {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val s = seconds.coerceIn(1, 3600)
        if (_deviceRateSec.value == s) return

        ble.sendCommand("RATE $s")
        _deviceRateSec.value = s
    }

    fun cmdGet() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        ble.sendCommand("GET")
    }

    fun cmdReset() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        ble.sendCommand("RESET")
    }

    private fun addReading(line: String) {
        val sample = parseSampleFrame(line)
        if (sample != null) {
            _latestTempC.value = sample.t
            _readings.value = (_readings.value + Reading(
                t = System.currentTimeMillis(),
                seq = sample.s,
                tempC = sample.t,
                moistureRaw = sample.m,
                checksumOk = sample.ok,
                rawLine = line
            )).takeLast(1000)

            return
        }

        if (line.startsWith("OK")) parseGetOkLine(line)
        _readings.value = (_readings.value + Reading(
            t = System.currentTimeMillis(),
            seq = null,
            tempC = null,
            moistureRaw = null,
            checksumOk = true,
            rawLine = line
        )).takeLast(1000)
    }

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

    private data class ParsedSample(val s: Long, val t: Double, val m: Int, val ok: Boolean)

    private fun parseSampleFrame(line: String): ParsedSample? {
        val star = line.lastIndexOf('*')
        if (star <= 0 || star + 3 > line.length) return null

        val payload = line.take(star).trim()
        val hh = line.substring(star + 1).trim().take(2).uppercase()
        if (hh.length != 2) return null
        val expected = checksum(payload)
        val ok = (hh == expected)

        fun findLong(key: String): Long? =
            Regex("\"$key\"\\s*:\\s*([0-9]+)").find(payload)?.groupValues?.getOrNull(1)
                ?.toLongOrNull()

        fun findDouble(key: String): Double? =
            Regex("\"$key\"\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+)").find(payload)?.groupValues?.getOrNull(
                1
            )?.toDoubleOrNull()

        val s = findLong("s") ?: return null
        val t = findDouble("t") ?: return null
        val m = findLong("m")?.toInt() ?: return null

        return ParsedSample(s, t, m, ok)
    }

    private fun parseGetOkLine(line: String) {
        if (!line.startsWith("OK")) return

        val s = Regex("\\bs:(\\d+)").find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
        val t = Regex("\\bt:([-+]?[0-9]*\\.?[0-9]+)").find(line)?.groupValues?.getOrNull(1)
            ?.toDoubleOrNull()
        val m = Regex("\\bm:(\\d+)").find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val r = Regex("\\br:(\\d+)").find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()

        if (t != null) _latestTempC.value = t
        if (r != null) _deviceRateSec.value = r
        if (s != null && t != null && m != null) _readings.value = (_readings.value + Reading(
            t = System.currentTimeMillis(),
            seq = s,
            tempC = t,
            moistureRaw = m,
            checksumOk = true,
            rawLine = line
        )).takeLast(1000)
    }

    private fun checksum(input: String): String {
        var x = 0

        input.toByteArray(Charsets.US_ASCII).forEach { b -> x = x xor (b.toInt() and 0xFF) }
        return x.toString(16).uppercase().padStart(2, '0')
    }

    companion object {
        fun preview() = MainViewModel(Application(), true)
    }
}
