package org.d3if0006.bayzzeapp.activity

import Product
import android.Manifest
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityAdminBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var reviewRecyclerView: RecyclerView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var reviewList: MutableList<Review>
    private lateinit var categoryRecyclerView: RecyclerView
    private lateinit var categoryList: MutableList<Category>
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var productList: MutableList<Product>
    private val WRITE_EXTERNAL_STORAGE_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_EXTERNAL_STORAGE_REQUEST)
        }

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        val bottomNavigationAdminView: BottomNavigationView = findViewById(R.id.bottomNavigationAdminView)
        bottomNavigationAdminView.selectedItemId = R.id.nav_beranda
        bottomNavigationAdminView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_beranda -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                    true
                }
                R.id.nav_pengiriman -> {
                    val intent = Intent(this, DeliveryActivity::class.java)
                    intent.putExtra("tabName", "Permintaan")
                    startActivity(intent)
                    true
                }
                R.id.nav_transaksi -> {
                    val intent = Intent(this, TransactionsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_pengaturan -> {
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }


        fetchReviewData()
        fetchCategoryData()
        fetchProductData()

        val profileActbutton = findViewById<ImageButton>(R.id.profile_act_btn)
        profileActbutton.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menu.add("Logout")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Logout" -> {
                        showConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        binding.pemasukanBulan.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }


        binding.penjualanBulan.setOnClickListener{
            val intent = Intent( this, ReportActivity::class.java)
            startActivity(intent)
        }

        val ulasanButton = findViewById<TextView>(R.id.ulasanSemua)
        ulasanButton.setOnClickListener {
            val intent = Intent( this, ReviewActivity::class.java)
            intent.putExtra("isAdmin", "admin")
            startActivity(intent)
        }

        val kategoriButton = findViewById<ImageView>(R.id.kategoriSemua)
        kategoriButton.setOnClickListener {
            val Intent = Intent( this, ManageCategoryActivity::class.java)
            startActivity(Intent)
        }

        val rekomendasiProdukButton = findViewById<ImageView>(R.id.rekomendasiProdukSemua)
        rekomendasiProdukButton.setOnClickListener {
            val Intent = Intent( this, ManageProductActivity::class.java)
            startActivity(Intent)
        }





        // Initialize RecyclerView
        reviewRecyclerView = findViewById(R.id.reviewRecyclerView)
        reviewRecyclerView.layoutManager = LinearLayoutManager(this)

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        productRecyclerView = findViewById(R.id.productRecomendRecyclerView)
        productRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)


        val firestore = FirebaseFirestore.getInstance()
        val today = Calendar.getInstance().apply {
            time = Date()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val ordersRef = firestore.collection("orders")
        val todayOrdersQuery = ordersRef
            .whereIn("status", listOf("Selesai Pembayaran", "Proses Pengiriman", "Selesai"))
            .orderBy("createdAt", Query.Direction.DESCENDING)

        todayOrdersQuery.get()
            .addOnSuccessListener { documents ->
                var numberOfOrders = 0
                var totalRevenue = 0.0

                documents.forEach { document ->

                    val dateString = today
                    val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'Z yyyy", Locale.US)
                    val dateNow = dateFormat.parse(dateString.toString())
                    val targetFormatNow = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    val formattedDateNow = targetFormatNow.format(dateNow)

                    val timestamp = document["createdAt"].toString()
                    val seconds = timestamp.substringAfter("seconds=").substringBefore(",").toLong()
                    val date = Date(seconds * 1000) // Convert seconds to milliseconds
                    val outputFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    val formattedDate = outputFormat.format(date)

//                    Log.d("pop", "today ${formattedDateNow} from data ${formattedDate}")

                    if (formattedDateNow == formattedDate) {
                        totalRevenue += document["total"] as Double
                        numberOfOrders++
                    }
                }



                val formattedRevenue = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalRevenue)
                val valuePemasukan = findViewById<TextView>(R.id.valuePemasukan)
                val valuePenjualan = findViewById<TextView>(R.id.valuePenjualan)
                valuePemasukan.text = formattedRevenue.replace(",00", "")
                valuePenjualan.text = numberOfOrders.toString()
            }
            .addOnFailureListener { exception ->
                Log.e("AdminActivity", "Error getting today's orders", exception)
            }

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // Month starts from 0, so add 1
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val currentMonthOrdersQuery = ordersRef
            .whereIn("status", listOf("Selesai Pembayaran", "Proses Pengiriman", "Selesai"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                var numberOfOrders = 0
                var totalRevenue = 0.0

                val calendar = Calendar.getInstance()

                documents.forEach { document ->
                    val createdAt = document["createdAt"] as Timestamp
                    calendar.time = createdAt.toDate()

                    val orderMonth = calendar.get(Calendar.MONTH) + 1 // Month starts from 0, so add 1
                    val orderYear = calendar.get(Calendar.YEAR)

                    if (orderYear == currentYear && orderMonth == currentMonth) {
                        totalRevenue += (document["total"] as Double?) ?: 0.0
                        numberOfOrders++
                    }
                }

                val formattedRevenue = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(totalRevenue)
                val valuePemasukanBulan = findViewById<TextView>(R.id.valuePemasukanBulan)
                val valuePenjualanBulan = findViewById<TextView>(R.id.valuePenjualanBulan)
                valuePemasukanBulan.text = formattedRevenue.replace(",00", "")
                valuePenjualanBulan.text = numberOfOrders.toString()
            }
            .addOnFailureListener { exception ->
                Log.e("AdminActivity", "Error getting current month's orders", exception)
            }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi")
        builder.setMessage("Apakah Anda yakin akan keluar aplikasi?")
        builder.setPositiveButton("Ya") { _, _ ->
            // If "Ya" is clicked, clear login details and navigate to SignInActivity
            clearLoginDetails()
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("Tidak") { dialog, _ ->
            // If "Tidak" is clicked, close the dialog
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun clearLoginDetails() {
        val sharedPreferences = getSharedPreferences("loginDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }


    private fun navigateToCategoryActivity(category: Category) {
        val intent = Intent(this, CategoryActivity::class.java)
        intent.putExtra("categoryName", category.name)
        startActivity(intent)
    }

    private fun fetchProductData() {
        showLoading()
        val db = FirebaseFirestore.getInstance()
        db.collection("products").whereEqualTo("isRecommended", true)
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

                    categoryList.add(Category(categoryName ?: "", categoryImage ?: ""))
                }
                val adapter =
                    CategoryAdapter(categoryList, this::navigateToCategoryActivity)

                categoryRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting categories: ", exception)
            }
    }

    private fun fetchReviewData() {
        showLoading()
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("reviews")
            .orderBy("date", Query.Direction.ASCENDING)
            .limit(1)
        query.get()
            .addOnSuccessListener { documents ->
                hideLoading()
                reviewList = mutableListOf()
                for (document in documents) {
                    val reviewId = document.id
                    val reviewName = document.getString("name") ?: "-"
                    val reviewDate = document.getString("date") ?: "01/01/2020"
                    val reviewReview = document.getString("review") ?: "-"
                    val reviewComments = document.get("comments") as? List<HashMap<String, String>>

                    val review = Review(
                        reviewId,
                        reviewName,
                        reviewDate,
                        reviewReview,
                        reviewComments
                    )
                    reviewList.add(review)
                }
                // Call a function to display or process the review list
                displayReviewList(reviewList)
                val adapter = ReviewAdapter(
                    reviewList,
                    this
                ) // Pass tabName to adapter
                reviewRecyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }


    private fun displayReviewList(reviewList: List<Review>) {
        // Implement your logic to display the review list, such as setting up a RecyclerView adapter
    }

    class ReviewAdapter(
        private var reviewList: List<Review>,
        private val context: Context
    ) : RecyclerView.Adapter<ReviewViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_review_card, parent, false)
            return ReviewViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
            val review = reviewList[position]

            holder.reviewNameTextView.text = review.name
            holder.reviewDateTextView.text = review.date
            holder.reviewReviewTextView.text = review.review

            val comments = review.comments
            if (comments != null && comments.isNotEmpty()) {
                holder.commentAuthorTextView.visibility = View.VISIBLE
                holder.commentContentTextView.visibility = View.VISIBLE
                holder.commentAuthorTextView.text = ""
                holder.commentContentTextView.text = ""
                for (comment in comments) {
                    holder.commentAuthorTextView.append(comment["name"] + "\n")
                    holder.commentContentTextView.append(comment["comment"] + "\n")
                }
            } else {
                // Hide the comment views if there are no comments or comments are null
                holder.commentAuthorTextView.visibility = View.GONE
                holder.commentContentTextView.visibility = View.GONE
            }

            holder.reviewSendIconImageView.setOnClickListener {
                val commentText = holder.reviewCommentEditText.text.toString().trim()

                if (commentText.isNotEmpty()) {
                    addCommentToFirestore(review.id, commentText)
                    holder.reviewCommentEditText.text.clear()
                } else {
                    Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount(): Int {
            return reviewList.size
        }

        private fun addCommentToFirestore(id: String, commentText: String) {
            val db = FirebaseFirestore.getInstance()
            val reviewRef = db.collection("reviews")
            val newComment = hashMapOf(
                "name" to "admin",
                "comment" to commentText
            )

            reviewRef.document(id).update("comments", FieldValue.arrayUnion(newComment))
                .addOnSuccessListener {
                    fetchReviewData()
                    Toast.makeText(context, "Comment added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error adding comment", Toast.LENGTH_SHORT).show()
                }

        }

        private fun fetchReviewData() {
            val db = FirebaseFirestore.getInstance()
            val query = db.collection("reviews")
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(1)
            query.get()
                .addOnSuccessListener { documents ->
                    var reviewListRaw = mutableListOf<Review>()
                    for (document in documents) {
                        val reviewId = document.id
                        val reviewName = document.getString("name") ?: "-"
                        val reviewDate = document.getString("date") ?: "01/01/2020"
                        val reviewReview = document.getString("review") ?: "-"
                        val reviewComments = document.get("comments") as? List<HashMap<String, String>>

                        val review = Review(
                            reviewId,
                            reviewName,
                            reviewDate,
                            reviewReview,
                            reviewComments
                        )
                        reviewListRaw.add(review)
                    }

                    reviewList = reviewListRaw

                    notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents: ", exception)
                }
        }

    }

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reviewNameTextView: TextView = itemView.findViewById(R.id.reviewNameTextView)
        val reviewDateTextView: TextView = itemView.findViewById(R.id.reviewDateTextView)
        val reviewCommentEditText: EditText = itemView.findViewById(R.id.reviewCommentEditText)
        val reviewReviewTextView: TextView = itemView.findViewById(R.id.reviewReviewTextView)
        val reviewSendIconImageView: ImageButton = itemView.findViewById(R.id.reviewSendIconImageView)
        val commentAuthorTextView: TextView = itemView.findViewById(R.id.commentAuthorTextView)
        val commentContentTextView: TextView = itemView.findViewById(R.id.commentContentTextView)
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

    private fun showLoading() {
        progressDialog.setMessage("Tunggu sebentar...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }

    data class Review(val id: String, val name: String, val date: String, val review: String, val comments: List<HashMap<String, String>>?)

    data class Category(val name: String, val image: String) // Example data class, adjust as needed

}