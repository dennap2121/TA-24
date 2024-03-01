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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityTimeBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class TimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimeBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var timeList: MutableList<Time>
    private lateinit var timeRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        fetchHistoryData()


        binding = ActivityTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileBackBtn.setOnClickListener{
            navigateToMainActivity()
        }

        binding.footerTextView.text = "Terakhir diperbaharui 06/12/2022"

        // Initialize RecyclerView
        timeRecyclerView = findViewById(R.id.timeRecyclerView)
        timeRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }

    private fun fetchHistoryData() {
        showLoading()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val query = db.collection("times")
                .orderBy("index", Query.Direction.ASCENDING) // Order by "index" field in ascending order

            query.get()
                .addOnSuccessListener { documents ->
                    hideLoading()
                    timeList = mutableListOf<Time>()
                    for (document in documents) {
                        val timeDay = document.getString("day") ?: "-"
                        val timeOpen = document.getString("open") ?: "00.00"
                        val timeClose = document.getString("close") ?: "00.00"
                        val index = document.getDouble("index") ?: 0.0

                        val time = Time(timeDay, timeOpen, timeClose, index)
                        timeList.add(time)
                    }
                    // Call a function to display or process the order list
                    displayTimeList(timeList)
                    val adapter = TimeAdapter(
                        timeList,
                        this
                    ) // Pass tabName to adapter
                    timeRecyclerView.adapter = adapter
                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents: ", exception)
                }
        }
    }


    private fun displayTimeList(timeList: List<Time>) {
        // Implement your logic to display the time list, such as setting up a RecyclerView adapter
    }

    class TimeAdapter(
        private val timeList: List<Time>,
        private val context: Context
    ) : RecyclerView.Adapter<OrderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_card, parent, false)
            return OrderViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
            val time = timeList[position]

            holder.timeDayTextView.text = time.day
            holder.timeOpenTextView.text = time.open
            holder.timeCloseTextView.text = time.close
        }

        override fun getItemCount(): Int {
            return timeList.size
        }
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeDayTextView: TextView = itemView.findViewById(R.id.timeDayTextView)
        val timeOpenTextView: TextView = itemView.findViewById(R.id.timeOpenTextView)
        val timeCloseTextView: TextView = itemView.findViewById(R.id.timeCloseTextView)
    }

    private fun showLoading() {
        progressDialog.setMessage("Tunggu sebentar...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }

    data class Time(val day: String, val open: String, val close: String, val index: Double)

}