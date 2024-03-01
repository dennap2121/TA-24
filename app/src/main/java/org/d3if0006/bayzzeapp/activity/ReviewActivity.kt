package org.d3if0006.bayzzeapp.activity

import android.app.Activity
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
import org.d3if0006.bayzzeapp.databinding.ActivityReviewBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var reviewList: MutableList<Review>
    private lateinit var reviewRecyclerView: RecyclerView
    private lateinit var isAdmin: String

    companion object {
        private const val ADD_REVIEW_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        isAdmin = intent.getStringExtra("isAdmin") ?: ""

        fetchReviewData()

        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileBackBtn.setOnClickListener {
            finish()
        }

        binding.addReview.setOnClickListener{
            navigateToReviewAddActivity()
        }

        // Initialize RecyclerView
        reviewRecyclerView = findViewById(R.id.reviewRecyclerView)
        reviewRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun navigateToReviewAddActivity() {
        val intent = Intent(this, ReviewAddActivity::class.java)
        startActivityForResult(intent, ADD_REVIEW_REQUEST_CODE)
    }

    private fun fetchReviewData() {
        showLoading()
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("reviews")
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

                    val review = Review(reviewId, reviewName, reviewDate, reviewReview, reviewComments)
                    reviewList.add(review)
                }
                // Call a function to display or process the review list
                displayReviewList(reviewList)
                val adapter = ReviewAdapter(
                    reviewList,
                    this,
                    isAdmin
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
        private val reviewList: List<Review>,
        private val context: Context,
        private val isAdmin: String
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
                    addCommentToFirestore(review.id, commentText, isAdmin)
                    holder.reviewCommentEditText.text.clear()
                } else {
                    Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount(): Int {
            return reviewList.size
        }

        private fun addCommentToFirestore(id: String, commentText: String, isAdmin: String) {
            val db = FirebaseFirestore.getInstance()
            val reviewRef = db.collection("reviews")
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val userRef = db.collection("user_info").document(uid)
                userRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val userProfile = document.toObject(ProfileActivity.UserProfile::class.java)
                            if (userProfile != null) {
                                // Populate the UI with user profile data

                                var name = userProfile.name

                                Log.d("plm", isAdmin)

                                if(isAdmin != "" || isAdmin != null){
                                    Log.d("plm", "isNotAdmin")
                                    name = "admin"
                                }

                                val newComment = hashMapOf(
                                    "name" to name,
                                    "comment" to commentText
                                )

                                // Update Firestore document with new comment
                                reviewRef.document(id).update("comments", FieldValue.arrayUnion(newComment))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Comment added successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Error adding comment", Toast.LENGTH_SHORT).show()
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

    private fun showLoading() {
        progressDialog.setMessage("Tunggu sebentar...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }

    data class Review(val id: String, val name: String, val date: String, val review: String, val comments: List<HashMap<String, String>>?)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("lopop", requestCode.toString())
        Log.d("lopop", ADD_REVIEW_REQUEST_CODE.toString())
        Log.d("lopop", resultCode.toString())
        Log.d("lopop", Activity.RESULT_OK.toString())
        if (requestCode == ADD_REVIEW_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh review list
            fetchReviewData()
        }
    }

}

