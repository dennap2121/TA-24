package org.d3if0006.bayzzeapp.activity

import Product
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityOrderBinding
import java.util.Calendar

class OrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderBinding
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var selectedProducts: MutableList<Product>
    private lateinit var layoutDeliveryInfo: LinearLayout
    private lateinit var switchDelivery: Switch
    private lateinit var editName: EditText
    private lateinit var editAddress: EditText
    private lateinit var editZIP: EditText
    private lateinit var editNotes: EditText
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var textSubtotal: TextView
    private lateinit var textDelivery: TextView
    private lateinit var textTotal: TextView
    private lateinit var submitButton: Button
    private lateinit var progressDialog: ProgressDialog
    private lateinit var areaSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize views
        switchDelivery = findViewById(R.id.switchDelivery)
        layoutDeliveryInfo = findViewById(R.id.layoutDeliveryInfo)
        editName = findViewById(R.id.editName)
        editAddress = findViewById(R.id.editAddress)
        editZIP = findViewById(R.id.editZIP)
        editNotes = findViewById(R.id.editNotes)
        textSubtotal = findViewById(R.id.textSubtotal)
        textDelivery = findViewById(R.id.textDelivery)
        textTotal = findViewById(R.id.textTotal)
        submitButton = findViewById(R.id.submitButton)
        areaSpinner = findViewById(R.id.areaSpinner)


        // Set up the area spinner
        val areas = arrayOf("Bandung Barat", "Bandung Timur", "Bandung Selatan", "Bandung Utara", "Bandung Tengah", "Kabupaten Bandung")
        val adapterArea = ArrayAdapter(this, android.R.layout.simple_spinner_item, areas)
        adapterArea.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        areaSpinner.adapter = adapterArea

        layoutDeliveryInfo = findViewById(R.id.layoutDeliveryInfo)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        // Set title and back button
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.switchDelivery.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layoutDeliveryInfo.visibility = View.VISIBLE
            } else {
                layoutDeliveryInfo.visibility = View.GONE
            }
            // Calculate and update order details
            updateOrderDetails()
        }

        binding.submitButton.setOnClickListener{
            showPaymentTypeDialog()
        }

        selectedProducts = intent.getParcelableArrayListExtra<Product>("selectedProducts") ?: mutableListOf()

        val adapter = OrderProductAdapter(selectedProducts)

        orderRecyclerView = findViewById(R.id.orderRecyclerView)
        orderRecyclerView.layoutManager = LinearLayoutManager(this)
        orderRecyclerView.adapter = adapter

        areaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Calculate and update order details when the area is changed
                updateOrderDetails()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        updateOrderDetails()

