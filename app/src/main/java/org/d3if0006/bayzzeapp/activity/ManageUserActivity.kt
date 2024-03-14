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
import androidx.appcompat.widget.SearchView
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
import java.util.Date

class ManageUserActivity : AppCompatActivity() {

    private lateinit var userAdapter: UserAdapter
    private var userList = mutableListOf<User>()
    private val CREATE_DOCUMENT_REQUEST = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_user)


        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTransactions)
        val export = findViewById<ImageView>(R.id.menu_export)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val search = findViewById<SearchView>(R.id.searchView)

        export.setOnClickListener{
            exportToCSV()
        }

        backButton.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        search.setQuery("", false)

        // Initialize RecyclerView and adapter
        userAdapter = UserAdapter(userList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ManageUserActivity)
            adapter = userAdapter
        }

        setupSearchView()

        // Fetch user data from Firestore
        fetchUsersFromFirestore("")

    }

    private fun setupSearchView() {
        val search = findViewById<SearchView>(R.id.searchView)

        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Called when the user submits the query by pressing the search button
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Called when the text in the search view changes
                // Fetch products based on the new search query
                fetchUsersFromFirestore(newText.orEmpty())
                return true
            }
        })
    }

    private fun fetchUsersFromFirestore(search: String) {
        val db = FirebaseFirestore.getInstance()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTransactions)

        db.collection("user_info")
            .get()
            .addOnSuccessListener { result ->
                userList = mutableListOf()
                for (document in result) {
                    val email = document.getString("email") ?: ""
                    val name = document.getString("name") ?: ""
                    val phone = document.getString("phone") ?: ""
                    if (name?.contains(search, ignoreCase = true) == true) {
                        Log.d("ggez", search)
                        userList.add(User(email, name, phone))
                    }

                }
                userAdapter = UserAdapter(userList)
                Log.d("ggez", userList.toString())
                recyclerView.apply {
                    layoutManager = LinearLayoutManager(this@ManageUserActivity)
                    adapter = userAdapter
                }
                userAdapter.notifyDataSetChanged()

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportToCSV() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/csv"
        intent.putExtra(Intent.EXTRA_TITLE, "users.csv")
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
                it.write("Email, Name, Phone\n")
                for (user in userList) {
                    it.write("${user.email}, ${user.name}, ${user.phone}\n")
                }
            }
            Toast.makeText(this, "Users exported to CSV", Toast.LENGTH_SHORT).show()
        }
    }

    // Inner class for the UserAdapter
    private inner class UserAdapter(private val userList: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

        // ViewHolder class for holding item views
        inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userEmailTextView: TextView = itemView.findViewById(R.id.userEmailTextView)
            val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
            val userPhoneTextView: TextView = itemView.findViewById(R.id.userPhoneTextView)
            val userNoTextView: TextView = itemView.findViewById(R.id.userNoTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return UserViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val currentUser = userList[position]
            val number = position + 1 // Calculate the user number

            holder.userEmailTextView.text = currentUser.email
            holder.userNameTextView.text = currentUser.name
            holder.userPhoneTextView.text = currentUser.phone
            holder.userNoTextView.text = number.toString()

            // Bind other user details to corresponding TextViews here
        }

        override fun getItemCount(): Int {
            return userList.size
        }
    }

    data class User(
        val name: String = "",
        val email: String = "",
        val phone: String = ""
    )
}
