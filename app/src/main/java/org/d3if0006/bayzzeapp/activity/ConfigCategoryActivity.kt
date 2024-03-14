package org.d3if0006.bayzzeapp.activity

import Category
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
import org.d3if0006.bayzzeapp.databinding.ActivityConfigCategoryBinding
import java.util.Calendar
import java.util.UUID

class ConfigCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigCategoryBinding
    private lateinit var layoutDeliveryInfo: LinearLayout
    private lateinit var editName: EditText
    private lateinit var submitButton: Button
    private lateinit var pickImageButton: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize views
        layoutDeliveryInfo = findViewById(R.id.layoutDeliveryInfo)
        editName = findViewById(R.id.editName)
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

        fetchCategories()

        val id = intent.getStringExtra("id")
        if (!id.isNullOrEmpty()) {
            // Edit category mode
            fetchCategoryDetails(id)
        }
    }

    private fun fetchCategoryDetails(categoryId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("categories").document(categoryId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val category = document.toObject(Category::class.java)
                    if (category != null) {
                        // Populate UI fields with the retrieved category details
                        editName.setText(category.name)
                        Glide.with(this)
                            .load(category.image)
                            .placeholder(R.drawable.background_oval_1) // Optional: Placeholder image while loading
                            .error(R.drawable.background_oval_1) // Optional: Image to display if loading fails
                            .into(pickImageButton)

                        selectedImageUri = Uri.parse(category.image)

                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error fetching category details", exception)
                Toast.makeText(this, "Failed to fetch category details", Toast.LENGTH_SHORT).show()
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
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting categories", exception)
            }
    }


    private fun submitOrder() {
        val name = editName.text.toString()

        // Check if an image is selected
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if it's an edit operation with no image change
        val id = intent.getStringExtra("id")
        if (!id.isNullOrEmpty() && selectedImageUri.toString() == intent.getStringExtra("previousImage")) {
            updateCategoryDetails(name)
        } else {
            // Otherwise, proceed with uploading the image and updating category details
            uploadImageAndCategoryDetails(name)
        }
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

    private fun updateCategoryDetails(name: String) {
        val id = intent.getStringExtra("id")
        if (id != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("categories").document(id)
                .update(
                    "name", name,
                    // Add more fields as needed
                )
                .addOnSuccessListener {
                    Log.d(TAG, "Category updated successfully")
                    Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ManageCategoryActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error updating category", e)
                    Toast.makeText(this, "Failed to update category", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun uploadImageAndCategoryDetails(name: String) {
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
                    // Update existing category
                    db.collection("categories").document(id)
                        .update(
                            "name", name,
                            "image", imageUrl.toString(),
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "Category updated successfully")
                            Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show()
                            hideLoading()
                            finish() // Finish the activity after updating the category
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error updating category", e)
                            Toast.makeText(this, "Failed to update category", Toast.LENGTH_SHORT).show()
                            hideLoading()
                        }
                } else {
                    // Add new category
                    val category = hashMapOf(
                        "name" to name,
                        "image" to imageUrl.toString() // Store the image URL
                        // Add more fields as needed
                    )

                    // Add the category to the "categories" collection in Firestore
                    db.collection("categories")
                        .add(category)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "Category added with ID: ${documentReference.id}")
                            Toast.makeText(this, "Category added successfully!", Toast.LENGTH_SHORT).show()
                            hideLoading()
                            finish() // Finish the activity after adding the category
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding category", e)
                            Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show()
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

    companion object {
        const val PICK_IMAGE_REQUEST = 1
        const val TAKE_PHOTO_REQUEST = 2
        private const val TAG = "ConfigCategoryActivity"
    }

}


