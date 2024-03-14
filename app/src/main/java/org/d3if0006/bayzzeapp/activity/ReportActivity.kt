package org.d3if0006.bayzzeapp.activity

import OrderProduct
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private lateinit var reportAdapter: ReportAdapter
    private val reportList = mutableListOf<MonthlyRevenue>()
    private val CREATE_DOCUMENT_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewReport)
        val export = findViewById<ImageView>(R.id.menu_export)

        export.setOnClickListener{
            exportToCSV()
        }


        // Initialize RecyclerView and adapter
        reportAdapter = ReportAdapter(reportList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReportActivity)
            adapter = reportAdapter
        }

        // Fetch report data from Firestore
        fetchReportsFromFirestore()

    }

    private fun fetchReportsFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("orders")
            .get()
            .addOnSuccessListener { result ->
                val monthlyRevenueMap = mutableMapOf<String, Double>()

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

//                    val order = Order(orderId, orderDeliveryType, orderTotal,
//                        orderCreatedAt!!, productList, orderPengiriman, orderSubtotal, orderName, orderUserId, orderAddress, orderNotes, orderStatus, orderPaymentImage, orderPaymentPayment, orderRejectReason)
//                    reportList.add(order)

                    val calendar = Calendar.getInstance()
                    calendar.time = orderCreatedAt

                    // Format month and year (e.g., "Jan 2024")
                    val monthYear = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.time)

                    // Update monthly revenue map
                    monthlyRevenueMap[monthYear] = (monthlyRevenueMap[monthYear] ?: 0.0) + orderTotal
                }

                monthlyRevenueMap.forEach { (monthYear, revenue) ->
                    reportList.add(MonthlyRevenue(monthYear, revenue))
                }

                reportAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching reports", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportToCSV() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "reports.csv")
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
                it.write("Bulan, Pendapatan\n")
                for (report in reportList) {
                    it.write("${report.monthYear}, ${report.revenue}\n")
                }
            }
            Toast.makeText(this, "Reports exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    // Inner class for the ReportAdapter
    private inner class ReportAdapter(private val reportList: List<MonthlyRevenue>) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

        // ViewHolder class for holding item views
        inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val reportNoTextView: TextView = itemView.findViewById(R.id.reportNoTextView)
            val reportMonthTextView: TextView = itemView.findViewById(R.id.reportMonthTextView)
            val reportRevenueTextView: TextView = itemView.findViewById(R.id.reportRevenueTextView)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
            return ReportViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
            val currentReport = reportList[position]
            val number = position + 1
            var products = ""

            Log.d("poilkj", products)



            holder.reportNoTextView.text = number.toString()
            holder.reportMonthTextView.text = currentReport.monthYear
            holder.reportRevenueTextView.text = "${formatCurrency(currentReport.revenue)}".replace(",00", "")



            // Bind other report details to corresponding TextViews here
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formattedAmount = formatter.format(amount)
            return formattedAmount.replace("$", "")
        }

        override fun getItemCount(): Int {
            return reportList.size
        }
    }

    data class MonthlyRevenue(val monthYear: String, val revenue: Double)

}
