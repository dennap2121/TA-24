package org.d3if0006.bayzzeapp.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.profileBackBtn.setOnClickListener{
            navigateToMainActivity()
        }

        binding.description.text = "Bayzze Apps adalah aplikasi mobile pembelian makanan secara online yang bertujuan untuk menyederhanakan akses pembelian product berupa makanan dan minuman dengan memanfaatkan aplikasi online agar bisa membeli makanan langsung lewat smartphone.\n" +
                "\n" +
                "Berikut merupakan Fitur-fitur untuk mengakses mobile Byze Apps dengan menggunakan smartphone yang terhubung ke internet.\n" +
                "\n" +
                "1. Berikut Fitur pada Bayzze Apps :\n" +
                "2. Home, memilih kategori makanan.\n" +
                "3. Catalog Product.\n" +
                "4. Detail Product.\n" +
                "5. Keranjang makanan."

        binding.footerTextView.text = "Terakhir diperbaharui 06/12/2022"
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        finish() // Finish SignInActivity to prevent returning to it when pressing back button from MainActivity
    }
}