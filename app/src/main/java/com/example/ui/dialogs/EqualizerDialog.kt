package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ThemeOption
import com.example.ui.MainViewModel

@Composable
fun EqualizerDialog(
    viewModel: MainViewModel,
    theme: ThemeOption,
    onDismiss: () -> Unit
) {
    val eqController = viewModel.playbackService?.equalizerController
    val prefs = viewModel.preferencesManager

    var isEnabled by remember { mutableStateOf(prefs.isEqEnabled()) }
    var selectedPreset by remember { mutableStateOf(prefs.getEqPreset()) }
    var bassBoost by remember { mutableStateOf(prefs.getBassBoost()) }

    // Band levels (5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
    val defaultFreqs = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val bandLevels = remember {
        mutableStateListOf(
            prefs.getBandLevel(0),
            prefs.getBandLevel(1),
            prefs.getBandLevel(2),
            prefs.getBandLevel(3),
            prefs.getBandLevel(4)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = theme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = theme.primary)
                        Text(
                            text = "Audio Equalizer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                prefs.setEqEnabled(it)
                                eqController?.setEnabled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = theme.primary),
                            modifier = Modifier.testTag("switch_equalizer_enabled")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.subtextColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets Horizontal Row
                Text(text = "Equalizer Presets", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                Spacer(modifier = Modifier.height(6.dp))

                val presets = eqController?.presetNames ?: listOf(
                    "Normal", "Bass Boost", "Treble Boost", "Vocal Boost",
                    "Rock", "Pop", "Jazz", "Classical", "Dance", "Custom"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.take(5).forEachIndexed { index, name ->
                        val isSelected = selectedPreset == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) theme.primary else theme.surfaceVariant)
                                .clickable(enabled = isEnabled) {
                                    selectedPreset = index
                                    prefs.setEqPreset(index)
                                    eqController?.applyPreset(index)
                                    // Update displayed slider values
                                    eqController?.getBands()?.forEachIndexed { i, band ->
                                        if (i in bandLevels.indices) {
                                            bandLevels[i] = band.currentLevelmB
                                            prefs.setBandLevel(i, band.currentLevelmB)
                                        }
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) theme.background else theme.textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.drop(5).forEachIndexed { dropIdx, name ->
                        val index = dropIdx + 5
                        val isSelected = selectedPreset == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) theme.primary else theme.surfaceVariant)
                                .clickable(enabled = isEnabled) {
                                    selectedPreset = index
                                    prefs.setEqPreset(index)
                                    eqController?.applyPreset(index)
                                    eqController?.getBands()?.forEachIndexed { i, band ->
                                        if (i in bandLevels.indices) {
                                            bandLevels[i] = band.currentLevelmB
                                            prefs.setBandLevel(i, band.currentLevelmB)
                                        }
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) theme.background else theme.textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5 Frequency Bands Sliders
                Text(text = "Frequency Bands", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                Spacer(modifier = Modifier.height(6.dp))

                for (i in 0 until 5) {
                    val freq = defaultFreqs.getOrElse(i) { "${i + 1}" }
                    val currentVal = bandLevels.getOrElse(i) { 0 }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = freq,
                            fontSize = 11.sp,
                            color = theme.subtextColor,
                            modifier = Modifier.width(55.dp)
                        )

                        Slider(
                            value = currentVal.toFloat(),
                            onValueChange = { newVal ->
                                val intVal = newVal.toInt()
                                bandLevels[i] = intVal
                                selectedPreset = 9 // Custom
                                prefs.setEqPreset(9)
                                prefs.setBandLevel(i, intVal)
                                eqController?.setBandLevel(i, intVal)
                            },
                            valueRange = -1500f..1500f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = theme.primary,
                                activeTrackColor = theme.primary,
                                inactiveTrackColor = theme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f).testTag("slider_eq_band_$i")
                        )

                        Text(
                            text = "${if (currentVal >= 0) "+" else ""}${currentVal / 100} dB",
                            fontSize = 11.sp,
                            color = theme.textColor,
                            modifier = Modifier.width(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bass Boost Wheel / Slider
                Text(text = "Bass Boost Effect", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bass",
                        fontSize = 12.sp,
                        color = theme.subtextColor,
                        modifier = Modifier.width(55.dp)
                    )

                    Slider(
                        value = bassBoost.toFloat(),
                        onValueChange = {
                            val intVal = it.toInt()
                            bassBoost = intVal
                            prefs.setBassBoost(intVal)
                            eqController?.setBassBoost(intVal)
                        },
                        valueRange = 0f..1000f,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primary,
                            activeTrackColor = theme.primary,
                            inactiveTrackColor = theme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f).testTag("slider_bass_boost")
                    )

                    Text(
                        text = "${bassBoost / 10}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primary,
                        modifier = Modifier.width(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("button_close_eq")
                ) {
                    Text("Done", color = theme.background, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
