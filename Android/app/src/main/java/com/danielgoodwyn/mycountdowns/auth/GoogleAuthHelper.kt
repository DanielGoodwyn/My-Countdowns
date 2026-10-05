package com.danielgoodwyn.mycountdowns.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"
    private const val WEB_CLIENT_ID = "77456450491-aheoi1qj740dadhtjn5g43gpkg57scbe.apps.googleusercontent.com"

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun handleSignInResult(data: Intent?, auth: FirebaseAuth): Pair<Boolean, String?> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                Pair(true, null)
            } else {
                Pair(false, "No ID Token returned from Google")
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign in failed", e)
            Pair(false, "Google Sign-In failed: ${e.statusCode}")
        } catch (e: Exception) {
            Log.e(TAG, "Unknown Exception", e)
            Pair(false, "Unknown Error: ${e.message}")
        }
    }
}
