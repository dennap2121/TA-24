package org.d3if0006.bayzzeapp.activity

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.textView.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        // Add a TextWatcher to emailEt for email validation on change
        binding.emailEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for this implementation
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Check email validity and update input text color accordingly
                if (isValidEmail(s.toString())) {
                    binding.emailEt.setTextColor(Color.BLACK) // Change text color to black if email is valid
                    binding.emailEt.error = null // Clear any error
                } else {
                    binding.emailEt.setTextColor(Color.RED) // Change text color to red if email is invalid
                    binding.emailEt.error = "Email tidak valid" // Set error message
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed for this implementation
            }
        })

        binding.button.setOnClickListener {
            showLoading()
            val email = binding.emailEt.text.toString()
            val pass = binding.passET.text.toString()
            val confirmPass = binding.confirmPassEt.text.toString()

            if (isValidEmail(email) && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {
                    firebaseAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val intent = Intent(this, SignInActivity::class.java)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "Email already registered!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Handle other failure cases
                            Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Kata sandi tidak sesuai", Toast.LENGTH_SHORT).show()
                }
                hideLoading()
            } else {
                Toast.makeText(this, "Email tidak valid atau form kosong tidak diizinkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun showLoading() {
        Log.d("jjj", "loadingggg")
        progressDialog.setMessage("Signup in...")
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun hideLoading() {
        progressDialog.dismiss()
    }
}
