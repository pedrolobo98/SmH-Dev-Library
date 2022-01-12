package com.example.smhdevlibrary

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.random.Random

class CameraAssistantActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null

    private lateinit var bitmapBuffer: Bitmap
    private lateinit var byteArray: ByteArray
    private lateinit var resultList: List<Float>
    private var imageRotationDegrees: Int = 0
    private var StartTime: Long = 0
    private var mode: Int = 0

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private lateinit var assist: TextView
    private lateinit var finder: PreviewView
    private lateinit var saveButton: ImageButton
    private lateinit var printAnalysis: TextView
    private lateinit var showDetection: ImageView
    private lateinit var cardViewDetection: CardView

    private var safeSave = false

    private val detector by lazy {
        ObjectDetectionAssistentHelper(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_assistant)

        cardViewDetection = findViewById(R.id.cardView)
        saveButton = findViewById(R.id.camera_capture_button)
        printAnalysis = findViewById(R.id.txtViewAnalysis)
        showDetection = findViewById(R.id.detectionsScree)
        assist = findViewById(R.id.textPrediction)
        finder = findViewById(R.id.view_Finder)

        StartTime = System.currentTimeMillis()
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

    fun cameraCapture(view: View){
        if (safeSave) {
            val intent = Intent(this, Class.forName(Utils().lastActivity))
            intent.putExtra(Utils().listOutKey, resultList.toFloatArray())
            intent.putExtra(Utils().pictureOutKey, byteArray)
            intent.putExtra(Utils().timerOutKey, System.currentTimeMillis()-StartTime)
            finish()
            startActivity(intent)
        }
    }

    private fun reportPrediction (detectedList: List<Float>, bitmap: Bitmap) = finder.post {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        showDetection.setImageBitmap(bitmap)
        cardViewDetection.visibility = View.VISIBLE
        printAnalysis.setText("")
        printAnalysis.bringToFront()
        if (detectedList[0] != 0f &&  detectedList[0] != 9f &&  detectedList[0] != 8f){
            assist.visibility = View.INVISIBLE
            if (detectedList[0] == 1f){
                if (3 < detectedList[5] && detectedList[5] < 200){
                    safeSave = true
                    saveButton.visibility = View.VISIBLE
                }
                printAnalysis.setText("Glucometer:\n" + detectedList[5].toString())
                byteArray  = stream.toByteArray()
                resultList = detectedList
            }else if (detectedList[0] == 2f){
                if(100f < detectedList[1] && detectedList[1] < 200f
                    && 20f < detectedList[2] && detectedList[2] < 100f && (detectedList[3] == 0f || (20f < detectedList[3] && detectedList[3] < 200f))){
                    safeSave = true
                    saveButton.visibility = View.VISIBLE
                }
                printAnalysis.setText("Blood Pressure:" + "\nSys:" + detectedList[1].toString()
                        + "\nDia:" + detectedList[2].toString()
                        + "\nPul:" + detectedList[3].toString())
                byteArray  = stream.toByteArray()
                resultList = detectedList
            }else if(detectedList[0] == 3f){
                if(70f < detectedList[4] && detectedList[4] < 101f
                    && 20f < detectedList[3] && detectedList[3] < 200f){
                    safeSave = true
                    saveButton.visibility = View.VISIBLE
                }
                printAnalysis.setText("Oximeter:" + "\nPul:" + detectedList[3].toString()
                        + "\nSpo2:" + detectedList[4].toString())
                byteArray  = stream.toByteArray()
                resultList = detectedList
            }else if(detectedList[0] == 4f){
                if(20 < detectedList[1] && detectedList[1] < 60){
                    safeSave = true
                    saveButton.visibility = View.VISIBLE
                }
                printAnalysis.setText("Termometer:\n" + detectedList[1].toString() + "\nCº:" )
                byteArray  = stream.toByteArray()
                resultList = detectedList
            }else if (detectedList[0] == 5f && 0 < detectedList[1] && detectedList[1] < 201){
                if(0 < detectedList[1] && detectedList[1] < 201){
                    safeSave = true
                    saveButton.visibility = View.VISIBLE
                }
                printAnalysis.setText("Weight Balance:\n" + detectedList[1].toString() + "\nKg:" )
                byteArray  = stream.toByteArray()
                resultList = detectedList
            }else{
                saveButton.visibility = View.INVISIBLE
                cardViewDetection.visibility = View.INVISIBLE
                assist.text = "Invalid Analysis "
                assist.visibility = View.VISIBLE
                safeSave = false
            }
        }else if(detectedList[0] == 9f){
            saveButton.visibility = View.INVISIBLE
            cardViewDetection.visibility = View.INVISIBLE
            assist.text = "Wrong Device Selected"
            assist.visibility = View.VISIBLE
            safeSave = false
        }else if(detectedList[0] == 8f){
            saveButton.visibility = View.INVISIBLE
            cardViewDetection.visibility = View.INVISIBLE
            assist.text = "Bring the device closer"
            assist.visibility = View.VISIBLE
            safeSave = false
        }else{
            cardViewDetection.visibility = View.INVISIBLE
            saveButton.visibility = View.INVISIBLE
            assist.visibility = View.GONE
            safeSave = false
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