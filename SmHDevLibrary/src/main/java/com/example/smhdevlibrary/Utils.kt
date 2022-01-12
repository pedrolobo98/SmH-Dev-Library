package com.example.smhdevlibrary

import android.graphics.Bitmap
import android.graphics.Color

class Utils {
    val colors = listOf(
        Color.BLUE, Color.CYAN, Color.DKGRAY, Color.GRAY, Color.GREEN,
        Color.LTGRAY, Color.BLACK, Color.MAGENTA, Color.RED, Color.WHITE,
        Color.YELLOW, Color.GRAY, Color.GREEN, Color.LTGRAY,
        Color.YELLOW, Color.GRAY, Color.GREEN, Color.LTGRAY)

    data class unit(var area: Int, var xmin: Int, var ymin: Int, var w: Int, var h: Int,
                    var xcent: Int, var ycent: Int, var classId: Int, var cluster: Int)

    data class returns (var unidade:List<unit>, var image: Bitmap, var area: Int )
    data class output (var resultado: List<Float>, var image: Bitmap)

    var lastActivity = "com.example.smh_demo.MainActivity"

    val modeSelectionKey = "modeSelectionKey"
    val homeActivityKey = "homeActivityKey"
    val pictureOutKey = "pictureOutKey"
    val timerOutKey = "timerOutKey"
    val listOutKey = "listOutKey"
}