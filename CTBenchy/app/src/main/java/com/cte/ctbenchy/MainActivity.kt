package com.cte.ctbenchy

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.cte.bluetooth.BluetoothHandler
import com.cte.bluetooth.BluetoothViewModel
import com.cte.ctbenchy.ui.theme.CTBenchyTheme


class MainActivity : ComponentActivity() {
    private  final val  TAG = "CTBEnchy"
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
        benchyHwCtl = BenchyHwCtl()
        benchyHwCtl.initialize(this,bluetoothViewModel)



        setContent {
            CTBenchyTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                     Greeting("Android")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        benchyHwCtl?.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        benchyHwCtl?.onPause(this)
    }
    override fun onDestroy() {
        super.onDestroy()
        benchyHwCtl?.onDestroy(this)
    }

}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CTBenchyTheme {
        Greeting("Android")
    }
}