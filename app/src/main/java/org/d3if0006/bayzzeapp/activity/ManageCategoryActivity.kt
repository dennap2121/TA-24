package org.d3if0006.bayzzeapp.activity

import Order
import OrderProduct
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityManageCategoryBinding
import org.d3if0006.bayzzeapp.databinding.ActivityManageContentBinding
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.util.Date

class ManageCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageCategoryBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private val categoryList = mutableListOf<Category>()
    private val CREATE_DOCUMENT_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCategoryBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_manage_category)


        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCategory)
        val export = findViewById<ImageView>(R.id.menu_export)
        val tambah = findViewById<Button>(R.id.btnTambah)

        export.setOnClickListener{
            exportToCSV()
        }

        tambah.setOnClickListener {
            Log.d("ppppp", "polod")
            // Inflate the dialog layout
            val dialogView = LayoutInflater.from(this).inflate(R.layout.popup_add_category, null)

            // Create the dialog
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)

            val alertDialog = builder.create()

            // Initialize views
            val editTextGambar = dialogView.findViewById<EditText>(R.id.editTextGambar)
            val editTextLink = dialogView.findViewById<EditText>(R.id.editTextLink)
            val buttonSimpan = dialogView.findViewById<Button>(R.id.buttonSimpan)

            // Handle Simpan button click
            buttonSimpan.setOnClickListener {
                val gambar = editTextGambar.text.toString().trim()
                val link = editTextLink.text.toString().trim()

                // Validate input fields (you can add more validation if needed)
                if (title.isNotEmpty() && gambar.isNotEmpty() && link.isNotEmpty()) {
                    // Add data to Firestore based on the tabName extra
                    addDataToFirestore(gambar, link)


                    alertDialog.dismiss() // Dismiss the dialog
                } else {
                    // Show error message if any field is empty
                    Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                }
            }

            // Show the dialog
            alertDialog.show()
        }


        // Initialize RecyclerView and adapter
        categoryAdapter = CategoryAdapter(categoryList, this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ManageCategoryActivity)
            adapter = categoryAdapter
        }

        // Fetch category data from Firestore
        fetchCategoryFromFirestore()

    }

    private fun addDataToFirestore(gambar: String, name: String) {
        // Add your Firestore logic here to add data to the specified collection
        // For example:
        val db = FirebaseFirestore.getInstance()
        val newData = hashMapOf(
            "image" to gambar,
            "name" to name
        )

        db.collection("categories")
            .add(newData)
            .addOnSuccessListener {
                fetchCategoryFromFirestore()
                Toast.makeText(this, "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                fetchCategoryFromFirestore()
                Toast.makeText(this, "Gagal menambahkan Kategori", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchCategoryFromFirestore() {
        categoryList.clear()
        val db = FirebaseFirestore.getInstance()
        db.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val categoryName = document.getString("name") ?: ""
                    val categoryImage = document.getString("image") ?: ""
                    val categoryId = document.id

                    val category = Category(categoryId, categoryName, categoryImage)
                    categoryList.add(category)
                }
                categoryAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching category", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportToCSV() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "category.csv")
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST)
    }

    // Override onActivityResult to handle the result of the ACTION_CREATE_DOCUMENT intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK) {
            // Get the URI of the document selected by the user
            val uri = data?.data
            uri?.let {
                // Write the CSV data to the selected document
                writeCsvToDocument(it)
            }
        }
    }

    // Write CSV data to the selected document
    private fun writeCsvToDocument(uri: Uri) {
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            val csvWriter = BufferedWriter(OutputStreamWriter(outputStream))
            csvWriter.use {
                it.write("Name, Image\n")
                for (category in categoryList) {
                    it.write("${category.name}, ${category.image}\n")
                }
            }
            Toast.makeText(this, "Category exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    // Inner class for the CategoryAdapter
    private inner class CategoryAdapter(
        private val categoryList: List<Category>,
        private val context: Context
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        // ViewHolder class for holding item views
        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val categoryNameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
            val categoryImageTextView: ImageView = itemView.findViewById(R.id.categoryImageTextView)
            val categoryNoTextView: TextView = itemView.findViewById(R.id.categoryNoTextView)
            val contentDeleteImageView: ImageView = itemView.findViewById(R.id.contentDeleteImageView)
            val contentEditImageView: ImageView = itemView.findViewById(R.id.contentEditImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return CategoryViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val currentCategory = categoryList[position]
            val number = position + 1 // Calculate the pemesanan number

            holder.categoryNameTextView.text = currentCategory.name
            holder.categoryNoTextView.text = number.toString()
            if (!currentCategory.image.isNullOrEmpty()) {
                Glide.with(context)
                    .load(currentCategory.image)
                    .placeholder(R.drawable.background_oval_1) // Placeholder image
                    .error(R.drawable.background_oval_1) // Error image
                    .into(holder.categoryImageTextView)
            }

            holder.contentEditImageView.setOnClickListener {
                showEditContentDialog(currentCategory)
            }

            holder.contentDeleteImageView.setOnClickListener {
                showDeleteConfirmationDialog(currentCategory.id)
            }

            // Bind other category details to corresponding TextViews here
        }

        override fun getItemCount(): Int {
            return categoryList.size
        }

        private fun showDeleteConfirmationDialog(id: String) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.popup_delete_category)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val buttonNo = dialog.findViewById<Button>(R.id.buttonNo)
            val buttonYes = dialog.findViewById<Button>(R.id.buttonYes)

            buttonNo.setOnClickListener {
                dialog.dismiss() // Dismiss the dialog when "Tidak" button is clicked
            }

            buttonYes.setOnClickListener {
                // Perform delete operation here
                // Call a function to delete the data for article or banner
                deleteContent(id)
                dialog.dismiss() // Dismiss the dialog after deleting the content
            }

            dialog.show()
        }

        private fun deleteContent(id: String) {
            val db = FirebaseFirestore.getInstance()
            val documentId = id

            db.collection("categories")
                .document(documentId)
                .delete()
                .addOnSuccessListener {
                    fetchCategoryFromFirestore()
                    // Document successfully deleted
                    // You can perform any additional actions here, such as updating the UI
                    Log.d(ContentValues.TAG, "DocumentSnapshot successfully deleted!")
                }
                .addOnFailureListener { e ->
                    // Failed to delete the document
                    Log.w(ContentValues.TAG, "Error deleting document", e)
                }
        }


        private fun showEditContentDialog(category: Category) {
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.popup_content_category)

            // Find views in the custom layout
            val imageEditText: EditText = dialog.findViewById(R.id.imageEditText)
            val linkEditText: EditText = dialog.findViewById(R.id.linkEditText)
            val saveButton: Button = dialog.findViewById(R.id.saveButton)

            // Fill input fields with existing content data
            imageEditText.setText(category.image)
            linkEditText.setText(category.name)

            // Set click listener for save button
            saveButton.setOnClickListener {
                // Get updated data from input fields
                val updatedImage = imageEditText.text.toString()
                val updatedLink = linkEditText.text.toString()

                // Update Firestore document with the new data
                val db = FirebaseFirestore.getInstance()
                val collectionName = "categories"
                db.collection(collectionName).document(category.id)
                    .update(
                        mapOf(
                            "image" to updatedImage,
                            "name" to updatedLink
                        )
                    )
                    .addOnSuccessListener {
                        // Dismiss the dialog upon successful update
                        dialog.dismiss()
                        // Show a toast or perform any other action to notify the user
                        Toast.makeText(context, "Kategori berhasil diperbarui", Toast.LENGTH_SHORT).show()

                        // Refresh the list of data
                        fetchCategoryFromFirestore() // Assuming you have a function to fetch data from Firestore

                        // Alternatively, you can refresh the RecyclerView adapter directly if you have access to it
                        // adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        // Handle failure to update Firestore document
                        Log.e(ContentValues.TAG, "Error updating document", e)
                        // Show a toast or perform any other action to notify the user
                        Toast.makeText(context, "Gagal memperbarui Kategori", Toast.LENGTH_SHORT).show()
                    }
            }

            // Show the dialog
            dialog.show()
        }
    }

    data class Category(val id: String, val name: String, val image: String) // Example data class, adjust as needed

}
