
package com.example.care2give

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class ReserveFoodActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var ivReserveFood: ImageView
    private lateinit var tvReserveFoodName: TextView
    private lateinit var tvReserveCategory: TextView
    private lateinit var tvReserveQuantity: TextView
    private lateinit var tvReserveLocation: TextView
    private lateinit var tvReserveExpiry: TextView
    private lateinit var tvReserveStatus: TextView
    private lateinit var progressReserve: ProgressBar
    private lateinit var btnConfirmReserve: Button

    private var foodId: String = ""
    private var foodName: String = ""
    private var donorId: String = ""
    private var foodAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserve_food)

        ivReserveFood = findViewById(R.id.ivReserveFood)
        tvReserveFoodName = findViewById(R.id.tvReserveFoodName)
        tvReserveCategory = findViewById(R.id.tvReserveCategory)
        tvReserveQuantity = findViewById(R.id.tvReserveQuantity)
        tvReserveLocation = findViewById(R.id.tvReserveLocation)
        tvReserveExpiry = findViewById(R.id.tvReserveExpiry)
        tvReserveStatus = findViewById(R.id.tvReserveStatus)
        progressReserve = findViewById(R.id.progressReserve)
        btnConfirmReserve = findViewById(R.id.btnConfirmReserve)

        foodId = intent.getStringExtra("foodId").orEmpty()

        findViewById<Button>(R.id.btnCancelReserve).setOnClickListener {
            finish()
        }

        btnConfirmReserve.isEnabled = false

        if (foodId.isBlank()) {
            Toast.makeText(
                this,
                "Food listing not found",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        btnConfirmReserve.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Reservation")
                .setMessage("Reserve \"$foodName\"?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reserve") { _, _ ->
                    checkRecipientAndReserve()
                }
                .show()
        }

        loadFoodDetails()
    }

    private fun loadFoodDetails() {
        progressReserve.visibility = View.VISIBLE
        btnConfirmReserve.isEnabled = false

        db.collection("food_listings")
            .document(foodId)
            .get()
            .addOnSuccessListener { document ->
                progressReserve.visibility = View.GONE

                if (!document.exists()) {
                    Toast.makeText(
                        this,
                        "This food listing no longer exists",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@addOnSuccessListener
                }

                foodName = document.getString("foodName") ?: "Unnamed Food"
                donorId = document.getString("donorId").orEmpty()

                val category = document.getString("category") ?: "-"
                val quantity = document.get("quantity")?.toString() ?: "-"
                val location = document.getString("pickupLocation") ?: "-"
                val status = document.getString("status") ?: "-"
                val imageUrl = document.getString("imageUrl").orEmpty()

                val expiry = document.getTimestamp("expiryDate")?.toDate()
                val formattedExpiry = if (expiry != null) {
                    SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    ).format(expiry)
                } else {
                    "-"
                }

                tvReserveFoodName.text = foodName
                tvReserveCategory.text = "Category: $category"
                tvReserveQuantity.text = "Quantity: $quantity"
                tvReserveLocation.text = "Pickup Location: $location"
                tvReserveExpiry.text = "Expiry Date: $formattedExpiry"
                tvReserveStatus.text = "Status: $status"

                foodAvailable = status == "Available"
                btnConfirmReserve.isEnabled = foodAvailable

                if (imageUrl.startsWith("https://")) {
                    loadImage(imageUrl)
                }
            }
            .addOnFailureListener { e ->
                progressReserve.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Failed to load food: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadImage(imageUrl: String) {
        Thread {
            try {
                val connection = URL(imageUrl).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val bitmap = connection.getInputStream().use {
                    BitmapFactory.decodeStream(it)
                }

                runOnUiThread {
                    if (!isFinishing && !isDestroyed && bitmap != null) {
                        ivReserveFood.setImageBitmap(bitmap)
                    }
                }
            } catch (_: Exception) {
                // Keep placeholder if image fails
            }
        }.start()
    }

    private fun checkRecipientAndReserve() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please login first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnConfirmReserve.isEnabled = false
        progressReserve.visibility = View.VISIBLE

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { profile ->

                if (profile.getString("role") != "Recipient") {
                    progressReserve.visibility = View.GONE
                    btnConfirmReserve.isEnabled = true

                    Toast.makeText(
                        this,
                        "Only Recipients can reserve food",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                reserveFood(user.uid)
            }
            .addOnFailureListener { e ->
                progressReserve.visibility = View.GONE
                btnConfirmReserve.isEnabled = true

                Toast.makeText(
                    this,
                    "Unable to verify role: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun reserveFood(recipientId: String) {

        val foodRef = db.collection("food_listings").document(foodId)
        val reservationRef = db.collection("reservations").document(foodId)

        db.runTransaction { transaction ->

            val foodSnapshot = transaction.get(foodRef)

            if (!foodSnapshot.exists()) {
                throw IllegalStateException("Food listing not found")
            }

            if (foodSnapshot.getString("status") != "Available") {
                throw IllegalStateException("This food is already reserved")
            }

            if (foodSnapshot.getString("donorId") == recipientId) {
                throw IllegalStateException("You cannot reserve your own food")
            }

            val reservation = hashMapOf<String, Any>(
                "foodId" to foodId,
                "foodName" to (foodSnapshot.getString("foodName") ?: ""),
                "donorId" to (foodSnapshot.getString("donorId") ?: ""),
                "recipientId" to recipientId,
                "status" to "Reserved",
                "createdAt" to FieldValue.serverTimestamp()
            )

            transaction.set(reservationRef, reservation)

            transaction.update(
                foodRef,
                "status",
                "Reserved"
            )

            null
        }
            .addOnSuccessListener {
                progressReserve.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Food reserved successfully!",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
            .addOnFailureListener { e ->
                progressReserve.visibility = View.GONE
                btnConfirmReserve.isEnabled = foodAvailable

                Toast.makeText(
                    this,
                    "Reservation failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()

                loadFoodDetails()
            }
    }
}
