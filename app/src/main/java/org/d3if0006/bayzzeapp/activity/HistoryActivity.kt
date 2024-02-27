package org.d3if0006.bayzzeapp.activity

import Order
import OrderProduct
import android.app.ProgressDialog
import android.content.ContentValues.TAG
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityHistoryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderList: MutableList<Order>
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog
    private lateinit var emptyStateImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        emptyStateImageView = findViewById(R.id.emptyStateImageView)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        val tabName = intent.getStringExtra("tabName")

        // Fetch history data from Firestore
        fetchHistoryData(tabName!!)


        // Add tabs
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val tabs = listOf("Sedang diproses", "Selesai")
        tabs.forEach { tab ->
            tabLayout.addTab(tabLayout.newTab().setText(tab))
        }


        // Set the selected tab
        val tabIndex = tabs.indexOf(tabName)
        if (tabIndex != -1) {
            tabLayout.getTabAt(tabIndex)?.select()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedTabText = tab.text.toString()
                fetchHistoryData(selectedTabText)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not used
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not used
            }
        })

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.nav_history
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


        // Initialize RecyclerView
        orderRecyclerView = findViewById(R.id.historyRecyclerView)
        orderRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchHistoryData(tabName: String) {
        showLoading()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val query = if (tabName == "Sedang diproses") {
                db.collection("orders").whereEqualTo("userId", uid).whereEqualTo("status", "Sedang diproses")
            } else {
                db.collection("orders").whereEqualTo("userId", uid).whereEqualTo("status", "Selesai")
            }
            query.get()
                .addOnSuccessListener { documents ->
                    hideLoading()
                    orderList = mutableListOf<Order>()
                    for (document in documents) {
                        val orderId = document.id
                        val orderDeliveryType = document.getString("deliveryType") ?: ""
                        val orderName = document.getString("name") ?: "-"
                        val orderTotal = document.getDouble("total") ?: 0.0
                        val orderPengiriman = document.getDouble("pengiriman") ?: 0.0
                        val orderSubtotal = document.getDouble("subtotal") ?: 0.0
                        val orderCreatedAt = document.getDate("createdAt")
                        val orderUserId = document.getString("userId") ?: ""
                        val orderAddress = document.getString("address") ?: ""
                        val orderNotes = document.getString("notes") ?: ""
                        val orderStatus = document.getString("status") ?: ""


                        val orderProducts = document.get("products") as? List<HashMap<String, Any>>
                        val productList = orderProducts?.map {
                            OrderProduct(
                                it["productId"] as? String ?: "",
                                it["productName"] as? String ?: "",
                                it["quantity"] as? Int ?: 0,
                                it["productImage"] as? String ?: "",
                                )
                        } ?: emptyList()

                        val order = Order(orderId, orderDeliveryType, orderTotal,
                            orderCreatedAt!!, productList, orderPengiriman, orderSubtotal, orderName, orderUserId, orderAddress, orderNotes, orderStatus)
                        orderList.add(order)
                    }
                    // Call a function to display or process the order list
                    displayOrderList(orderList)
                    val adapter = OrderAdapter(orderList, tabName, this) // Pass tabName to adapter
                    orderRecyclerView.adapter = adapter

                    // Check if the RecyclerView data is empty
                    if (orderList.isEmpty()) {
                        emptyStateImageView.visibility = View.VISIBLE // Show the placeholder image
                        orderRecyclerView.visibility = View.GONE // Hide the RecyclerView
                    } else {
                        emptyStateImageView.visibility = View.GONE // Hide the placeholder image
                        orderRecyclerView.visibility = View.VISIBLE // Show the RecyclerView
                    }

                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }
    }


    private fun displayOrderList(orderList: List<Order>) {
        // Implement your logic to display the order list, such as setting up a RecyclerView adapter
    }

    class OrderAdapter(
        private val orderList: List<Order>,
        private val tabName: String,
        private val context: Context
    ) : RecyclerView.Adapter<OrderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_card, parent, false)
            return OrderViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
            val order = orderList[position]
            val formattedDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(order.createdAt)
            val formattedTotal = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(order.total)
            val pemesananNumber = position + 1 // Calculate the pemesanan number

            holder.orderDateTextView.text = formattedDate
            holder.orderProductTextView.text = order.products.size.toString()
            holder.orderTotalTextView.text = formattedTotal
            holder.orderNumberTextView.text = "Pemesanan #$pemesananNumber" // Set pemesanan number

            // Set the delivery type text
            holder.orderDeliveryTypeView.text = order.deliveryType

            // Set the background and text color based on the delivery type
            if (order.deliveryType == "Ambil ke Toko") {
                holder.orderDeliveryTypeView.setTextColor(ContextCompat.getColor(context, R.color.red)) // Set red color
                holder.orderDeliveryTypeView.setBackgroundResource(R.drawable.red_border) // Set red border background
            } else {
                holder.orderDeliveryTypeView.setTextColor(ContextCompat.getColor(context, R.color.green)) // Set blue color
                holder.orderDeliveryTypeView.setBackgroundResource(R.drawable.green_border) // Set blue border background
            }

            // Add click listener to the item
            holder.itemView.setOnClickListener {
                if (tabName == "Sedang diproses") {
                    val orderId = order.id // Get the orderId for the clicked item
                    val subtotal = order.subtotal
                    val deliveryCost = order.pengiriman
                    val total = subtotal + deliveryCost
                    // Launch PaymentActivity with necessary data
                    val intent = Intent(context, PaymentActivity::class.java).apply {
                        putExtra("orderId", orderId)
                        putExtra("subtotal", subtotal)
                        putExtra("deliveryCost", deliveryCost)
                        putExtra("total", total)
                    }
                    context.startActivity(intent)
                }else{
                    val orderId = order.id // Get the orderId for the clicked item
                    // Launch ReceiptActivity with necessary data
                    val intent = Intent(context, ReceiptActivity::class.java).apply {
                        putExtra("orderId", orderId)
                    }
                    context.startActivity(intent)
                }
            }
        }

        override fun getItemCount(): Int {
            return orderList.size
        }
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderDeliveryTypeView: TextView = itemView.findViewById(R.id.orderDeliveryTypeTextView)
        val orderDateTextView: TextView = itemView.findViewById(R.id.orderCreatedAtTextView)
        val orderProductTextView: TextView = itemView.findViewById(R.id.orderProductTextView)
        val orderTotalTextView: TextView = itemView.findViewById(R.id.orderTotalTextView)
        val orderNumberTextView: TextView = itemView.findViewById(R.id.orderNumberTextView)
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

