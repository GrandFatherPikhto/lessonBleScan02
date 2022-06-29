package com.example.lessonblescan02.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanRecord
import com.example.lessonblescan02.BleScanApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BleScanManagerTest {
    companion object {
        private const val ADDRESS="01:02:03:04:05:06"
        private const val NAME="TEST_DEVICE"

        private const val TEST_NAME="TEST_DEVICE_%02d"
    }

    private val bleScanApplication: BleScanApplication by lazy {
        RuntimeEnvironment.getApplication() as BleScanApplication
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        (bleScanApplication.applicationContext
            .getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    private val bleScanManager: BleScanManager by lazy {
        BleScanManager(context = bleScanApplication.baseContext, UnconfinedTestDispatcher())
    }

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    private fun createShadowBluetoothDevice(address: String,
                                            name: String? = null,
                                            uuids: List<ParcelUuid>? = null) : BluetoothDevice {
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
        if (!name.isNullOrEmpty()) {
            shadowOf(bluetoothDevice).setName(name)
        }

        if (uuids != null) {
            shadowOf(bluetoothDevice).setUuids(uuids.toTypedArray())
        }

        return bluetoothDevice
    }

    private fun randomBluetoothAddress() : String =
        Random.nextBytes(6).joinToString(separator = ":") { String.format("%02X", it) }


    private fun mockScanResult(deviceAddress: String? = null,
                               deviceName: String? = null,
                               uuids: List<ParcelUuid>? = null
    ) : ScanResult {
        val scanResult = mock(ScanResult::class.java)
        val bluetoothDevice = createShadowBluetoothDevice(
            address = deviceAddress ?: randomBluetoothAddress(),
            name = deviceName,
            uuids = uuids
        )
        // org.mockito.kotlin:mockito-kotlin:4.0.0
        whenever(scanResult.device).thenReturn(bluetoothDevice)

        return scanResult
    }

    @Test
    fun testBluetoothDevice() {
        val uuids = listOf(
            ParcelUuid(UUID.randomUUID()),
            ParcelUuid(UUID.randomUUID())
        )
        val bluetoothDevice = createShadowBluetoothDevice(ADDRESS, NAME, uuids)
        assertEquals(NAME, bluetoothDevice.name)
        assertEquals(ADDRESS, bluetoothDevice.address)
        assertArrayEquals(uuids.toTypedArray(), bluetoothDevice.uuids)
    }

    @Test
    fun intentWithScanResults() {
        val intent = Intent(bleScanApplication, ScanResult::class.java)
        intent.action = BleScanReceiver.ACTION_BLE_SCAN
        val scanResults = mutableListOf<ScanResult>()
        val devices = mutableListOf<BluetoothDevice>()
        for (i in 0..7) {
            scanResults.add(mockScanResult(deviceName = String.format("BLE_DEVICE_%02d", i)))
        }
        intent.putParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            scanResults.toCollection(ArrayList()))

        intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
            ?.let { results ->
                results.forEach { result ->
                    result.device?.let { device ->
                        devices.add(device)
                    }
                }
            }

        assertEquals(
            scanResults.map { it.device }.toList(),
            devices)
    }

    @Test
    fun filterByNameAndStop() = runTest(UnconfinedTestDispatcher()){
        val intent = Intent(bleScanApplication, ScanResult::class.java)
        intent.action = BleScanReceiver.ACTION_BLE_SCAN
        val scanResults = mutableListOf<ScanResult>()
        for (i in 1..7) {
            scanResults.add(mockScanResult(deviceName = String.format(TEST_NAME, i)))
        }
        intent.putParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            scanResults.toCollection(ArrayList()))
        bleScanManager.startScan(names = listOf(scanResults[2].device.name), stopOnFind = true)
        bleScanManager.onScanReceived(intent)
        assertEquals(bleScanManager.valueDevice!!.address, scanResults[2].device.address)
        assertFalse(bleScanManager.valueScanning)
    }

    @Test
    fun filterByAddressAndStop() = runTest(UnconfinedTestDispatcher()) {
        val intent = Intent(bleScanApplication, ScanResult::class.java)
        intent.action = BleScanReceiver.ACTION_BLE_SCAN
        val scanResults = mutableListOf<ScanResult>()
        for (i in 1..7) {
            scanResults.add(mockScanResult(deviceName = String.format(TEST_NAME, i)))
        }
        intent.putParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            scanResults.toCollection(ArrayList()))
        bleScanManager.startScan(addresses = listOf(scanResults[2].device.address), stopOnFind = true)
        bleScanManager.onScanReceived(intent)
        assertEquals(bleScanManager.valueDevice!!.address, scanResults[2].device.address)
        assertFalse(bleScanManager.valueScanning)
    }

    @Test
    fun filterByUuidsAndStop() = runTest(UnconfinedTestDispatcher()) {
        val intent = Intent(bleScanApplication, ScanResult::class.java)
        intent.action = BleScanReceiver.ACTION_BLE_SCAN
        val scanResults = mutableListOf<ScanResult>()
        for (i in 1..7) {
            scanResults.add(mockScanResult(deviceName = String.format(TEST_NAME, i)))
        }
        val uuids1 = listOf(
            ParcelUuid(UUID.randomUUID()),
            ParcelUuid(UUID.randomUUID())
        )
        scanResults.add(5, mockScanResult(ADDRESS, NAME, uuids1))
        val uuids2 = listOf(
            ParcelUuid(UUID.randomUUID()),
            ParcelUuid(UUID.randomUUID())
        )
        scanResults.add(6, mockScanResult(ADDRESS, NAME, uuids2))
        intent.putParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
            scanResults.toCollection(ArrayList()))
        bleScanManager.startScan(uuids = uuids2, stopOnFind = true)
        bleScanManager.onScanReceived(intent)
        assertEquals(scanResults[6].device.address, bleScanManager.valueDevice!!.address)
        assertFalse(bleScanManager.valueScanning)
    }
}
