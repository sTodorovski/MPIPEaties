package com.example.eaties

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ReviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)
        supportActionBar?.hide()
        init()
    }

    private fun init() {
        val openCameraButton = findViewById<Button>(R.id.openCameraButton)
        openCameraButton.setOnClickListener{ startActivity(Intent(this, CameraActivity::class.java)) }
    }
}