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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class BleScanManager constructor(private val context: Context, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
    DefaultLifecycleObserver {
    private val logTag = this.javaClass.simpleName

    companion object {
        private const val REQUEST_CODE_BLE_SCANNER_PENDING_INTENT = 1000
    }

    enum class State(val value: Int) {
        Stopped(0x00),
        Scanning(0x01),
        Error(0xFF)
    }

    private val mutableStateFlowScanning = MutableStateFlow(State.Stopped)
    val stateFlowScanning get() = mutableStateFlowScanning.asStateFlow()
    val valueScanning get() = mutableStateFlowScanning.value

    private val mutableStateFlowError = MutableStateFlow<Int>(-1)
    val stateFlowError get() = mutableStateFlowError.asStateFlow()
    val valueError get() = mutableStateFlowError.value

    private val mutableSharedFlowDevice = MutableSharedFlow<BluetoothDevice>(replay = 100)
    val sharedFlowDevice get() = mutableSharedFlowDevice.asSharedFlow()

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
     * ???????????? ????????????????
     */
    private val addresses      = mutableListOf<String>()
    private val names          = mutableListOf<String>()
    private val uuids          = mutableListOf<ParcelUuid>()
    private var stopOnFind     = false
    private var notEmitRepeat  = true
    private val scannedDevices = mutableListOf<BluetoothDevice>()
    val devices get() = scannedDevices.toList()

    init {
        initDefaultScanSettings()
        if (context.applicationContext is BleScanApplication) {
            (context.applicationContext as BleScanApplication).emitScanManager(this)
        }



        scope.launch {
            mutableSharedFlowDevice.collect {
                Log.d(logTag, "BLE Device: $it")
            }
        }

        scope.launch {
            mutableStateFlowScanning.collect {
                Log.d(logTag, "Scanning: $it")
            }
        }

        scope.launch {
            mutableStateFlowError.collect {
                Log.e(logTag, "Error code: $it")
            }
        }
    }

    fun startScan(names:List<String> = listOf(),
                  addresses:List<String> = listOf(),
                  uuids:List<ParcelUuid> = listOf(),
                  stopOnFind:Boolean = false,
                  notEmitRepeat:Boolean = true
    ) {
        if (valueScanning != State.Scanning) {
            Log.d(logTag, "startScan()")

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
                mutableStateFlowScanning.tryEmit(State.Scanning)
            } else {
                mutableStateFlowScanning.tryEmit(State.Error)
            }
        }
    }

    fun stopScan() {
        if ( valueScanning == State.Scanning ) {
            Log.d(logTag, "stopScan()")
            bluetoothLeScanner.stopScan(pendingIntent)
            mutableStateFlowScanning.tryEmit(State.Stopped)
        }
    }

    private fun isNotError(intent: Intent) : Boolean {
        val callbackType =
            intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
        if (callbackType != -1) {
            if (intent.hasExtra(BluetoothLeScanner.EXTRA_ERROR_CODE)) {
                val errorCode = intent.getIntExtra(
                    BluetoothLeScanner.EXTRA_ERROR_CODE,
                    -1
                )
                Log.e(logTag, "Error code: $errorCode")
                println("Error code: $errorCode")
                mutableStateFlowError.tryEmit(errorCode)
                return false
            }
        }

        return true
    }

    private fun filterName(bluetoothDevice: BluetoothDevice) : Boolean =
        names.isEmpty()
            .or(names.isNotEmpty()
                .and(bluetoothDevice.name != null)
                .and(names.contains(bluetoothDevice.name)))

    private fun filterAddress(bluetoothDevice: BluetoothDevice) : Boolean =
        addresses.isEmpty()
            .or(addresses.isNotEmpty().and(addresses.contains(bluetoothDevice.address)))

    private fun filterUuids(uuids: Array<ParcelUuid>?) : Boolean {
        if (this.uuids.isEmpty()) return true
        // println("UUIDS: ${this.uuids}")
        if (uuids.isNullOrEmpty()) return false
        if (this.uuids.containsAll(uuids.toList())) return true
        return false
    }

    private fun filterDevice (bluetoothDevice: BluetoothDevice) {
        if (filterName(bluetoothDevice)
                .and(filterAddress(bluetoothDevice))
                .and(filterUuids(bluetoothDevice.uuids)))
            if(notEmitRepeat.and(!scannedDevices.contains(bluetoothDevice))) {
                scannedDevices.add(bluetoothDevice)
                mutableSharedFlowDevice.tryEmit(bluetoothDevice)
                if (stopOnFind) {
                    stopScan()
                }
            } else {
                mutableSharedFlowDevice.tryEmit(bluetoothDevice)
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

    fun multipleLaunchStartStopScan() = runBlocking(ioDispatcher) {
        for (i in 0..21) {
            startScan()
            delay(500)
            stopScan()
            delay(500)
        }
    }

    private fun initDefaultScanSettings() {
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        scanSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        // setReportDelay() -- ??????????????????????. ???? ????????????????! ?????????? ???????????????? ????????????!
        // ?? ?????????????????????? ???????????????????????? scanSettingsBuilder.setReportDelay(1000)
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