package com.danielgoodwyn.mycountdowns.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

@Composable
fun AdvancedColorPicker(
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("RGB", "HSL", "HEX")

    val initialColor = try {
        Color(android.graphics.Color.parseColor(if (initialColorHex.startsWith("#")) initialColorHex else "#$initialColorHex"))
    } catch (e: Exception) {
        Color.Red
    }

    var r by remember { mutableFloatStateOf(initialColor.red) }
    var g by remember { mutableFloatStateOf(initialColor.green) }
    var b by remember { mutableFloatStateOf(initialColor.blue) }

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(initialColor.toArgb(), hsl)
    var h by remember { mutableFloatStateOf(hsl[0]) }
    var s by remember { mutableFloatStateOf(hsl[1]) }
    var l by remember { mutableFloatStateOf(hsl[2]) }

    var hexInput by remember { mutableStateOf(initialColorHex.removePrefix("#")) }

    // Sync state
    val currentColor = when (selectedTab) {
        0 -> Color(r, g, b)
        1 -> Color(ColorUtils.HSLToColor(floatArrayOf(h, s, l)))
        2 -> {
            try {
                Color(android.graphics.Color.parseColor("#$hexInput"))
            } catch (e: Exception) {
                Color.Red
            }
        }
        else -> Color.Red
    }

    LaunchedEffect(currentColor) {
        if (selectedTab != 0) {
            r = currentColor.red
            g = currentColor.green
            b = currentColor.blue
        }
        if (selectedTab != 1) {
            val hsvOut = FloatArray(3)
            ColorUtils.colorToHSL(currentColor.toArgb(), hsvOut)
            h = hsvOut[0]
            s = hsvOut[1]
            l = hsvOut[2]
        }
        if (selectedTab != 2) {
            val hexStr = Integer.toHexString(currentColor.toArgb()).uppercase().takeLast(6)
            hexInput = if (hexStr.length == 6) hexStr else hexInput
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a Color") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> { // RGB
                        ColorSlider("Red", r, Color.Red) { r = it }
                        ColorSlider("Green", g, Color.Green) { g = it }
                        ColorSlider("Blue", b, Color.Blue) { b = it }
                    }
                    1 -> { // HSL
                        ColorSlider("Hue", h, Color.Cyan, 360f) { h = it }
                        ColorSlider("Saturation", s, Color.Magenta, 1f) { s = it }
                        ColorSlider("Lightness", l, Color.Gray, 1f) { l = it }
                    }
                    2 -> { // HEX
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { if (it.length <= 6) hexInput = it },
                            label = { Text("Hex Code") },
                            prefix = { Text("#") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hexStr = Integer.toHexString(currentColor.toArgb()).uppercase().takeLast(6)
                onColorSelected(hexStr)
            }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ColorSlider(name: String, value: Float, trackColor: Color, max: Float = 1f, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodySmall)
            Text(String.format(java.util.Locale.US, "%.1f", value), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..max,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor.copy(alpha = 0.5f)
            )
        )
    }
}
