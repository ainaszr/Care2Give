
package com.example.care2give

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                email.isEmpty() -> {
                    etEmail.error = "Please enter your email"
                }

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "Please enter a valid email"
                }

                password.isEmpty() -> {
                    etPassword.error = "Please enter your password"
                }

                else -> {
                    btnLogin.isEnabled = false
                    btnLogin.text = "Logging in..."

                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->

                            if (task.isSuccessful) {

                                val userId = auth.currentUser?.uid

                                if (userId == null) {
                                    resetButton(btnLogin)
                                    Toast.makeText(
                                        this,
                                        "Unable to retrieve user account",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnCompleteListener
                                }

                                db.collection("users")
                                    .document(userId)
                                    .get()
                                    .addOnSuccessListener { document ->

                                        if (document.exists()) {

                                            val role = document.getString("role")

                                            Toast.makeText(
                                                this,
                                                "Login successful! Role: $role",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            val intent = Intent(this, DashboardActivity::class.java)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()

                                        } else {
                                            Toast.makeText(
                                                this,
                                                "User profile not found",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        resetButton(btnLogin)
                                    }
                                    .addOnFailureListener { e ->

                                        resetButton(btnLogin)

                                        Toast.makeText(
                                            this,
                                            "Unable to load profile: ${e.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                            } else {

                                resetButton(btnLogin)

                                Toast.makeText(
                                    this,
                                    "Login failed: ${task.exception?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
        }

        tvRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    private fun resetButton(button: Button) {
        button.isEnabled = true
        button.text = "Login"
    }
}
