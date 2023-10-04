package com.cte.ctbenchy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue


import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.cte.ctbenchy.ui.theme.CTBenchyTheme



@Composable
fun BenchyScreen(
benchyViewModel: BenchyViewModel
) {

    Greeting("Benchy")
    Row(modifier = Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically){
        RudderSlider(modifier = Modifier.padding(10.dp))
        ThrottleSlider(modifier = Modifier.padding(10.dp))
    }
}



@Preview
@Composable
fun RudderSlider(modifier:Modifier = Modifier) {
    var sliderPosition by remember { mutableStateOf(0f) }
    Column {
        Slider(modifier= modifier
            .width(180.dp)
            .height(50.dp),
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            steps = 100,
            valueRange = -50f..50f
        )
        Text(text = sliderPosition.toString())
    }
}

@Preview
@Composable
fun ThrottleSlider(modifier:Modifier = Modifier) {
    var sliderPosition by remember { mutableStateOf(0f) }
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

            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            steps = 100,
            valueRange = -50f..50f
        )
        Text(text = sliderPosition.toString())
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
fun ScreenPreview() {
    CTBenchyTheme {
        BenchyScreen(benchyViewModel = BenchyViewModel())

    }
}