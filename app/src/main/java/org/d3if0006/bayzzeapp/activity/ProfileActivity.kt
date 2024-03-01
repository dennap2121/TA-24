package org.d3if0006.bayzzeapp.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        // Call function to load user profile
        loadUserProfile()

        binding.profileBackBtn.setOnClickListener{
//            navigateToMainActivity()
            finish()
        }

        binding.tentang.setOnClickListener{
            navigateToAboutActivity()
        }

        binding.jamBuka.setOnClickListener{
            navigateToTimeActivity()
        }

        binding.keluar.setOnClickListener {
            showConfirmationDialog()
        }

        binding.ulasan.setOnClickListener {
            navigateToReviewActivity()
        }

        binding.editProfile.setOnClickListener{
            navigateToEditProfileActivity()
        }

        binding.bagikan.setOnClickListener {
            val appDescription = "Your app description goes here"
            val appDownloadLink = "Your app download link goes here"

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing App")
            shareIntent.putExtra(Intent.EXTRA_TEXT, "$appDescription\n\nDownload the app from: $appDownloadLink")

            startActivity(Intent.createChooser(shareIntent, "Bagikan dengan"))
        }
    }

    private fun clearLoginDetails() {
        val sharedPreferences = getSharedPreferences("loginDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun loadUserProfile() {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            showLoading()
            val userRef = db.collection("user_info").document(uid)
            userRef.get()
                .addOnSuccessListener { document ->
                    hideLoading()
                    if (document.exists()) {
                        val userProfile = document.toObject(UserProfile::class.java)
                        if (userProfile != null) {
                            // Populate the UI with user profile data
                            binding.name.text = userProfile.name
                            binding.email.text = userProfile.email
                            binding.phone.text = userProfile.phone
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

    private fun navigateToTimeActivity() {
        val intent = Intent(this, TimeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToAboutActivity() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }

    private fun navigateToReviewActivity() {
        val intent = Intent(this, ReviewActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }

    data class UserProfile(
        val name: String = "",
        val email: String = "",
        val phone: String = ""
    )

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi")
        builder.setMessage("Apakah Anda yakin akan keluar aplikasi?")
        builder.setPositiveButton("Ya") { _, _ ->
            // If "Ya" is clicked, clear login details and navigate to SignInActivity
            clearLoginDetails()
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        builder.setNegativeButton("Tidak") { dialog, _ ->
            // If "Tidak" is clicked, close the dialog
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showLoading() {
        progressDialog.setMessage("Tunggu sebentar...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }

    private fun navigateToEditProfileActivity() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Profile")

        // Inflate the custom layout for the dialog
        val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        builder.setView(view)

        // Get references to the EditText fields and the update button in the dialog layout
        val nameEditText = view.findViewById<EditText>(R.id.editTextName)
        val phoneEditText = view.findViewById<EditText>(R.id.editTextPhone)
        val updateButton = view.findViewById<Button>(R.id.buttonUpdate)

        // Populate EditText fields with current user profile data
        nameEditText.setText(binding.name.text)
        phoneEditText.setText(binding.phone.text)

        // Create the dialog
        val dialog = builder.create()

        // Set click listener for the update button
        updateButton.setOnClickListener {
            // Get updated name and phone number from EditText fields
            val updatedName = nameEditText.text.toString()
            val updatedPhone = phoneEditText.text.toString()

            // Perform validation if needed

            // Update user information in Firestore
            updateUserProfile(updatedName, updatedPhone)

            // Dismiss the dialog
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    private fun updateUserProfile(name: String, phone: String) {
        val db = FirebaseFirestore.getInstance()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userRef = db.collection("user_info").document(uid)
            userRef.update(mapOf(
                "name" to name,
                "phone" to phone
            ))
                .addOnSuccessListener {
                    // Update UI with new data if needed
                    binding.name.text = name
                    binding.phone.text = phone

                    // Show a toast or message indicating successful update
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    // Show a toast or message indicating failure
                    Toast.makeText(this, "Failed to update profile: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

}