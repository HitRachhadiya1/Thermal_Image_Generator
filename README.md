# Thermal Image Generator with CameraX

A powerful Android application that captures images and applies a thermal imaging effect, transforming them into heat map visualizations. Built with CameraX for reliable camera functionality and modern Android development practices.

## Features

- **Thermal Imaging**: Converts standard images to thermal representations with realistic color mapping
- **Camera Integration**: Built with CameraX for reliable camera functionality
- **Real-time Processing**: Applies thermal effect with smooth color gradients
- **Image Saving**: Save processed images to device gallery
- **Modern UI**: Clean and intuitive user interface
- **Responsive Design**: Works across different screen sizes and orientations

## Color Mapping

The thermal effect uses the following color gradient to represent temperature variations:
-  **Blue (0-31)**: Coldest temperatures
-  **Cyan (32-95)**: Cool temperatures
-  **Green (96-159)**: Moderate temperatures
-  **Yellow (160-223)**: Warm temperatures
-  **Red (224-255)**: Hottest temperatures

## Getting Started

### Prerequisites
- Android Studio (latest version recommended)
- Android SDK 21 or higher
- Gradle 7.0.0 or higher
- Kotlin 1.5.0 or higher

### Installation
1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/Thermal_Image_Generator.git
   ```
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build and run the app on an Android device or emulator

## How to Use

1. Launch the app
2. Grant camera and storage permissions when prompted
3. Point the camera at the subject
4. Tap the capture button to take a photo
5. Wait for the thermal effect to be applied
6. Use the buttons to:
   - Save the image to your gallery
   - Retake the photo

##  Technical Details

- **Minimum SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: Latest Android version
- **Architecture**: MVVM (Model-View-ViewModel)
- **Libraries**:
  - CameraX for camera functionality
  - Coroutines for asynchronous operations
  - ViewBinding for view interaction
  - AndroidX libraries

## Permissions

The app requires the following permissions:
- Camera: To capture photos
- Storage: To save processed images
