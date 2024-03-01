package org.d3if0006.bayzzeapp.activity

import Order
import OrderProduct
import android.app.Dialog
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
import android.widget.CheckBox
import android.widget.EditText
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
import org.d3if0006.bayzzeapp.databinding.ActivityDeliveryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DeliveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeliveryBinding
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderList: MutableList<Order>
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog
    private lateinit var emptyStateImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeliveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        emptyStateImageView = findViewById(R.id.emptyStateImageView)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        val tabName = intent.getStringExtra("tabName")

        // Fetch delivery data from Firestore
        fetchDeliveryData(tabName!!)


        // Add tabs
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val tabs = listOf("Permintaan", "Selesai")
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
                fetchDeliveryData(selectedTabText)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not used
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not used
            }
        })

        val bottomNavigationAdminView: BottomNavigationView = findViewById(R.id.bottomNavigationAdminView)
        bottomNavigationAdminView.selectedItemId = R.id.nav_pengiriman
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


        // Initialize RecyclerView
        orderRecyclerView = findViewById(R.id.deliveryRecyclerView)
        orderRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun fetchDeliveryData(tabName: String) {
        showLoading()
        val db = FirebaseFirestore.getInstance()
        val query = if (tabName == "Permintaan") {
            db.collection("orders")
                .whereIn("status", listOf("Selesai Pembayaran", "Proses Pengiriman"))
        } else {
            db.collection("orders")
                .whereIn("status", listOf("Selesai", "Ditolak"))
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
                    val orderPaymentImage = document.getString("buktiPembayaran") ?: ""
                    val orderPaymentType = document.getString("paymentType") ?: ""
                    val orderRejectReason = document.getString("rejectReason") ?: ""


                    val orderProducts = document.get("products") as? List<HashMap<String, Any>>
                    val productList = orderProducts?.map {
                        val quantity = it["quantity"].toString()
                        val parsedQuantity = when (quantity) {
                            is String -> quantity.toIntOrNull() ?: 0 // Try to parse String to Int, fallback to 0 if not possible
                            else -> 0 // Default value if it's neither Int nor String
                        }
                        OrderProduct(
                            it["productId"] as? String ?: "",
                            it["productName"] as? String ?: "",
                            parsedQuantity,
                            it["productImage"] as? String ?: "https://cdn-icons-png.freepik.com/512/5787/5787100.png",
                        )
                    } ?: emptyList()

                    val order = Order(orderId, orderDeliveryType, orderTotal,
                        orderCreatedAt!!, productList, orderPengiriman, orderSubtotal, orderName, orderUserId, orderAddress, orderNotes, orderStatus, orderPaymentImage, orderPaymentType, orderRejectReason)
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


    private fun displayOrderList(orderList: List<Order>) {
        // Implement your logic to display the order list, such as setting up a RecyclerView adapter
    }

    class OrderAdapter(
        private val orderList: List<Order>,
        private val tabName: String,
        private val context: Context
    ) : RecyclerView.Adapter<OrderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery_card, parent, false)
            return OrderViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
            val order = orderList[position]

            Log.d("ioi", order.status)

            if(order.status == "Proses Pengiriman"){
                holder.deliveryTolakTextView.visibility = View.GONE
                holder.deliveryTerimaTextView.text = "Selesai"
            }

            if(order.status == "Selesai"){
                holder.deliveryTolakTextView.visibility = View.GONE
                holder.deliveryDetailTextView.visibility = View.GONE
                holder.deliveryTerimaTextView.visibility = View.GONE
//                holder.deliveryTerimaTextView.text = "Selesai"
//                holder.deliveryTerimaTextView.setTextColor(ContextCompat.getColor(context, R.color.green_light)) // Set red color
//                holder.deliveryTerimaTextView.setBackgroundResource(R.drawable.green_border) // Set red border background
            }

            if(order.status == "Ditolak"){
                holder.deliveryTolakTextView.visibility = View.GONE
                holder.deliveryDetailTextView.visibility = View.GONE
                holder.deliveryTerimaTextView.text = "Ditolak"
                holder.deliveryTerimaTextView.setTextColor(ContextCompat.getColor(context, R.color.red)) // Set red color
                holder.deliveryTerimaTextView.setBackgroundResource(R.drawable.red_border) // Set red border background
            }

            holder.deliveryNameTextView.text = order.name
            holder.deliveryTimeTextView.text = formatTimestampStringToTime(order.createdAt.toString())

            val products = order.products
            if (products != null && products.isNotEmpty()) {

                holder.deliveryProductNameTextView.visibility = View.VISIBLE
                holder.deliveryProductTotalTextView.visibility = View.VISIBLE

                holder.deliveryProductNameTextView.text = ""
                holder.deliveryProductTotalTextView.text = ""

                for (product in products) {
                    holder.deliveryProductTotalTextView.append(product.quantity.toString() + " pcs" + "\n")
                    holder.deliveryProductNameTextView.append(product.productName + "\n")
                }
            } else {
                // Hide the comment views if there are no comments or comments are null
                holder.deliveryProductNameTextView.visibility = View.GONE
                holder.deliveryProductTotalTextView.visibility = View.GONE
            }

            holder.deliveryDetailTextView.setOnClickListener{
                val dialog = Dialog(holder.itemView.context)
                dialog.setContentView(R.layout.popup_order_details) // Set custom layout for the dialog

                // Find views in the custom layout
                val titleTextView: TextView = dialog.findViewById(R.id.titleTextView)
                val userInfoTextView: TextView = dialog.findViewById(R.id.userInfoTextView)
                val addressTextView: TextView = dialog.findViewById(R.id.addressTextView)
                val notesTextView: TextView = dialog.findViewById(R.id.notesTextView)
//                val productsTextView: TextView = dialog.findViewById(R.id.productsTextView)
                val totalTextView: TextView = dialog.findViewById(R.id.totalTextView)
                val deliveryChargeTextView: TextView = dialog.findViewById(R.id.deliveryChargeTextView)
                val subtotalTextView: TextView = dialog.findViewById(R.id.subtotalTextView)

                // Set title
                titleTextView.text = "Alamat Penjemputan"

                // Set user info (name and phone)
                val userId = order.userId
                val userRef = FirebaseFirestore.getInstance().collection("user_info").document(userId)
                userRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val name = documentSnapshot.getString("name")
                            val phone = documentSnapshot.getString("phone")
                            userInfoTextView.text = "$name | $phone"
                        }
                    }

                // Set address and notes
                addressTextView.text = "${order.address}"
                notesTextView.text = "${order.notes}"

                // Set products
                val productsString = StringBuilder()
                for (product in order.products) {
                    val productPrice = FirebaseFirestore.getInstance().collection("products").document(product.productId)
                    var price = 0.00
                    userRef.get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                price = documentSnapshot.getDouble("price") ?: 0.00
                            }
                        }
                    productsString.append("${product.productName} ${product.quantity * price}\n")
                }
