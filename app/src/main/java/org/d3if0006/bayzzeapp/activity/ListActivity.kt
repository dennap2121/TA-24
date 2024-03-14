package org.d3if0006.bayzzeapp.activity

import Product
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.common.reflect.TypeToken
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityListBinding

class ListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding
    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var selectedProducts: MutableList<Product>
    private lateinit var categoryList: MutableList<Category>
    private lateinit var progressDialog: ProgressDialog
    private lateinit var categoryRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog


        binding.backButton.setOnClickListener {
            finish()
        }

        fetchCategoryData()

        val recyclerView = findViewById<RecyclerView>(R.id.listRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        categoryRecyclerView = findViewById(R.id.listRecyclerView)

    }

    private fun navigateToCategoryActivity(category: Category) {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("categoryName", category.name)
        startActivity(intent)
    }

    private fun fetchCategoryData() {
        showLoading()
        val db = FirebaseFirestore.getInstance()
        db.collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                hideLoading()
                categoryList = mutableListOf()
                for (document in documents) {
                    val categoryImage = document.getString("image")
                    val categoryName = document.getString("name")

                    categoryList.add(
                        Category(
                            categoryName ?: "",
                            categoryImage ?: ""
                        )
                    )
                }
                val adapter = CategoryAdapter(categoryList, this::navigateToCategoryActivity)

                categoryRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting categories: ", exception)
            }
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.categoryImageView)
        val categoryTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
    }

    class CategoryAdapter(private val categories: List<Category>, private val onItemClick: (Category) -> Unit) : RecyclerView.Adapter<CategoryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_card, parent, false)
            return CategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            // Bind category data to the ViewHolder
            if (!category.image.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(category.image)
                    .placeholder(R.drawable.product1) // Placeholder image while loading
                    .error(R.drawable.product2) // Error image if unable to load
                    .into(holder.imageView)
            }
            holder.categoryTextView.text = category.name

            holder.itemView.setOnClickListener {
                onItemClick(category)
            }
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }

    private fun showLoading() {
        progressDialog.setMessage("Tunggu sebentar...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }

    data class Category(val name: String, val image: String)


}


