package org.d3if0006.bayzzeapp.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityManageTimeBinding
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale


class ManageTimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTimeBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var timeList: MutableList<Time>
    private val CREATE_DOCUMENT_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        fetchTimeData()

        binding = ActivityManageTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.profileBackBtn.setOnClickListener {
            navigateToMainActivity()
        }

        binding.saveButton.setOnClickListener {
            saveChangesToFirestore()
        }

        binding.exportPdf.setOnClickListener{
            exportToCSV()
        }
    }

    private fun exportToCSV() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "time.csv")
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
                it.write("Day, Open, Close\n")
                for (time in timeList) {
                    it.write("${time.day}, ${time.open}, ${time.close}\n")
                }
            }
            Toast.makeText(this, "Transactions exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, SettingActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }

    private fun saveChangesToFirestore() {
        val db = FirebaseFirestore.getInstance()

        // Iterate over timeList and update Firestore documents
        timeList.forEach { time ->
            val documentRef = db.collection("times").document(time.id)

            // Check if document exists
            documentRef.get().addOnSuccessListener { documentSnapshot ->
                val batch = db.batch() // Create a new batch for each document operation

                if (documentSnapshot.exists()) {
                    // Document exists, update it
                    val data: MutableMap<String, Any> = mutableMapOf(
                        "open" to time.open,
                        "close" to time.close
                    )
                    batch.update(documentRef, data)
                } else {
                    // Document does not exist, create it
                    val data: MutableMap<String, Any> = mutableMapOf(
                        "open" to time.open,
                        "close" to time.close
                    )
                    batch.set(documentRef, data)
                }

                // Commit the batch after all operations are added
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating documents", e)
                        Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun fetchTimeData() {
        showLoading()
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("times")
        query.get()
            .addOnSuccessListener { documents ->
                hideLoading()
                timeList = mutableListOf<Time>()
                for (document in documents) {
                    val timeId = document.id
                    val timeDay = document.getString("day") ?: "-"
                    val timeOpen = document.getString("open") ?: "00.00"
                    val timeClose = document.getString("close") ?: "00.00"

                    val time = Time(timeId, timeDay, timeOpen, timeClose)
                    timeList.add(time)
                }
                // Set data to views after fetching
                setDataToViews()
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    private fun setDataToViews() {
        timeList.forEach { time ->
            when (time.day) {
                "Senin" -> {
                    binding.seninOpen.setText(time.open)
                    binding.seninOpen.addTextChangedListener(createTextWatcher(time, "open"))

                    binding.seninClose.setText(time.close)
                    binding.seninClose.addTextChangedListener(createTextWatcher(time, "close"))
                }
                "Selasa" -> {
                    binding.selasaOpen.setText(time.open)
                    binding.selasaOpen.addTextChangedListener(createTextWatcher(time, "open"))

                    binding.selasaClose.setText(time.close)
                    binding.selasaClose.addTextChangedListener(createTextWatcher(time, "close"))
                }
                "Rabu" -> {
                    binding.rabuOpen.setText(time.open)
                    binding.rabuOpen.addTextChangedListener(createTextWatcher(time, "open"))

                    binding.rabuClose.setText(time.close)
                    binding.rabuClose.addTextChangedListener(createTextWatcher(time, "close"))
                }
                "Kamis" -> {
                    binding.kamisOpen.setText(time.open)
                    binding.kamisOpen.addTextChangedListener(createTextWatcher(time, "open"))

                    binding.kamisClose.setText(time.close)
                    binding.kamisClose.addTextChangedListener(createTextWatcher(time, "close"))
                }
                "Jumat" -> {
                    binding.jumatOpen.setText(time.open)
                    binding.jumatOpen.addTextChangedListener(createTextWatcher(time, "open"))

                    binding.jumatClose.setText(time.close)
                    binding.jumatClose.addTextChangedListener(createTextWatcher(time, "close"))
                }
                "Sabtu" -> {
                    binding.sabtuOpen.setText(time.open)
                    binding.sabtuOpen.addTextChangedListener(createTextWatcher(time, "open"))

                    binding.sabtuClose.setText(time.close)
                    binding.sabtuClose.addTextChangedListener(createTextWatcher(time, "close"))
                }
                "Minggu" -> {
                    binding.mingguOpen.setText(time.open)
                    binding.mingguOpen.addTextChangedListener(createTextWatcher(time, "open"))

                    binding.mingguClose.setText(time.close)
                    binding.mingguClose.addTextChangedListener(createTextWatcher(time, "close"))
                }
                // You can add cases for other days as needed
                // For days not mentioned, you can ignore or handle accordingly
            }
        }
    }

    private fun createTextWatcher(time: Time, field: String): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                when (field) {
                    "open" -> time.open = s.toString() // Update the open time when text changes
                    "close" -> time.close = s.toString() // Update the close time when text changes
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
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

    companion object {
        private const val TAG = "ManageTimeActivity"
    }

    data class Time(var id: String, val day: String, var open: String, var close: String)

}
