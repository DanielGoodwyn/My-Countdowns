package com.danielgoodwyn.mycountdowns.data

import android.content.Context
import android.content.SharedPreferences
import com.danielgoodwyn.mycountdowns.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MultiProfileManager private constructor(private val context: Context) {
    
    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val prefs: SharedPreferences = context.getSharedPreferences("multi_profile_prefs", Context.MODE_PRIVATE)
    
    private val authStateListeners = mutableMapOf<String, FirebaseAuth.AuthStateListener>()
    private val profilePhotoListeners = mutableMapOf<String, ListenerRegistration>()
    
    companion object {
        @Volatile
        private var instance: MultiProfileManager? = null
        
        fun getInstance(context: Context): MultiProfileManager {
            return instance ?: synchronized(this) {
                instance ?: MultiProfileManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    fun configure() {
        if (authStateListeners.isNotEmpty()) return
        
        val savedAppsSet = prefs.getStringSet("activeAppNames", setOf("[DEFAULT]")) ?: setOf("[DEFAULT]")
        val savedApps = savedAppsSet.toMutableList()
        
        if (!savedApps.contains("[DEFAULT]")) {
            savedApps.add("[DEFAULT]")
        }
        
        val defaultOptions = FirebaseApp.getInstance().options
        
        for (appName in savedApps) {
            if (appName == "[DEFAULT]") {
                setupListener(appName)
            } else {
                try {
                    FirebaseApp.getInstance(appName)
                    setupListener(appName)
                } catch (e: IllegalStateException) {
                    FirebaseApp.initializeApp(context, defaultOptions, appName)
                    setupListener(appName)
                }
            }
        }
    }
    
    private fun setupListener(appName: String) {
        val app = FirebaseApp.getInstance(appName)
        val auth = FirebaseAuth.getInstance(app)
        
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val currentProfiles = _profiles.value.toMutableList()
            
            if (user != null) {
                // If this uid is already in another profile, sign out this one (duplicate login)
                if (currentProfiles.any { it.uid == user.uid && it.appName != appName }) {
                    firebaseAuth.signOut()
                    return@AuthStateListener
                }
                
                val profile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: "Unknown",
                    photoUrl = user.photoUrl?.toString(),
                    appName = appName
                )
                
                val index = currentProfiles.indexOfFirst { it.appName == appName }
                if (index != -1) {
                    currentProfiles[index] = profile
                } else {
                    currentProfiles.add(profile)
                }
                
                _profiles.value = currentProfiles
                setupProfilePhotoListener(appName, user.uid)
            } else {
                currentProfiles.removeAll { it.appName == appName }
                _profiles.value = currentProfiles
                profilePhotoListeners[appName]?.remove()
                profilePhotoListeners.remove(appName)
            }
            saveActiveApps()
        }
        
        auth.addAuthStateListener(listener)
        authStateListeners[appName] = listener
    }
    
    private fun setupProfilePhotoListener(appName: String, uid: String) {
        val app = FirebaseApp.getInstance(appName)
        val db = FirebaseFirestore.getInstance(app)
        
        profilePhotoListeners[appName]?.remove()
        
        val listener = db.collection("users").document(uid).collection("settings").document("profile")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val data = snapshot?.data
                val photoBase64 = data?.get("photoBase64") as? String
                val ringColor = data?.get("ringColor") as? String
                
                val currentProfiles = _profiles.value.toMutableList()
                val index = currentProfiles.indexOfFirst { it.appName == appName }
                if (index != -1) {
                    val profile = currentProfiles[index].copy(
                        customPhotoBase64 = photoBase64,
                        ringColor = ringColor
                    )
                    currentProfiles[index] = profile
                    _profiles.value = currentProfiles
                }
            }
        
        profilePhotoListeners[appName] = listener
    }
    
    private fun saveActiveApps() {
        val activeApps = _profiles.value.map { it.appName }.toMutableList()
        if (authStateListeners.containsKey("[DEFAULT]") && !activeApps.contains("[DEFAULT]")) {
            val defaultAuth = try { FirebaseAuth.getInstance(FirebaseApp.getInstance("[DEFAULT]")) } catch (e: Exception) { null }
            if (defaultAuth?.currentUser != null) {
                activeApps.add("[DEFAULT]")
            }
        }
        
        prefs.edit().putStringSet("activeAppNames", activeApps.toSet()).apply()
    }
    
    fun addProfile(): FirebaseAuth {
        val newAppName = "profile_${UUID.randomUUID()}"
        val defaultOptions = FirebaseApp.getInstance().options
        
        FirebaseApp.initializeApp(context, defaultOptions, newAppName)
        setupListener(newAppName)
        
        return FirebaseAuth.getInstance(FirebaseApp.getInstance(newAppName))
    }
    
    fun getDefaultAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance(FirebaseApp.getInstance("[DEFAULT]"))
    }
    
    fun logout(appName: String) {
        val app = try { FirebaseApp.getInstance(appName) } catch (e: Exception) { return }
        val auth = FirebaseAuth.getInstance(app)
        
        auth.signOut()
        
        if (appName != "[DEFAULT]") {
            authStateListeners[appName]?.let {
                auth.removeAuthStateListener(it)
                authStateListeners.remove(appName)
            }
        }
        
        val currentProfiles = _profiles.value.toMutableList()
        currentProfiles.removeAll { it.appName == appName }
        _profiles.value = currentProfiles
        
        saveActiveApps()
    }
    
    fun getFirestore(appName: String): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(appName))
        } catch (e: Exception) {
            null
        }
    }
}
