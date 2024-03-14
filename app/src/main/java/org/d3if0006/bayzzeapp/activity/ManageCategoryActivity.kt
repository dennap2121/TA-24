package org.d3if0006.bayzzeapp.activity

import Order
import OrderProduct
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerView = binding.recyclerViewCategory
        val export = binding.menuExport
        val tambah = binding.btnTambah

        export.setOnClickListener{
            exportToCSV()
        }

        tambah.setOnClickListener {
            val intent = Intent(this, ConfigCategoryActivity::class.java)
            intent.putExtra("id", "")
            startActivity(intent)
            recreate()
        }

        categoryAdapter = CategoryAdapter(categoryList, this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ManageCategoryActivity)
            adapter = categoryAdapter
        }

        fetchCategoryFromFirestore()
    }

    private fun addDataToFirestore(gambar: String, name: String) {
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

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, ConfigProductActivity.PICK_IMAGE_REQUEST)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            uri?.let {
                writeCsvToDocument(it)
            }
        }

        if (resultCode == Activity.RESULT_OK) {
            Log.d("lmn", "aman")
            Log.d("lmn", requestCode.toString())
            Log.d("lmn", data?.data.toString())

            when (requestCode) {
                ConfigProductActivity.PICK_IMAGE_REQUEST -> {
                    // Set selected image to the ImageView in the dialog
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.popup_add_category, null)
                    val imageView = dialogView.findViewById<ImageView>(R.id.imageView)
                    val textImage = dialogView.findViewById<TextView>(R.id.textImage)

                    textImage.text = "ddhjhfjsdhf"
                    selectedImageUri = data?.data
                    selectedImageUri?.let { uri ->
                        Log.d("dddf", uri.toString())
                        imageView.setImageURI(uri)
                    }
                }
            }
        }
    }

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

    private inner class CategoryAdapter(
        private val categoryList: List<Category>,
        private val context: Context
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

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
            val number = position + 1

            holder.categoryNameTextView.text = currentCategory.name
            holder.categoryNoTextView.text = number.toString()
            if (!currentCategory.image.isNullOrEmpty()) {
                Glide.with(context)
                    .load(currentCategory.image)
                    .placeholder(R.drawable.background_oval_1)
                    .error(R.drawable.background_oval_1)
                    .into(holder.categoryImageTextView)
            }

            holder.contentEditImageView.setOnClickListener {
                val intent = Intent(context, ConfigCategoryActivity::class.java)
                intent.putExtra("id", currentCategory.id)
                intent.putExtra("previousImage", currentCategory.image)
                context.startActivity(intent)
            }

            holder.contentDeleteImageView.setOnClickListener {
                showDeleteConfirmationDialog(currentCategory.id)
            }
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
                dialog.dismiss()
            }

            buttonYes.setOnClickListener {
                deleteContent(id)
                dialog.dismiss()
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
                    Log.d(ContentValues.TAG, "DocumentSnapshot successfully deleted!")
                }
                .addOnFailureListener { e ->
                    Log.w(ContentValues.TAG, "Error deleting document", e)
                }
        }

        private fun showEditContentDialog(category: Category) {
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.popup_content_category)

            val imageEditText: EditText = dialog.findViewById(R.id.imageEditText)
            val linkEditText: EditText = dialog.findViewById(R.id.linkEditText)
            val saveButton: Button = dialog.findViewById(R.id.saveButton)
            val pickImageButton: ImageButton = dialog.findViewById(R.id.pickImageButton)

            imageEditText.setText(category.image)
            linkEditText.setText(category.name)

            saveButton.setOnClickListener {
                val updatedImage = imageEditText.text.toString()
                val updatedLink = linkEditText.text.toString()

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
                        dialog.dismiss()
                        Toast.makeText(context, "Kategori berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        fetchCategoryFromFirestore()
                    }
                    .addOnFailureListener { e ->
                        Log.e(ContentValues.TAG, "Error updating document", e)
                        Toast.makeText(context, "Gagal memperbarui Kategori", Toast.LENGTH_SHORT).show()
                    }
            }

            pickImageButton.setOnClickListener {
                pickImageFromGallery()
            }

            dialog.show()
        }
    }

    data class Category(val id: String, val name: String, val image: String)

}


