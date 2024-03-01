package org.d3if0006.bayzzeapp.activity

import Product
import android.app.Activity
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
import org.d3if0006.bayzzeapp.databinding.ActivityManageProductBinding
import androidx.appcompat.widget.SearchView

import android.graphics.Color
import android.net.Uri
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import org.d3if0006.bayzzeapp.databinding.ItemProductCardBinding
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.*

class ManageProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageProductBinding
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var productList: MutableList<Product>
    private lateinit var productListCart: MutableList<Product>
    private lateinit var progressDialog: ProgressDialog

    // Selected products for deletion
    private val selectedProducts = mutableListOf<Product>()
    private val CREATE_DOCUMENT_REQUEST = 123


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)

        val searchName = intent.getStringExtra("searchName")
        productListCart = mutableListOf()

        binding.delete.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.searchView.setQuery(searchName, false)
        binding.backButton.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }
        binding.exportPdf.setOnClickListener{
            exportToCSV()
        }


        binding.openPopup.setOnClickListener {
            // Show popup with "Hapus semua produk" message
            val popup = PopupMenu(this, it)
            popup.menu.add("Tambah Produk")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Tambah Produk" -> {
                        val intent = Intent(this, ConfigProductActivity::class.java)
                        intent.putExtra("id", "")
                        startActivity(intent)
                        recreate()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        productRecyclerView = findViewById(R.id.productRecyclerView)
        productRecyclerView.layoutManager = GridLayoutManager(this, 2)

        setupSearchView()
        fetchProductData(searchName ?: "")
    }

    private fun exportToCSV() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "product.csv")
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
                it.write("Name, Price, Qty, Category, Description, Image\n")
                for (product in productList) {
                    it.write("${product.name}, ${product.price}, ${product.quantity}, ${product.category}, ${product.description}, ${product.image}\n")
                }
            }
            Toast.makeText(this, "Transactions exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Penyesuaian Produk Katalog")
        alertDialogBuilder.setMessage("Apakah anda sudah yakin untuk menghapus produk katalog tersebut?")
        alertDialogBuilder.setPositiveButton("Hapus") { _, _ ->
            deleteSelectedProducts()
        }
        alertDialogBuilder.setNegativeButton("Batal") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun deleteSelectedProducts() {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        for (product in selectedProducts) {
            val productRef = db.collection("products").document(product.id)
            batch.delete(productRef)
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Produk dihapus", Toast.LENGTH_SHORT).show()
                selectedProducts.clear() // Clear selected products list
                binding.delete.visibility = View.GONE // Hide delete icon
                refreshProductList() // Refresh the product list
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal menghapus produk: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun refreshProductList() {
        fetchProductData(binding.searchView.query.toString())
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                fetchProductData(newText.orEmpty())
                return true
            }
        })
    }

    private fun fetchProductData(search: String) {
        val db = FirebaseFirestore.getInstance()
        showLoading()
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                hideLoading()
                productList = mutableListOf()
                for (document in documents) {
                    val productId = document.id
                    val productName = document.getString("name")
                    val productPrice = document.getDouble("price")
                    val productImageURL = document.getString("image")
                    val productDescription = document.getString("description")
                    val productCategory = document.getString("category")
                    if (productName?.contains(search, ignoreCase = true) == true) {
                        productList.add(Product(productId ?: "", productName, productPrice ?: 0.0, 0, productDescription ?: "", productImageURL ?: "", productCategory ?: ""))
                    }
                }
                val adapter = ProductAdapter(productList, this::showProductDetailModal, this::addToCart, this::onDeleteProduct)
                productRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    private fun showProductDetailModal(product: Product) {
        val modalBottomSheet = ProductDetailModalBottomSheet.newInstance(product)
        modalBottomSheet.show(supportFragmentManager, modalBottomSheet.tag)
    }

    private fun addToCart(product: Product) {
        if (product.quantity > 0) {
            val existingProduct = productListCart.find { it.id == product.id }
            if (existingProduct != null) {
                existingProduct.quantity = product.quantity
            } else {
                productListCart.add(product)
            }
        } else {
            productListCart.remove(product)
        }
    }

    private fun onDeleteProduct(product: Product) {
        if (selectedProducts.contains(product)) {
            selectedProducts.remove(product)
            if (selectedProducts.isEmpty()) {
                // Hide delete icon when no items are selected
                binding.delete.visibility = View.GONE
            }
        } else {
            selectedProducts.add(product)
            // Show delete icon when items are selected
            binding.delete.visibility = View.VISIBLE
        }
        // Update UI to reflect selection
        productRecyclerView.adapter?.notifyDataSetChanged()
    }

    // Update ProductAdapter to accept a onDeleteProduct function parameter
    inner class ProductAdapter(
        private val productList: List<Product>,
        private val onItemClick: (Product) -> Unit,
        private val onAddToCart: (Product) -> Unit,
        private val onDeleteProduct: (Product) -> Unit // New parameter
    ) : RecyclerView.Adapter<ProductViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
            return ProductViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            val product = productList[position]
            holder.bind(product)
            holder.itemView.setOnLongClickListener {
                onDeleteProduct(product)
                true
            }
        }

        override fun getItemCount(): Int {
            return productList.size
        }
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = ItemProductCardBinding.bind(itemView)

        fun bind(product: Product) {
            binding.apply {
                productNameTextView.text = product.name
                addToCartButton.text = "Edit"
                val formattedPrice = String.format("Harga: Rp. %.0f,-", product.price)
                productPriceTextView.text = formattedPrice
                Glide.with(itemView.context)
                    .load(product.image)
                    .placeholder(R.drawable.product2)
                    .error(R.drawable.product2)
                    .into(productImageView)

                // Set item background color based on selection
                if (selectedProducts.contains(product)) {
                    itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.primary))
                } else {
                    itemView.setBackgroundColor(Color.TRANSPARENT)
                }

                // Increment quantity on button click
                plusButton.setOnClickListener {
                    // Increment quantity logic here
                }

                // Decrement quantity on button click
                minusButton.setOnClickListener {
                    // Decrement quantity logic here
                }

                // Add to cart button click listener
                addToCartButton.setOnClickListener {
                    // Navigate to ConfigAdapterActivity
                    val intent = Intent(itemView.context, ConfigProductActivity::class.java)
                    intent.putExtra("id", product.id)
                    intent.putExtra("previousImage", product.image)
                    itemView.context.startActivity(intent)
                }

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

    companion object {
        private const val TAG = "SearchActivity"
    }
}


