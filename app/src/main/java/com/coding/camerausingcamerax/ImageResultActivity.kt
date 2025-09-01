package com.coding.camerausingcamerax

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.coding.camerausingcamerax.databinding.ActivityImageResultBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageResultBinding
    private var imagePath: String? = null
    private var convertedImage: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the image path from the intent
        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        
        // Start processing the image
        processImage()
        
        // Set up button click listeners
        binding.retakeButton.setOnClickListener {
            finish()
        }
        
        binding.saveButton.setOnClickListener {
            saveImageToGallery()
        }
    }

    private fun processImage() {
        showLoading(true)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load the image
                val file = File(imagePath)
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                
                // Simulate image processing (replace with your actual image processing)
                Thread.sleep(1000) // Simulate processing time
                
                // Convert to thermal effect
                convertedImage = processImageWithThermal(bitmap)
                
                // Update UI on the main thread
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    displayImage(convertedImage!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@ImageResultActivity, "Error processing image", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    private fun processImageWithThermal(bitmap: Bitmap): Bitmap {
        // Create a mutable copy of the bitmap
        val thermalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Get the width and height of the image
        val width = thermalBitmap.width
        val height = thermalBitmap.height
        
        // Create an array to store the pixel data
        val pixels = IntArray(width * height)
        thermalBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Process each pixel to create thermal effect
        for (i in pixels.indices) {
            val pixel = pixels[i]
            
            // Extract RGB components
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            
            // Convert to grayscale (luminosity method)
            val gray = (0.21 * r + 0.72 * g + 0.07 * b).toInt()
            
            // Map grayscale to thermal colors (blue -> cyan -> green -> yellow -> red)
            val thermalColor = when {
                // Black (coldest) to Blue
                gray < 32 -> android.graphics.Color.rgb(0, 0, 51 + gray * 6)
                // Blue to Cyan
                gray < 96 -> android.graphics.Color.rgb(0, (gray - 32) * 4, 255)
                // Cyan to Green
                gray < 160 -> android.graphics.Color.rgb(0, 255, 255 - (gray - 96) * 4)
                // Green to Yellow
                gray < 224 -> android.graphics.Color.rgb((gray - 160) * 4, 255, 0)
                // Yellow to Red (hottest)
                else -> android.graphics.Color.rgb(255, 255 - (gray - 224) * 8, 0)
            }
            
            // Preserve alpha channel
            pixels[i] = thermalColor or (pixel and 0xFF000000.toInt())
        }
        
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }
    
    private fun displayImage(bitmap: Bitmap) {
        binding.resultImageView.setImageBitmap(bitmap)
        binding.resultImageView.visibility = View.VISIBLE
    }
    
    private fun showLoading(show: Boolean) {
        binding.loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonContainer.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }
    
    private fun saveImageToGallery() {
        val bitmap = convertedImage ?: return
        
        binding.saveButton.isEnabled = false
        binding.saveButton.text = getString(R.string.saving)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val savedImageURL = MediaStore.Images.Media.insertImage(
                    contentResolver,
                    bitmap,
                    "Thermal_${System.currentTimeMillis()}",
                    "Thermal image captured with ThermalX"
                )
                
                withContext(Dispatchers.Main) {
                    if (savedImageURL != null) {
                        // Notify the gallery to refresh
                        sendBroadcast(
                            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(savedImageURL))
                        )
                        Toast.makeText(
                            this@ImageResultActivity,
                            R.string.image_saved,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ImageResultActivity,
                            R.string.error_saving,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.saveButton.isEnabled = true
                    binding.saveButton.text = getString(R.string.save)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ImageResultActivity,
                        R.string.error_saving,
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.saveButton.isEnabled = true
                    binding.saveButton.text = getString(R.string.save)
                }
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_PATH = "extra_image_path"
    }
}
