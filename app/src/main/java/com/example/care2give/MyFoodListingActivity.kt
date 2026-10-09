
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
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class MyFoodListingsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var listingsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private val green = Color.rgb(53, 107, 75)
    private val brown = Color.rgb(93, 64, 55)
    private val muted = Color.rgb(130, 140, 132)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_food_listing)

        listingsContainer = findViewById(R.id.listingsContainer)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        loadFoodListings()
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

    private fun loadFoodListings() {

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

        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        listingsContainer.removeAllViews()

        db.collection("food_listings")
            .whereEqualTo("donorId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->

                progressBar.visibility = View.GONE

                if (snapshot.isEmpty) {
                    tvEmpty.text = "No food listings yet."
                    tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val documents = snapshot.documents.sortedByDescending {
                    it.getTimestamp("createdAt")?.seconds ?: 0L
                }

                for (document in documents) {

                    val foodName =
                        document.getString("foodName")
                            ?: "Unnamed Food"

                    val category =
                        document.getString("category") ?: "-"

                    val quantity =
                        document.get("quantity")?.toString() ?: "-"

                    val location =
                        document.getString("pickupLocation") ?: "-"

                    val status =
                        document.getString("status") ?: "Unknown"

                    val imageUrl =
                        document.getString("imageUrl") ?: ""

                    val expiryDate =
                        document.getTimestamp("expiryDate")?.toDate()

                    val formattedExpiry = if (expiryDate != null) {
                        SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(expiryDate)
                    } else {
                        "-"
                    }

                    addFoodCard(
                        documentId = document.id,
                        foodName = foodName,
                        category = category,
                        quantity = quantity,
                        location = location,
                        expiryDate = formattedExpiry,
                        status = status,
                        imageUrl = imageUrl
                    )
                }
            }
            .addOnFailureListener { e ->

                progressBar.visibility = View.GONE
                tvEmpty.text = "Unable to load food listings."
                tvEmpty.visibility = View.VISIBLE

                Toast.makeText(
                    this,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun addFoodCard(
        documentId: String,
        foodName: String,
        category: String,
        quantity: String,
        location: String,
        expiryDate: String,
        status: String,
        imageUrl: String
    ) {

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

            contentDescription = "Image of $foodName"
        }

        card.addView(imageView)

        // LOAD IMAGE FROM CLOUDINARY
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
                            bitmap != null
                        ) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }

                } catch (_: Exception) {
                    // Keep placeholder if image fails
                }
            }.start()
        }

        // CARD CONTENT
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(16),
                dp(18),
                dp(18)
            )
        }

        // FOOD NAME
        val nameText = TextView(this).apply {
            text = foodName
            textSize = 21f
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
        }

        content.addView(nameText)

        // STATUS BADGE COLORS
        val statusColors = when (status) {

            "Available" ->
                Pair("#E4F2E8", "#356B4B")

            "Reserved" ->
                Pair("#FFF0D9", "#A66A1F")

            "Assigned" ->
                Pair("#E4EEFF", "#4169A8")

            "Collected" ->
                Pair("#F1E6FF", "#7952A3")

            "Delivered" ->
                Pair("#DFF4E7", "#28764A")

            else ->
                Pair("#EEEEEE", "#666666")
        }

        val statusBadge = TextView(this).apply {

            text = "● $status"
            textSize = 13f

            setTypeface(null, Typeface.BOLD)

            setTextColor(
                Color.parseColor(statusColors.second)
            )

            background = roundedBackground(
                Color.parseColor(statusColors.first),
                20
            )

            setPadding(
                dp(14),
                dp(8),
                dp(14),
                dp(8)
            )
        }

        val badgeParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(12)
            bottomMargin = dp(14)
        }

        content.addView(statusBadge, badgeParams)

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

        // FOOD INFORMATION HELPER
        fun addInfo(
            icon: String,
            label: String,
            value: String
        ) {

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

            val rowParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }

            content.addView(row, rowParams)
        }

        // DETAILS
        addInfo(
            "🍽️",
            "Food Category",
            category
        )

        addInfo(
            "📦",
            "Quantity",
            quantity
        )

        addInfo(
            "📍",
            "Pickup Location",
            location
        )

        addInfo(
            "📅",
            "Expiry Date",
            expiryDate
        )

        // DELETE BUTTON
        if (status == "Available") {

            val deleteButton = Button(this).apply {

                text = "🗑 Delete Listing"
                isAllCaps = false
                textSize = 15f

                setTextColor(Color.WHITE)

                backgroundTintList =
                    ColorStateList.valueOf(
                        Color.rgb(183, 74, 74)
                    )

                setOnClickListener {

                    AlertDialog.Builder(
                        this@MyFoodListingsActivity
                    )
                        .setTitle("Delete Food Listing?")
                        .setMessage(
                            "Are you sure you want to delete \"$foodName\"? This action cannot be undone."
                        )
                        .setNegativeButton(
                            "Cancel",
                            null
                        )
                        .setPositiveButton("Delete") { _, _ ->

                            deleteFoodListing(documentId)

                        }
                        .show()
                }
            }

            val deleteParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(20)
            }

            content.addView(
                deleteButton,
                deleteParams
            )

        } else {

            // LISTING CANNOT BE DELETED AFTER RESERVATION
            val infoText = TextView(this).apply {

                text = "🔒 This listing is already in the food rescue process."
                textSize = 12f
                setTextColor(muted)
                gravity = Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(14),
                    dp(8),
                    dp(4)
                )
            }

            content.addView(infoText)
        }

        // ADD CONTENT TO CARD
        card.addView(content)

        // ADD CARD TO LIST
        listingsContainer.addView(
            card,
            cardParams
        )
    }

    // DELETE FOOD LISTING FROM FIRESTORE
    private fun deleteFoodListing(documentId: String) {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                "Please login first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val documentRef =
            db.collection("food_listings")
                .document(documentId)

        documentRef.get()
            .addOnSuccessListener { document ->

                // VERIFY LISTING OWNER
                if (
                    !document.exists() ||
                    document.getString("donorId") != currentUser.uid
                ) {

                    Toast.makeText(
                        this,
                        "You cannot delete this listing",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                // ONLY AVAILABLE LISTINGS CAN BE DELETED
                if (document.getString("status") != "Available") {

                    Toast.makeText(
                        this,
                        "Only available listings can be deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadFoodListings()
                    return@addOnSuccessListener
                }

                // DELETE DOCUMENT
                documentRef.delete()
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Food listing deleted successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadFoodListings()
                    }
                    .addOnFailureListener { e ->

                        Toast.makeText(
                            this,
                            "Delete failed: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
