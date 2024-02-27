package org.d3if0006.bayzzeapp.activity

import Order
import OrderProduct
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityManageContentBinding
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ManageContentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageContentBinding
    private lateinit var contentRecyclerView: RecyclerView
    private lateinit var contentList: MutableList<Content>
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog
    private lateinit var emptyStateImageView: ImageView
    private lateinit var selectedTabText: String
    private val CREATE_DOCUMENT_REQUEST = 123


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageContentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        emptyStateImageView = findViewById(R.id.emptyStateImageView)

        selectedTabText = "Artikel"


        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        val tabName = intent.getStringExtra("tabName")

        // Fetch content data from Firestore
        fetchContentData(tabName ?: "Artikel")



        // Add tabs
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val tabs = listOf("Artikel", "Banner")
        tabs.forEach { tab ->
            tabLayout.addTab(tabLayout.newTab().setText(tab))
        }


        // Set the selected tab
        val tabIndex = tabs.indexOf(tabName)
        if (tabIndex != -1) {
            tabLayout.getTabAt(tabIndex)?.select()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTabText = tab.text.toString()
                fetchContentData(selectedTabText)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not used
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not used
            }
        })

        binding.exportPdf.setOnClickListener{
            exportToCSV()
        }

        binding.btnTambah.setOnClickListener {
            // Inflate the dialog layout
            val dialogView = LayoutInflater.from(this).inflate(R.layout.popup_add, null)

            // Create the dialog
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)

            val alertDialog = builder.create()

            // Initialize views
            val editTextGambar = dialogView.findViewById<EditText>(R.id.editTextGambar)
            val editTextLink = dialogView.findViewById<EditText>(R.id.editTextLink)
            val buttonSimpan = dialogView.findViewById<Button>(R.id.buttonSimpan)

            // Handle Simpan button click
            buttonSimpan.setOnClickListener {
                val gambar = editTextGambar.text.toString().trim()
                val link = editTextLink.text.toString().trim()

                // Validate input fields (you can add more validation if needed)
                if (title.isNotEmpty() && gambar.isNotEmpty() && link.isNotEmpty()) {
                    // Add data to Firestore based on the tabName extra
                    if (selectedTabText == "Artikel") {
                        // Add data to articles collection
                        addDataToFirestore("Artikel", gambar, link)
                    } else {
                        // Add data to banner collection
                        addDataToFirestore("Banner", gambar, link)
                    }

                    alertDialog.dismiss() // Dismiss the dialog
                } else {
                    // Show error message if any field is empty
                    Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                }
            }

            // Show the dialog
            alertDialog.show()
        }


        // Initialize RecyclerView
        contentRecyclerView = findViewById(R.id.contentRecyclerView)
        contentRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun exportToCSV() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "content.csv")
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
                it.write("Link, Image\n")
                for (content in contentList) {
                    it.write("${content.link}, ${content.image}\n")
                }
            }
            Toast.makeText(this, "Transactions exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addDataToFirestore(collection: String, gambar: String, link: String) {
        // Add your Firestore logic here to add data to the specified collection
        // For example:
        val db = FirebaseFirestore.getInstance()
        val newData = hashMapOf(
            "image" to gambar,
            "link" to link
        )

        Log.d("pppo", collection)

        var collectionnya = ""

        if(collection == "Artikel"){
            collectionnya = "articles"
        }else{
            collectionnya = "banners"
        }


        db.collection(collectionnya)
            .add(newData)
            .addOnSuccessListener {
                fetchContentData(collection)
                Toast.makeText(this, "Data berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                fetchContentData(collection)
                Toast.makeText(this, "Gagal menambahkan data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchContentData(tabName: String) {
        showLoading()
        val db = FirebaseFirestore.getInstance()
        val query = if (tabName == "Artikel") {
            db.collection("articles")
        } else if(tabName == "Banner"){
            db.collection("banners")
        }else{
            db.collection("articles")
        }
        query.get()
            .addOnSuccessListener { documents ->
                hideLoading()
                contentList = mutableListOf<Content>()
                for (document in documents) {
                    val contentId = document.id
                    val contentImage = document.getString("image") ?: ""
                    val contentLink = document.getString("link") ?: ""

                    val content = Content(contentId, contentImage, contentLink)
                    contentList.add(content)
                }
                // Call a function to display or process the content list
                displayContentList(contentList)
                val adapter = ContentAdapter(contentList, selectedTabText, this) // Pass tabName to adapter
                contentRecyclerView.adapter = adapter

                // Check if the RecyclerView data is empty
                if (contentList.isEmpty()) {
                    emptyStateImageView.visibility = View.VISIBLE // Show the placeholder image
                    contentRecyclerView.visibility = View.GONE // Hide the RecyclerView

                } else {
                    emptyStateImageView.visibility = View.GONE // Hide the placeholder image
                    contentRecyclerView.visibility = View.VISIBLE // Show the RecyclerView
                }

            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    private fun displayContentList(contentList: List<Content>) {
        // Implement your logic to display the content list, such as setting up a RecyclerView adapter
    }

    class ContentAdapter(
        private var contentList: MutableList<Content> = mutableListOf(),
        private val selectedTabText: String,
        private val context: Context
    ) : RecyclerView.Adapter<ContentViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_content, parent, false)
            return ContentViewHolder(view)
        }

        override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
            val content = contentList[position]
            val number = position + 1

            holder.contentNoTextView.text = number.toString()


            Log.d("po", content.image)
            if (!content.image.isNullOrEmpty()) {
                Glide.with(context)
                    .load(content.image)
                    .placeholder(R.drawable.background_oval_1) // Placeholder image
                    .error(R.drawable.background_oval_1) // Error image
                    .into(holder.contentImageImageView)
            }


            val maxLength = 30 // Maximum length of the shortened URL
            val shortenedLink = if (content.link.length > maxLength) {
                content.link.substring(0, maxLength) + "..." // Truncate URL and append ellipses
            } else {
                content.link // Use full URL if it's shorter than maxLength
            }
            holder.contentLinkTextView.text = shortenedLink

            holder.contentEditImageView.setOnClickListener {
                showEditContentDialog(content)
            }

            holder.contentDeleteImageView.setOnClickListener {
                showDeleteConfirmationDialog(content.id)
            }

            // Implement click listeners or any other functionality here
        }

        private fun deleteContent(id: String) {
            val db = FirebaseFirestore.getInstance()
            val documentId = id

            // Determine the collection based on the tabName
            var collectionNya = ""

            if (selectedTabText == "Artikel") {
                collectionNya = "articles"
            } else {
                collectionNya = "banners"
            }

            db.collection(collectionNya)
                .document(documentId)
                .delete()
                .addOnSuccessListener {
                    fetchContentData(selectedTabText)
                    // Document successfully deleted
                    // You can perform any additional actions here, such as updating the UI
                    Log.d(TAG, "DocumentSnapshot successfully deleted!")
                }
                .addOnFailureListener { e ->
                    // Failed to delete the document
                    Log.w(TAG, "Error deleting document", e)
                }
        }


        private fun showDeleteConfirmationDialog(id: String) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.popup_delete)
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

        private fun showEditContentDialog(content: Content) {
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.popup_content)

            // Find views in the custom layout
            val imageEditText: EditText = dialog.findViewById(R.id.imageEditText)
            val linkEditText: EditText = dialog.findViewById(R.id.linkEditText)
            val saveButton: Button = dialog.findViewById(R.id.saveButton)

            // Fill input fields with existing content data
            imageEditText.setText(content.image)
            linkEditText.setText(content.link)

            // Set click listener for save button
            saveButton.setOnClickListener {
                // Get updated data from input fields
                val updatedImage = imageEditText.text.toString()
                val updatedLink = linkEditText.text.toString()

                // Update Firestore document with the new data
                val db = FirebaseFirestore.getInstance()
                val collectionName = if (selectedTabText == "Artikel") "articles" else "banners"
                db.collection(collectionName).document(content.id)
                    .update(
                        mapOf(
                            "image" to updatedImage,
                            "link" to updatedLink
                        )
                    )
                    .addOnSuccessListener {
                        // Dismiss the dialog upon successful update
                        dialog.dismiss()
                        // Show a toast or perform any other action to notify the user
                        Toast.makeText(context, "Artikel berhasil diperbarui", Toast.LENGTH_SHORT).show()

                        // Refresh the list of data
                        fetchContentData(selectedTabText) // Assuming you have a function to fetch data from Firestore

                        // Alternatively, you can refresh the RecyclerView adapter directly if you have access to it
                        // adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        // Handle failure to update Firestore document
                        Log.e(TAG, "Error updating document", e)
                        // Show a toast or perform any other action to notify the user
                        Toast.makeText(context, "Gagal memperbarui artikel", Toast.LENGTH_SHORT).show()
                    }
            }

            // Show the dialog
            dialog.show()
        }

        override fun getItemCount(): Int {
            return contentList.size
        }

        private fun fetchContentData(tabName: String) {
            val db = FirebaseFirestore.getInstance()
            val collectionName = if (tabName == "Artikel") "articles" else "banners"
            db.collection(collectionName)
                .get()
                .addOnSuccessListener { documents ->
                    val updatedContentList = mutableListOf<Content>() // Create a new list to hold the updated content
                    for (document in documents) {
                        val contentId = document.id
                        val image = document.getString("image") ?: ""
                        val link = document.getString("link") ?: ""
                        val content = Content(contentId, image, link)
                        updatedContentList.add(content)
                    }

                    // Assign the updated contentList with the new data
                    contentList = updatedContentList

                    // Notify the adapter that the dataset has changed
                    notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching content data", exception)
                    // Handle failure to fetch content data
                }
        }

    }

    class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentImageImageView: ImageView = itemView.findViewById(R.id.contentImageImageView)
        val contentLinkTextView: TextView = itemView.findViewById(R.id.contentLinkTextView)
        val contentNoTextView: TextView = itemView.findViewById(R.id.contentNoTextView)
        val contentDeleteImageView: ImageView = itemView.findViewById(R.id.contentDeleteImageView)
        val contentEditImageView: ImageView = itemView.findViewById(R.id.contentEditImageView)

    }

    private fun showLoading() {
        progressDialog.setMessage("Tunggu sebentar...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }

    data class Content(
        val id: String = "",
        val image: String = "",
        val link: String = "",
    )
}



