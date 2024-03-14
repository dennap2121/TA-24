package org.d3if0006.bayzzeapp.activity

import Order
import OrderProduct
import Transaction
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityAboutBinding
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionsActivity : AppCompatActivity() {

    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Order>()
    private val CREATE_DOCUMENT_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        val bottomNavigationAdminView: BottomNavigationView = findViewById(R.id.bottomNavigationAdminView)
        bottomNavigationAdminView.selectedItemId = R.id.nav_transaksi
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


        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTransactions)
        val export = findViewById<ImageView>(R.id.menu_export)

        export.setOnClickListener{
            exportToCSV()
        }


        // Initialize RecyclerView and adapter
        transactionAdapter = TransactionAdapter(transactionList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TransactionsActivity)
            adapter = transactionAdapter
        }

        // Fetch transaction data from Firestore
        fetchTransactionsFromFirestore()

    }

    private fun fetchTransactionsFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("orders")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
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
                    val orderPaymentPayment = document.getString("paymentType") ?: ""
                    val orderRejectReason = document.getString("rejectReason") ?: ""

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
                        orderCreatedAt!!, productList, orderPengiriman, orderSubtotal, orderName, orderUserId, orderAddress, orderNotes, orderStatus, orderPaymentImage, orderPaymentPayment, orderRejectReason)
                    transactionList.add(order)
                }
                transactionAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching transactions", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportToCSV() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "transactions.csv")
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
                it.write("Name, Date, Product, Total\n")
                for (transaction in transactionList) {
                    var products = ""
                    transaction.products.forEachIndexed { index, product ->
                        if(index == 0){
                            products = product.productName
                        }else{
                            products = products+" | "+product.productName
                        }
                    }
                    it.write("${transaction.name}, ${transaction.createdAt}, ${products}, ${transaction.total}\n")
                }
            }
            Toast.makeText(this, "Transactions exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    // Inner class for the TransactionAdapter
    private inner class TransactionAdapter(private val transactionList: List<Order>) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

        // ViewHolder class for holding item views
        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val transactionNameTextView: TextView = itemView.findViewById(R.id.transactionNameTextView)
            val transactionDateTextView: TextView = itemView.findViewById(R.id.transactionDateTextView)
            val transactionTotalTextView: TextView = itemView.findViewById(R.id.transactionTotalTextView)
            val transactionNoTextView: TextView = itemView.findViewById(R.id.transactionNoTextView)
            val transactionProductsTextView: TextView = itemView.findViewById(R.id.transactionProductsTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return TransactionViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            val currentTransaction = transactionList[position]
            val number = position + 1
            var products = ""
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(currentTransaction.createdAt)

            currentTransaction.products.forEachIndexed { index, product ->
                if(index == 0){
                    products = product.productName
                }else{
                    products = products+", "+product.productName
                }
            }

            Log.d("poilkj", products)

            holder.transactionNameTextView.text = currentTransaction.name
            holder.transactionDateTextView.text = formattedDate
            holder.transactionTotalTextView.text = "${formatCurrency(currentTransaction.total)}".replace(",00", "")
            holder.transactionNoTextView.text = number.toString()
            holder.transactionProductsTextView.text = products


            // Bind other transaction details to corresponding TextViews here
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formattedAmount = formatter.format(amount)
            return formattedAmount.replace("$", "")
        }

        override fun getItemCount(): Int {
            return transactionList.size
        }
    }
}
