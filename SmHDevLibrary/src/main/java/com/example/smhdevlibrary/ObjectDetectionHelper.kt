package com.example.smhdevlibrary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectionHelper(context: Context) {
    val context: Context = context

    var digitClassifier = DigitClassifier(context)

    init {
        digitClassifier
            .initialize()
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    fun runObjectDetection(bitmap: Bitmap, imageRotation: Int, mode: Int): Utils.output {

        val matrix = Matrix().apply {
            postRotate(imageRotation.toFloat())
        }
        val bitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)

        // Setup digit classifier.

        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(20)
            .setScoreThreshold(0.5f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            context,
            "ssd_640_2d_ecra_metadata.tflite",
            options
        )

        // Step 3: Feed given image to the detector
        val results = detector.detect(image)

        // Step 4: Parse the detection result and show it
        var resultToDisplay = results.map {
            // Get the top-1 category and craft the display text
            val category = it.categories.first()
            val text = "${category.label}, ${category.score.times(100).toInt()}%"

            // Create a data object to display the detection result
            DetectionResult(it.boundingBox, category.label)
        }
        resultToDisplay = resultToDisplay.filter { it.text == "1\r"}
        // Draw the detection result on the bitmap and show it.
        var conditionTree = ConditionTree(bitmap, context)
        var BuildList = buildList(bitmap, resultToDisplay)
        var FilteresArea = conditionTree.filterByArea(BuildList)
        var (resizeBoundinBox, img, area) = conditionTree.resizeByBoundingBoxLimits(FilteresArea)
        if(area > (bitmap.height * bitmap.width)/5){
            if (resizeBoundinBox.size != 0){
                when(mode) {
                    1 -> return Utils.output(
                        conditionTree.autoDeviceDetection(resizeBoundinBox),
                        img
                    )
                    2 -> return Utils.output(
                        conditionTree.termoDeviceDetection(resizeBoundinBox),
                        img
                    )
                    3 -> return Utils.output(
                        conditionTree.balanceDeviceDetection(
                            resizeBoundinBox
                        ), img
                    )
                    4 -> return Utils.output(
                        conditionTree.glucoDeviceDetection(resizeBoundinBox),
                        img
                    )
                    5 -> return Utils.output(
                        conditionTree.tenseDeviceDetection(resizeBoundinBox),
                        img
                    )
                    else -> return Utils.output(
                        conditionTree.oxiDeviceDetection(
                            resizeBoundinBox
                        ), img
                    )
                }
            }else{
                val conf = Bitmap.Config.ARGB_8888 // see other conf types
                val bmp = Bitmap.createBitmap(320, 320, conf)
                return Utils.output(mutableListOf(0f, 0f, 0f, 0f, 0f, 0f), bmp)
            }
        }else{
            val conf = Bitmap.Config.ARGB_8888 // see other conf types
            val bmp = Bitmap.createBitmap(320, 320, conf)
            return Utils.output(mutableListOf(8f, 0f, 0f, 0f, 0f, 0f), bmp)
        }
        //return conditionTree.autoDeviceDetection(resizeBoundinBox)
    }

    fun buildList(bitmap: Bitmap, detectionResults: List<DetectionResult>): List<Utils.unit> {

        var outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val results = mutableListOf<Utils.unit>()

        detectionResults.forEach {

            val box = it.boundingBox
            if (!(box.left.toInt() < 0 || box.top.toInt() < 0 || box.right.toInt() > bitmap.width || box.bottom.toInt() > bitmap.height)){
                //Crop digit to send to CNN
                var croppedBmp = Bitmap.createBitmap(
                    outputBitmap,
                    box.left.toInt(), box.top.toInt(), box.right.toInt() - box.left.toInt(), box.bottom.toInt() - box.top.toInt()
                )
                croppedBmp = Bitmap.createScaledBitmap(croppedBmp, 32, 32, true)
                var digit = 0
                if (digitClassifier.isInitialized){
                    digit = digitClassifier.classify(croppedBmp).toInt()
                }

                //List to save [area, min. X, min. Y, width, height, center x, center y, digit, cluster, group value]
                results.add(
                    Utils.unit(
                        (box.right.toInt() - box.left.toInt()) * (box.bottom.toInt() - box.top.toInt()),
                        box.left.toInt(),
                        box.top.toInt(),
                        box.right.toInt() - box.left.toInt(),
                        box.bottom.toInt() - box.top.toInt(),
                        (box.right.toInt() - box.left.toInt()) / 2 + box.left.toInt(),
                        (box.bottom.toInt() - box.top.toInt()) / 2 + box.top.toInt(),
                        digit,
                        0
                    )
                )
            }
        }
        return results
    }
}
/**
 * DetectionResult
 *      A class to store the visualization info of a detected object.
 */
data class DetectionResult(val boundingBox: RectF, val text: String)