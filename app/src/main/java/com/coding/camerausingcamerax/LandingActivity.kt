package com.coding.camerausingcamerax

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.coding.camerausingcamerax.databinding.ActivityLandingBinding

class LandingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLandingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make the activity fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        // Initialize view binding
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set click listener for the start button
        binding.startButton.setOnClickListener {
            // Navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            // Add a nice transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
    
    override fun onBackPressed() {
        // Exit the app when back is pressed from landing page
        finishAffinity()
    }
}
