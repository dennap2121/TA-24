package org.d3if0006.bayzzeapp.activity

import Product
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityOrderBinding
import java.text.DecimalFormat
import java.util.Calendar

class PaymentActivity : AppCompatActivity() {

    // Views
    private lateinit var textSubtotal: TextView
    private lateinit var textDelivery: TextView
    private lateinit var textTotal: TextView

    private lateinit var textBankAccountMandiri: TextView
    private lateinit var textBankAccountBCA: TextView
    private lateinit var textBankAccountBRI: TextView

    private lateinit var backButton: ImageButton

    private lateinit var selectedImageUri: Uri
    private val storageReference = FirebaseStorage.getInstance().reference

    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        // Initialize views
        textSubtotal = findViewById(R.id.textSubtotalValue)
        textDelivery = findViewById(R.id.textDeliveryValue)
        textTotal = findViewById(R.id.textTotalValue)
        backButton = findViewById(R.id.backButton)

        textBankAccountMandiri = findViewById(R.id.textBankAccountMandiri)
        textBankAccountBCA = findViewById(R.id.textBankAccountBCA)
        textBankAccountBRI = findViewById(R.id.textBankAccountBRI)

        // Get order details from intent extras
        val subtotal = intent.getDoubleExtra("subtotal", 0.0)
        val deliveryCost = intent.getDoubleExtra("deliveryCost", 0.0)
        val total = intent.getDoubleExtra("total", 0.0)

        // Set order details in the UI
        textSubtotal.text = "Rp ${DecimalFormat("#,###.##").format(subtotal)}"
        textDelivery.text = "Rp ${DecimalFormat("#,###.##").format(deliveryCost)}"
        textTotal.text = "Rp ${DecimalFormat("#,###.##").format(total)}"

        // Set bank account details
        textBankAccountMandiri.text = "123-456-789 (Mandiri)"
        textBankAccountBCA.text = "987-654-321 (BCA)"
        textBankAccountBRI.text = "567-890-123 (BRI)"

        backButton.setOnClickListener{
            finish()
        }
    }

    // Copy Total value
    fun copyTotal(view: View) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Total", textTotal.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Total copied", Toast.LENGTH_SHORT).show()
    }

    // Copy Bank Account value
    fun copyBankAccount(view: View) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = when (view.id) {
            R.id.iconCopyMandiri -> ClipData.newPlainText("Mandiri Account", textBankAccountMandiri.text)
            R.id.iconCopyBCA -> ClipData.newPlainText("BCA Account", textBankAccountBCA.text)
            R.id.iconCopyBRI -> ClipData.newPlainText("BRI Account", textBankAccountBRI.text)
            else -> ClipData.newPlainText("Bank Account", "")
        }
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Bank Account copied", Toast.LENGTH_SHORT).show()
    }

    // Function to handle image upload
    fun uploadImage(view: View) {
        // Create an intent to select an image from the device
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
    }

    // Function to handle result of image selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                uploadImageToStorage(uri)
            }
        }
    }

    // Function to upload image to Firebase Storage
    private fun uploadImageToStorage(imageUri: Uri) {
        showLoading()

        val imageName = "bukti_pembayaran_${System.currentTimeMillis()}"
        val imageRef = storageReference.child("bukti_pembayaran/$imageName")

        val uploadTask = imageRef.putFile(imageUri)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Image uploaded successfully
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { downloadUri ->
                // Image download URL obtained
                val imageUrl = downloadUri.toString()
                // Store the image URL in Firestore
                saveImageUrlToFirestore(imageUrl)
            }
        }.addOnFailureListener { exception ->
            // Handle error
            Toast.makeText(this, "Image upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to save image URL in Firestore
    private fun saveImageUrlToFirestore(imageUrl: String) {
        // Get order ID from intent extras
        val orderId = intent.getStringExtra("orderId")

        // Update Firestore document with the image URL
        val db = FirebaseFirestore.getInstance()
        db.collection("orders")
            .document(orderId ?: "")
            .update(
                mapOf(
                    "buktiPembayaran" to imageUrl,
                    "status" to "Selesai Pembayaran"
                )
            )
            .addOnSuccessListener {
                hideLoading()
                Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                // Handle error
                Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val IMAGE_PICK_REQUEST_CODE = 100
    }

    private fun showLoading() {
        progressDialog.setMessage("Upload bukti pembayaran...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }
}
