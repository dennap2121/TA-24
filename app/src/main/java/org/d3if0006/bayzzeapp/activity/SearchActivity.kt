package org.d3if0006.bayzzeapp.activity

import Product
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivitySearchBinding
import androidx.appcompat.widget.SearchView

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var productList: MutableList<Product> // Store all products here
    private lateinit var productListCart: MutableList<Product> // Store all products here
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        val searchName = intent.getStringExtra("searchName")

        // Initialize productListCart
        productListCart = mutableListOf()

        // Set title and back button
        binding.searchView.setQuery(searchName, false)
        binding.backButton.setOnClickListener {
            finish()
        }

        setupFloatingButton()

        // Initialize RecyclerView
        productRecyclerView = findViewById(R.id.productRecyclerView)
        productRecyclerView.layoutManager = GridLayoutManager(this, 2) // 2 items horizontally

        setupSearchView()

        // Fetch products initially with a default search query
        fetchProductData(searchName ?: "")
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Called when the user submits the query by pressing the search button
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Called when the text in the search view changes
                // Fetch products based on the new search query
                fetchProductData(newText.orEmpty())
                return true
            }
        })
    }

    private fun showProductDetailModal(product: Product) {
        val modalBottomSheet = ProductDetailModalBottomSheet.newInstance(product)
        modalBottomSheet.show(supportFragmentManager, modalBottomSheet.tag)
    }

    private fun fetchProductData(search: String) {
        val db = FirebaseFirestore.getInstance()
        showLoading()
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                hideLoading()
                productList = mutableListOf()
                Log.d("ggwp", documents.size().toString())
                for (document in documents) {
                    val productId = document.id
                    val productName = document.getString("name")
                    val productPrice = document.getDouble("price")
                    val productImageURL = document.getString("image")
                    val productDescription = document.getString("description")
                    val productCategory = document.getString("category")
                    // Add product to the list if its name contains the search query
                    if (productName?.contains(search, ignoreCase = true) == true) {
                        productList.add(Product(productId ?: "", productName, productPrice ?: 0.0, 0, productDescription ?: "", productImageURL ?: "", productCategory ?: ""))
                    }
                }
                val adapter = ProductAdapter(productList, this::showProductDetailModal, this::addToCart)
                productRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }


    private fun addToCart(product: Product) {
        // Check if the product quantity is greater than 0
        if (product.quantity > 0) {
            // Check if the product already exists in the cart list
            val existingProduct = productListCart.find { it.id == product.id }
            if (existingProduct != null) {
                // If the product already exists, update its quantity
                existingProduct.quantity = product.quantity
                // Optionally, you can perform additional operations here, such as updating UI or saving the cart state
            } else {
                // If the product does not exist, add it to the cart list
                productListCart.add(product)
                // Optionally, you can perform additional operations here, such as updating UI or saving the cart state
            }
        } else {
            // If the product quantity is 0, remove it from the cart list if it exists
            productListCart.remove(product)
            // Optionally, you can perform additional operations here, such as updating UI or saving the cart state
        }
    }

    private fun setupFloatingButton() {
        binding.floatingButton.setOnClickListener {
            // Check if any products have been added to the cart
            if (productListCart.isNotEmpty()) {
                // Redirect to the cart activity
                val intent = Intent(this, CartActivity::class.java)
                // Pass the list of selected products to the cart activity
                intent.putExtra("selectedProducts", ArrayList(productListCart))
                startActivity(intent)
                finish()
            } else {
                // Show a message indicating that the cart is empty
                Toast.makeText(this, "No products added to the cart", Toast.LENGTH_SHORT).show()
            }

            // Add selected products to SharedPreferences
            addProductsToSharedPreferences(productListCart)
        }
    }

    private fun addProductsToSharedPreferences(newProducts: List<Product>) {
        val sharedPreferences = getSharedPreferences("products", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        // Retrieve existing products from SharedPreferences
        val jsonProducts = sharedPreferences.getString("productList", null)
        val existingProducts = if (jsonProducts != null) {
            Gson().fromJson(jsonProducts, Array<Product>::class.java).toMutableList()
        } else {
            mutableListOf()
        }
        // Merge or update existing products with the new products
        for (newProduct in newProducts) {
            val existingProductIndex = existingProducts.indexOfFirst { it.id == newProduct.id }
            if (existingProductIndex != -1) {
                // Update existing product
                existingProducts[existingProductIndex] = newProduct
            } else {
                // Add new product
                existingProducts.add(newProduct)
            }
        }
        // Serialize the merged list of products to JSON
        val jsonMergedProducts = Gson().toJson(existingProducts)
        // Save the JSON string to SharedPreferences
        editor.putString("productList", jsonMergedProducts)
        editor.apply()
    }

    companion object {
        private const val TAG = "SearchActivity"
    }

    class ProductAdapter(
        private val productList: List<Product>,
        private val onItemClick: (Product) -> Unit,
        private val onAddToCart: (Product) -> Unit
    ) : RecyclerView.Adapter<ProductViewHolder>() {

        // Total quantity variable to keep track of the sum
        private var totalQuantity = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
            return ProductViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            val product = productList[position]
            // Bind product data to the ViewHolder
            holder.productNameTextView.text = product.name
            val formattedPrice = String.format("Harga: Rp. %.0f,-", product.price)
            holder.productPriceTextView.text = formattedPrice
            // Load product image from URL using a library like Picasso, Glide, or Coil
            if (!product.image.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(product.image)
                    .placeholder(R.drawable.product2) // Placeholder image while loading
                    .error(R.drawable.product2) // Error image if unable to load
                    .into(holder.productImageView)
            }
//            holder.itemView.setOnClickListener {
//                onItemClick(product)
//            }
            holder.itemView.findViewById<Button>(R.id.addToCartButton).setOnClickListener {
                holder.showCounterLayout()
                if(product.quantity >= 0){
                    totalQuantity++
                }
                product.quantity++
                onAddToCart(product)
                updateFloatingActionButton(holder.itemView.context)
            }
            holder.itemView.findViewById<ImageButton>(R.id.plusButton).setOnClickListener {
                holder.incrementQuantity()
                totalQuantity++
                product.quantity++
                onAddToCart(product)
                updateFloatingActionButton(holder.itemView.context) // Call this function to update floating action button
            }
            holder.itemView.findViewById<ImageButton>(R.id.minusButton).setOnClickListener {
                holder.decrementQuantity()
                if(product.quantity > 0){
                    totalQuantity--
                }
                product.quantity--
                onAddToCart(product)
                updateFloatingActionButton(holder.itemView.context) // Call this function to update floating action button
            }
        }

        // Function to update the floating action button's text
        private fun updateFloatingActionButton(context: Context) {
            // Get a reference to your floating action button
            val fabTextView: TextView = (context as SearchActivity).findViewById(R.id.floatingButton)
            // Update the text with the new total quantity
//            fabTextView.text = "Tambahkan ($totalQuantity) produk"
            fabTextView.text = "Tambahkan produk"
        }

        override fun getItemCount(): Int {
            return productList.size
        }
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImageView: ImageView = itemView.findViewById(R.id.productImageView)
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
        private val addToCartButton: Button = itemView.findViewById(R.id.addToCartButton)
        private val counterLayout: LinearLayout = itemView.findViewById(R.id.counterLayout)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)

        fun showCounterLayout() {
            addToCartButton.visibility = View.GONE
            counterLayout.visibility = View.VISIBLE
        }

        fun incrementQuantity() {
            val currentQuantity = quantityTextView.text.toString().toInt()
            quantityTextView.text = (currentQuantity + 1).toString()
        }

        fun decrementQuantity() {
            val currentQuantity = quantityTextView.text.toString().toInt()
            if (currentQuantity > 0) {
                quantityTextView.text = (currentQuantity - 1).toString()
            }else{
                addToCartButton.visibility = View.VISIBLE
                counterLayout.visibility = View.GONE
            }
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
}


