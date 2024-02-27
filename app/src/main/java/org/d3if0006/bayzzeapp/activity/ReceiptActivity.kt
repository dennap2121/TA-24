package org.d3if0006.bayzzeapp.activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptActivity : AppCompatActivity() {

    private lateinit var logoImageView: ImageView
    private lateinit var orderNumberTextView: TextView
    private lateinit var shareButton: Button
    private lateinit var orderDateTextView: TextView
    private lateinit var deliveryTypeTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var notesTextView: TextView
    private lateinit var productsTextView: TextView
    private lateinit var subtotalTextView: TextView
    private lateinit var deliveryChargeTextView: TextView
    private lateinit var totalTextView: TextView
    private lateinit var zipCodeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt)

        // Retrieve order details from intent
        val orderId = intent.getStringExtra("orderId")
        // Retrieve other order details as needed

        logoImageView = findViewById(R.id.logoImageView)
        orderNumberTextView = findViewById(R.id.orderNumberTextView)
        shareButton = findViewById(R.id.shareButton)
        logoImageView = findViewById(R.id.logoImageView)
        orderNumberTextView = findViewById(R.id.orderNumberTextView)
        orderDateTextView = findViewById(R.id.orderDateTextView)
        deliveryTypeTextView = findViewById(R.id.deliveryTypeTextView)
        addressTextView = findViewById(R.id.addressTextView)
        notesTextView = findViewById(R.id.notesTextView)
        productsTextView = findViewById(R.id.productsTextView)
        subtotalTextView = findViewById(R.id.subtotalTextView)
        deliveryChargeTextView = findViewById(R.id.deliveryChargeTextView)
        totalTextView = findViewById(R.id.totalTextView)
        zipCodeTextView = findViewById(R.id.zipCodeTextView)
        shareButton = findViewById(R.id.shareButton)

        // Query Firestore for the order details
        val db = FirebaseFirestore.getInstance()
        val orderRef = db.collection("orders").document(orderId!!)
        orderRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Extract order data from Firestore document
                    val orderDate = document.getDate("createdAt")
                    val deliveryType = document.getString("deliveryType")
                    val address = document.getString("address")
                    val notes = document.getString("notes")
                    val pengiriman = document.getDouble("pengiriman")
                    val subtotal = document.getDouble("subtotal")
                    val total = document.getDouble("total")
                    val zipCode = document.getString("zipCode")

                    // Extract product details
                    val products = document.get("products") as? List<Map<String, Any>>
                    val productList = products?.map {
                        "${it["productName"]} - ${it["quantity"]}"
                    }?.joinToString(", ")

                    // Assuming subtotal, deliveryCharge, and total are Double values
                    val formattedSubtotal = getString(R.string.subtotal_label, formatCurrency(subtotal!!))
                    val formattedDeliveryCharge = getString(R.string.delivery_charge_label, formatCurrency(pengiriman!!))
                    val formattedTotal = getString(R.string.total_label, formatCurrency(total!!))

                    // Set the formatted strings to the corresponding TextViews
                    subtotalTextView.text = formattedSubtotal
                    deliveryChargeTextView.text = formattedDeliveryCharge
                    totalTextView.text = formattedTotal

                    // Populate views with order details
                    orderNumberTextView.text = "Order ID: $orderId"
                    orderDateTextView.text = "Order Date: $orderDate"
                    deliveryTypeTextView.text = "Delivery Type: $deliveryType"
                    addressTextView.text = "Address: $address"
                    notesTextView.text = "Notes: $notes"
                    productsTextView.text = "Products: $productList"
                    zipCodeTextView.text = "Zip Code: $zipCode"

                    // Set click listener for share button
                    shareButton.setOnClickListener {
                        shareReceipt()
                    }
                } else {
                    Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
                    finish() // Finish activity if order not found
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to retrieve order details: $e", Toast.LENGTH_SHORT).show()
                finish() // Finish activity if there's a failure in retrieving order details
            }



        // Set order details to views
        orderNumberTextView.text = "Order ID: $orderId"
        // Set other order details to corresponding TextViews

        // Set click listener for share button
        shareButton.setOnClickListener {
            shareReceipt()
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val formattedAmount = formatter.format(amount)
        return formattedAmount.replace("$", "")
    }

    private fun shareReceipt() {
        // Get the bitmap of the layout
        val bitmap = getBitmapFromView(findViewById(R.id.receiptContainer))

        // Save bitmap to a temporary file
        val file = saveBitmap(bitmap)

        // Get the URI using FileProvider
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)

        // Create a new Intent to share the receipt
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/png"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(intent, "Share Receipt"))
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap): File {
        val directory = File(Environment.getExternalStorageDirectory().absolutePath + "/Download")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "receipt.png")
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }
}
