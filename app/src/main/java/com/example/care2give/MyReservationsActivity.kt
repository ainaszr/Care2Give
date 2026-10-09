
package com.example.care2give

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
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class MyReservationsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var reservationsContainer: LinearLayout
    private lateinit var progressReservations: ProgressBar
    private lateinit var tvEmptyReservations: TextView

    private val green = Color.rgb(53, 107, 75)
    private val brown = Color.rgb(93, 64, 55)
    private val muted = Color.rgb(130, 140, 132)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reservations)

        reservationsContainer =
            findViewById(R.id.reservationsContainer)

        progressReservations =
            findViewById(R.id.progressReservations)

        tvEmptyReservations =
            findViewById(R.id.tvEmptyReservations)

        findViewById<Button>(
            R.id.btnBackReservations
        ).setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        loadReservations()
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

    private fun loadReservations() {

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

        progressReservations.visibility = View.VISIBLE
        tvEmptyReservations.visibility = View.GONE
        reservationsContainer.removeAllViews()

        db.collection("reservations")
            .whereEqualTo("recipientId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->

                progressReservations.visibility = View.GONE

                if (snapshot.isEmpty) {
                    tvEmptyReservations.text =
                        "🍱 No reservations yet.\n\n" +
                                "Explore available food and start rescuing meals!"

                    tvEmptyReservations.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val documents =
                    snapshot.documents.sortedByDescending {
                        it.getTimestamp("createdAt")?.seconds ?: 0L
                    }

                documents.forEach { document ->
                    addReservationCard(document)
                }
            }
            .addOnFailureListener { e ->

                progressReservations.visibility = View.GONE

                tvEmptyReservations.text =
                    "Unable to load reservations."

                tvEmptyReservations.visibility = View.VISIBLE

                Toast.makeText(
                    this,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun addReservationCard(
        document: DocumentSnapshot
    ) {

        val foodName =
            document.getString("foodName") ?: "Unnamed Food"

        val status =
            document.getString("status") ?: "Reserved"

        val foodId =
            document.getString("foodId") ?: document.id

        val createdAt =
            document.getTimestamp("createdAt")?.toDate()

        val reservationDate = if (createdAt != null) {
            SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
            ).format(createdAt)
        } else {
            "-"
        }

        // STATUS COLORS
        val statusColor = when (status.lowercase()) {
            "reserved" -> Color.rgb(193, 122, 37)
            "assigned" -> Color.rgb(51, 105, 173)
            "collected" -> Color.rgb(130, 86, 168)
            "delivered" -> green
            "cancelled" -> Color.rgb(180, 65, 65)
            else -> green
        }

        val statusBackground = when (status.lowercase()) {
            "reserved" -> Color.rgb(255, 242, 219)
            "assigned" -> Color.rgb(228, 239, 255)
            "collected" -> Color.rgb(242, 231, 251)
            "delivered" -> Color.rgb(228, 242, 232)
            "cancelled" -> Color.rgb(255, 232, 232)
            else -> Color.rgb(228, 242, 232)
        }

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

            contentDescription = "Reserved food image"
        }

        card.addView(imageView)

        // CONTENT CONTAINER
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
        val foodNameText = TextView(this).apply {
            text = "🍱 $foodName"
            textSize = 21f
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
        }

        content.addView(foodNameText)

        // STATUS BADGE
        val statusBadge = TextView(this).apply {
            text = "● $status"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(statusColor)

            background = roundedBackground(
                statusBackground,
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

        // RESERVATION DATE
        addInfo(
            "📅",
            "Reservation Date",
            reservationDate
        )

        // PLACEHOLDERS FOR FIRESTORE DATA
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
            "⏰",
            "Expiry Date",
            "Loading..."
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

                // LOAD FOOD IMAGE FROM CLOUDINARY
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

        reservationsContainer.addView(
            card,
            cardParams
        )
    }
}
