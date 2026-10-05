package com.danielgoodwyn.mycountdowns.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielgoodwyn.mycountdowns.model.ColorSwatch
import com.danielgoodwyn.mycountdowns.model.UserProfile

@Composable
fun EditSwatchDialog(
    swatch: ColorSwatch?,
    profiles: List<UserProfile>,
    onDismiss: () -> Unit,
    onSave: (name: String, hexColor: String, profiles: Set<String>) -> Unit,
    onDelete: (ColorSwatch) -> Unit
) {
    if (swatch == null) return

    var name by remember { mutableStateOf(swatch.name) }
    var hexColor by remember { mutableStateOf(swatch.hexColor) }
    var selectedProfiles by remember { mutableStateOf(setOf(swatch.profileId ?: "")) }
    
    var showAdvancedPicker by remember { mutableStateOf(false) }

    if (showAdvancedPicker) {
        AdvancedColorPicker(
            initialColorHex = hexColor,
            onColorSelected = { 
                hexColor = it
                showAdvancedPicker = false 
            },
            onDismiss = { showAdvancedPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (swatch.name.isEmpty()) "New Swatch" else "Edit Swatch") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Swatch Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Color", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    
                    val colorStr = if (hexColor.startsWith("#")) hexColor else "#$hexColor"
                    val color = try { Color(android.graphics.Color.parseColor(colorStr)) } catch (e: Exception) { Color.Gray }
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { showAdvancedPicker = true }
                    )
                }
                
                if (profiles.size > 1) {
                    Text("Apply to Accounts", fontWeight = FontWeight.SemiBold)
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(profiles) { profile ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedProfiles.contains(profile.appName)) {
                                            selectedProfiles = selectedProfiles - profile.appName
                                        } else {
                                            selectedProfiles = selectedProfiles + profile.appName
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedProfiles.contains(profile.appName),
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(profile.email)
                            }
                        }
                    }
                }

                Button(
                    onClick = { onDelete(swatch) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Swatch")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, hexColor, selectedProfiles) },
                enabled = name.isNotBlank() && selectedProfiles.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
