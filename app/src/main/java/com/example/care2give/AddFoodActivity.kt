
package com.example.care2give

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddFoodActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val httpClient = OkHttpClient()

    private var selectedExpiryMillis: Long? = null
    private var selectedImageUri: Uri? = null

    private lateinit var ivFoodPreview: ImageView
    private lateinit var btnSubmitFood: Button

    private val cloudName = "ypgry0tc"
    private val uploadPreset = "care2give_food"

    private val pickImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                ivFoodPreview.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_food)

        ivFoodPreview = findViewById(R.id.ivFoodPreview)

        val btnChooseImage =
            findViewById<Button>(R.id.btnChooseImage)

        val etFoodName =
            findViewById<EditText>(R.id.etFoodName)

        val etQuantity =
            findViewById<EditText>(R.id.etQuantity)

        val etPickupLocation =
            findViewById<EditText>(R.id.etPickupLocation)

        val etExpiryDate =
            findViewById<EditText>(R.id.etExpiryDate)

        val etDescription =
            findViewById<EditText>(R.id.etDescription)

        val spinnerCategory =
            findViewById<Spinner>(R.id.spinnerCategory)

        btnSubmitFood = findViewById(R.id.btnSubmitFood)

        val btnBack =
            findViewById<Button>(R.id.btnBack)

        btnChooseImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val categories = arrayOf(
            "Select food category",
            "Cooked Food",
            "Bread & Bakery",
            "Fruits & Vegetables",
            "Packaged Food",
            "Beverages",
            "Others"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerCategory.adapter = adapter

        etExpiryDate.setOnClickListener {

            val today = Calendar.getInstance()

            val picker = DatePickerDialog(
                this,
                { _, year, month, day ->

                    val selected = Calendar.getInstance().apply {
                        set(year, month, day, 23, 59, 59)
                        set(Calendar.MILLISECOND, 0)
                    }

                    selectedExpiryMillis = selected.timeInMillis

                    etExpiryDate.setText(
                        SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(selected.time)
                    )
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            )

            picker.datePicker.minDate =
                System.currentTimeMillis()

            picker.show()
        }

        btnSubmitFood.setOnClickListener {

            val user = auth.currentUser

            if (user == null) {
                showMessage("Please login first")
                return@setOnClickListener
            }

            val foodName = etFoodName.text.toString().trim()
            val quantity = etQuantity.text.toString().toIntOrNull()
            val location = etPickupLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val category = spinnerCategory.selectedItem.toString()
            val expiry = selectedExpiryMillis
            val imageUri = selectedImageUri

            when {
                foodName.isEmpty() -> {
                    etFoodName.error = "Enter food name"
                }

                spinnerCategory.selectedItemPosition == 0 -> {
                    showMessage("Select a food category")
                }

                quantity == null || quantity <= 0 -> {
                    etQuantity.error = "Enter a valid quantity"
                }

                location.isEmpty() -> {
                    etPickupLocation.error = "Enter pickup location"
                }

                expiry == null -> {
                    showMessage("Select an expiry date")
                }

                expiry < System.currentTimeMillis() -> {
                    showMessage("Expiry date has passed")
                }

                imageUri == null -> {
                    showMessage("Please choose a food image")
                }

                else -> {

                    setLoading(true, "Checking donor...")

                    db.collection("users")
                        .document(user.uid)
                        .get()
                        .addOnSuccessListener { profile ->

                            if (profile.getString("role") != "Donor") {
                                setLoading(false)
                                showMessage("Only donors can add food")
                                return@addOnSuccessListener
                            }

                            setLoading(true, "Uploading image...")

                            uploadImageToCloudinary(
                                imageUri,
                                onSuccess = { imageUrl ->

                                    setLoading(true, "Saving food...")

                                    val listing = hashMapOf(
                                        "donorId" to user.uid,
                                        "foodName" to foodName,
                                        "category" to category,
                                        "quantity" to quantity,
                                        "pickupLocation" to location,
                                        "expiryDate" to Timestamp(Date(expiry)),
                                        "description" to description,
                                        "imageUrl" to imageUrl,
                                        "status" to "Available",
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )

                                    db.collection("food_listings")
                                        .add(listing)
                                        .addOnSuccessListener {
                                            showMessage(
                                                "Food donation added successfully!"
                                            )
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            setLoading(false)
                                            showMessage(
                                                "Failed to save: ${e.localizedMessage}"
                                            )
                                        }
                                },
                                onFailure = { error ->
                                    setLoading(false)
                                    showMessage(
                                        "Image upload failed: $error"
                                    )
                                }
                            )
                        }
                        .addOnFailureListener { e ->
                            setLoading(false)
                            showMessage(
                                "Failed to verify donor: ${e.localizedMessage}"
                            )
                        }
                }
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun uploadImageToCloudinary(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        Thread {
            try {
                val mimeType =
                    contentResolver.getType(uri) ?: "image/jpeg"

                if (!mimeType.startsWith("image/")) {
                    runOnUiThread {
                        onFailure("Please select an image file")
                    }
                    return@Thread
                }

                val imageBytes = contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: throw IOException("Cannot read selected image")

                // Limit uploads to 10 MB
                if (imageBytes.size > 10 * 1024 * 1024) {
                    runOnUiThread {
                        onFailure("Image must be smaller than 10 MB")
                    }
                    return@Thread
                }

                val imageBody = imageBytes.toRequestBody(
                    mimeType.toMediaTypeOrNull()
                )

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        "food_image.jpg",
                        imageBody
                    )
                    .addFormDataPart(
                        "upload_preset",
                        uploadPreset
                    )
                    .build()

                val request = Request.Builder()
                    .url(
                        "https://api.cloudinary.com/v1_1/" +
                                cloudName + "/image/upload"
                    )
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).enqueue(
                    object : Callback {

                        override fun onFailure(
                            call: Call,
                            e: IOException
                        ) {
                            runOnUiThread {
                                onFailure(
                                    e.localizedMessage ?: "Network error"
                                )
                            }
                        }

                        override fun onResponse(
                            call: Call,
                            response: Response
                        ) {
                            response.use {
                                try {
                                    val responseText =
                                        it.body?.string().orEmpty()

                                    if (!it.isSuccessful) {
                                        throw IOException(
                                            "Cloudinary HTTP ${it.code}: " +
                                                    responseText.take(250)
                                        )
                                    }

                                    val json = JSONObject(responseText)
                                    val imageUrl =
                                        json.getString("secure_url")

                                    runOnUiThread {
                                        onSuccess(imageUrl)
                                    }

                                } catch (e: Exception) {
                                    runOnUiThread {
                                        onFailure(
                                            e.localizedMessage
                                                ?: "Upload failed"
                                        )
                                    }
                                }
                            }
                        }
                    }
                )

            } catch (e: Exception) {
                runOnUiThread {
                    onFailure(
                        e.localizedMessage ?: "Cannot upload image"
                    )
                }
            }
        }.start()
    }

    private fun setLoading(
        loading: Boolean,
        message: String = "Submit Food Donation"
    ) {
        btnSubmitFood.isEnabled = !loading
        btnSubmitFood.text = message
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
