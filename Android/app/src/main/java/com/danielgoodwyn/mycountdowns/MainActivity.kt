package com.danielgoodwyn.mycountdowns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import com.danielgoodwyn.mycountdowns.ui.LoginScreen
import com.danielgoodwyn.mycountdowns.ui.DashboardScreen
import com.danielgoodwyn.mycountdowns.ui.CountdownsViewModel
import com.danielgoodwyn.mycountdowns.ui.theme.MyCountdownsTheme
import com.google.firebase.auth.FirebaseAuth
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Notification permission granted or denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Disable app verification for testing to avoid Recaptcha failures on emulators
        FirebaseAuth.getInstance().firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
        
        // Request POST_NOTIFICATIONS runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        com.danielgoodwyn.mycountdowns.notifications.NotificationScheduler.ensureChannel(this)
        
        setContent {
            MyCountdownsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: CountdownsViewModel = viewModel()
                    val profiles by viewModel.profiles.collectAsState()
                    
                    var showLoginScreen by remember { mutableStateOf(false) }
                    var isAddingProfile by remember { mutableStateOf(false) }
                    
                    if (profiles.isEmpty() || showLoginScreen) {
                        LoginScreen(
                            authInstance = if (isAddingProfile) viewModel.addProfile() else viewModel.getDefaultAuth(),
                            profiles = profiles,
                            isAddingProfile = isAddingProfile,
                            onSuccessfulLogin = { 
                                showLoginScreen = false 
                                isAddingProfile = false
                            },
                            onCancel = { 
                                showLoginScreen = false
                                isAddingProfile = false
                            },
                            onLogout = {
                                // Handled inside or we can pass the app name
                            }
                        )
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            onLoginClick = {
                                isAddingProfile = true
                                showLoginScreen = true
                            }
                        )
                    }
                }
            }
        }
    }
}
