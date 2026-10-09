
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

class MyPickupsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var myPickupsContainer: LinearLayout
    private lateinit var progressMyPickups: ProgressBar
    private lateinit var tvEmptyMyPickups: TextView

    private val green = Color.rgb(53, 107, 75)
    private val brown = Color.rgb(93, 64, 55)
    private val muted = Color.rgb(130, 140, 132)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_pickups)

        myPickupsContainer = findViewById(R.id.myPickupsContainer)
        progressMyPickups = findViewById(R.id.progressMyPickups)
        tvEmptyMyPickups = findViewById(R.id.tvEmptyMyPickups)

        findViewById<Button>(R.id.btnBackMyPickups)
            .setOnClickListener {
                finish()
            }
    }

    override fun onStart() {
        super.onStart()
        loadMyPickups()
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

    private fun loadMyPickups() {

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

        progressMyPickups.visibility = View.VISIBLE
        tvEmptyMyPickups.visibility = View.GONE
        myPickupsContainer.removeAllViews()

        // VERIFY VOLUNTEER ROLE
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { profile ->

                if (profile.getString("role") != "Volunteer Collector") {

                    progressMyPickups.visibility = View.GONE

                    Toast.makeText(
                        this,
                        "Only Volunteer Collectors can access My Pickups",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                // LOAD VOLUNTEER PICKUPS
                db.collection("reservations")
                    .whereEqualTo("volunteerId", user.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->

                        progressMyPickups.visibility = View.GONE

                        if (snapshot.isEmpty) {
                            tvEmptyMyPickups.text =
                                "🚚 No pickups assigned yet.\n\n" +
                                        "Visit Available Pickups to start your first food rescue!"

                            tvEmptyMyPickups.visibility = View.VISIBLE
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

                        progressMyPickups.visibility = View.GONE
                        tvEmptyMyPickups.text =
                            "Unable to load your pickups."
                        tvEmptyMyPickups.visibility = View.VISIBLE

                        Toast.makeText(
                            this,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->

                progressMyPickups.visibility = View.GONE
                tvEmptyMyPickups.text =
                    "Unable to verify volunteer account."
                tvEmptyMyPickups.visibility = View.VISIBLE

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

        val status =
            reservation.getString("status") ?: "-"

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

            contentDescription = "Pickup food image"
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

        // STATUS COLORS
        val badgeBackground: Int
        val badgeTextColor: Int
        val badgeLabel: String

        when (status) {
            "Assigned" -> {
                badgeBackground = Color.rgb(225, 239, 255)
                badgeTextColor = Color.rgb(43, 105, 170)
                badgeLabel = "● Assigned"
            }

            "Collected" -> {
                badgeBackground = Color.rgb(239, 229, 255)
                badgeTextColor = Color.rgb(115, 69, 165)
                badgeLabel = "● Collected"
            }

            "Delivered" -> {
                badgeBackground = Color.rgb(224, 243, 229)
                badgeTextColor = Color.rgb(46, 125, 75)
                badgeLabel = "✓ Delivered"
            }

            else -> {
                badgeBackground = Color.rgb(240, 240, 240)
                badgeTextColor = Color.DKGRAY
                badgeLabel = status
            }
        }

        val statusBadge = TextView(this).apply {
            text = badgeLabel
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(badgeTextColor)

            background = roundedBackground(
                badgeBackground,
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

        // PICKUP DETAILS
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

        // ACTION BUTTONS BY STATUS
        when (status) {

            "Assigned" -> {
                addActionButton(
                    content,
                    "📦 Mark as Collected",
                    reservation.id,
                    "Assigned",
                    "Collected"
                )
            }

            "Collected" -> {
                addActionButton(
                    content,
                    "🚚 Mark as Delivered",
                    reservation.id,
                    "Collected",
                    "Delivered"
                )
            }

            "Delivered" -> {

                val completedText = TextView(this).apply {
                    text = "✓ Delivery Completed!"
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(green)

                    background = roundedBackground(
                        Color.rgb(224, 243, 229),
                        12
                    )

                    gravity = Gravity.CENTER

                    setPadding(
                        dp(14),
                        dp(16),
                        dp(14),
                        dp(16)
                    )
                }

                content.addView(
                    completedText,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(22)
                    }
                )
            }
        }

        // LOAD FOOD DETAILS FROM FIRESTORE
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

                // CLOUDINARY FOOD IMAGE
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
                            // Keep image placeholder
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

        myPickupsContainer.addView(
            card,
            cardParams
        )
    }

    // ACTION BUTTON
    private fun addActionButton(
        container: LinearLayout,
        buttonText: String,
        reservationId: String,
        currentStatus: String,
        newStatus: String
    ) {

        val button = Button(this).apply {

            text = buttonText
            isAllCaps = false
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)

            backgroundTintList = ColorStateList.valueOf(
                Color.rgb(71, 122, 89)
            )

            setOnClickListener {

                AlertDialog.Builder(this@MyPickupsActivity)
                    .setTitle(buttonText)
                    .setMessage(
                        "Are you sure you want to change the status to $newStatus?"
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Confirm") { _, _ ->

                        updatePickupStatus(
                            reservationId,
                            currentStatus,
                            newStatus
                        )
                    }
                    .show()
            }
        }

        container.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(22)
            }
        )
    }

    // FIRESTORE TRANSACTION
    private fun updatePickupStatus(
        reservationId: String,
        currentStatus: String,
        newStatus: String
    ) {

        val user = auth.currentUser ?: return

        val reservationRef =
            db.collection("reservations")
                .document(reservationId)

        val foodRef =
            db.collection("food_listings")
                .document(reservationId)

        progressMyPickups.visibility = View.VISIBLE

        db.runTransaction { transaction ->

            val reservation = transaction.get(reservationRef)
            val food = transaction.get(foodRef)

            if (
                !reservation.exists() ||
                reservation.getString("volunteerId") != user.uid ||
                reservation.getString("status") != currentStatus
            ) {
                throw IllegalStateException(
                    "Pickup is no longer available for this action"
                )
            }

            if (
                !food.exists() ||
                food.getString("status") != currentStatus
            ) {
                throw IllegalStateException(
                    "Food status does not match"
                )
            }

            transaction.update(
                reservationRef,
                "status",
                newStatus
            )

            transaction.update(
                foodRef,
                "status",
                newStatus
            )

            null
        }
            .addOnSuccessListener {

                progressMyPickups.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Status updated to $newStatus!",
                    Toast.LENGTH_LONG
                ).show()

                loadMyPickups()
            }
            .addOnFailureListener { e ->

                progressMyPickups.visibility = View.GONE

                Toast.makeText(
                    this,
                    "Failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
