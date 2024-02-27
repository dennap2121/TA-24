package org.d3if0006.bayzzeapp.activity

import Product
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var searchView:SearchView
    private lateinit var imageSlider: ImageSlider
    private lateinit var productList: MutableList<Product>
    private lateinit var articleList: MutableList<Article>
    private lateinit var categoryList: MutableList<Category>
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var articleRecyclerView: RecyclerView
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_beranda
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_beranda -> {
                    // Handle click on "Beranda" tab
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_keranjang -> {
                    // Handle click on "Keranjang" tab
                    startActivity(Intent(this, CartActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    intent.putExtra("tabName", "Sedang diproses")
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        fetchProductData()
        fetchCategoryData()
        fetchArticleData()
        fetchBannerData()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            checkIfUserExistsInFirestore(uid)
        }

        imageSlider = findViewById(R.id.image_slider)

        val profileActbutton = findViewById<ImageButton>(R.id.profile_act_btn)
        profileActbutton.setOnClickListener {
            val Intent = Intent( this, ProfileActivity::class.java)
            startActivity(Intent)
        }



        searchView = findViewById(R.id.searchView)

        binding.kategoriSemua.setOnClickListener{
            val intent = Intent(this, ListActivity::class.java)
            startActivity(intent)
        }


        // Initialize RecyclerView
        productRecyclerView = findViewById(R.id.productRecomendRecyclerView)
        productRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        articleRecyclerView = findViewById(R.id.articleRecyclerView)
        articleRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)


        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)


        setupSearchView()

//        productRecyclerView.layoutManager = GridLayoutManager(this, 2) // 2 items horizontally
//        productRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL)) // Add vertical divider
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Called when the user submits the query by pressing the search button
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Called when the text in the search view changes
                // Start a new SearchActivity and pass the newText as an extra
                val intent = Intent(this@MainActivity, SearchActivity::class.java)
                intent.putExtra("searchName", newText)
                startActivity(intent)
                return true
            }
        })
    }

    private fun checkIfUserExistsInFirestore(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userInfoRef = db.collection("user_info").document(uid)

        userInfoRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    showProfileCompletionPopup()
                }
            }
            .addOnFailureListener { exception ->
                // Error occurred while accessing Firestore
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProfileCompletionPopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_profile_completion, null)

        val etFullName = dialogView.findViewById<EditText>(R.id.etFullName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Complete Profile")

        val dialog = dialogBuilder.create()

        // Retrieve email from SharedPreferences
        val sharedPreferences = getSharedPreferences("loginDetails", Context.MODE_PRIVATE)
        val email = sharedPreferences.getString("email", "")

        btnSubmit.setOnClickListener {
            // Here, you can handle form submission
            val name = etFullName.text.toString()
            val phone = etPhone.text.toString()

            // Validate form fields if needed
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                // Save the user information to Firestore or perform any other actions
                if (email != null) {
                    saveUserProfileToFirestore(name, phone, email)
                }

                // Dismiss the dialog
                dialog.dismiss()

            } else {
                // Show error message if form fields are not valid
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun saveUserProfileToFirestore(fullName: String, phone: String, email: String) {
        // Implement the logic to save user profile to Firestore
        // For simplicity, let's assume you have a Firebase Firestore instance and a "users" collection
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            "name" to fullName,
            "phone" to phone,
            "email" to email
        )
        db.collection("user_info").document(FirebaseAuth.getInstance().currentUser?.uid ?: "").set(user)
            .addOnSuccessListener {
                Log.d("ProfileActivity", "User profile successfully created")
            }
            .addOnFailureListener { e ->
                Log.w("ProfileActivity", "Error adding document", e)
            }
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
    }

    class CategoryAdapter(private val categories: List<Category>, private val onItemClick: (Category) -> Unit) : RecyclerView.Adapter<CategoryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_card, parent, false)
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

    class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    class ArticleAdapter(private val articles: List<Article>, private val onItemClick: (Article) -> Unit) : RecyclerView.Adapter<ArticleViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_article_card, parent, false)
            return ArticleViewHolder(view)
        }

        override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
            val article = articles[position]
            // Bind category data to the ViewHolder
            if (!article.image.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(article.image)
                    .placeholder(R.drawable.product1) // Placeholder image while loading
                    .error(R.drawable.product2) // Error image if unable to load
                    .into(holder.imageView)
            }
            holder.itemView.setOnClickListener {
                onItemClick(article)
            }
        }

        override fun getItemCount(): Int {
            return articles.size
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCategoryActivity(category: Category) {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("categoryName", category.name)
        startActivity(intent)
    }

    private fun openBrowser(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }

    private fun fetchProductData() {
        showLoading("Produk")
        val db = FirebaseFirestore.getInstance()
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
                    // Add product to the list
                    productList.add(Product(productId ?: "", productName ?: "Product", productPrice ?: 0.0, 0, productDescription ?: "", productImageURL ?: "", productCategory ?: ""))
                }
                val adapter = ProductAdapter(productList)
                productRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    private fun fetchCategoryData() {
        showLoading("Kategori")
        val db = FirebaseFirestore.getInstance()
        db.collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                hideLoading()
                categoryList = mutableListOf()
                for (document in documents) {
                    val categoryImage = document.getString("image")
                    val categoryName = document.getString("name")

                    categoryList.add(Category(categoryName ?: "", categoryImage ?: ""))
                }
                val adapter = CategoryAdapter(categoryList, this::navigateToCategoryActivity)

                categoryRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting categories: ", exception)
            }
    }

    private fun fetchBannerData() {
        val db = FirebaseFirestore.getInstance()
        db.collection("banners")
            .get()
            .addOnSuccessListener { documents ->

                val imageList =  ArrayList<SlideModel>()
                for (document in documents) {
                    val bannerImage = document.getString("image")

                    imageList.add(SlideModel( imageUrl = bannerImage ))
                }

                imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting banners: ", exception)
            }
    }

    private fun fetchArticleData() {
        showLoading("Artikel")
        val db = FirebaseFirestore.getInstance()
        db.collection("articles")
            .get()
            .addOnSuccessListener { documents ->
                hideLoading()
                articleList = mutableListOf()
                for (document in documents) {
                    val articleImage = document.getString("image")
                    val articleLink = document.getString("link")
                    // Add article to the list
                    articleList.add(Article(articleImage ?: "", articleLink ?: ""))
                }
                val adapter = ArticleAdapter(articleList) { data ->
                    openBrowser(data.link)
                }
                articleRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    class ProductAdapter(
        private val productList: List<Product>
    ) : RecyclerView.Adapter<ProductViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recomend_card, parent, false)
            return ProductViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            val product = productList[position]
            // Bind product data to the ViewHolder
            if (!product.image.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(product.image)
                    .placeholder(R.drawable.product1) // Placeholder image while loading
                    .error(R.drawable.product2) // Error image if unable to load
                    .into(holder.productImageView)
            }
        }

        override fun getItemCount(): Int {
            return productList.size
        }
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    private fun showLoading(string: String) {
        progressDialog.setMessage("Memuat data ${string}...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }

    data class Banner(val image: String) // Example data class, adjust as needed

    data class Article(val image: String, val link: String) // Example data class, adjust as needed

    data class Category(val name: String, val image: String) // Example data class, adjust as needed
}