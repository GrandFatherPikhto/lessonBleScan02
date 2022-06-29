package com.example.lessonblescan02

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.lessonblescan02.databinding.ActivityMainBinding
import com.example.lessonblescan02.scanner.BleScanManager
import com.example.lessonblescan02.scanner.RequestPermissions
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val requestPermissions by lazy {
        RequestPermissions(this)
    }

    private val bleScanManager: BleScanManager by lazy {
        BleScanManager(this).let {
            lifecycle.addObserver(it)
            it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        requestPermissions.requestPermissions(listOf (
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
        ))

        lifecycleScope.launch {
            requestPermissions.stateFlowRequestPermission.filterNotNull()
                .collect { requestPermission ->
                    if (requestPermission.granted) {
                        Toast.makeText(baseContext,
                            getString(R.string.message_permission_granted, requestPermission.permission),
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(baseContext,
                            getString(R.string.message_permission_not_granted, requestPermission.permission),
                            Toast.LENGTH_SHORT).show()
                        finishAndRemoveTask()
                        exitProcess(0)
                    }
                }
            }

        lifecycleScope.launch {
            bleScanManager.stateFlowScanning.collect { scanning ->
                if (scanning) {
                    Toast.makeText(baseContext,
                        getString(R.string.message_start_scan),
                            Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(baseContext,
                        getString(R.string.message_stop_scan),
                            Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_scan).let { actionScan ->
            lifecycleScope.launch {
                bleScanManager.stateFlowScanning.collect { scanning ->
                    if (scanning) {
                        actionScan.title = getString(R.string.action_stop_scan)
                        actionScan.setIcon(R.drawable.ic_baseline_man_2_48)
                    } else {
                        actionScan.title = getString(R.string.action_start_scan)
                        actionScan.setIcon(R.drawable.ic_baseline_directions_run_48)
                    }
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_scan -> {
                if (bleScanManager.valueScanning) {
                    bleScanManager.stopScan()
                } else {
                    bleScanManager.startScan()
                }
                true
            }
            R.id.action_test_multiple_launch -> {
                bleScanManager.multipleLaunchStartStopScan()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}