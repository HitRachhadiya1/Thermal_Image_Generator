package com.coding.camerausingcamerax


import android.content.ContentValues
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.resolutionselector.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.coding.camerausingcamerax.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }


    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_MEDIA_IMAGES
        )
    } else if (Build.VERSION.SDK_INT >= 29) {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }


    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null

    private var isPhoto = true

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private var orientationEventListener: OrientationEventListener? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var aspectRatio = AspectRatio.RATIO_16_9

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)

        if (checkMultiplePermission()) {
            startCamera()
        }

        mainBinding.flipCameraIB.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUserCases()
        }
        mainBinding.aspectRatioTxt.setOnClickListener {
            if (aspectRatio == AspectRatio.RATIO_16_9) {
                aspectRatio = AspectRatio.RATIO_4_3
                setAspectRatio("H,4:3")
                mainBinding.aspectRatioTxt.text = "4:3"
            } else {
                aspectRatio = AspectRatio.RATIO_16_9
                setAspectRatio("H,0:0")
                mainBinding.aspectRatioTxt.text = "16:9"
            }
            bindCameraUserCases()
        }
        mainBinding.changeCameraToVideoIB.setOnClickListener {
            isPhoto = !isPhoto
            if (isPhoto){
                mainBinding.changeCameraToVideoIB.setImageResource(R.drawable.ic_photo)
                mainBinding.captureIB.setImageResource(R.drawable.camera)
            }else{
                mainBinding.changeCameraToVideoIB.setImageResource(R.drawable.ic_videocam)
                mainBinding.captureIB.setImageResource(R.drawable.ic_start)
            }

        }

        mainBinding.captureIB.setOnClickListener {
            if (isPhoto) {
                takePhoto()
            }else{
                captureVideo()
            }
        }
        mainBinding.flashToggleIB.setOnClickListener {
            setFlashIcon(camera)
        }
    }


    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    // here all permission granted successfully
                    startCamera()
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        // here app Setting open because all permission is not granted
                        // and permanent denied
                        appSettingOpen(this)
                    } else {
                        // here warning permission show
                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUserCases()
        }, ContextCompat.getMainExecutor(this))
    }


    private fun bindCameraUserCases() {
        val rotation = mainBinding.previewView.display.rotation

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    aspectRatio,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.surfaceProvider = mainBinding.previewView.surfaceProvider
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            )
            .setAspectRatio(aspectRatio)
            .build()

        videoCapture = VideoCapture.withOutput(recorder).apply {
            targetRotation = rotation
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
               val myRotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture.targetRotation = myRotation
                videoCapture.targetRotation = myRotation
            }
        }
        orientationEventListener?.enable()

        try {
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture,videoCapture
            )
            setUpZoomTapToFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUpZoomTapToFocus(){
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener(){
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoomRatio = camera.cameraInfo.zoomState.value?.zoomRatio  ?: 1f
                val delta = detector.scaleFactor
                camera.cameraControl.setZoomRatio(currentZoomRatio * delta)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(this,listener)

        mainBinding.previewView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN){
                val factory = mainBinding.previewView.meteringPointFactory
                val point = factory.createPoint(event.x,event.y)
                val action = FocusMeteringAction.Builder(point,FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(2,TimeUnit.SECONDS)
                    .build()

                val x = event.x
                val y = event.y

                val focusCircle = RectF(x-50,y-50, x+50,y+50)

                mainBinding.focusCircleView.focusCircle = focusCircle
                mainBinding.focusCircleView.invalidate()

                camera.cameraControl.startFocusAndMetering(action)

                view.performClick()
            }
            true
        }
    }

    private fun setFlashIcon(camera: Camera) {
        if (camera.cameraInfo.hasFlashUnit()) {
            if (camera.cameraInfo.torchState.value == 0) {
                camera.cameraControl.enableTorch(true)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_off)
            } else {
                camera.cameraControl.enableTorch(false)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_on)
            }
        } else {
            Toast.makeText(
                this,
                "Flash is Not Available",
                Toast.LENGTH_LONG
            ).show()
            mainBinding.flashToggleIB.isEnabled = false
        }
    }



    private fun takePhoto() {
        val imageFolder = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "Images"
        )
        if (!imageFolder.exists()) {
            imageFolder.mkdir()
        }

        val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Images")
            }
        }

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        }

        // Capture image with in-memory callback for thermal processing
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Convert ImageProxy to Bitmap
                    val bitmap = imageProxyToBitmap(image)

                    // Process with thermal generation
                    processImageWithThermal(bitmap)

                    // Also save the original image
                    saveOriginalImage(bitmap, fileName, contentValues, metadata)

                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@MainActivity,
                        exception.message.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun saveOriginalImage(bitmap: Bitmap, fileName: String, contentValues: ContentValues, metadata: ImageCapture.Metadata) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        try {
            val outputStream = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                contentResolver.openOutputStream(contentResolver.insert(uri, contentValues)!!)
            } else {
                val imageFolder = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Images"
                )
                val imageFile = File(imageFolder, fileName)
                imageFile.outputStream()
            }

            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }

            Toast.makeText(this, "Original photo saved!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save original: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    private fun processImageWithThermal(bitmap: Bitmap) {
        Log.d("ThermalSave", "processImageWithThermal: starting")
        Toast.makeText(this, "Processing thermal image…", Toast.LENGTH_SHORT).show()

        val thermalBitmap = ThermalGenerator.generateThermalImage(bitmap)

        runOnUiThread {
            Toast.makeText(this, "Saving thermal image…", Toast.LENGTH_SHORT).show()
            saveThermalImage(thermalBitmap)
            Log.d("ThermalSave", "processImageWithThermal: saveThermalImage called")
        }
    }


    private fun saveThermalImage(thermalBitmap: Bitmap) {
        val filename = "thermal_" +
                SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                    .format(System.currentTimeMillis()) + ".jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ThermalImages"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = contentResolver
        val collection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)

        if (uri == null) {
            Toast.makeText(this, "Failed to create MediaStore entry", Toast.LENGTH_SHORT).show()
            Log.e("ThermalSave", "insert returned null URI")
            return
        }

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!thermalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)) {
                    Toast.makeText(this, "Compress failed", Toast.LENGTH_SHORT).show()
                    Log.e("ThermalSave", "thermalBitmap.compress returned false")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Toast.makeText(this, "Thermal image saved!", Toast.LENGTH_SHORT).show()
            Log.d("ThermalSave", "saveThermalImage: success at $uri")
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving thermal image: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ThermalSave", "saveThermalImage exception", e)
        }
    }




    private fun setAspectRatio(ratio: String) {
        mainBinding.previewView.layoutParams = mainBinding.previewView.layoutParams.apply {
            if (this is ConstraintLayout.LayoutParams) {
                dimensionRatio = ratio
            }
        }
    }

    private fun captureVideo(){

        mainBinding.captureIB.isEnabled = false

        mainBinding.flashToggleIB.gone()
        mainBinding.flipCameraIB.gone()
        mainBinding.aspectRatioTxt.gone()
        mainBinding.changeCameraToVideoIB.gone()


        if (recording != null){
            recording?.stop()
            stopRecording()
            recording = null
            return
        }
        startRecording()
        val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME,fileName)
            put(MediaStore.Video.Media.MIME_TYPE,"video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)){recordEvent->
                when(recordEvent){
                    is VideoRecordEvent.Start -> {
                        mainBinding.captureIB.setImageResource(R.drawable.ic_stop)
                        mainBinding.captureIB.isEnabled = true
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()){
                            val message = "Video Capture Succeeded: " + "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(
                                this@MainActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }else{
                            recording?.close()
                            recording = null
                            Log.d("error", recordEvent.error.toString())
                        }
                        mainBinding.captureIB.setImageResource(R.drawable.ic_start)
                        mainBinding.captureIB.isEnabled = true

                        mainBinding.flashToggleIB.visible()
                        mainBinding.flipCameraIB.visible()
                        mainBinding.aspectRatioTxt.visible()
                        mainBinding.changeCameraToVideoIB.visible()
                    }
                }
            }

    }


    override fun onResume() {
        super.onResume()
        orientationEventListener?.enable()
    }

    override fun onPause() {
        orientationEventListener?.disable()
        if (recording != null){
            recording?.stop()
            captureVideo()
        }
        super.onPause()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimer = object : Runnable{
        override fun run() {
            val currentTime = SystemClock.elapsedRealtime() - mainBinding.recodingTimerC.base
            val timeString = currentTime.toFormattedTime()
            mainBinding.recodingTimerC.text = timeString
            handler.postDelayed(this,1000)
        }
    }

    private fun Long.toFormattedTime():String{
        val seconds = ((this / 1000) % 60).toInt()
        val minutes = ((this / (1000 * 60)) % 60).toInt()
        val hours = ((this / (1000 * 60 * 60)) % 24).toInt()

       return if (hours >0){
            String.format("%02d:%02d:%02d",hours,minutes,seconds)
        }else{
            String.format("%02d:%02d",minutes,seconds)
        }
    }

    private fun startRecording(){
        mainBinding.recodingTimerC.visible()
        mainBinding.recodingTimerC.base = SystemClock.elapsedRealtime()
        mainBinding.recodingTimerC.start()
        handler.post(updateTimer)
    }
    private fun stopRecording(){
        mainBinding.recodingTimerC.gone()
        mainBinding.recodingTimerC.stop()
        handler.removeCallbacks(updateTimer)
    }

}
