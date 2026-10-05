package com.danielgoodwyn.mycountdowns.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danielgoodwyn.mycountdowns.model.ColorSwatch
import com.danielgoodwyn.mycountdowns.model.ResetItem
import com.danielgoodwyn.mycountdowns.model.UserProfile
import java.io.ByteArrayOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditCountdownDialog(
    item: ResetItem?,
    profiles: List<UserProfile>,
    swatches: List<ColorSwatch>,
    onDismiss: () -> Unit,
    onSave: (ResetItem) -> Unit,
    onDelete: ((ResetItem) -> Unit)? = null,
    onSaveSwatch: ((ColorSwatch) -> Unit)? = null,
    onDeleteSwatch: ((ColorSwatch) -> Unit)? = null
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(item?.name ?: "") }
    var hexColor by remember { mutableStateOf(item?.hexColor ?: "#2196F3") }
    var selectedProfileId by remember { mutableStateOf(item?.profileId ?: profiles.firstOrNull()?.appName ?: "") }
    var imageDataBase64 by remember { mutableStateOf(item?.imageDataBase64) }
    var resetTime by remember { mutableStateOf(item?.resetTime ?: (System.currentTimeMillis() + 86400000)) }
    
    var showProfileDropdown by remember { mutableStateOf(false) }

    var showNewSwatchDialog by remember { mutableStateOf(false) }
    var swatchToEdit by remember { mutableStateOf<ColorSwatch?>(null) }
    var newSwatchName by remember { mutableStateOf("") }
    
    var showAdvancedPicker by remember { mutableStateOf(false) }

    var swatchToDelete by remember { mutableStateOf<ColorSwatch?>(null) }

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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                
                // Scale down bitmap to save space
                val scaled = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                val outputStream = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val byteArray = outputStream.toByteArray()
                imageDataBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (swatchToEdit != null) {
        EditSwatchDialog(
            swatch = swatchToEdit,
            profiles = profiles,
            onDismiss = { swatchToEdit = null },
            onSave = { name, hex, selectedProfiles ->
                // First delete the original if it was an edit
                if (swatchToEdit?.name?.isNotEmpty() == true) {
                    onDeleteSwatch?.invoke(swatchToEdit!!)
                }
                
                // Then save for all selected profiles
                selectedProfiles.forEach { pid ->
                    onSaveSwatch?.invoke(ColorSwatch(name = name, hexColor = hex, profileId = pid))
                }
                
                swatchToEdit = null
            },
            onDelete = { swatch ->
                onDeleteSwatch?.invoke(swatch)
                swatchToEdit = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "New Countdown" else "Edit Countdown") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Image Picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(imageDataBase64) {
                        imageDataBase64?.let { decodeBase64ToImageBitmap(it) }
                    }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bitmap,
                            contentDescription = "Countdown Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .clickable { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Add Photo", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                TimeEditor(
                    resetTime = resetTime,
                    onTimeChanged = { resetTime = it }
                )
                
                // Color Picker
                Text("Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item {
                        // Advanced Picker Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                                .border(2.dp, Color.LightGray, CircleShape)
                                .clickable { showAdvancedPicker = true }
                        )
                    }

                    item {
                        // Selected Color (if not in swatches)
                        val isCustomSelected = swatches.none { it.hexColor.equals(hexColor.removePrefix("#"), ignoreCase = true) }
                        if (isCustomSelected) {
                            val colorStr = if (hexColor.startsWith("#")) hexColor else "#$hexColor"
                            val color = try { Color(android.graphics.Color.parseColor(colorStr)) } catch (e: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                    
                    items(swatches) { swatch ->
                        val colorStr = if (swatch.hexColor.startsWith("#")) swatch.hexColor else "#${swatch.hexColor}"
                        val color = try { Color(android.graphics.Color.parseColor(colorStr)) } catch (e: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (hexColor.equals(colorStr, ignoreCase = true) || hexColor.equals(colorStr.removePrefix("#"), ignoreCase = true)) 3.dp else 0.dp,
                                    color = if (hexColor.equals(colorStr, ignoreCase = true) || hexColor.equals(colorStr.removePrefix("#"), ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .combinedClickable(
                                    onClick = { hexColor = colorStr },
                                    onLongClick = { swatchToEdit = swatch }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (swatch.name.isNotBlank()) {
                                Text(swatch.name.take(1).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    item {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .clickable { swatchToEdit = ColorSwatch(name = "", hexColor = hexColor, profileId = selectedProfileId) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Swatch", tint = Color.DarkGray)
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showProfileDropdown,
                    onExpandedChange = { showProfileDropdown = !showProfileDropdown }
                ) {
                    val currentProfile = profiles.firstOrNull { it.appName == selectedProfileId }
                    OutlinedTextField(
                        value = currentProfile?.email ?: "Select Profile",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Profile") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProfileDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showProfileDropdown,
                        onDismissRequest = { showProfileDropdown = false }
                    ) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.email) },
                                onClick = {
                                    selectedProfileId = profile.appName
                                    showProfileDropdown = false
                                }
                            )
                        }
                    }
                }
                
                if (item != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onDelete?.invoke(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Countdown")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newItem = ResetItem(
                        id = item?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        hexColor = hexColor,
                        resetTime = item?.resetTime ?: (System.currentTimeMillis() + 86400000), // Default +1 day
                        imageDataBase64 = imageDataBase64,
                        orderIndex = item?.orderIndex ?: 0,
                        profileId = selectedProfileId
                    )
                    onSave(newItem)
                },
                enabled = name.isNotBlank() && selectedProfileId.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimeEditor(resetTime: Long, onTimeChanged: (Long) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isExactDate by remember { mutableStateOf(true) }

    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = resetTime

    val dateFormat = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    val timeFormat = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Target Time", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Duration", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, style = MaterialTheme.typography.bodySmall)
            Switch(
                checked = isExactDate,
                onCheckedChange = { isExactDate = it },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text("Exact Date", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        }

        if (isExactDate) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(year, month, dayOfMonth)
                                onTimeChanged(calendar.timeInMillis)
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(dateFormat.format(calendar.time))
                }
                
                OutlinedButton(
                    onClick = {
                        android.app.TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                calendar.set(java.util.Calendar.MINUTE, minute)
                                calendar.set(java.util.Calendar.SECOND, 0)
                                onTimeChanged(calendar.timeInMillis)
                            },
                            calendar.get(java.util.Calendar.HOUR_OF_DAY),
                            calendar.get(java.util.Calendar.MINUTE),
                            false
                        ).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(timeFormat.format(calendar.time))
                }
            }
        } else {
            val diff = resetTime - System.currentTimeMillis()
            val days = (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            val hours = ((diff / (1000 * 60 * 60)) % 24).coerceAtLeast(0)
            val mins = ((diff / (1000 * 60)) % 60).coerceAtLeast(0)

            var strDays by remember(days) { mutableStateOf(days.toString()) }
            var strHours by remember(hours) { mutableStateOf(hours.toString()) }
            var strMins by remember(mins) { mutableStateOf(mins.toString()) }

            val updateTime = {
                val d = strDays.toLongOrNull() ?: 0L
                val h = strHours.toLongOrNull() ?: 0L
                val m = strMins.toLongOrNull() ?: 0L
                val newDiff = (d * 24 * 60 * 60 * 1000) + (h * 60 * 60 * 1000) + (m * 60 * 1000)
                onTimeChanged(System.currentTimeMillis() + newDiff)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = strDays,
                    onValueChange = { strDays = it; updateTime() },
                    label = { Text("Days") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = strHours,
                    onValueChange = { strHours = it; updateTime() },
                    label = { Text("Hours") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = strMins,
                    onValueChange = { strMins = it; updateTime() },
                    label = { Text("Mins") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }
    }
}
