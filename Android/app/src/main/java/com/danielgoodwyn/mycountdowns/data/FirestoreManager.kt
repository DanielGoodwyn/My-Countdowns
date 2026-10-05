package com.danielgoodwyn.mycountdowns.data

import android.content.Context
import com.danielgoodwyn.mycountdowns.model.ColorSwatch
import com.danielgoodwyn.mycountdowns.model.ResetItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class FirestoreManager private constructor(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.Main)
    private val multiProfileManager = MultiProfileManager.getInstance(context)
    
    private val _items = MutableStateFlow<List<ResetItem>>(emptyList())
    val items: StateFlow<List<ResetItem>> = _items.asStateFlow()
    
    private val _swatches = MutableStateFlow<List<ColorSwatch>>(emptyList())
    val swatches: StateFlow<List<ColorSwatch>> = _swatches.asStateFlow()
    
    private val _sortOption = MutableStateFlow("Soonest First")
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()
    
    private val itemListeners = mutableMapOf<String, ListenerRegistration>()
    private val swatchListeners = mutableMapOf<String, ListenerRegistration>()
    private val settingsListeners = mutableMapOf<String, ListenerRegistration>()
    
    companion object {
        @Volatile
        private var instance: FirestoreManager? = null
        
        fun getInstance(context: Context): FirestoreManager {
            return instance ?: synchronized(this) {
                instance ?: FirestoreManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        scope.launch {
            multiProfileManager.profiles.collect {
                // Restart listeners when profiles change
                startListening()
                startListeningToSwatches()
                startListeningToSettings()
            }
        }
    }
    
    fun startListening() {
        stopListening()
        
        val profiles = multiProfileManager.profiles.value
        
        for (profile in profiles) {
            val db = multiProfileManager.getFirestore(profile.appName) ?: continue
            
            val listener = db.collection("users").document(profile.uid).collection("resets")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreManager", "Listen failed for ${profile.appName}", error)
                        return@addSnapshotListener
                    }
                    android.util.Log.d("FirestoreManager", "Snapshot received for ${profile.appName} with ${snapshot?.documents?.size} docs")
                    val currentItems = _items.value.toMutableList()
                    // Remove items for this profile to replace them
                    currentItems.removeAll { it.profileId == profile.appName }
                    
                    snapshot?.documents?.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        
                        android.util.Log.d("FirestoreManager", "Received doc: ${doc.id} with data: $data")
                        
                        val item = ResetItem(
                            id = doc.id,
                            name = data["name"] as? String ?: "",
                            resetTime = (data["resetTime"] as? Number)?.toLong() ?: 0L,
                            hexColor = data["hexColor"] as? String ?: "#0000FF",
                            imageDataBase64 = data["imageDataBase64"] as? String,
                            orderIndex = (data["orderIndex"] as? Number)?.toInt() ?: 0,
                            profileId = profile.appName
                        )
                        currentItems.add(item)
                    }
                    android.util.Log.d("FirestoreManager", "Total items loaded for ${profile.appName}: ${currentItems.size}")
                    _items.value = currentItems
                }
            itemListeners[profile.appName] = listener
        }
    }
    
    fun stopListening() {
        itemListeners.values.forEach { it.remove() }
        itemListeners.clear()
        _items.value = emptyList()
    }
    
    fun saveItem(item: ResetItem) {
        val profileId = item.profileId
        val profile = multiProfileManager.profiles.value.firstOrNull { it.appName == profileId } ?: return
        val db = multiProfileManager.getFirestore(profile.appName) ?: return
        
        val data = hashMapOf(
            "name" to item.name,
            "hexColor" to item.hexColor,
            "resetTime" to item.resetTime,
            "orderIndex" to item.orderIndex
        )
        item.imageDataBase64?.let {
            data["imageDataBase64"] = it
        }
        
        db.collection("users").document(profile.uid).collection("resets").document(item.id)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
    }
    
    fun deleteItem(item: ResetItem) {
        val profileId = item.profileId
        val profile = multiProfileManager.profiles.value.firstOrNull { it.appName == profileId } ?: return
        val db = multiProfileManager.getFirestore(profile.appName) ?: return
        
        db.collection("users").document(profile.uid).collection("resets").document(item.id).delete()
    }
    
    fun startListeningToSwatches() {
        stopListeningToSwatches()
        
        val profiles = multiProfileManager.profiles.value
        
        for (profile in profiles) {
            val db = multiProfileManager.getFirestore(profile.appName) ?: continue
            
            val listener = db.collection("users").document(profile.uid).collection("swatches")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    
                    val currentSwatches = _swatches.value.toMutableList()
                    currentSwatches.removeAll { it.profileId == profile.appName }
                    
                    snapshot?.documents?.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        
                        val swatch = ColorSwatch(
                            id = doc.id,
                            name = data["name"] as? String ?: "",
                            hexColor = data["hexColor"] as? String ?: "#0000FF",
                            profileId = profile.appName
                        )
                        currentSwatches.add(swatch)
                    }
                    _swatches.value = currentSwatches
                }
            swatchListeners[profile.appName] = listener
        }
    }
    
    fun stopListeningToSwatches() {
        swatchListeners.values.forEach { it.remove() }
        swatchListeners.clear()
        _swatches.value = emptyList()
    }
    
    fun saveSwatch(swatch: ColorSwatch) {
        val profileId = swatch.profileId
        val profile = multiProfileManager.profiles.value.firstOrNull { it.appName == profileId } ?: return
        val db = multiProfileManager.getFirestore(profile.appName) ?: return
        
        val data = hashMapOf(
            "name" to swatch.name,
            "hexColor" to swatch.hexColor
        )
        
        db.collection("users").document(profile.uid).collection("swatches").document(swatch.id)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
    }
    
    fun deleteSwatch(swatch: ColorSwatch) {
        val profileId = swatch.profileId
        val profile = multiProfileManager.profiles.value.firstOrNull { it.appName == profileId } ?: return
        val db = multiProfileManager.getFirestore(profile.appName) ?: return
        
        db.collection("users").document(profile.uid).collection("swatches").document(swatch.id).delete()
    }
    
    fun startListeningToSettings() {
        stopListeningToSettings()
        
        val profiles = multiProfileManager.profiles.value
        
        for (profile in profiles) {
            val db = multiProfileManager.getFirestore(profile.appName) ?: continue
            
            val listener = db.collection("users").document(profile.uid).collection("settings").document("preferences")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    
                    val sortString = snapshot?.getString("sortOption") ?: return@addSnapshotListener
                    val iosFormat = when (sortString) {
                        "soonestAsc" -> "Soonest First"
                        "soonestDesc" -> "Latest First"
                        "alphabetical" -> "Alphabetical"
                        "custom" -> "Custom Order"
                        else -> sortString
                    }
                    _sortOption.value = iosFormat
                }
            settingsListeners[profile.appName] = listener
        }
    }
    
    fun stopListeningToSettings() {
        settingsListeners.values.forEach { it.remove() }
        settingsListeners.clear()
    }
    
    fun updateSortOption(sortString: String) {
        val webFormat = when (sortString) {
            "Soonest First" -> "soonestAsc"
            "Latest First" -> "soonestDesc"
            "Alphabetical" -> "alphabetical"
            "Custom Order" -> "custom"
            else -> sortString
        }
        
        val profiles = multiProfileManager.profiles.value
        for (profile in profiles) {
            val db = multiProfileManager.getFirestore(profile.appName) ?: continue
            db.collection("users").document(profile.uid).collection("settings").document("preferences")
                .set(hashMapOf("sortOption" to webFormat), com.google.firebase.firestore.SetOptions.merge())
        }
    }
    
    fun updateProfilePhoto(base64Data: String, profileId: String) {
        val profile = multiProfileManager.profiles.value.firstOrNull { it.appName == profileId } ?: return
        val db = multiProfileManager.getFirestore(profile.appName) ?: return
        
        db.collection("users").document(profile.uid).collection("settings").document("profile")
            .set(hashMapOf("photoBase64" to base64Data), com.google.firebase.firestore.SetOptions.merge())
    }
    
    fun updateProfileRingColor(hex: String, profileId: String) {
        val profile = multiProfileManager.profiles.value.firstOrNull { it.appName == profileId } ?: return
        val db = multiProfileManager.getFirestore(profile.appName) ?: return
        
        db.collection("users").document(profile.uid).collection("settings").document("profile")
            .set(hashMapOf("ringColor" to hex), com.google.firebase.firestore.SetOptions.merge())
    }
}
