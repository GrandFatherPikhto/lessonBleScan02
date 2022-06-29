package com.example.lessonblescan02.scanner

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lessonblescan02.BleScanApplication

class BleScanReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BleScanReceiver"
        const val ACTION_BLE_SCAN = "com.grandfatherpikhto.lessonBleScan02.ACTION_BLE_SCAN"

        private fun newIntent(context: Context): Intent {
            Log.e(TAG, "newIntent()")
            val intent = Intent(
                context,
                BleScanReceiver::class.java
            )
            intent.action = ACTION_BLE_SCAN
            return intent
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        fun getBroadcast(context: Context, requestCode: Int): PendingIntent {
            Log.e(TAG, "getBroadcast()")
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                newIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private var bleScanManager:BleScanManager? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if ( context != null && intent != null ) {
            if (context.applicationContext is BleScanApplication) {
                bleScanManager = (context.applicationContext as BleScanApplication).bleScanManager
            }

            when (intent.action) {
                ACTION_BLE_SCAN -> {
                    bleScanManager?.onScanReceived(intent)
                }
                else -> {
                    Log.d(TAG, "Action: ${intent.action}")
                }
            }
        }
    }
}