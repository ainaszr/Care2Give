
package com.example.care2give

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Patterns

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

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

        btnRegister.setOnClickListener {

            val nameInput = fullName.text.toString().trim()
            val emailInput = email.text.toString().trim()
            val passwordInput = password.text.toString()
            val confirmInput = confirmPassword.text.toString()

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
                    Toast.makeText(
                        this,
                        "Form validated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