//                productsTextView.text = productsString.toString()

                // Set total, delivery charge, and subtotal
                totalTextView.text = "${formatCurrency(order.total)}"
                deliveryChargeTextView.text = "${formatCurrency(order.pengiriman)}"
                subtotalTextView.text = "${formatCurrency(order.subtotal)}"

                // Show the dialog
                dialog.show()
            }

            holder.deliveryTolakTextView.setOnClickListener {
                val dialog = Dialog(holder.itemView.context)
                dialog.setContentView(R.layout.popup_reject) // Set custom layout for the dialog

                // Find views in the custom layout
                val titleTextView: TextView = dialog.findViewById(R.id.titleTextView)
                val subtitleTextView: TextView = dialog.findViewById(R.id.subtitleTextView)
                val distanceCheckbox: CheckBox = dialog.findViewById(R.id.distanceCheckbox)
                val busyCheckbox: CheckBox = dialog.findViewById(R.id.busyCheckbox)
                val storeBusyCheckbox: CheckBox = dialog.findViewById(R.id.storeBusyCheckbox)
                val otherCheckbox: CheckBox = dialog.findViewById(R.id.otherCheckbox)
                val reasonEditText: EditText = dialog.findViewById(R.id.reasonEditText)
                val rejectButton: Button = dialog.findViewById(R.id.rejectButton)

                // Set title and subtitle
                titleTextView.text = "Konfirmasi Penolakan"
                subtitleTextView.text = "Berikan alasan kenapa anda menolak permintaan pick up ini"

                // Set the reject button click listener
                rejectButton.setOnClickListener {
                    // Show loading indicator
                    val progressDialog = ProgressDialog(holder.itemView.context)
                    progressDialog.setMessage("Mengirimkan...")
                    progressDialog.setCancelable(false)
                    progressDialog.show()

                    // Initialize a list to hold the reasons
                    val reasons = mutableListOf<String>()

                    // Check which checkboxes are checked and add their texts to the reasons list
                    if (distanceCheckbox.isChecked) {
                        reasons.add(distanceCheckbox.text.toString())
                    }
                    if (busyCheckbox.isChecked) {
                        reasons.add(busyCheckbox.text.toString())
                    }
                    if (storeBusyCheckbox.isChecked) {
                        reasons.add(storeBusyCheckbox.text.toString())
                    }
                    if (otherCheckbox.isChecked) {
                        reasons.add(otherCheckbox.text.toString())
                    }

                    // If there is text in the reasonEditText, add it to the reasons list
                    val customReason = reasonEditText.text.toString().trim()
                    if (customReason.isNotEmpty()) {
                        reasons.add(customReason)
                    }

                    // Update Firestore orders field
                    val db = FirebaseFirestore.getInstance()
                    val orderRef = db.collection("orders").document(order.id)
                    orderRef.update(
                        mapOf(
                            "status" to "Ditolak",
                            "rejectReason" to reasons.joinToString(", ")
                        )
                    )
                        .addOnSuccessListener {
                            // If the update is successful, dismiss the dialog and hide loading indicator
                            dialog.dismiss()
                            progressDialog.dismiss()
                            // Show a toast or perform any other action to notify the user
                            Toast.makeText(
                                holder.itemView.context,
                                "Permintaan Pengiriman Ditolak",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Refresh the page here (call a function to reload data)
                            // For example:
                            // fetchDataFromFirestore()
                        }
                        .addOnFailureListener { e ->
                            // If the update fails, log the error, hide loading indicator, and show a toast
                            progressDialog.dismiss()
                            Log.e("AdminActivity", "Error updating document", e)
                            Toast.makeText(
                                holder.itemView.context,
                                "Error: Permintaan Pengiriman Gagal Ditolak",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                // Show the dialog
                dialog.show()
            }


            holder.deliveryTerimaTextView.setOnClickListener {
                if(order.status == "Selesai" || order.status == "Ditolak" ){

                }else{
                    // Show loading indicator
                    val progressDialog = ProgressDialog(holder.itemView.context)
                    progressDialog.setMessage("Loading...")
                    progressDialog.setCancelable(false)
                    progressDialog.show()

                    val db = FirebaseFirestore.getInstance()
                    val orderRef = db.collection("orders").document(order.id)

                    orderRef.get().addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val currentStatus = documentSnapshot.getString("status")
                            val updatedStatus = if (currentStatus == "Proses Pengiriman") {
                                "Selesai"
                            } else {
                                "Proses Pengiriman"
                            }

                            // Update the status field
                            orderRef.update("status", updatedStatus)
                                .addOnSuccessListener {
                                    // If the update is successful, change the button text and hide the reject button
                                    holder.deliveryTolakTextView.visibility = View.GONE
                                    holder.deliveryTerimaTextView.text = if (updatedStatus == "Proses Pengiriman") {
                                        "Selesai"
                                    } else {
                                        "Diterima"
                                    }

                                    if(updatedStatus == "Selesai"){
                                        holder.deliveryTerimaTextView.visibility = View.GONE
                                    }

                                    // Dismiss the loading indicator
                                    progressDialog.dismiss()

                                    // Show a toast or perform any other action to notify the user
                                    Toast.makeText(
                                        holder.itemView.context,
                                        "Status Pesanan diubah menjadi $updatedStatus",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    // If the update fails, log the error, dismiss the loading indicator, and show a toast
                                    Log.e("AdminActivity", "Error updating document", e)
                                    progressDialog.dismiss()
                                    Toast.makeText(
                                        holder.itemView.context,
                                        "Error: Gagal mengubah status pesanan",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }

            }


        }

        override fun getItemCount(): Int {
            return orderList.size
        }

        fun formatTimestampStringToTime(inputTimestamp: String): String {
            val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT'z yyyy", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("HH.mm", Locale.ENGLISH)

            val date = inputFormat.parse(inputTimestamp)
            return outputFormat.format(date)
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formattedAmount = formatter.format(amount)
            return formattedAmount.replace("$", "")
        }



    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deliveryNameTextView: TextView = itemView.findViewById(R.id.deliveryNameTextView)
        val deliveryTimeTextView: TextView = itemView.findViewById(R.id.deliveryTimeTextView)
        val deliveryProductNameTextView: TextView = itemView.findViewById(R.id.deliveryProductNameTextView)
        val deliveryProductTotalTextView: TextView = itemView.findViewById(R.id.deliveryProductTotalTextView)
        val deliveryDetailTextView: TextView = itemView.findViewById(R.id.deliveryDetailTextView)
        val deliveryTolakTextView: TextView = itemView.findViewById(R.id.deliveryTolakTextView)
        val deliveryTerimaTextView: TextView = itemView.findViewById(R.id.deliveryTerimaTextView)
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


