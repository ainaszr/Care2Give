
package com.example.care2give

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val fullName = findViewById<EditText>(R.id.etFullName)
        val email = findViewById<EditText>(R.id.etRegisterEmail)
        val password = findViewById<EditText>(R.id.etRegisterPassword)
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        val roles = arrayOf(
            "Select your role",
            "Donor",
            "Recipient",
            "Volunteer Collector"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerRole.adapter = adapter

        // REGISTER BUTTON
        btnRegister.setOnClickListener {

            val nameInput = fullName.text.toString().trim()
            val emailInput = email.text.toString().trim()
            val passwordInput = password.text.toString()
            val confirmInput = confirmPassword.text.toString()
            val selectedRole = spinnerRole.selectedItem.toString()

            when {
                nameInput.isEmpty() -> {
                    fullName.error = "Please enter your full name"
                }

                emailInput.isEmpty() -> {
                    email.error = "Please enter your email"
                }

                !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches() -> {
                    email.error = "Please enter a valid email"
                }

                passwordInput.length < 6 -> {
                    password.error = "Password must be at least 6 characters"
                }

                passwordInput != confirmInput -> {
                    confirmPassword.error = "Passwords do not match"
                }

                spinnerRole.selectedItemPosition == 0 -> {
                    Toast.makeText(
                        this,
                        "Please select your role",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {

                    btnRegister.isEnabled = false
                    btnRegister.text = "Creating Account..."

                    auth.createUserWithEmailAndPassword(
                        emailInput,
                        passwordInput
                    ).addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            val user = auth.currentUser

                            if (user == null) {
                                btnRegister.isEnabled = true
                                btnRegister.text = "Create Account"

                                Toast.makeText(
                                    this,
                                    "Unable to get user information",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@addOnCompleteListener
                            }

                            val userId = user.uid

                            val userData = hashMapOf(
                                "userId" to userId,
                                "fullName" to nameInput,
                                "email" to emailInput,
                                "role" to selectedRole
                            )

                            db.collection("users")
                                .document(userId)
                                .set(userData)
                                .addOnSuccessListener {

                                    Toast.makeText(
                                        this,
                                        "Account created successfully!",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    auth.signOut()

                                    val intent = Intent(
                                        this,
                                        LoginActivity::class.java
                                    )

                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->

                                    btnRegister.isEnabled = true
                                    btnRegister.text = "Create Account"

                                    Toast.makeText(
                                        this,
                                        "Profile saving failed: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                        } else {

                            btnRegister.isEnabled = true
                            btnRegister.text = "Create Account"

                            Toast.makeText(
                                this,
                                "Registration failed: ${task.exception?.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        // BACK TO LOGIN
        tvLogin.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            finish()
        }
    }
}
