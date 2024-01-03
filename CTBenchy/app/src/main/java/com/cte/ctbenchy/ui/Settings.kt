package com.cte.ctbenchy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cte.ctbenchy.BenchyHwCtl
import com.cte.ctbenchy.ui.theme.CTBenchyTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onClose: () -> Unit, modifier: Modifier,
    benchyHwCtl: BenchyHwCtl
) {
    val benchyUiState by benchyHwCtl.benchyViewModel.uiState.collectAsState()
    var throttleLimit by remember { mutableStateOf("${benchyUiState.throttleTrim}") }
    var isError by rememberSaveable { mutableStateOf(false) }

    fun validate(text: String): Int? {
        try {
            val ret = Integer.parseInt(text)
            isError = ret > 90
            return ret
        } catch (e: Exception) {
            isError = true
        }
        return 0

    }

    // Settings page content goes here
    // For example, a drop-down control, numeric control, etc.
    // Replace this with your actual settings content
    Box(
        modifier = modifier
            .fillMaxSize(1.0f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(1.0f)
                .padding(16.dp)
        ) {
            Text(
                text = "Settings Page",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(1.0f)
            )


            ModeDropdown(
                modifier = Modifier.width(30.dp),
                mode = benchyUiState.mode.toInt(),
                onChange = { benchyHwCtl.setMode(it) })

            // For example, a numeric control
            // Replace this with your actual numeric control

            TextField(
                modifier = Modifier.padding(32.dp),
                value = throttleLimit,
                onValueChange = {
                    throttleLimit = it
                    if (it.isNotEmpty()) {
                        validate(it)?.let { trim ->
                            benchyUiState.throttleTrim = trim
                        }

                    }
                },
                label = { Text("Throttle Limit") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                supportingText = {
                    if (isError) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Enter a number between 0 and 90",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                isError = isError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Close button
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeDropdown(modifier: Modifier = Modifier, mode: Int, onChange: (Int) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf("Uni-Directional", "Bi-Directional", "program")

    val context = LocalContext.current
    var selectedText by remember { mutableStateOf(items[mode]) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            TextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEachIndexed { index, s ->
                    DropdownMenuItem(
                        text = { Text(text = s) },
                        onClick = {
                            selectedText = s
                            expanded = false
                            onChange(index)

                        }
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)

@Composable
fun PreviewSettingsPage() {
    CTBenchyTheme {
        SettingsPage(
            onClose = {},
            modifier = Modifier,
            benchyHwCtl = BenchyHwCtl(BenchyViewModel())
        )
    }
}