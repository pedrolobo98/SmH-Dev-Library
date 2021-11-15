package com.example.smhdevlibrary

import android.content.Intent
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

    data class returns (var unidade:List<unit>, var image: Bitmap)
    data class output (var resultado: List<Float>, var image: Bitmap)

    val lastActivity = "com.example.smh_demo.MainActivity"
}