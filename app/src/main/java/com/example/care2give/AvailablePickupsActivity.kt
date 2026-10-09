
package com.example.care2give

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class AvailablePickupsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var pickupsContainer: LinearLayout
    private lateinit var progressPickups: ProgressBar
    private lateinit var tvEmptyPickups: TextView

    private val green = Color.rgb(53, 107, 75)
    private val brown = Color.rgb(93, 64, 55)
    private val muted = Color.rgb(130, 140, 132)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_available_pickups)

        pickupsContainer = findViewById(R.id.pickupsContainer)
        progressPickups = findViewById(R.id.progressPickups)
        tvEmptyPickups = findViewById(R.id.tvEmptyPickups)

        findViewById<Button>(R.id.btnBackPickups).setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        loadAvailablePickups()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun roundedBackground(
        color: Int,
        radius: Int = 18,
        strokeColor: Int? = null
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (strokeColor != null) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun loadAvailablePickups() {

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please login first",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        progressPickups.visibility = View.VISIBLE
        tvEmptyPickups.visibility = View.GONE
        pickupsContainer.removeAllViews()

        // VERIFY VOLUNTEER ROLE
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { profile ->

                if (profile.getString("role") != "Volunteer Collector") {
                    progressPickups.visibility = View.GONE

                    Toast.makeText(
                        this,
                        "Only Volunteer Collectors can access pickups",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                // LOAD RESERVED PICKUPS
                db.collection("reservations")
                    .whereEqualTo("status", "Reserved")
                    .get()
                    .addOnSuccessListener { snapshot ->

                        progressPickups.visibility = View.GONE

                        if (snapshot.isEmpty) {
                            tvEmptyPickups.text =
                                "🚚 No pickups available right now.\n\n" +
                                        "Check back later for new food rescue opportunities!"

                            tvEmptyPickups.visibility = View.VISIBLE
                            return@addOnSuccessListener
                        }

                        val documents =
                            snapshot.documents.sortedByDescending {
                                it.getTimestamp("createdAt")?.seconds ?: 0L
                            }

                        documents.forEach { reservation ->
                            addPickupCard(reservation)
                        }
                    }
                    .addOnFailureListener { e ->

                        progressPickups.visibility = View.GONE
                        tvEmptyPickups.text = "Unable to load pickups."
                        tvEmptyPickups.visibility = View.VISIBLE

                        Toast.makeText(
                            this,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->

                progressPickups.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Failed to verify role: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun addPickupCard(
        reservation: DocumentSnapshot
    ) {

        val foodName =
            reservation.getString("foodName") ?: "Unnamed Food"

        val foodId =
            reservation.getString("foodId") ?: reservation.id

        // MAIN CARD
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            background = roundedBackground(
                Color.WHITE,
                20,
                Color.rgb(232, 237, 230)
            )

            elevation = dp(3).toFloat()
            clipToOutline = true
        }

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(18)
            leftMargin = dp(3)
            rightMargin = dp(3)
        }

        // FOOD IMAGE
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(190)
            )

            scaleType = ImageView.ScaleType.CENTER_CROP

            setBackgroundColor(
                Color.rgb(232, 237, 229)
            )

            contentDescription = "Food pickup image"
        }

        card.addView(imageView)

        // CARD CONTENT
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(20)
            )
        }

        // FOOD NAME
        val nameText = TextView(this).apply {
            text = "🍱 $foodName"
            textSize = 21f
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
        }

        content.addView(nameText)

        // STATUS BADGE
        val statusBadge = TextView(this).apply {
            text = "● Ready for Pickup"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(193, 122, 37))

            background = roundedBackground(
                Color.rgb(255, 242, 219),
                20
            )

            setPadding(
                dp(14),
                dp(8),
                dp(14),
                dp(8)
            )
        }

        content.addView(
            statusBadge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(14)
            }
        )

        // DIVIDER
        val divider = View(this).apply {
            setBackgroundColor(
                Color.rgb(235, 239, 233)
            )
        }

        content.addView(
            divider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )

        // INFORMATION ROW HELPER
        fun addInfo(
            icon: String,
            label: String,
            value: String
        ): TextView {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }

            val iconText = TextView(this).apply {
                text = icon
                textSize = 17f
                gravity = Gravity.CENTER
            }

            row.addView(
                iconText,
                LinearLayout.LayoutParams(
                    dp(30),
                    dp(30)
                )
            )

            val textContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val labelText = TextView(this).apply {
                text = label
                textSize = 12f
                setTextColor(muted)
            }

            val valueText = TextView(this).apply {
                text = value
                textSize = 15f
                setTextColor(brown)
                setTypeface(null, Typeface.BOLD)
            }

            textContainer.addView(labelText)
            textContainer.addView(valueText)

            row.addView(
                textContainer,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            content.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(14)
                }
            )

            return valueText
        }

        // FOOD DETAILS
        val locationText = addInfo(
            "📍",
            "Pickup Location",
            "Loading..."
        )

        val categoryText = addInfo(
            "🍽️",
            "Food Category",
            "Loading..."
        )

        val quantityText = addInfo(
            "📦",
            "Quantity",
            "Loading..."
        )

        val expiryText = addInfo(
            "📅",
            "Expiry Date",
            "Loading..."
        )

        // ACCEPT PICKUP BUTTON
        val btnAccept = Button(this).apply {

            text = "🚚 Accept Pickup"
            isAllCaps = false
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)

            backgroundTintList = ColorStateList.valueOf(
                Color.rgb(71, 122, 89)
            )

            setOnClickListener {

                AlertDialog.Builder(this@AvailablePickupsActivity)
                    .setTitle("Accept Pickup?")
                    .setMessage("Accept pickup for \"$foodName\"?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Accept") { _, _ ->
                        acceptPickup(reservation.id)
                    }
                    .show()
            }
        }

        content.addView(
            btnAccept,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(22)
            }
        )

        // LOAD ORIGINAL FOOD DETAILS
        db.collection("food_listings")
            .document(foodId)
            .get()
            .addOnSuccessListener { food ->

                if (!food.exists()) {
                    locationText.text = "Not available"
                    categoryText.text = "-"
                    quantityText.text = "-"
                    expiryText.text = "-"
                    return@addOnSuccessListener
                }

                locationText.text =
                    food.getString("pickupLocation") ?: "-"

                categoryText.text =
                    food.getString("category") ?: "-"

                quantityText.text =
                    food.get("quantity")?.toString() ?: "-"

                val expiryDate =
                    food.getTimestamp("expiryDate")?.toDate()

                expiryText.text = if (expiryDate != null) {
                    SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.getDefault()
                    ).format(expiryDate)
                } else {
                    "-"
                }

                // LOAD CLOUDINARY IMAGE
                val imageUrl =
                    food.getString("imageUrl").orEmpty()

                if (imageUrl.startsWith("https://")) {

                    Thread {
                        try {

                            val connection =
                                URL(imageUrl).openConnection()

                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000

                            val bitmap =
                                connection.getInputStream().use {
                                    BitmapFactory.decodeStream(it)
                                }

                            runOnUiThread {
                                if (
                                    !isFinishing &&
                                    !isDestroyed &&
                                    bitmap != null &&
                                    imageView.parent != null
                                ) {
                                    imageView.setImageBitmap(bitmap)
                                }
                            }

                        } catch (_: Exception) {
                            // Keep placeholder
                        }
                    }.start()
                }
            }
            .addOnFailureListener {

                locationText.text = "Unable to load"
                categoryText.text = "-"
                quantityText.text = "-"
                expiryText.text = "-"
            }

        card.addView(content)

        pickupsContainer.addView(
            card,
            cardParams
        )
    }

    // FIRESTORE TRANSACTION — KEEP PICKUP FLOW SAFE
    private fun acceptPickup(reservationId: String) {

        val user = auth.currentUser ?: return

        val reservationRef =
            db.collection("reservations").document(reservationId)

        val foodRef =
            db.collection("food_listings").document(reservationId)

        progressPickups.visibility = View.VISIBLE

        db.runTransaction { transaction ->

            val reservation = transaction.get(reservationRef)
            val food = transaction.get(foodRef)

            if (
                !reservation.exists() ||
                reservation.getString("status") != "Reserved"
            ) {
                throw IllegalStateException(
                    "Pickup is no longer available"
                )
            }

            if (
                !food.exists() ||
                food.getString("status") != "Reserved"
            ) {
                throw IllegalStateException(
                    "Food is no longer available"
                )
            }

            transaction.update(
                reservationRef,
                mapOf(
                    "status" to "Assigned",
                    "volunteerId" to user.uid
                )
            )

            transaction.update(
                foodRef,
                "status",
                "Assigned"
            )

            null
        }
            .addOnSuccessListener {

                progressPickups.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Pickup accepted successfully!",
                    Toast.LENGTH_LONG
                ).show()

                loadAvailablePickups()
            }
            .addOnFailureListener { e ->

                progressPickups.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
