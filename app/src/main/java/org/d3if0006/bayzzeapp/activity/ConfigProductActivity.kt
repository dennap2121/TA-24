package org.d3if0006.bayzzeapp.activity

import Product
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityConfigProductBinding
import java.util.Calendar
import java.util.UUID

class ConfigProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigProductBinding
    private lateinit var selectedProducts: MutableList<Product>
    private lateinit var layoutDeliveryInfo: LinearLayout
    private lateinit var editName: EditText
    private lateinit var editQty: EditText
    private lateinit var editPrice: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var submitButton: Button
    private lateinit var pickImageButton: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize views
        layoutDeliveryInfo = findViewById(R.id.layoutDeliveryInfo)
        editName = findViewById(R.id.editName)
        editQty = findViewById(R.id.editQty)
        editPrice = findViewById(R.id.editPrice)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        submitButton = findViewById(R.id.submitButton)
        pickImageButton = findViewById(R.id.pickImageButton)
        imageView = findViewById(R.id.imageView)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        // Set title and back button
        binding.backButton.setOnClickListener {
            finish()
        }

        pickImageButton.setOnClickListener {
            showImagePickerDialog()
        }

        binding.submitButton.setOnClickListener{
            submitOrder()
        }

        selectedProducts = intent.getParcelableArrayListExtra<Product>("selectedProducts") ?: mutableListOf()

        fetchCategories()

        val id = intent.getStringExtra("id")
        if (!id.isNullOrEmpty()) {
            // Edit product mode
            fetchProductDetails(id)
        }
    }

    private fun fetchProductDetails(productId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val product = document.toObject(Product::class.java)
                    if (product != null) {
                        // Populate UI fields with the retrieved product details
                        editName.setText(product.name)
                        editQty.setText(product.quantity.toString())
                        editPrice.setText(product.price.toString())
                        if (spinnerCategory.adapter is ArrayAdapter<*>) {
                            val categoryIndex = (spinnerCategory.adapter as ArrayAdapter<String>).getPosition(product.category)
                            spinnerCategory.setSelection(categoryIndex)
                        }
                        Glide.with(this)
                            .load(product.image)
                            .placeholder(R.drawable.background_oval_1) // Optional: Placeholder image while loading
                            .error(R.drawable.background_oval_1) // Optional: Image to display if loading fails
                            .into(pickImageButton)


                        // Save the selected image URI
                        selectedImageUri = Uri.parse(product.image)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error fetching product details", exception)
                Toast.makeText(this, "Failed to fetch product details", Toast.LENGTH_SHORT).show()
            }
    }



    private fun fetchCategories() {
        val db = FirebaseFirestore.getInstance()
        db.collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                val categoryList = mutableListOf<String>()
                for (document in documents) {
                    val categoryName = document.getString("name")
                    categoryName?.let {
                        categoryList.add(categoryName)
                    }
                }
                // Populate the spinner with categories
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting categories", exception)
            }
    }


    private fun submitOrder() {
        val name = editName.text.toString()
        val qty = editQty.text.toString().toInt() // Convert quantity to Int
        val price = editPrice.text.toString().toDouble() // Convert price to Double
        val category = spinnerCategory.selectedItem.toString()

        // Check if an image is selected
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a reference to Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference

        // Generate a random UUID for the image filename
        val imageName = UUID.randomUUID().toString()

        // Create a reference to the image location in Firebase Storage
        val imageRef = storageRef.child("images/$imageName")

        // Upload the image to Firebase Storage
        val uploadTask = imageRef.putFile(selectedImageUri!!)

        showLoading()

        // Register observers to listen for upload progress or failure
        uploadTask.addOnFailureListener { exception ->
            Log.e(TAG, "Image upload failed", exception)
            Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            hideLoading()
        }.addOnSuccessListener { taskSnapshot ->
            // Get the image URL from Firebase Storage
            imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                val db = FirebaseFirestore.getInstance()

                // Check if it's an update operation
                val id = intent.getStringExtra("id")
                if (!id.isNullOrEmpty()) {
                    // Update existing product
                    db.collection("products").document(id)
                        .update(
                            "name", name,
                            "quantity", qty,
                            "price", price,
                            "category", category,
                            "image", imageUrl.toString() // Store the image URL
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "Product updated successfully")
                            Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show()
                            hideLoading()
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error updating product", e)
                            Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show()
                            hideLoading()
                        }
                } else {
                    // Add new product
                    val product = hashMapOf(
                        "name" to name,
                        "quantity" to qty,
                        "price" to price,
                        "category" to category,
                        "image" to imageUrl.toString() // Store the image URL
                        // Add more fields as needed
                    )

                    // Add the product to the "products" collection in Firestore
                    db.collection("products")
                        .add(product)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "Product added with ID: ${documentReference.id}")
                            Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show()
                            hideLoading()
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding product", e)
                            Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show()
                            hideLoading()
                        }
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Error getting image URL", exception)
                Toast.makeText(this, "Error getting image URL", Toast.LENGTH_SHORT).show()
                hideLoading()
            }
        }
    }


    private fun clearProductsSharedPreferences() {
        val sharedPreferences = getSharedPreferences("products", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Clear all data in the SharedPreferences
        editor.apply()
    }

    private fun showLoading() {
        progressDialog.setMessage("Tunggu Sebentar...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Pick from Gallery", "Take Photo")

        AlertDialog.Builder(this)
            .setTitle("Choose Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> takePhoto()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun takePhoto() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, TAKE_PHOTO_REQUEST)
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening camera", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    // Handle image picked from gallery
                    selectedImageUri = data?.data
                    // Display the selected image
                    imageView.setImageURI(selectedImageUri)
                    // Set the selected image to the pickImageButton
                    selectedImageUri?.let {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                        pickImageButton.setImageBitmap(bitmap)
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    // Handle photo taken from camera
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    // Display the captured photo
                    imageView.setImageBitmap(imageBitmap)
                    // Set the captured photo to the pickImageButton
                    pickImageButton.setImageBitmap(imageBitmap)

                }
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAKE_PHOTO_REQUEST = 2
        private const val TAG = "ConfigProductActivity"
    }
}


