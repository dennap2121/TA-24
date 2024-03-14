package org.d3if0006.bayzzeapp.activity

import OrderProduct
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
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

class ExpiredActivity : AppCompatActivity() {

    private lateinit var expiredAdapter: ExpiredAdapter
    private var expiredList = mutableListOf<Expired>()
    private val CREATE_DOCUMENT_REQUEST = 123
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expired)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog


        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewExpired)
        val export = findViewById<ImageView>(R.id.menu_export)

        export.setOnClickListener{
            exportToCSV()
        }


        // Initialize RecyclerView and adapter
        expiredAdapter = ExpiredAdapter(expiredList, this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExpiredActivity)
            adapter = expiredAdapter
        }

        // Fetch expired data from Firestore
        fetchExpiredsFromFirestore()

    }

    private fun fetchExpiredsFromFirestore() {
        showLoading()
        val db = FirebaseFirestore.getInstance()
        val currentTime = Calendar.getInstance().time
        val twentyFourHoursAgo = Calendar.getInstance()
        twentyFourHoursAgo.add(Calendar.HOUR_OF_DAY, -24)

        db.collection("orders")
            .whereEqualTo("deliveryType", "Ambil ke Toko")
            .whereEqualTo("paymentType", "COD")
            .get()
            .addOnSuccessListener { result ->
                hideLoading()
                expiredList = mutableListOf<Expired>()
                for (document in result) {
                    val orderCreatedAt = document.getDate("createdAt")
                    if (orderCreatedAt != null) {
                        val diffInMillis = currentTime.time - orderCreatedAt.time
                        val diffInHours = diffInMillis / (1000 * 60 * 60)

                        // Check if the order is expired (createdAt more than 24 hours ago)
                        if (diffInHours > 24) {
                            val dateFormat = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault())
                            val formattedDate = dateFormat.format(orderCreatedAt)

                            val orderId = document.id
                            val orderName = document.getString("name") ?: "-"
                            val expired = Expired(orderId, formattedDate.toString(), orderName)
                            expiredList.add(expired)
                        }
                    }
                }
                expiredAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching expired orders", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportToCSV() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "expireds.csv")
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
                for (expired in expiredList) {
                    it.write("${expired.order}, ${expired.tanggal}\n")
                }
            }
            Toast.makeText(this, "Expireds exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    // Inner class for the ExpiredAdapter
    private inner class ExpiredAdapter(
        private val expiredList: List<Expired>,
        private val context: Context
    ) : RecyclerView.Adapter<ExpiredAdapter.ExpiredViewHolder>() {

        // ViewHolder class for holding item views
        inner class ExpiredViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val expiredNoTextView: TextView = itemView.findViewById(R.id.expiredNoTextView)
            val expiredTanggalTextView: TextView = itemView.findViewById(R.id.expiredTanggalTextView)
            val expiredOrderTextView: TextView = itemView.findViewById(R.id.expiredOrderTextView)
            val expiredDeleteImageView: ImageView = itemView.findViewById(R.id.expiredDeleteImageView)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpiredViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_expired, parent, false)
            return ExpiredViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ExpiredViewHolder, position: Int) {
            val currentExpired = expiredList[position]
            val number = position + 1
            var products = ""

            Log.d("poilkj", products)



            holder.expiredNoTextView.text = number.toString()
            holder.expiredTanggalTextView.text = currentExpired.tanggal
            holder.expiredOrderTextView.text = currentExpired.order

            holder.expiredDeleteImageView.setOnClickListener {
                showDeleteConfirmationDialog(currentExpired.id)
            }



        }

        private fun showDeleteConfirmationDialog(id: String) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.popup_delete_expired)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val buttonNo = dialog.findViewById<Button>(R.id.buttonNo)
            val buttonYes = dialog.findViewById<Button>(R.id.buttonYes)

            buttonNo.setOnClickListener {
                dialog.dismiss() // Dismiss the dialog when "Tidak" button is clicked
            }

            buttonYes.setOnClickListener {
                // Perform delete operation here
                // Call a function to delete the data for article or banner
                deleteContent(id)
                dialog.dismiss() // Dismiss the dialog after deleting the content
            }

            dialog.show()
        }

        private fun deleteContent(id: String) {
            val db = FirebaseFirestore.getInstance()
            val documentId = id


            db.collection("orders")
                .document(documentId)
                .delete()
                .addOnSuccessListener {

                    fetchExpiredsFromFirestore()
                    expiredAdapter.notifyDataSetChanged()

                    // Document successfully deleted
                    // You can perform any additional actions here, such as updating the UI
                    Log.d(ContentValues.TAG, "DocumentSnapshot successfully deleted!")
                }
                .addOnFailureListener { e ->
                    // Failed to delete the document
                    Log.w(ContentValues.TAG, "Error deleting document", e)
                }
        }

        private fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formattedAmount = formatter.format(amount)
            return formattedAmount.replace("$", "")
        }

        override fun getItemCount(): Int {
            return expiredList.size
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

    data class Expired(val id: String, val tanggal: String, val order: String)

}
