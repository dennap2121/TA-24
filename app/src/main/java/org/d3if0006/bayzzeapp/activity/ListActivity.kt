package org.d3if0006.bayzzeapp.activity

import Product
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.google.gson.Gson
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityListBinding

class ListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding
    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var selectedProducts: MutableList<Product>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        val categories = listOf(
            Category("Coklat", R.drawable.icon_candy),
            Category("Camilan Manis", R.drawable.icon_cupcake),
            Category("Camilan Asin", R.drawable.icon_burger),
            Category("Minuman", R.drawable.icon_drink),
            Category("Makanan", R.drawable.icon_food)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.listRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = CategoryAdapter(categories) { category ->
            val intent = Intent(this, CategoryActivity::class.java)
            intent.putExtra("categoryName", category.name)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
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
            holder.imageView.setImageResource(category.imageResource)
            holder.categoryTextView.text = category.name

            holder.itemView.setOnClickListener {
                onItemClick(category)
            }
        }

        override fun getItemCount(): Int {
            return categories.size
        }
    }

    data class Category(val name: String, val imageResource: Int) // Example data class, adjust as needed

}


