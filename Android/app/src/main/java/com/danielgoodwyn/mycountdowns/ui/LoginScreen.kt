package com.danielgoodwyn.mycountdowns.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.danielgoodwyn.mycountdowns.auth.GoogleAuthHelper
import com.danielgoodwyn.mycountdowns.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authInstance: FirebaseAuth,
    profiles: List<UserProfile>,
    isAddingProfile: Boolean,
    onSuccessfulLogin: () -> Unit,
    onCancel: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    val currentUser = authInstance.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAddingProfile) "Add Profile" else "My Countdowns") },
                navigationIcon = {
                    if (isAddingProfile) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            if (currentUser != null) {
                Text("Currently logged in as:", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentUser.photoUrl != null) {
                        AsyncImage(
                            model = currentUser.photoUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Blue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (currentUser.email?.take(1) ?: "?").uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(currentUser.email ?: "Unknown", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isAddingProfile) {
                    Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text("Continue to Dashboard")
                    }
                } else {
                    Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text("Go to Dashboard")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(onClick = onLogout) {
                    Text("Log Out", color = Color.Red)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Text("Not logged in", color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red, fontSize = 12.sp)
            }
            if (message.isNotEmpty()) {
                Text(message, color = Color(0xFF4CAF50), fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    errorMessage = ""
                    message = ""
                    val trimmedEmail = email.trim()
                    
                    if (trimmedEmail.isEmpty() || password.isEmpty()) {
                        errorMessage = "Please enter both email and password."
                        return@Button
                    }
                    
                    if (isLogin) {
                        authInstance.signInWithEmailAndPassword(trimmedEmail, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = task.result?.user?.uid
                                    val isDuplicate = profiles.any { it.uid == uid && it.appName != authInstance.app.name }
                                    if (isDuplicate) {
                                        authInstance.signOut()
                                        errorMessage = "This email is already logged in as another active account! You can only have one instance per email."
                                        isLogin = true
                                    } else {
                                        message = "Successfully logged in!"
                                        onSuccessfulLogin()
                                    }
                                } else {
                                    errorMessage = task.exception?.localizedMessage ?: "Sign in failed"
                                }
                            }
                    } else {
                        authInstance.createUserWithEmailAndPassword(trimmedEmail, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    message = "Successfully logged in!"
                                    onSuccessfulLogin()
                                } else {
                                    errorMessage = task.exception?.localizedMessage ?: "Sign up failed"
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isLogin) "Sign In" else "Create Account")
            }
            
            if (isLogin) {
                TextButton(onClick = {
                    if (email.isEmpty()) {
                        errorMessage = "Please enter your email first to reset your password."
                        message = ""
                        return@TextButton
                    }
                    authInstance.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                message = "Password reset email sent! Check your inbox."
                                errorMessage = ""
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Failed to send reset email"
                                message = ""
                            }
                        }
                }) {
                    Text("Forgot my password", color = Color.Blue, fontSize = 12.sp)
                }
            }
            
            TextButton(onClick = { isLogin = !isLogin }) {
                Text(
                    text = if (isLogin) "Don't have an account? Sign up" else "Already have an account? Sign in",
                    color = Color.Blue,
                    fontSize = 12.sp
                )
            }
            
            
            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                scope.launch {
                    val (success, errorStr) = GoogleAuthHelper.handleSignInResult(result.data, authInstance)
                    if (success) {
                        val uid = authInstance.currentUser?.uid
                        val isDuplicate = profiles.any { it.uid == uid && it.appName != authInstance.app.name }
                        if (isDuplicate) {
                            authInstance.signOut()
                            errorMessage = "This email is already logged in as another active account!"
                        } else {
                            message = "Successfully logged in with Google!"
                            onSuccessfulLogin()
                        }
                    } else {
                        errorMessage = errorStr ?: "Google Sign-In failed or was cancelled."
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    errorMessage = ""
                    val intent = GoogleAuthHelper.getSignInClient(context).signInIntent
                    googleSignInLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Continue with Google", color = Color.Black)
            }
        }
    }
}
