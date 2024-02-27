package org.d3if0006.bayzzeapp.activity

import Order
import OrderProduct
import android.app.Activity
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
import android.widget.Button
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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.util.Date

class ManageCategoryActivity : AppCompatActivity() {

    private lateinit var categoryAdapter: CategoryAdapter
    private val categoryList = mutableListOf<Category>()
    private val CREATE_DOCUMENT_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_category)


        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCategory)
        val export = findViewById<ImageView>(R.id.menu_export)

        export.setOnClickListener{
            exportToCSV()
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

    private fun fetchCategoryFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("categories")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val categoryName = document.getString("name") ?: ""
                    val categoryImage = document.getString("image") ?: ""

                    val category = Category(categoryName, categoryImage)
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

            // Bind other category details to corresponding TextViews here
        }

        override fun getItemCount(): Int {
            return categoryList.size
        }
    }

    data class Category(val name: String, val image: String) // Example data class, adjust as needed

}
