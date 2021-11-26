package com.example.smhdevlibrary

import android.content.Context
import android.graphics.*
import org.nield.kotlinstatistics.dbScanCluster
import kotlin.math.abs

class ConditionTree (bitmap: Bitmap, context: Context){
    val context:Context = context
    var outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    var copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val pen = Paint()

    fun filterByArea(results: List<Utils.unit>): List<Utils.unit> {
        val max = results.map { it.area }.maxOrNull()
        var filteredArea = results.filter { it.area > max!!/3 || it.classId > 9}

        return filteredArea
    }

    fun resizeByBoundingBoxLimits(filteredArea: List<Utils.unit>): Utils.returns {
        var xmin = 9999
        var ymin = 9999
        var xmax = 0
        var ymax = 0
        filteredArea.forEachIndexed{ index, item ->
            if (item.xmin < xmin){
                xmin = item.xmin
            }
            if ((item.xmin + item.w) > xmax) {
                xmax = item.xmin + item.w
            }
            if (item.ymin < ymin){
                ymin = item.ymin
            }
            if ((item.ymin + item.h) > ymax){
                ymax = item.ymin + item.h
            }
        }

        if (xmin != 9999 && ymin != 9999 && xmax != 0 && ymax != 0){
            //ADD Border to keep 1:1
            copyBitmap = Bitmap.createBitmap(
                copyBitmap,
                xmin, ymin, xmax - xmin, ymax - ymin
            )
            var newHW = 0
            if (copyBitmap.width > copyBitmap.height){
                newHW = copyBitmap.width
                filteredArea.forEachIndexed{ index, item ->
                    item.xmin = (item.xmin - xmin)
                    item.ymin = (item.ymin - ymin) + (newHW * 0.5 - copyBitmap.height * 0.5).toInt()
                }
            }else{
                newHW = copyBitmap.height
                filteredArea.forEachIndexed{ index, item ->
                    item.xmin = (item.xmin - xmin) + (newHW * 0.5 - copyBitmap.width * 0.5).toInt()
                    item.ymin = (item.ymin - ymin)
                }
            }
            outputBitmap = Bitmap.createBitmap(newHW, newHW, copyBitmap.config)
            val canvas = Canvas(outputBitmap)
            val rect_paint = Paint()
            rect_paint.style = Paint.Style.FILL
            rect_paint.color = Color.rgb(0, 0, 0)

            canvas.drawRect(0f, 0f, newHW.toFloat(), newHW.toFloat(), rect_paint)
            canvas.drawBitmap(copyBitmap, (newHW * 0.5 - copyBitmap.width * 0.5).toFloat(),
                (newHW * 0.5 - copyBitmap.height * 0.5).toFloat(), null)

            filteredArea.forEachIndexed{ index, item ->
                pen.color = Utils().colors[item.classId]
                pen.strokeWidth = 2F
                pen.style = Paint.Style.STROKE

                canvas.drawRect(
                    RectF(item.xmin.toFloat(),
                        item.ymin.toFloat(),
                        item.xmin.toFloat() + item.w.toFloat(),
                        item.ymin.toFloat() + item.h.toFloat()), pen)
            }
        }
        return Utils.returns(filteredArea, outputBitmap)
    }
    fun termoDeviceDetection(filteredArea: List<Utils.unit>): List<Float> {
        var copyFilteredArea = filteredArea
        var analysisList = mutableListOf(0f, 0f, 0f, 0f, 0f, 0f)

        if (copyFilteredArea.any { item -> item.classId > 9 }) {
            analysisList[0] = 9f
        }else{
            copyFilteredArea = copyFilteredArea.filter { it.classId < 10 }
            copyFilteredArea = copyFilteredArea.sortedBy { item -> item.xcent }
            var finalValue =
                (copyFilteredArea.joinToString(separator = "") { it -> "${it.classId}" }).toInt()
            analysisList = mutableListOf(4f, finalValue.toFloat(), 0f, 0f, 0f, 0f)
        }
        return analysisList
    }
    fun balanceDeviceDetection(filteredArea: List<Utils.unit>): List<Float> {
        var copyFilteredArea = filteredArea
        var analysisList = mutableListOf(0f, 0f, 0f, 0f, 0f, 0f)

        if (copyFilteredArea.any { item -> item.classId > 9 }) {
            analysisList[0] = 9f
        }else{
            copyFilteredArea = copyFilteredArea.filter { it.classId < 10 }
            copyFilteredArea = copyFilteredArea.sortedBy { item -> item.xcent }
            var finalValue =
                (copyFilteredArea.joinToString(separator = "") { it -> "${it.classId}" }).toInt()
            analysisList = mutableListOf(5f, finalValue.toFloat(), 0f, 0f, 0f, 0f)
        }
        return analysisList
    }
    fun glucoDeviceDetection(filteredArea: List<Utils.unit>): List<Float> {
        var copyFilteredArea = filteredArea
        var analysisList = mutableListOf(0f, 0f, 0f, 0f, 0f, 0f)

        if ((copyFilteredArea.any { item -> item.classId > 11 }))  {
            analysisList[0] = 9f
        }else{
            copyFilteredArea = copyFilteredArea.filter { it.classId < 10 }
            copyFilteredArea = copyFilteredArea.sortedBy { item -> item.xcent }
            var finalValue =
                (copyFilteredArea.joinToString(separator = "") { it -> "${it.classId}" }).toInt()
            analysisList = mutableListOf(1f, 0f, 0f, 0f, 0f, finalValue.toFloat())
        }
        return analysisList
    }
    fun tenseDeviceDetection(filteredArea: List<Utils.unit>): List<Float> {
        var copyFilteredArea = filteredArea
        var analysisList = mutableListOf(0f, 0f, 0f, 0f, 0f, 0f)

        if ((copyFilteredArea.any { item -> item.classId == 11 }) || (copyFilteredArea.any { item -> item.classId > 13 }))  {
            analysisList[0] = 9f
        }else{
            copyFilteredArea = copyFilteredArea.filter { it.classId < 10}
            val clusters = copyFilteredArea.dbScanCluster(45.0,
                1,
                xSelector = { it.ycent.toDouble() },
                ySelector = { it.cluster.toDouble() })
            if (clusters.size == 2){
                var yMed = 0
                if (clusters[1].points.sumBy { it -> it.ycent} > clusters[0].points.sumBy { it -> it.ycent}){
                    if ((clusters[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt() > 1000){
                        val clr = clusters[0].points.dbScanCluster(25.0,
                            1,
                            xSelector = { it.ycent.toDouble() },
                            ySelector = { it.cluster.toDouble() })
                        if (clr.size == 2){
                            if (abs(clr[0].points.sumBy { it -> it.xcent} - clusters[1].points.sumBy { it -> it.ycent}) >
                                abs(clr[1].points.sumBy { it -> it.xcent} - clusters[1].points.sumBy { it -> it.ycent})){
                                analysisList = mutableListOf(2f
                                    , (clusters[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , (clr[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , (clr[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , 0f, 0f)
                            }else{
                                analysisList = mutableListOf(2f
                                    , (clusters[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , (clr[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , (clr[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , 0f, 0f)
                            }
                        }
                    }else{
                        analysisList = mutableListOf(2f
                            , (clusters[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                            , (clusters[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                            , 0f, 0f, 0f)
                    }
                }else{
                    if ((clusters[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt() > 1000){
                        val clr = clusters[1].points.dbScanCluster(25.0,
                            1,
                            xSelector = { it.ycent.toDouble() },
                            ySelector = { it.cluster.toDouble() })
                        if (clr.size == 2){
                            if (abs(clr[0].points.sumBy { it -> it.xcent} - clusters[0].points.sumBy { it -> it.ycent}) >
                                abs(clr[1].points.sumBy { it -> it.xcent} - clusters[0].points.sumBy { it -> it.ycent})){
                                analysisList = mutableListOf(2f
                                    , (clusters[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , (clr[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , (clr[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , 0f, 0f)
                            }else{
                                analysisList = mutableListOf(2f
                                    , (clusters[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , (clr[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , (clr[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                                    , 0f, 0f)
                            }
                        }
                    }else{
                        analysisList = mutableListOf(2f
                            , (clusters[0].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                            , (clusters[1].points.sortedBy { it -> it.xcent }.joinToString (separator = "") { it -> "${it.classId}" }).toInt().toFloat()
                            , 0f, 0f, 0f)
                    }
                }
            }else if (clusters.size == 3){
                var yMed0 = 0
                var yMed1 = 0
                var yMed2 = 0
                var valFlag0 = 0
                var valFlag1 = 0
                analysisList = mutableListOf(2f, 0f, 0f, 0f, 0f, 0f)
                clusters.forEachIndexed { index, item ->
                    var Item = item.points
                    if (index == 0){
                        Item.forEachIndexed{index, item -> yMed0 = yMed0 + item.ycent}
                        yMed0 = yMed0 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        valFlag0 = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                    }else if(index == 1){
                        Item.forEachIndexed{index, item -> yMed1 = yMed1 + item.ycent}
                        yMed1 = yMed1 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        valFlag1 = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                    }else if(index == 2){
                        Item.forEachIndexed{index, item -> yMed2 = yMed2 + item.ycent}
                        yMed2 = yMed2 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        var list = listOf(yMed0, yMed1,yMed2).sorted()
                        if (list[0] == yMed0){
                            analysisList[1] = valFlag0.toFloat()
                            if (list[1] == yMed1){
                                analysisList[2] = valFlag1.toFloat()
                                analysisList[3] = finalValue.toFloat()
                            }else{
                                analysisList[3] = valFlag1.toFloat()
                                analysisList[2] = finalValue.toFloat()
                            }
                        }else if (list[0] == yMed1){
                            analysisList[1] = valFlag1.toFloat()
                            if (list[1] == yMed0){
                                analysisList[2] = valFlag0.toFloat()
                                analysisList[3] = finalValue.toFloat()
                            }else{
                                analysisList[3] = valFlag0.toFloat()
                                analysisList[2] = finalValue.toFloat()
                            }
                        }else if (list[0] == yMed2){
                            analysisList[1] = finalValue.toFloat()
                            if (list[1] == yMed1){
                                analysisList[2] = valFlag1.toFloat()
                                analysisList[3] = valFlag0.toFloat()
                            }else{
                                analysisList[3] = valFlag1.toFloat()
                                analysisList[2] = valFlag0.toFloat()
                            }
                        }
                    }
                }
            }
        }
        return analysisList
    }
    fun oxiDeviceDetection(filteredArea: List<Utils.unit>): List<Float> {
        var copyFilteredArea = filteredArea
        var analysisList = mutableListOf(0f, 0f, 0f, 0f, 0f, 0f)

        if ((copyFilteredArea.any { item -> item.classId == 11 }) || (copyFilteredArea.any { item -> item.classId == 12 }) || (copyFilteredArea.any { item -> item.classId == 13 }))  {
            analysisList[0] = 9f
        }else{
            if (copyFilteredArea.any { item -> item.classId == 14 }){
                var spo2 = copyFilteredArea.filter { it.classId == 14}
                copyFilteredArea = copyFilteredArea.filter { it.classId < 10}
                analysisList = mutableListOf(3f, 0f, 0f, 0f, 0f, 0f)
                val clusters = copyFilteredArea.dbScanCluster(25.0,
                    1,
                    xSelector = { it.ycent.toDouble() },
                    ySelector = { it.cluster.toDouble() })
                if (clusters.size != 2){
                    val clusters = copyFilteredArea.dbScanCluster(25.0,
                        1,
                        xSelector = { it.xcent.toDouble() },
                        ySelector = { it.cluster.toDouble() })
                    if (clusters.size == 2){
                        var valFlag = 0
                        var xMed0 = 0
                        var xMed1 = 0
                        clusters.forEachIndexed { index, item ->
                            var Item = item.points
                            Item = Item.sortedBy { it -> it.xcent }
                            if (index == 0){
                                valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                                Item.forEachIndexed{index, item -> xMed0 = xMed0 + item.xcent}
                                xMed0 = xMed0 / Item.size
                            }else if(index == 1){
                                var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                                Item.forEachIndexed{index, item -> xMed1 = xMed1 + item.xcent}
                                xMed1 = xMed1 / Item.size
                                if(xMed1 - spo2.first().xcent < xMed0 - spo2.first().xcent){
                                    analysisList[4] = finalValue.toFloat()
                                    analysisList[3] = valFlag.toFloat()
                                }else{
                                    analysisList[3] = finalValue.toFloat()
                                    analysisList[4] = valFlag.toFloat()
                                }

                            }
                        }
                    }
                }else{
                    var valFlag = 0
                    var yMed0 = 0
                    var yMed1 = 0
                    clusters.forEachIndexed { index, item ->
                        var Item = item.points
                        Item = Item.sortedBy { it -> it.xcent }
                        if (index == 0){
                            valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                            Item.forEachIndexed{index, item -> yMed0 = yMed0 + item.ycent}
                            yMed0 = yMed0 / Item.size
                        }else if(index == 1){
                            var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                            Item.forEachIndexed{index, item -> yMed1 = yMed1 + item.ycent}
                            yMed1 = yMed1 / Item.size
                            if ((spo2.first().xcent < yMed1) && (spo2.first().xcent < yMed0)){
                                if (abs(spo2.first().xcent - yMed1) < abs(spo2.first().xcent - yMed0)){
                                    analysisList[4] = finalValue.toFloat()
                                    analysisList[3] = valFlag.toFloat()
                                }else{
                                    analysisList[3] = finalValue.toFloat()
                                    analysisList[4] = valFlag.toFloat()
                                }
                            }else if ((spo2.first().xcent > yMed1) && (spo2.first().xcent > yMed0)) {
                                if (abs(spo2.first().xcent - yMed1) < abs(spo2.first().xcent - yMed0)) {
                                    analysisList[4] = finalValue.toFloat()
                                    analysisList[3] = valFlag.toFloat()
                                } else {
                                    analysisList[3] = finalValue.toFloat()
                                    analysisList[4] = valFlag.toFloat()
                                }
                            }else{
                                if (abs(100 - finalValue) < abs(100 - valFlag)){
                                    analysisList[4] = finalValue.toFloat()
                                    analysisList[3] = valFlag.toFloat()
                                }else{
                                    analysisList[3] = finalValue.toFloat()
                                    analysisList[4] = valFlag.toFloat()
                                }
                            }
                        }
                    }
                }
            }else{
                copyFilteredArea = copyFilteredArea.filter { it.classId < 10}
                analysisList = mutableListOf(3f, 0f, 0f, 0f, 0f, 0f)
                val clusters = copyFilteredArea.dbScanCluster(25.0,
                    1,
                    xSelector = { it.ycent.toDouble() },
                    ySelector = { it.cluster.toDouble() })
                if (clusters.size != 2){
                    val clusters = copyFilteredArea.dbScanCluster(15.0,
                        1,
                        xSelector = { it.xcent.toDouble() },
                        ySelector = { it.cluster.toDouble() })
                    if (clusters.size == 2){
                        var valFlag = 0
                        clusters.forEachIndexed { index, item ->
                            var Item = item.points
                            Item = Item.sortedBy { it -> it.xcent }
                            if (index == 0){
                                valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                            }else if(index == 1){
                                var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                                if (abs(100 - finalValue) < abs(100 - valFlag)){
                                    analysisList[4] = finalValue.toFloat()
                                    analysisList[3] = valFlag.toFloat()
                                }else{
                                    analysisList[3] = finalValue.toFloat()
                                    analysisList[4] = valFlag.toFloat()
                                }
                            }
                        }
                    }
                }else{
                    var valFlag = 0
                    clusters.forEachIndexed { index, item ->
                        var Item = item.points
                        Item = Item.sortedBy { it -> it.xcent }
                        if (index == 0){
                            valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        }else if(index == 1){
                            var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                            if (abs(100 - finalValue) < abs(100 - valFlag)){
                                analysisList[4] = finalValue.toFloat()
                                analysisList[3] = valFlag.toFloat()
                            }else{
                                analysisList[3] = finalValue.toFloat()
                                analysisList[4] = valFlag.toFloat()
                            }
                        }
                    }
                }
            }
        }
        return analysisList
    }
    fun autoDeviceDetection(filteredArea: List<Utils.unit>): List<Float> {

        /*[Device Type, Sys val, Dia val, Pul val, Oxi val, gluco]
        ** Device type:
        ** 0 - not detected | 1 - gluco | 2 - tenso | 3 - Oxi
        */
        var copyFilteredArea = filteredArea
        var analysisList = mutableListOf(0f, 0f, 0f, 0f, 0f, 0f)

        if (copyFilteredArea.any { item -> item.classId == 11 }){
            copyFilteredArea = copyFilteredArea.filter { it.classId < 10}
            copyFilteredArea = copyFilteredArea.sortedBy { item -> item.xcent }
            var finalValue = (copyFilteredArea.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
            analysisList = mutableListOf(1f, 0f, 0f, 0f, 0f, finalValue.toFloat())

        }else if (copyFilteredArea.any { item -> item.classId == 12 } || copyFilteredArea.any { item -> item.classId == 13 }){
            copyFilteredArea = copyFilteredArea.filter { it.classId < 10}
            val clusters = copyFilteredArea.dbScanCluster(60.0,
                1,
                xSelector = { it.ycent.toDouble() },
                ySelector = { it.cluster.toDouble() })
            if (clusters.size == 2){
                var yMed = 0
                var valFlag = 0

                clusters.forEachIndexed { index, item ->
                    var Item = item.points
                    if (index == 0){
                        yMed = Item.sumBy { it -> it.ycent }
                        Item = Item.sortedBy { it -> it.xcent }
                        valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()

                    }else if(index == 1){
                        Item = Item.sortedBy { it -> it.xcent }
                        var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        if (Item.sumBy { it -> it.ycent } > yMed){
                            analysisList = mutableListOf(2f, finalValue.toFloat(), valFlag.toFloat(), 0f, 0f, 0f)
                        }else{
                            analysisList = mutableListOf(2f, valFlag.toFloat(), finalValue.toFloat(), 0f, 0f, 0f)
                        }
                    }
                }
            }else{
                var yMed0 = 0
                var yMed1 = 0
                var yMed2 = 0
                var valFlag0 = 0
                var valFlag1 = 0
                analysisList = mutableListOf(2f, 0f, 0f, 0f, 0f, 0f)
                clusters.forEachIndexed { index, item ->
                    var Item = item.points
                    if (index == 0){
                        Item.forEachIndexed{index, item -> yMed0 = yMed0 + item.ycent}
                        yMed0 = yMed0 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        valFlag0 = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                    }else if(index == 1){
                        Item.forEachIndexed{index, item -> yMed1 = yMed1 + item.ycent}
                        yMed1 = yMed1 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        valFlag1 = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                    }else if(index == 2){
                        Item.forEachIndexed{index, item -> yMed2 = yMed2 + item.ycent}
                        yMed2 = yMed2 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        var list = listOf(yMed0, yMed1,yMed2).sorted()
                        if (list[0] == yMed0){
                            analysisList[1] = valFlag0.toFloat()
                            if (list[1] == yMed1){
                                analysisList[2] = valFlag1.toFloat()
                                analysisList[3] = finalValue.toFloat()
                            }else{
                                analysisList[3] = valFlag1.toFloat()
                                analysisList[2] = finalValue.toFloat()
                            }
                        }else if (list[0] == yMed1){
                            analysisList[1] = valFlag1.toFloat()
                            if (list[1] == yMed0){
                                analysisList[2] = valFlag0.toFloat()
                                analysisList[3] = finalValue.toFloat()
                            }else{
                                analysisList[3] = valFlag0.toFloat()
                                analysisList[2] = finalValue.toFloat()
                            }
                        }else if (list[0] == yMed2){
                            analysisList[1] = finalValue.toFloat()
                            if (list[1] == yMed1){
                                analysisList[2] = valFlag1.toFloat()
                                analysisList[3] = valFlag0.toFloat()
                            }else{
                                analysisList[3] = valFlag1.toFloat()
                                analysisList[2] = valFlag0.toFloat()
                            }
                        }
                    }
                }
            }
        }else if ((copyFilteredArea.any { item -> item.classId == 14 })){
            var spo2 = copyFilteredArea.filter { it.classId == 14}
            copyFilteredArea = copyFilteredArea.filter { it.classId < 10}
            analysisList = mutableListOf(3f, 0f, 0f, 0f, 0f, 0f)
            val clusters = copyFilteredArea.dbScanCluster(25.0,
                1,
                xSelector = { it.ycent.toDouble() },
                ySelector = { it.cluster.toDouble() })
            if (clusters.size != 2){
                val clusters = copyFilteredArea.dbScanCluster(15.0,
                    1,
                    xSelector = { it.xcent.toDouble() },
                    ySelector = { it.cluster.toDouble() })
                if (clusters.size == 2){
                    var valFlag = 0
                    var xMed0 = 0
                    var xMed1 = 0
                    clusters.forEachIndexed { index, item ->
                        var Item = item.points
                        Item = Item.sortedBy { it -> it.xcent }
                        if (index == 0){
                            valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                            Item.forEachIndexed{index, item -> xMed0 = xMed0 + item.xcent}
                            xMed0 = xMed0 / Item.size
                        }else if(index == 1){
                            var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                            Item.forEachIndexed{index, item -> xMed1 = xMed1 + item.xcent}
                            xMed1 = xMed1 / Item.size
                            if(xMed1 - spo2.first().xcent < xMed0 - spo2.first().xcent){
                                analysisList[4] = finalValue.toFloat()
                                analysisList[3] = valFlag.toFloat()
                            }else{
                                analysisList[3] = finalValue.toFloat()
                                analysisList[4] = valFlag.toFloat()
                            }

                        }
                    }
                }
            }else{
                var valFlag = 0
                var yMed0 = 0
                var yMed1 = 0
                clusters.forEachIndexed { index, item ->
                    var Item = item.points
                    Item = Item.sortedBy { it -> it.xcent }
                    if (index == 0){
                        valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        Item.forEachIndexed{index, item -> yMed0 = yMed0 + item.ycent}
                        yMed0 = yMed0 / Item.size
                    }else if(index == 1){
                        var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        Item.forEachIndexed{index, item -> yMed1 = yMed1 + item.ycent}
                        yMed1 = yMed1 / Item.size
                        if ((spo2.first().xcent < yMed1) && (spo2.first().xcent < yMed0)){
                            if (abs(spo2.first().xcent - yMed1) < abs(spo2.first().xcent - yMed0)){
                                analysisList[4] = finalValue.toFloat()
                                analysisList[3] = valFlag.toFloat()
                            }else{
                                analysisList[3] = finalValue.toFloat()
                                analysisList[4] = valFlag.toFloat()
                            }
                        }else if ((spo2.first().xcent > yMed1) && (spo2.first().xcent > yMed0)) {
                            if (abs(spo2.first().xcent - yMed1) < abs(spo2.first().xcent - yMed0)) {
                                analysisList[4] = finalValue.toFloat()
                                analysisList[3] = valFlag.toFloat()
                            } else {
                                analysisList[3] = finalValue.toFloat()
                                analysisList[4] = valFlag.toFloat()
                            }
                        }else{
                            if (abs(100 - finalValue) < abs(100 - valFlag)){
                                analysisList[4] = finalValue.toFloat()
                                analysisList[3] = valFlag.toFloat()
                            }else{
                                analysisList[3] = finalValue.toFloat()
                                analysisList[4] = valFlag.toFloat()
                            }
                        }
                    }
                }
            }
        }else{
            val clusters = copyFilteredArea.dbScanCluster(35.0,
                1,
                xSelector = { it.ycent.toDouble() },
                ySelector = { it.cluster.toDouble() })
            if (clusters.size == 2){
                var yMed0 = 0
                var yMed1 = 0
                var valFlag = 0
                clusters.forEachIndexed { index, item ->
                    var Item = item.points
                    Item = Item.sortedBy { it -> it.xcent }
                    if (index == 0){
                        Item.forEachIndexed{index, item -> yMed0 = yMed0 + item.ycent}
                        yMed0 = yMed0 / Item.size
                        valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                    }else if(index == 1){
                        Item.forEachIndexed{index, item -> yMed1 = yMed1 + item.ycent}
                        yMed1 = yMed1 / Item.size
                        var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        if (yMed0 < yMed1 && valFlag > 100){
                            analysisList = mutableListOf(2f, 0f, 0f, 0f, 0f, 0f)
                            analysisList[1] = valFlag.toFloat()
                            analysisList[2] = finalValue.toFloat()
                        }else if (yMed1 < yMed0 && finalValue > 100){
                            analysisList = mutableListOf(2f, 0f, 0f, 0f, 0f, 0f)
                            analysisList[2] = valFlag.toFloat()
                            analysisList[1] = finalValue.toFloat()
                        }else{
                            if(abs(100 - finalValue) < abs(100 - valFlag)){
                                analysisList = mutableListOf(3f, 0f, 0f, 0f, 0f, 0f)
                                analysisList[3] = valFlag.toFloat()
                                analysisList[4] = finalValue.toFloat()
                            }else if (abs(100 - finalValue) > abs(100 - valFlag)){
                                analysisList = mutableListOf(3f, 0f, 0f, 0f, 0f, 0f)
                                analysisList[4] = valFlag.toFloat()
                                analysisList[3] = finalValue.toFloat()
                            }
                        }
                    }
                }
            }else if (clusters.size == 3){
                var yMed0 = 0
                var yMed1 = 0
                var yMed2 = 0
                var valFlag0 = 0
                var valFlag1 = 0
                analysisList = mutableListOf(2f, 0f, 0f, 0f, 0f, 0f)
                clusters.forEachIndexed { index, item ->
                    var Item = item.points
                    if (index == 0){
                        Item.forEachIndexed{index, item -> yMed0 = yMed0 + item.ycent}
                        yMed0 = yMed0 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        valFlag0 = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                    }else if(index == 1){
                        Item.forEachIndexed{index, item -> yMed1 = yMed1 + item.ycent}
                        yMed1 = yMed1 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        valFlag1 = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                    }else if(index == 2){
                        Item.forEachIndexed{index, item -> yMed2 = yMed2 + item.ycent}
                        yMed2 = yMed2 / Item.size
                        Item = Item.sortedBy { it -> it.xcent }
                        var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        var list = listOf(yMed0, yMed1,yMed2).sorted()
                        if (list[0] == yMed0){
                            analysisList[1] = valFlag0.toFloat()
                            if (list[1] == yMed1){
                                analysisList[2] = valFlag1.toFloat()
                                analysisList[3] = finalValue.toFloat()
                            }else{
                                analysisList[3] = valFlag1.toFloat()
                                analysisList[2] = finalValue.toFloat()
                            }
                        }else if (list[0] == yMed1){
                            analysisList[1] = valFlag1.toFloat()
                            if (list[1] == yMed0){
                                analysisList[2] = valFlag0.toFloat()
                                analysisList[3] = finalValue.toFloat()
                            }else{
                                analysisList[3] = valFlag0.toFloat()
                                analysisList[2] = finalValue.toFloat()
                            }
                        }else if (list[0] == yMed2){
                            analysisList[1] = finalValue.toFloat()
                            if (list[1] == yMed1){
                                analysisList[2] = valFlag1.toFloat()
                                analysisList[3] = valFlag0.toFloat()
                            }else{
                                analysisList[3] = valFlag1.toFloat()
                                analysisList[2] = valFlag0.toFloat()
                            }
                        }
                    }
                }
            }else{
                val clusters = copyFilteredArea.dbScanCluster(35.0,
                    1,
                    xSelector = { it.xcent.toDouble() },
                    ySelector = { it.cluster.toDouble() })
                var yMed = 0
                var valFlag = 0
                if (clusters.size == 2){
                    analysisList = mutableListOf(3f, 0f, 0f, 0f, 0f, 0f)
                    clusters.forEachIndexed { index, item ->
                        var Item = item.points.filter { it.classId < 10}
                        Item = Item.sortedBy { it -> it.xcent }
                        if (index == 0){
                            valFlag = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                        }else if(index == 1){
                            var finalValue = (Item.joinToString (separator = "") { it -> "${it.classId}" }).toInt()
                            if(abs(100 - finalValue) < abs(100 - valFlag)){
                                analysisList[3] = valFlag.toFloat()
                                analysisList[4] = finalValue.toFloat()
                            }else if (abs(100 - finalValue) > abs(100 - valFlag)){
                                analysisList[4] = valFlag.toFloat()
                                analysisList[3] = finalValue.toFloat()
                            }
                        }
                    }
                }
            }
        }
        return analysisList
    }
}