package org.d3if0006.bayzzeapp.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
import org.d3if0006.bayzzeapp.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private lateinit var progressDialog: ProgressDialog // Declare ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this) // Initialize ProgressDialog

        val bottomNavigationAdminView: BottomNavigationView = findViewById(R.id.bottomNavigationAdminView)
        bottomNavigationAdminView.selectedItemId = R.id.nav_pengaturan
        bottomNavigationAdminView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_beranda -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                    true
                }
                R.id.nav_pengiriman -> {
                    val intent = Intent(this, DeliveryActivity::class.java)
                    intent.putExtra("tabName", "Permintaan")
                    startActivity(intent)
                    true
                }
                R.id.nav_transaksi -> {
                    val intent = Intent(this, TransactionsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_pengaturan -> {
                    val intent = Intent(this, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }


        binding.produk.setOnClickListener{
            navigateToManageProductActivity()
        }

        binding.pengguna.setOnClickListener{
            navigateToManageUserActivity()
        }

        binding.konten.setOnClickListener {
            navigateToManageContentActivity()
        }

        binding.toko.setOnClickListener {
            navigateToManageTimeActivity()
        }
    }

    private fun clearLoginDetails() {
        val sharedPreferences = getSharedPreferences("loginDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }


    private fun navigateToManageContentActivity() {
        val intent = Intent(this, ManageContentActivity::class.java)
        intent.putExtra("tabName", "Artikel")
        startActivity(intent)
        finish()
    }

    private fun navigateToManageProductActivity() {
        val intent = Intent(this, ManageProductActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToManageUserActivity() {
        val intent = Intent(this, ManageUserActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToManageTimeActivity() {
        val intent = Intent(this, ManageTimeActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }

    private fun navigateToReviewActivity() {
        val intent = Intent(this, ReviewActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }
}