package com.example.lessonblescan02

import android.app.Application
import com.example.lessonblescan02.scanner.BleScanManager
import kotlin.properties.Delegates

class BleScanApplication : Application() {
    companion object {
        private const val TAG = "BleScanApplication"
    }

    var bleScanManager:BleScanManager? = null

    override fun onCreate() {
        super.onCreate()
    }
}