//        updateSubmitButtonText()
    }

    private fun updateSubmitButtonText() {
        val productCount = selectedProducts.size
        val buttonText = "Tambahkan ($productCount) produk"
        submitButton.text = buttonText
    }

    private fun updateOrderDetails() {
        val subtotal = calculateSubtotal(selectedProducts)
        val deliveryCost = calculateDeliveryCost()
        val total = subtotal + deliveryCost

        textSubtotal.text = String.format("Rp. %.0f", subtotal)
        textDelivery.text = String.format("Rp. %.0f", deliveryCost)
        textTotal.text = String.format("Rp. %.0f", total)
    }
    private fun calculateSubtotal(products: List<Product>): Double {
        var subtotal = 0.0
        for (product in products) {
            subtotal += product.price * product.quantity
        }
        return subtotal
    }

    private fun calculateDeliveryCost(): Double {
        val selectedArea = areaSpinner.selectedItem.toString()
        return when (selectedArea) {
            "Bandung Barat" -> 10000.0
            "Bandung Timur" -> 11000.0
            "Bandung Utara" -> 12000.0
            "Bandung Selatan" -> 13000.0
            "Bandung Tengah" -> 14000.0
            "Kabupaten Bandung" -> 15000.0
            else -> 0.0 // Default case
        }
    }

    class OrderProductAdapter(
        private val productList: List<Product>
        ) : RecyclerView.Adapter<OrderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_summary_card, parent, false)
            return OrderViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
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
            val formattedQty = String.format("Qty: %d", product.quantity)
            holder.productQuantityTextView.text = formattedQty
        }

        override fun getItemCount(): Int {
            return productList.size
        }
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
        val productImageTextView: ImageView = itemView.findViewById(R.id.productImageView)
        val productQuantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
    }

    private fun showPaymentTypeDialog() {
        var paymentOptions = arrayOf("")

        if (switchDelivery.isChecked) {
            paymentOptions = arrayOf("Bayar Via Transfer", "Bayar COD")
        } else {
            paymentOptions = arrayOf("Bayar Via Transfer", "Bayar Ditempat")
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Metode Pembayaran")
        builder.setItems(paymentOptions) { _, which ->
            val paymentType = if (which == 0) {
                "transfer"
            } else {
                "COD"
            }
            submitOrder(paymentType)
        }
        builder.show()
    }

    private fun show24HourLimitPopup() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Batas pengambilan 24 jam")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, HistoryActivity::class.java)
                intent.putExtra("tabName", "Sedang diproses")
                startActivity(intent)
                finish()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun submitOrder(paymentType: String) {
        showLoading() // Show loading
        // Calculate subtotal, delivery cost, and total
        val subtotal = calculateSubtotal(selectedProducts)
        val deliveryCost = calculateDeliveryCost()
        val total = subtotal + deliveryCost

        // Get UID of the current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        // Get current date and time
        val currentDate = Calendar.getInstance().time

        // Retrieve order details from XML inputs
        val deliveryType = if (switchDelivery.isChecked) {
            "Diantar Toko"
        } else {
            "Ambil ke Toko"
        }
        val name = editName.text.toString()
        val address = editAddress.text.toString()
        val zipCode = editZIP.text.toString()
        val notes = editNotes.text.toString()
        var status = "Sedang diproses"

        if(paymentType == "COD"){
            status = "Selesai Pembayaran"
        }

        val orderData = mapOf(
            "userId" to uid,
            "subtotal" to subtotal,
            "pengiriman" to deliveryCost,
            "total" to total,
            "createdAt" to currentDate,
            "deliveryType" to deliveryType,
            "name" to name,
            "address" to address,
            "zipCode" to zipCode,
            "notes" to notes,
            "status" to status,
            "paymentType" to paymentType,
            // Add other order details as needed
            // For example, you can add products data from the list
            "products" to selectedProducts.map { product ->
                mapOf(
                    "productId" to product.id,
                    "productName" to product.name,
                    "quantity" to product.quantity,
                    // Add other product details as needed
                )
            }
        )

        val db = FirebaseFirestore.getInstance()
        db.collection("orders")
            .add(orderData)
            .addOnSuccessListener { documentReference ->
                hideLoading() // Hide loading
                val orderId = documentReference.id // Get the orderId from the document reference
                Toast.makeText(this, "Success add item to cart", Toast.LENGTH_SHORT).show()
                clearProductsSharedPreferences()
                if(paymentType == "transfer"){
                    val intent = Intent(this, PaymentActivity::class.java)
                    intent.putExtra("orderId", orderId) // Pass the orderId to the PaymentActivity intent
                    intent.putExtra("subtotal", subtotal)
                    intent.putExtra("deliveryCost", deliveryCost)
                    intent.putExtra("total", total)
                    startActivity(intent)
                    finish()
                }else{
                    if (paymentType == "COD" && !switchDelivery.isChecked) {
                        show24HourLimitPopup()
                    } else {
                        val intent = Intent(this, HistoryActivity::class.java)
                        intent.putExtra("tabName", "Sedang diproses")
                        startActivity(intent)
                        finish()
                    }

                }

            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add item to cart", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearProductsSharedPreferences() {
        val sharedPreferences = getSharedPreferences("products", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear() // Clear all data in the SharedPreferences
        editor.apply()
    }

    private fun showLoading() {
        progressDialog.setMessage("Pembayaran...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }
}


