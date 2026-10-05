package com.danielgoodwyn.mycountdowns.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.AsyncImage
import com.danielgoodwyn.mycountdowns.model.ResetItem
import com.danielgoodwyn.mycountdowns.model.UserProfile
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CountdownsViewModel,
    onLoginClick: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val items by viewModel.items.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val swatches by viewModel.swatches.collectAsState()

    var editingItem by remember { mutableStateOf<ResetItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    if (editingItem != null || isCreatingNew) {
        EditCountdownDialog(
            item = editingItem,
            profiles = profiles,
            swatches = swatches,
            onDismiss = {
                editingItem = null
                isCreatingNew = false
            },
            onSave = { newItem ->
                viewModel.saveItem(newItem)
                editingItem = null
                isCreatingNew = false
            },
            onDelete = { itemToDelete ->
                viewModel.deleteItem(itemToDelete)
                editingItem = null
                isCreatingNew = false
            },
            onSaveSwatch = { viewModel.saveSwatch(it) },
            onDeleteSwatch = { viewModel.deleteSwatch(it) }
        )
    }

    var expandedProfileMenu by remember { mutableStateOf<String?>(null) }
    var profileToChangePhoto by remember { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            profileToChangePhoto?.let { appName ->
                try {
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, it))
                    } else {
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    }
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                    val outputStream = java.io.ByteArrayOutputStream()
                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val base64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                    
                    viewModel.updateProfilePhoto(base64, appName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        profileToChangePhoto = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Countdowns", fontWeight = FontWeight.Bold) },
                actions = {
                    profiles.forEach { profile ->
                        Box {
                            ProfileAvatar(
                                profile = profile,
                                onClick = { expandedProfileMenu = profile.appName }
                            )
                            DropdownMenu(
                                expanded = expandedProfileMenu == profile.appName,
                                onDismissRequest = { expandedProfileMenu = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Change Photo") },
                                    onClick = {
                                        profileToChangePhoto = profile.appName
                                        expandedProfileMenu = null
                                        photoLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Log Out") },
                                    onClick = {
                                        viewModel.logout(profile.appName)
                                        expandedProfileMenu = null
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onLoginClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Account")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isCreatingNew = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        val sortedItems = when (sortOption) {
            "Soonest First" -> items.sortedBy { it.resetTime }
            "Latest First" -> items.sortedByDescending { it.resetTime }
            "Alphabetical" -> items.sortedBy { it.name.lowercase() }
            "Custom Order" -> items.sortedBy { it.orderIndex }
            else -> items
        }
        
        LaunchedEffect(sortedItems.size) {
            android.util.Log.d("DashboardScreen", "Rendering ${sortedItems.size} items, profiles size: ${profiles.size}")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = listOf("Soonest First", "Latest First", "Alphabetical", "Custom Order").indexOf(sortOption).coerceAtLeast(0),
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Soonest First", "Latest First", "Alphabetical", "Custom Order").forEach { title ->
                    Tab(
                        selected = sortOption == title,
                        onClick = { viewModel.updateSortOption(title) },
                        text = { Text(title) }
                    )
                }
            }

            if (profiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Welcome to My Countdowns!", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onLoginClick) {
                            Text("Sign In with Google")
                        }
                    }
                }
            } else {
                val isCustomOrder = sortOption == "Custom Order"
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedItems.size, key = { sortedItems[it].id }) { index ->
                        CountdownRow(
                            item = sortedItems[index],
                            isCustomOrder = isCustomOrder,
                            onMoveUp = if (isCustomOrder && index > 0) { { viewModel.moveItem(index, index - 1, sortedItems) } } else null,
                            onMoveDown = if (isCustomOrder && index < sortedItems.size - 1) { { viewModel.moveItem(index, index + 1, sortedItems) } } else null,
                            onClick = { editingItem = sortedItems[index] }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAvatar(profile: UserProfile, onClick: () -> Unit) {
    val ringColor = try {
        Color(android.graphics.Color.parseColor(profile.ringColor ?: "#FFFFFF"))
    } catch (e: Exception) {
        Color.White
    }
    
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(40.dp)
            .clip(CircleShape)
            .border(2.dp, ringColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val customBitmap = remember(profile.customPhotoBase64) {
            profile.customPhotoBase64?.let { decodeBase64ToImageBitmap(it) }
        }
        
        if (customBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = customBitmap,
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else if (profile.photoUrl != null) {
            AsyncImage(
                model = profile.photoUrl,
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun CountdownRow(
    item: ResetItem,
    isCustomOrder: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(calculateTimeLeft(item.resetTime)) }
    
    LaunchedEffect(item.resetTime) {
        while (true) {
            timeLeft = calculateTimeLeft(item.resetTime)
            delay(1000)
        }
    }
    
    val hexString = if (item.hexColor.startsWith("#")) item.hexColor else "#${item.hexColor}"
    val bgColor = try {
        Color(android.graphics.Color.parseColor(hexString))
    } catch (e: Exception) {
        Color.DarkGray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor.copy(alpha = 0.2f))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(bgColor))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val bgBitmap = remember(item.imageDataBase64) {
                item.imageDataBase64?.let { decodeBase64ToImageBitmap(it) }
            }
            if (bgBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bgBitmap,
                    contentDescription = "Background",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, bgColor, CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(2.dp, bgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                val dateFormat = remember { java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault()) }
                Text(
                    text = dateFormat.format(java.util.Date(item.resetTime)),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                
                Text(
                    text = "$timeLeft left",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (item.resetTime > System.currentTimeMillis()) MaterialTheme.colorScheme.onSurface else Color.Red
                )
            }

            if (isCustomOrder) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null, modifier = Modifier.size(32.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                    }
                    IconButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null, modifier = Modifier.size(32.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                    }
                }
            }
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(bgColor))
    }
}

fun decodeBase64ToImageBitmap(base64Str: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val imageBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

fun calculateTimeLeft(resetTimeMs: Long): String {
    val diff = resetTimeMs - System.currentTimeMillis()
    if (diff <= 0) return "Ready!"
    
    val days = diff / (1000 * 60 * 60 * 24)
    val hours = (diff / (1000 * 60 * 60)) % 24
    val mins = (diff / (1000 * 60)) % 60
    val secs = (diff / 1000) % 60
    
    if (days > 0) return "${days}d ${hours}h"
    if (hours > 0) return "${hours}h ${mins}m"
    return "${mins}m ${secs}s"
}
