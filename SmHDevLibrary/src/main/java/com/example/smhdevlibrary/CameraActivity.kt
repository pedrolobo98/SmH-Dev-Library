package com.example.smhdevlibrary

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.random.Random


class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null

    private lateinit var bitmapBuffer: Bitmap
    private var imageRotationDegrees: Int = 0
    private var mode: Int = 0

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private lateinit var assist: TextView
    private lateinit var finder: PreviewView

    private lateinit var view_timer: Chronometer

    private val detector by lazy { ObjectDetectionHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camera)
        finder = findViewById(R.id.view_Finder)
        assist = findViewById(R.id.textPrediction)
        view_timer = findViewById(R.id.timer)

        //view_timer.isCountDown = true
        view_timer.base = SystemClock.elapsedRealtime()
        view_timer.start()
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindCameraUseCases() = finder.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable{

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetRotation(finder.display.rotation)
                .build()

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val converter = YuvToRgbConverter(this)

            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->

                if (!::bitmapBuffer.isInitialized) {
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running
                    imageRotationDegrees = image.imageInfo.rotationDegrees
                    bitmapBuffer = Bitmap.createBitmap(
                        image.width, image.height, Bitmap.Config.ARGB_8888)
                }



                // Convert the image to RGB and place it in our shared buffer
                converter.yuvToRgb(image.image!!, bitmapBuffer)

                // Perform the object detection for the current frame
                var (numDetections, imageRe) = detector.runObjectDetection(bitmapBuffer, imageRotationDegrees, mode)
                reportPrediction(numDetections, imageRe)
                image.close()
            })

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
                preview?.setSurfaceProvider(finder.surfaceProvider)


            } catch(exc: Exception) {
                Log.e("SmH", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun reportPrediction (detectedList: List<Float>, bitmap: Bitmap) = finder.post {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray: ByteArray = stream.toByteArray()
        if (detectedList[0] != 0f &&  detectedList[0] != 9f &&  detectedList[0] != 8f){
            //text_prediction.text = "Device Detected"
            //text_prediction.visibility = View.VISIBLE
            if (detectedList[0] == 1f && 3 < detectedList[5] && detectedList[5] < 200){
                view_timer.getText()
                val intent = Intent(this, Class.forName(Utils().lastActivity))
                intent.putExtra(Utils().listOutKey, detectedList.toFloatArray())
                intent.putExtra(Utils().pictureOutKey, byteArray)
                intent.putExtra(Utils().timerOutKey, SystemClock.elapsedRealtime())
                finish()
                startActivity(intent)
            }else if (detectedList[0] == 2f && 100f < detectedList[1] && detectedList[1] < 200f
                && 20f < detectedList[2] && detectedList[2] < 100f && (detectedList[3] == 0f || (20f < detectedList[3] && detectedList[3] < 200f))){
                val intent = Intent(this, Class.forName(Utils().lastActivity))
                intent.putExtra(Utils().listOutKey, detectedList.toFloatArray())
                intent.putExtra(Utils().pictureOutKey, byteArray)
                intent.putExtra(Utils().timerOutKey, SystemClock.elapsedRealtime())
                finish()
                startActivity(intent)
            }else if(detectedList[0] == 3f && 70f < detectedList[4] && detectedList[4] < 101f
                && 20f < detectedList[3] && detectedList[3] < 200f){
                val intent = Intent(this, Class.forName(Utils().lastActivity))
                intent.putExtra(Utils().listOutKey, detectedList.toFloatArray())
                intent.putExtra(Utils().pictureOutKey, byteArray)
                intent.putExtra(Utils().timerOutKey, SystemClock.elapsedRealtime())
                finish()
                startActivity(intent)
            }else if(detectedList[0] == 4f && (20 < detectedList[1] && detectedList[1] < 60)){
                val intent = Intent(this, Class.forName(Utils().lastActivity))
                intent.putExtra(Utils().listOutKey, detectedList.toFloatArray())
                intent.putExtra(Utils().pictureOutKey, byteArray)
                intent.putExtra(Utils().timerOutKey, SystemClock.elapsedRealtime())
                finish()
                startActivity(intent)
            }else if (detectedList[0] == 5f && 0 < detectedList[1] && detectedList[1] < 201){
                val intent = Intent(this, Class.forName(Utils().lastActivity))
                intent.putExtra(Utils().listOutKey, detectedList.toFloatArray())
                intent.putExtra(Utils().pictureOutKey, byteArray)
                intent.putExtra(Utils().timerOutKey, SystemClock.elapsedRealtime())
                finish()
                startActivity(intent)
            }else{
                assist.visibility = View.GONE
            }
        }else if(detectedList[0] == 9f){
            assist.text = "Wrong Device Selected"
            assist.visibility = View.VISIBLE
        }
        else{
            assist.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode)
        } else {
            if ((intent.extras?.getInt(Utils().modeSelectionKey) != null) || (intent.extras?.getInt(Utils().modeSelectionKey) != 0)
                || (intent.extras?.getString(Utils().homeActivityKey) != null)){
                intent.extras?.getString(Utils().homeActivityKey, Utils().lastActivity)
                mode = intent.extras?.getInt(Utils().modeSelectionKey)!!
                bindCameraUseCases()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            if ((intent.extras?.getInt(Utils().modeSelectionKey) != null) || (intent.extras?.getInt(Utils().modeSelectionKey) != 0)
                || (intent.extras?.getString(Utils().homeActivityKey) != null)){
                intent.extras?.getString(Utils().homeActivityKey, Utils().lastActivity)
                mode = intent.extras?.getInt(Utils().modeSelectionKey)!!
                bindCameraUseCases()
            }
        } else {
            val intent = Intent(this, Class.forName(Utils().lastActivity))
            startActivity(intent)
            Toast.makeText(this, "Enable camera permission from settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, Class.forName(Utils().lastActivity))
        finish()
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}