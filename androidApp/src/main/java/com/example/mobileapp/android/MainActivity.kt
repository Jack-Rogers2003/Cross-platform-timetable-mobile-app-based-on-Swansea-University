package com.example.mobileapp.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the MainActivity from the shared module directly
        val intent = Intent(this, com.example.mobileapp.LoginOrSignUpActivity::class.java)
        startActivity(intent)
        finish()  // Finish the current MainActivity if needed
    }
}
