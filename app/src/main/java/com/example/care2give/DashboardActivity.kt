
package com.example.care2give

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvRole = findViewById<TextView>(R.id.tvRole)
        val btnAction1 = findViewById<Button>(R.id.btnAction1)
        val btnAction2 = findViewById<Button>(R.id.btnAction2)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val user = auth.currentUser

        if (user == null) {
            goToLogin()
            return
        }

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    Toast.makeText(
                        this,
                        "User profile not found",
                        Toast.LENGTH_LONG
                    ).show()

                    goToLogin()
                    return@addOnSuccessListener
                }

                val name = document.getString("fullName") ?: "User"
                val role = document.getString("role") ?: ""

                tvWelcome.text = "Hello, $name! 👋"
                tvRole.text = "Logged in as: $role"

                when (role) {

                    "Donor" -> {
                        btnAction1.text = "🍱 Add Food"
                        btnAction2.text = "📋 My Food Listings"
                    }

                    "Recipient" -> {
                        btnAction1.text = "🔎 Browse Food"
                        btnAction2.text = "🛒 My Reservations"
                    }

                    "Volunteer Collector" -> {
                        btnAction1.text = "🚚 Available Pickups"
                        btnAction2.text = "📦 My Pickups"
                    }

                    else -> {
                        Toast.makeText(
                            this,
                            "Unknown user role",
                            Toast.LENGTH_LONG
                        ).show()

                        goToLogin()
                        return@addOnSuccessListener
                    }
                }

                // FIRST BUTTON
                btnAction1.setOnClickListener {

                    when (role) {

                        "Donor" -> {
                            startActivity(
                                Intent(
                                    this,
                                    AddFoodActivity::class.java
                                )
                            )
                        }

                        "Recipient" -> {
                            startActivity(
                                Intent(
                                    this,
                                    BrowseFoodActivity::class.java
                                )
                            )
                        }

                        "Volunteer Collector" -> {
                            startActivity(
                                Intent(
                                    this,
                                    AvailablePickupsActivity::class.java
                                )
                            )
                        }
                    }
                }

                // SECOND BUTTON
                btnAction2.setOnClickListener {

                    when (role) {

                        "Donor" -> {
                            startActivity(
                                Intent(
                                    this,
                                    MyFoodListingsActivity::class.java
                                )
                            )
                        }

                        "Recipient" -> {
                            startActivity(
                                Intent(
                                    this,
                                    MyReservationsActivity::class.java
                                )
                            )
                        }

                        "Volunteer Collector" -> {
                            startActivity(
                                Intent(
                                    this,
                                    MyPickupsActivity::class.java
                                )
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Failed to load profile: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

        // LOGOUT
        btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {

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
}
