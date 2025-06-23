package com.example.alphamonitoringapp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.alphamonitoringapp.R
import com.example.alphamonitoringapp.ui.SplashActivity
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginScreen : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 1001
    private val RC_AUTH_RECOVER = 999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // ✅ Configure Google Sign-In with FULL Drive scope
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestServerAuthCode(getString(R.string.default_web_client_id)) // Optional
            .requestScopes(
                Scope("https://www.googleapis.com/auth/gmail.readonly"),
                Scope("https://www.googleapis.com/auth/drive"), // ⬅️ FULL access, needed for webViewLink
                Scope("https://www.googleapis.com/auth/photoslibrary.readonly")
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ✅ Set up sign-in button
        val googleSignInBtn: SignInButton = findViewById(R.id.google_sign_in_button)
        googleSignInBtn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_SIGN_IN -> {
                val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("LoginScreen", "firebaseAuthWithGoogle: ${account.id}")
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    Log.w("LoginScreen", "Google sign in failed", e)
                    Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            RC_AUTH_RECOVER -> {
                Toast.makeText(this, "Permission granted, please reopen app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val idToken = account.idToken
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()

                    // ✅ Use FULL scope (not readonly) to get access token
                    val scope = "oauth2:https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/drive https://www.googleapis.com/auth/photoslibrary.readonly"

                    if (account.account != null) {
                        Thread {
                            try {
                                val token = GoogleAuthUtil.getToken(applicationContext, account.account!!, scope)
                                Log.d("AccessToken", "✅ Token: $token")
                                // You can now use this token to access Gmail, Drive (viewable), and Photos

                            } catch (e: UserRecoverableAuthException) {
                                Log.e("AccessToken", "🔁 Recoverable error: ${e.message}")
                                startActivityForResult(e.intent!!, RC_AUTH_RECOVER)

                            } catch (e: Exception) {
                                Log.e("AccessToken", "❌ Token fetch failed: ${e.message}")
                                e.printStackTrace()
                            }
                        }.start()
                    } else {
                        Log.e("AccessToken", "❌ Google account is null")
                    }

                    // ✅ Move to SplashActivity
                    startActivity(Intent(this, SplashActivity::class.java))
                    finish()

                } else {
                    Log.w("LoginScreen", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Firebase login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
