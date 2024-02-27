package org.d3if0006.bayzzeapp.activity

import Product
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var selectedProducts: MutableList<Product>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFloatingButton()

        // Set title and back button
        binding.titleTextView.text = "Keranjang"
        binding.backButton.setOnClickListener {
            finish()
        }

        // Assuming 'titleTextView' is defined in your activity or fragment
        binding.openPopup.setOnClickListener {
            // Show popup with "Hapus semua produk" message
            val popup = PopupMenu(this, it)
            popup.menu.add("Hapus semua produk")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Hapus semua produk" -> {
                        clearProductsSharedPreferences()
                        recreate()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        selectedProducts = getSelectedProductsFromSharedPreferences()

        val adapter = CartProductAdapter(selectedProducts, this::updateToCart)

        cartRecyclerView = findViewById(R.id.cartRecyclerView)
        cartRecyclerView.layoutManager = LinearLayoutManager(this)
        cartRecyclerView.adapter = adapter
    }

    private fun getSelectedProductsFromSharedPreferences(): MutableList<Product> {
        val sharedPreferences = getSharedPreferences("products", Context.MODE_PRIVATE)
        val jsonProducts = sharedPreferences.getString("productList", null)
        if (jsonProducts != null) {
            // Deserialize the JSON string back into a list of Product objects using Gson
            val productListType = object : TypeToken<List<Product>>() {}.type
            return Gson().fromJson(jsonProducts, productListType)
        }
        return mutableListOf()
    }

    private fun updateToCart(product: Product) {
        // Find the index of the product in the selectedProducts list
        val index = selectedProducts.indexOfFirst { it.id == product.id }
        if (index != -1) {
            // Update the product in the list with the new quantity
            selectedProducts[index] = product

            // If the product quantity is zero, remove it from the list
            if (product.quantity == 0) {
                selectedProducts.removeAt(index)
            }

            // Notify the adapter that the data set has changed
            val adapter = cartRecyclerView.adapter as? CartProductAdapter
            adapter?.notifyDataSetChanged()
        }
    }

    private fun setupFloatingButton() {
        binding.floatingButton.setOnClickListener {
            // Check if any products have been added to the cart
            if (selectedProducts.isNotEmpty()) {
                // Redirect to the cart activity
                val intent = Intent(this, OrderActivity::class.java)
                // Pass the list of selected products to the cart activity
                intent.putExtra("selectedProducts", ArrayList(selectedProducts))
                startActivity(intent)
            } else {
                // Show a message indicating that the cart is empty
                Toast.makeText(this, "No products added to the cart", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class CartProductAdapter(
        private val productList: List<Product>,
        private val onUpdateToCart: (Product) -> Unit
        ) : RecyclerView.Adapter<CartViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart_card, parent, false)
            return CartViewHolder(view)
        }

        override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
            val product = productList[position]
            holder.productNameTextView.text = product.name
            val formattedPrice = String.format("Harga: Rp. %.0f,-", product.price)
            holder.productPriceTextView.text = formattedPrice
            if (!product.image.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(product.image)
                    .placeholder(R.drawable.product1) // Placeholder image while loading
                    .error(R.drawable.product2) // Error image if unable to load
                    .into(holder.productImageTextView)
            }
            holder.productQuantityTextView.text = product.quantity.toString()
            holder.itemView.findViewById<ImageButton>(R.id.plusButton).setOnClickListener {
                holder.incrementQuantity()
                product.quantity++
                onUpdateToCart(product)
            }
            holder.itemView.findViewById<ImageButton>(R.id.minusButton).setOnClickListener {
                holder.decrementQuantity()
                product.quantity--
                onUpdateToCart(product)
            }
        }

        override fun getItemCount(): Int {
            return productList.size
        }
    }

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
        val productImageTextView: ImageView = itemView.findViewById(R.id.productImageView)
        val productQuantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)

        fun incrementQuantity() {
            val currentQuantity = quantityTextView.text.toString().toInt()
            quantityTextView.text = (currentQuantity + 1).toString()
        }

        fun decrementQuantity() {
            val currentQuantity = quantityTextView.text.toString().toInt()
            if (currentQuantity > 0) {
                quantityTextView.text = (currentQuantity - 1).toString()
            }
        }
    }

    private fun clearProductsSharedPreferences() {
        val sharedPreferences = getSharedPreferences("products", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Clear all data in the SharedPreferences
        editor.apply()
    }
}

