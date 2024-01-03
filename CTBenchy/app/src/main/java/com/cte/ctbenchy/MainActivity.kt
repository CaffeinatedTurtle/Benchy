package com.cte.ctbenchy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
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
        ActivityCompat.requestPermissions(
            this, arrayOf<String>(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ), 0
        )
        val benchyViewModel = BenchyViewModel()
        benchyHwCtl = BenchyHwCtl(benchyViewModel)
        benchyHwCtl.initialize(this)



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
        benchyHwCtl.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        benchyHwCtl.onPause(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        benchyHwCtl.onDestroy(this)
    }

}


