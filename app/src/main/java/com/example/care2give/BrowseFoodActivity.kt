
package com.example.care2give

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class BrowseFoodActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var etSearchFood: EditText
    private lateinit var browseFoodContainer: LinearLayout
    private lateinit var progressBarBrowse: ProgressBar
    private lateinit var tvEmptyBrowse: TextView

    private var allFoodListings: List<DocumentSnapshot> = emptyList()

    private val green = Color.rgb(53, 107, 75)
    private val brown = Color.rgb(93, 64, 55)
    private val muted = Color.rgb(130, 140, 132)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_food)

        etSearchFood = findViewById(R.id.etSearchFood)
        browseFoodContainer = findViewById(R.id.browseFoodContainer)
        progressBarBrowse = findViewById(R.id.progressBarBrowse)
        tvEmptyBrowse = findViewById(R.id.tvEmptyBrowse)

        findViewById<Button>(R.id.btnBackBrowse).setOnClickListener {
            finish()
        }

        etSearchFood.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filterFood(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onStart() {
        super.onStart()
        loadAvailableFood()
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

    private fun loadAvailableFood() {

        progressBarBrowse.visibility = View.VISIBLE
        tvEmptyBrowse.visibility = View.GONE
        browseFoodContainer.removeAllViews()

        db.collection("food_listings")
            .whereEqualTo("status", "Available")
            .get()
            .addOnSuccessListener { snapshot ->

                progressBarBrowse.visibility = View.GONE

                allFoodListings = snapshot.documents.sortedByDescending {
                    it.getTimestamp("createdAt")?.seconds ?: 0L
                }

                filterFood(etSearchFood.text.toString())
            }
            .addOnFailureListener { e ->

                progressBarBrowse.visibility = View.GONE
                allFoodListings = emptyList()
                browseFoodContainer.removeAllViews()

                tvEmptyBrowse.text = "Unable to load available food."
                tvEmptyBrowse.visibility = View.VISIBLE

                Toast.makeText(
                    this,
                    "Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun filterFood(query: String) {

        val filteredListings = allFoodListings.filter { document ->

            val foodName = document.getString("foodName").orEmpty()

            foodName.contains(
                query.trim(),
                ignoreCase = true
            )
        }

        browseFoodContainer.removeAllViews()

        if (filteredListings.isEmpty()) {
            tvEmptyBrowse.text = "🌱 No available food found."
            tvEmptyBrowse.visibility = View.VISIBLE
            return
        }

        tvEmptyBrowse.visibility = View.GONE

        filteredListings.forEach { document ->
            addFoodCard(document)
        }
    }

    private fun addFoodCard(document: DocumentSnapshot) {

        val foodName =
            document.getString("foodName") ?: "Unnamed Food"

        val category =
            document.getString("category") ?: "-"

        val quantity =
            document.get("quantity")?.toString() ?: "-"

        val location =
            document.getString("pickupLocation") ?: "-"

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

        // MAIN FOOD CARD
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
                            bitmap != null &&
                            imageView.parent != null
                        ) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }

                } catch (_: Exception) {
                    // Keep placeholder if image fails
                }
            }.start()
        }

        // FOOD CARD CONTENT
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

        // AVAILABLE BADGE
        val statusBadge = TextView(this).apply {

            text = "● Available"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(green)

            background = roundedBackground(
                Color.rgb(228, 242, 232),
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

        // FOOD DETAILS
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
            formattedExpiry
        )

        // RESERVE BUTTON
        val btnReserve = Button(this).apply {

            text = "🍱 Reserve This Food"
            isAllCaps = false
            textSize = 16f
            setTypeface(null, Typeface.BOLD)

            setTextColor(Color.WHITE)

            backgroundTintList =
                ColorStateList.valueOf(
                    Color.rgb(71, 122, 89)
                )

            setOnClickListener {

                val intent = Intent(
                    this@BrowseFoodActivity,
                    ReserveFoodActivity::class.java
                )

                intent.putExtra("foodId", document.id)
                startActivity(intent)
            }
        }

        val reserveParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56)
        ).apply {
            topMargin = dp(22)
        }

        content.addView(
            btnReserve,
            reserveParams
        )

        // ADD CONTENT TO CARD
        card.addView(content)

        // DISPLAY CARD
        browseFoodContainer.addView(
            card,
            cardParams
        )
    }
}
