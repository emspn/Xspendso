package com.app.xspendso.auth

import android.content.Context
import com.app.xspendso.R
import com.app.xspendso.data.PrefsManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context, private val prefsManager: PrefsManager) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(idToken: String): Boolean {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { saveUserInfo(it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun saveUserInfo(user: FirebaseUser) {
        prefsManager.userEmail = user.email
        if (prefsManager.userName == "User" || prefsManager.userName.isNullOrBlank()) {
            prefsManager.userName = user.displayName
        }
        prefsManager.userPhotoUrl = user.photoUrl?.toString()
    }

    fun isUserLoggedIn(): Boolean {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            saveUserInfo(currentUser)
        }
        return currentUser != null
    }

    fun signOut() {
        auth.signOut()
        getGoogleSignInClient().signOut()
    }
}
