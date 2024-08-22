package com.cte.ctbenchy

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cte.bluetooth.BluetoothViewModel
import com.cte.ctbenchy.ui.BenchyViewModel
import com.cte.ctbenchy.ui.MainScreen
import com.cte.ctbenchy.ui.theme.CTBenchyTheme


class MainActivity : ComponentActivity() {
    private final val TAG = "CTBenchy"
    private val bluetoothViewModel: BluetoothViewModel by viewModels()
    private lateinit var benchyHwCtl: BenchyHwCtl


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

         requestPermissions()





        setContent {
            CTBenchyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(benchyHwCtl = benchyHwCtl, modifier = Modifier)
                }
            }
        }


    }


    override fun onResume() {
        super.onResume()
        if (::benchyHwCtl.isInitialized) {
            benchyHwCtl.onResume(this)
        }
    }

    override fun onPause() {
        super.onPause()
        benchyHwCtl.onPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        benchyHwCtl.onDestroy(this)
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            benchyHwCtl = BenchyHwCtl( BenchyViewModel())
            benchyHwCtl.initialize(this)
            benchyHwCtl.updateViewModel();
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
               Log.i(TAG,"permissions granted")
            } else {
                benchyHwCtl = BenchyHwCtl( BenchyViewModel())
                benchyHwCtl.initialize(this)
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}




