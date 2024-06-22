import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CircularThumbVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var thumbPosition by remember {
        mutableStateOf(0f)
    }

    val thumbRadius = 16.dp

    Box(
        modifier = modifier
            .background(Color.Gray, CircleShape)
            .padding(thumbRadius)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    when {
                        dragAmount != null -> {
                            val originalY = thumbPosition
                            val newValue = (originalY + dragAmount).coerceIn(0f, size.height - 50.dp.toPx())
                            thumbPosition = newValue
                        }
                        else -> isDragging = false
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight(value)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .width(8.dp)
                .offset(x = 8.dp)
                .padding(thumbRadius)
        ) {
            IconButton(
                onClick = { /* Handle click if needed */ },
                modifier = Modifier
                    .offset(x = -8.dp)
                    .size(thumbRadius * 2)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun CircularThumbVerticalSliderScreen() {
    var sliderValue by remember { mutableStateOf(0.5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        CircularThumbVerticalSlider(
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue
            },
            modifier = Modifier.fillMaxHeight()
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text("Slider Value: ${sliderValue * 100}%")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCircularThumbVerticalSliderScreen() {
    CircularThumbVerticalSliderScreen()
}
