package com.danielgoodwyn.mycountdowns.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danielgoodwyn.mycountdowns.data.FirestoreManager
import com.danielgoodwyn.mycountdowns.data.MultiProfileManager
import com.danielgoodwyn.mycountdowns.model.ColorSwatch
import com.danielgoodwyn.mycountdowns.model.ResetItem
import com.danielgoodwyn.mycountdowns.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CountdownsViewModel(application: Application) : AndroidViewModel(application) {

    private val multiProfileManager = MultiProfileManager.getInstance(application)
    private val firestoreManager = FirestoreManager.getInstance(application)

    val profiles: StateFlow<List<UserProfile>> = multiProfileManager.profiles
    
    val sortOption: StateFlow<String> = firestoreManager.sortOption
    
    val items: StateFlow<List<ResetItem>> = firestoreManager.items
    
    val swatches: StateFlow<List<ColorSwatch>> = firestoreManager.swatches

    init {
        multiProfileManager.configure()
        firestoreManager.startListeningToSettings()
        firestoreManager.startListening()
        firestoreManager.startListeningToSwatches()
    }
    
    fun addProfile(): FirebaseAuth {
        return multiProfileManager.addProfile()
    }
    
    fun getDefaultAuth(): FirebaseAuth {
        return multiProfileManager.getDefaultAuth()
    }
    
    fun logout(appName: String) {
        multiProfileManager.logout(appName)
    }
    
    fun saveItem(item: ResetItem) {
        firestoreManager.saveItem(item)
    }
    
    fun deleteItem(item: ResetItem) {
        firestoreManager.deleteItem(item)
    }
    
    fun saveSwatch(swatch: ColorSwatch) {
        firestoreManager.saveSwatch(swatch)
    }
    
    fun deleteSwatch(swatch: ColorSwatch) {
        firestoreManager.deleteSwatch(swatch)
    }
    
    fun moveItem(fromIndex: Int, toIndex: Int, currentList: List<ResetItem>) {
        val mutableList = currentList.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
        mutableList.forEachIndexed { index, resetItem ->
            if (resetItem.orderIndex != index) {
                resetItem.orderIndex = index
                saveItem(resetItem)
            }
        }
    }
    
    fun updateSortOption(sort: String) {
        firestoreManager.updateSortOption(sort)
    }
    
    fun updateProfilePhoto(base64: String, profileId: String) {
        firestoreManager.updateProfilePhoto(base64, profileId)
    }
    
    fun updateProfileRingColor(hex: String, profileId: String) {
        firestoreManager.updateProfileRingColor(hex, profileId)
    }
    
    override fun onCleared() {
        super.onCleared()
        firestoreManager.stopListening()
        firestoreManager.stopListeningToSwatches()
        firestoreManager.stopListeningToSettings()
    }
}
