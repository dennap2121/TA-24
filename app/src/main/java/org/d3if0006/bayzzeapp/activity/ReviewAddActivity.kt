package org.d3if0006.bayzzeapp.activity

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityReviewAddBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReviewAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewAddBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var commentEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        binding = ActivityReviewAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        commentEditText = findViewById(R.id.commentEditText)

        binding.profileBackBtn.setOnClickListener {
            navigateToReviewActivity()
        }

        binding.cancelButton.setOnClickListener {
            navigateToReviewActivity()
        }

        binding.sendButton.setOnClickListener {
            if (commentEditText.text.isNotEmpty()) {
                submitOrder()
            } else {
                Toast.makeText(this, "Review cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun submitOrder() {
        showLoading() // Show loading

        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid
        val currentDate = Calendar.getInstance().time

        if (uid != null) {
            val userRef = db.collection("user_info").document(uid)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userProfile = document.toObject(ProfileActivity.UserProfile::class.java)
                        Log.d("ggez", currentDate.toString())

                        if (userProfile != null) {

                            val name = userProfile.name
                            val review = commentEditText.text.toString()
                            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDate)
                            val data = mapOf(
                                "name" to name,
                                "review" to review,
                                "date" to date,
                            )

                            db.collection("reviews")
                                .add(data)
                                .addOnSuccessListener { documentReference ->

                                    hideLoading() // Hide loading
                                    navigateToReviewActivity()
                                    Toast.makeText(this, "Success to add review", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Log.d("ggez", "888")

                                    Toast.makeText(this, "Failed to add review", Toast.LENGTH_SHORT).show()
                                }

                        }
                    } else {
                        Log.d("ggwp", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("ggwp", "get failed with ", exception)
                }
        }


    }


    private fun navigateToReviewActivity() {
        val intent = Intent(this, ReviewActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
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

