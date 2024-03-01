package org.d3if0006.bayzzeapp.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.d3if0006.bayzzeapp.R
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

    // Function to open Instagram profile
    fun openInstagram(view: View) {
        val instagramUsername = resources.getString(R.string.instagram_username)
        val instagramUrl = "https://www.instagram.com/$instagramUsername"
        openLinkInBrowser(instagramUrl)
    }

    // Function to open Facebook profile
    fun openFacebook(view: View) {
        val facebookUsername = resources.getString(R.string.facebook_username)
        val facebookUrl = "https://www.facebook.com/$facebookUsername"
        openLinkInBrowser(facebookUrl)
    }

    // Function to open WhatsApp chat
    fun openWhatsApp(view: View) {
        val phoneNumber = resources.getString(R.string.whatsapp_number)
        val whatsappUrl = "https://wa.me/$phoneNumber"
        openLinkInBrowser(whatsappUrl)
    }

    // Function to open Google Maps with the store location
    fun openMap(view: View) {
        val storeAddress = resources.getString(R.string.store_address)
        val mapUri = Uri.parse("geo:0,0?q=$storeAddress")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }

    // Function to open a link in the browser
    private fun openLinkInBrowser(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }
}