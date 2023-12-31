package com.cte.ctbenchy.ui


import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.cte.bluetooth.BluetoothMgr
import com.cte.ctbenchy.BenchyHwCtl
import com.cte.ctbenchy.BenchyHwCtl.Companion.MODE_BI
import com.cte.ctbenchy.BenchyHwCtl.Companion.MODE_PROG
import com.cte.ctbenchy.ui.theme.CTBenchyTheme
import kotlin.experimental.and


@Composable
fun BenchyScreen(
    benchyHwCtl: BenchyHwCtl,
    modifier: Modifier
) {
    val benchyUiState by benchyHwCtl.benchyViewModel.uiState.collectAsState()
    benchyUiState.ledMask

    val motorOnColor =
        if ((benchyUiState.ledMask and 16).toInt() == 16) Color.Magenta else Color.LightGray

    val portLightColor =
        if ((benchyUiState.ledMask and 2).toInt() == 2) Color.Red else Color.LightGray
    val sternLightColor =
        if ((benchyUiState.ledMask and 4).toInt() == 4) Color.Yellow else Color.LightGray
    val stbdLightColor =
        if ((benchyUiState.ledMask and 1).toInt() == 1) Color.Green else Color.LightGray

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isSettingsPageVisible by remember { mutableStateOf(false) }


    Column(modifier = modifier.padding(10.dp)) {

        when (benchyUiState.connectState) {
            BluetoothMgr.STATE_CONNECTED -> {
                Status("CONNECTED")
            }

            BluetoothMgr.STATE_CONNECTING -> {
                Status("CONNECTING")
            }

            BluetoothMgr.STATE_SCANNING -> {
                Status("SCANNING")
            }

            BluetoothMgr.STATE_DISCONNECTING -> {
                Status("DISCONNECTING")
            }

            BluetoothMgr.STATE_DISCONNECTED -> {
                Status("DISCONNECTED")
            }
        }


        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RudderSlider(
                modifier = Modifier.padding(10.dp),
                onChange = { benchyHwCtl.setRudder(it) },
                value = benchyUiState.rudder.toFloat()
            )
            ThrottleSlider(
                modifier = Modifier.padding(10.dp),
                mode = benchyUiState.mode,
                onChange = { benchyHwCtl.setThrottle(it) },
                value = benchyUiState.throttle,
                limit = benchyUiState.throttleTrim
            )
        }
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    Log.i("BATMAN ", " press MOTOR");
                    benchyHwCtl.toggleLed(BenchyHwCtl.PORT_LED)
                }, colors = ButtonDefaults.textButtonColors(
                    containerColor = portLightColor,
                    contentColor = Color.Black
                )
            ) {
                Text("Port")
            }
            OutlinedButton(
                onClick = { benchyHwCtl.toggleLed(BenchyHwCtl.STERN_LED) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = sternLightColor,
                    contentColor = Color.Black
                )
            ) {
                Text("Stern")
            }
            OutlinedButton(
                onClick = { benchyHwCtl.toggleLed(BenchyHwCtl.STBD_LED) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = stbdLightColor,
                    contentColor = Color.Black
                )
            ) {
                Text("Stbd")
            }

        }
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    benchyHwCtl.toggleLed(BenchyHwCtl.HORN)
                }, colors = ButtonDefaults.textButtonColors(
                    containerColor = if (isPressed) Color.Blue else Color.LightGray,
                    contentColor = Color.Black
                ), interactionSource = interactionSource
            ) {
                Text("Horn")
            }
            OutlinedButton(
                onClick = { benchyHwCtl.toggleLed(BenchyHwCtl.MOTOR_SOUND) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = motorOnColor,
                    contentColor = Color.Black
                )
            ) {
                Text("Motor")
            }
        }

    }

}


@Composable
fun RudderSlider(
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit = {},
    value: Float
) {
    var sliderPosition by remember { mutableStateOf(value) }
    Column {
        Slider(
            modifier = modifier
                .width(180.dp)
                .height(50.dp),
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                onChange(it)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            steps = 120,
            valueRange = -60f..60f
        )
        Text(text = value.toString())
    }
}


@Composable
fun ThrottleSlider(
    modifier: Modifier = Modifier,
    mode: Byte,
    onChange: (Int) -> Unit = {},
    value: Int,
    limit: Int
) {
    var sliderPosition by remember { mutableStateOf(value) }

    var min = 0f
    var max = 180f
    var range = 0f..180f


    if (limit > 0) {
        range = 0f..limit.toFloat()
    }

    var steps = 100
    if (mode == MODE_BI) {
        range = -90f..90f
        if (limit > 0) {
            range = (-1.0f * limit.toFloat())..limit.toFloat()
        }

    }
    if (mode == MODE_PROG) {
        steps = 2
        range = 0f..180f
    }
    Column {
        Slider(modifier = modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth,
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(-placeable.width, 0)
                }
            }
            .width(180.dp)
            .height(50.dp),

            value = sliderPosition.toFloat(),
            onValueChange = {
                sliderPosition = it.toInt()
                if (mode == MODE_PROG) {
                    sliderPosition = if (it >= 90.0f) {
                        180
                    } else {
                        0
                    }
                }
                onChange(sliderPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            steps = steps,
            valueRange = range
        )
        Text(text = value.toString())
    }
}

@Composable
fun Status(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Benchy $name",
        modifier = modifier
    )
}


@Preview(showBackground = true)
@Composable
fun ScreenPreview() {
    CTBenchyTheme {
        val benchyHwCtl = BenchyHwCtl(BenchyViewModel())
        BenchyScreen(benchyHwCtl, modifier = Modifier)

    }
}



