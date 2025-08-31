package com.coding.camerausingcamerax

import android.graphics.Bitmap
import android.graphics.Color

object ThermalGenerator {

    fun generateThermalImage(inputBitmap: Bitmap): Bitmap {
        val width = inputBitmap.width
        val height = inputBitmap.height

        val thermalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = inputBitmap.getPixel(x, y)

                // Convert to grayscale intensity
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                val brightness = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()

                // Apply thermal color mapping
                val thermalColor = getThermalColor(brightness)
                thermalBitmap.setPixel(x, y, thermalColor)
            }
        }

        return thermalBitmap
    }

    private fun getThermalColor(intensity: Int): Int {
        return when {
            intensity < 50 -> Color.rgb(0, 0, intensity * 2) // Blue (cold)
            intensity < 100 -> Color.rgb(0, (intensity - 50) * 3, 100) // Cyan
            intensity < 150 -> Color.rgb((intensity - 100) * 5, 150, 50) // Green
            intensity < 200 -> Color.rgb(255, 255 - (intensity - 150), 0) // Yellow
            else -> Color.rgb(255, maxOf(0, 205 - (intensity - 200)), 0) // Red (hot)
        }
    }
}
