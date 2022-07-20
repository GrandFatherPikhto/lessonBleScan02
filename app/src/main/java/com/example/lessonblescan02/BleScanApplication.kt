package com.example.lessonblescan02

import android.app.Application
import com.example.lessonblescan02.scanner.BleScanManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.properties.Delegates

class BleScanApplication : Application() {
    private val mutableStateFlowScanManager = MutableStateFlow<BleScanManager?>(null)
    val stateFlowScanManager get() = mutableStateFlowScanManager.asStateFlow()
    val bleScanManager:BleScanManager? get() = mutableStateFlowScanManager.value

    fun emitScanManager(bleScanManager: BleScanManager) {
        mutableStateFlowScanManager.tryEmit(bleScanManager)
    }
}