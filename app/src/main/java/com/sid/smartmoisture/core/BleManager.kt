package com.sid.smartmoisture.core

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import java.util.UUID

data class ScannedDevice(val name: String?, val address: String, val rssi: Int)

class BleManager(private val context: Context) {
    private val deviceUuid = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val rxUuid = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    private val descriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val scanner get() = adapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var rxChar: BluetoothGattCharacteristic? = null

    private val _lines = MutableSharedFlow<String>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val lines: SharedFlow<String> = _lines

    private val seen = LinkedHashMap<String, ScannedDevice>()
    private val _devices = MutableSharedFlow<List<ScannedDevice>>(
        replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val devices: SharedFlow<List<ScannedDevice>> = _devices

    private val _connected = MutableSharedFlow<String?>(
        replay = 1, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val connected: SharedFlow<String?> = _connected

    private fun hasConnectPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= 31) ActivityCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        else true

    private fun hasScanPermission(): Boolean = when {
        Build.VERSION.SDK_INT >= 31 -> ActivityCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        Build.VERSION.SDK_INT >= 29 -> ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        else -> ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requireBluetoothOn(): Boolean = adapter?.isEnabled == true

    private val scanCb = object : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val address = result.device?.address ?: return
            val name = result.scanRecord?.deviceName ?: result.device?.name
            val dev = ScannedDevice(name, address, result.rssi)

            val existing = seen[address]
            if (existing == null || existing.rssi != dev.rssi || existing.name != dev.name) {
                seen[address] = dev
                _devices.tryEmit(seen.values.toList())
            }
        }

        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
        override fun onBatchScanResults(results: MutableList<ScanResult>) =
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }

        override fun onScanFailed(errorCode: Int) = Timber.e("Scan failed: code=$errorCode")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan(force: Boolean = false) {
        if (!force && (!hasScanPermission() || !requireBluetoothOn())) return

        try {
            scanner?.stopScan(scanCb)
        } catch (_: Exception) {
            Timber.e("stopScan failed")
        }

        seen.clear()
        _devices.tryEmit(emptyList())

        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build()
        scanner?.startScan(emptyList(), settings, scanCb)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        try {
            scanner?.stopScan(scanCb)
        } catch (_: Exception) {
            Timber.e("stopScan failed")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    fun connect(address: String) {
        if (!hasConnectPermission() || !requireBluetoothOn()) return
        try {
            stopScan()
            _connected.tryEmit(null)
            val dev = adapter?.getRemoteDevice(address) ?: return
            dev.connectGatt(context.applicationContext, false, gattCb, BluetoothDevice.TRANSPORT_LE)
        } catch (_: SecurityException) {
            Timber.e("connect: security exception")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    fun disconnect() {
        if (!hasConnectPermission()) return
        stopScan()
        gatt?.let {
            try {
                it.disconnect()
            } catch (_: Exception) {
                Timber.e("disconnect failed")
            }
        }
    }

    private val gattCb = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (!hasConnectPermission()) return

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                try {
                    gatt.close()
                } catch (_: Exception) {
                    Timber.e("gatt.close failed")
                }
                _connected.tryEmit(null)
                cleanupGatt()
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            this@BleManager.gatt = gatt

            val service = gatt.getService(deviceUuid) ?: run {
                Timber.e("No service found")
                return
            }
            rxChar = service.getCharacteristic(rxUuid) ?: run {
                Timber.e("No RX characteristic found")
                return
            }

            enableNotify(gatt, rxChar)
            _connected.tryEmit(gatt.device.address)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, c: BluetoothGattCharacteristic) {
            if (c.uuid != rxUuid) return
            handleIncoming(c.value ?: return)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray
        ) {
            if (c.uuid != rxUuid) return
            handleIncoming(value)
        }
    }

    private fun handleIncoming(bytes: ByteArray) =
        bytes.toString(Charsets.UTF_8).split('\n').forEach { raw ->
            val line = raw.trim()
            if (line.isNotEmpty()) _lines.tryEmit(line)
        }

    private fun cleanupGatt() {
        gatt = null
        rxChar = null
    }

    @Suppress("DEPRECATION")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableNotify(gatt: BluetoothGatt, c: BluetoothGattCharacteristic?) {
        if (!hasConnectPermission()) return

        c ?: return
        gatt.setCharacteristicNotification(c, true)

        val desc = c.getDescriptor(descriptorUuid) ?: return
        if (Build.VERSION.SDK_INT >= 33) gatt.writeDescriptor(
            desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        ) else {
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(desc)
        }
    }
}
