package com.example.lessonblescan02.scanner

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.lessonblescan02.BleScanApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleScanManager constructor(private val context: Context, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    DefaultLifecycleObserver {
    companion object {
        private const val TAG = "BleScanManager"
        private const val REQUEST_CODE_BLE_SCANNER_PENDING_INTENT = 1000
    }

    private val mutableStateFlowScanning = MutableStateFlow<Boolean>(false)
    val stateFlowScanning get() = mutableStateFlowScanning.asStateFlow()
    val valueScanning get() = mutableStateFlowScanning.value

    private val mutableStateFlowError = MutableStateFlow<Int>(-1)
    val stateFlowError get() = mutableStateFlowError.asStateFlow()
    val valueError get() = mutableStateFlowError.value

    private val mutableStateFlowDevice = MutableStateFlow<BleDevice?>(null)
    val stateFlowDevice get() = mutableStateFlowDevice.asStateFlow()
    val valueDevice get() = mutableStateFlowDevice.value

    private val scope = CoroutineScope(ioDispatcher)

    private val scanSettingsBuilder:ScanSettings.Builder = ScanSettings.Builder()
    private val scanFilters = mutableListOf<ScanFilter>()

    private val pendingIntent by lazy {
        BleScanReceiver.getBroadcast(context,
            REQUEST_CODE_BLE_SCANNER_PENDING_INTENT)
    }

    private val bluetoothLeScanner:BluetoothLeScanner by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter.bluetoothLeScanner
    }

    /**
     * Наборы фильтров
     */
    private val addresses      = mutableListOf<String>()
    private val names          = mutableListOf<String>()
    private val uuids          = mutableListOf<ParcelUuid>()
    private var stopOnFind     = false
    private var notEmitRepeat  = true
    private val scannedDevices = mutableListOf<BleDevice>()

    init {
        initDefaultScanSettings()
        if (context.applicationContext is BleScanApplication) {
            (context.applicationContext as BleScanApplication).bleScanManager = this
        }
    }

    fun startScan(names:List<String> = listOf(),
                  addresses:List<String> = listOf(),
                  uuids:List<ParcelUuid> = listOf(),
                  stopOnFind:Boolean = false,
                  notEmitRepeat:Boolean = true
    ) {
        if (!valueScanning) {
            this.addresses.clear()
            this.addresses.addAll(addresses)

            this.names.clear()
            this.names.addAll(names)

            this.uuids.clear()
            this.uuids.addAll(uuids)

            this.stopOnFind = stopOnFind

            this.notEmitRepeat = notEmitRepeat

            this.scannedDevices.clear()

            val res = bluetoothLeScanner.startScan(scanFilters,
                scanSettingsBuilder.build(),
                pendingIntent)

            if (res == 0) {
                mutableStateFlowScanning.tryEmit(true)
            } else {
                mutableStateFlowScanning.tryEmit(false)
            }
        }
    }

    fun stopScan() {
        if ( valueScanning ) {
            bluetoothLeScanner.stopScan(pendingIntent)
            mutableStateFlowScanning.tryEmit(false)
        }
    }

    private fun isNotError(intent: Intent) : Boolean {
        val callbackType =
            intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
        if (callbackType != -1) {
            if (intent.hasExtra(BluetoothLeScanner.EXTRA_ERROR_CODE)) {
                val errorCode = intent.getIntExtra(
                    "android.bluetooth.le.extra.LIST_SCAN_RESULT",
                    -1
                )
                mutableStateFlowError.tryEmit(errorCode)
                return false
            }
        }

        return true
    }

    private fun filterName(bleDevice: BleDevice) : Boolean =
        names.isEmpty()
            .or(names.isNotEmpty()
                .and(bleDevice.name != null)
                .and(names.contains(bleDevice.name)))

    private fun filterAddress(bleDevice: BleDevice) : Boolean =
        addresses.isEmpty()
            .or(addresses.isNotEmpty().and(addresses.contains(bleDevice.address)))

    private fun filterUuids(uuids: Array<ParcelUuid>?) : Boolean {
        if (this.uuids.isEmpty()) return true
        // println("UUIDS: ${this.uuids}")
        if (uuids.isNullOrEmpty()) return false
        if (this.uuids.containsAll(uuids.toList())) return true
        return false
    }

    private fun filterDevice (bluetoothDevice: BluetoothDevice) {
        val bleDevice = BleDevice(bluetoothDevice)
        if (filterName(bleDevice)
                .and(filterAddress(bleDevice))
                .and(filterUuids(bluetoothDevice.uuids)))
            if(notEmitRepeat.and(!scannedDevices.contains(bleDevice))) {
                scannedDevices.add(bleDevice)
                mutableStateFlowDevice.tryEmit(bleDevice)
                if (stopOnFind) {
                    stopScan()
                }
            } else {
                mutableStateFlowDevice.tryEmit(bleDevice)
            }
    }

    fun onScanReceived(intent: Intent) {
        if (isNotError(intent)) {
            if (intent.hasExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)) {
                intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)?.let {

                }
                intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
                    ?.let { results ->
                        results.forEach { result ->
                            result.device?.let { device ->
                                filterDevice(device)
                            }
                        }
                    }
                }
            }
        }


    private fun initDefaultScanSettings() {
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        // setReportDelay() -- отсутствует. Не вызывать! Ответ приходит ПУСТОЙ!
        // В официальной документации scanSettingsBuilder.setReportDelay(1000)
        scanSettingsBuilder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
        scanSettingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        scanSettingsBuilder.setLegacy(false)
        scanSettingsBuilder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopScan()
        super.onDestroy(owner)
    }
}