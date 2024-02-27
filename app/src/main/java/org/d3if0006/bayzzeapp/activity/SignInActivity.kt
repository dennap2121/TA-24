package org.d3if0006.bayzzeapp.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        // Check if user is already logged in
        if (isUserLoggedIn()) {

            val sharedPreferences = getSharedPreferences("loginDetails", Context.MODE_PRIVATE)
            val email = sharedPreferences.getString("email", "")
            if(email == "admin"){
                navigateToAdminActivity()
            }else{
                navigateToMainActivity()
            }
            return
        }

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        binding.button.setOnClickListener {
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()

            if(email == "admin" && pass == "admin"){
                navigateToAdminActivity()
                saveLoginDetails(email, pass) // Save login details
            }else{
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    showLoading() // Show loading before sign-in
                    firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                        hideLoading() // Hide loading after sign-in
                        if (it.isSuccessful) {
                            saveLoginDetails(email, pass) // Save login details
                            navigateToMainActivity()
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Form Kosong Tidak Diizinkan !!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences = getSharedPreferences("loginDetails", Context.MODE_PRIVATE)
        return sharedPreferences.contains("email") && sharedPreferences.contains("password")
    }

    private fun saveLoginDetails(email: String, password: String) {
        val sharedPreferences = getSharedPreferences("loginDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.apply()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }

    private fun navigateToAdminActivity() {
        val intent = Intent(this, AdminActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }

    private fun checkIfUserExistsInFirestore(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userInfoRef = db.collection("user_info").document(uid)

        userInfoRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // User exists in Firestore
                    navigateToMainActivity()
                } else {
                    // User does not exist in Firestore
                    Toast.makeText(this, "User does not exist", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                // Error occurred while accessing Firestore
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading() {
        progressDialog.setMessage("Signing in...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }
}