package com.cte.ctbenchy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cte.bluetooth.BluetoothMgr
import com.cte.ctbenchy.BenchyHwCtl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(benchyHwCtl: BenchyHwCtl, modifier: Modifier) {
    var isSettingsPageVisible by remember { mutableStateOf(false) }
    val benchyUiState by benchyHwCtl.benchyViewModel.uiState.collectAsState()

    Scaffold(modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    when (benchyUiState.connectState) {
                        BluetoothMgr.STATE_CONNECTED -> {
                            Status("Connected")
                        }

                        BluetoothMgr.STATE_CONNECTING -> {
                            Status("Connecting")
                        }

                        BluetoothMgr.STATE_SCANNING -> {
                            Status("Scanning")
                        }

                        BluetoothMgr.STATE_DISCONNECTING -> {
                            Status("Disconnecting")
                        }

                        BluetoothMgr.STATE_DISCONNECTED -> {
                            Status("Disconnected")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSettingsPageVisible = true
                    }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isSettingsPageVisible) {
            SettingsPage(
                modifier = Modifier.padding(innerPadding),
                onClose = {
                    isSettingsPageVisible = false
                },
                benchyHwCtl = benchyHwCtl
            )
        } else {
            BenchyScreen(benchyHwCtl = benchyHwCtl, modifier = Modifier.padding(innerPadding))

        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewMyApp() {
    MainScreen(BenchyHwCtl(BenchyViewModel()), modifier = Modifier)
}