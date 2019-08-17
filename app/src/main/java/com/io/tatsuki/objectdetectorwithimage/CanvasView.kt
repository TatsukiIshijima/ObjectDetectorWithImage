package com.io.tatsuki.objectdetectorwithimage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View


class CanvasView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val tag = CanvasView::class.java.simpleName

    private var paint: Paint = Paint()
    private var bitmap: Bitmap? = null
    private var predictions = ArrayList<Recognition>()
    private var inputImageWidth = 0
    private var inputImageHeight = 0

    fun showCanvas(bitmap: Bitmap, predictions: ArrayList<Recognition>, inputImageWidth: Int, inputImageHeight: Int) {
        Log.d(tag, "showCanvas")
        this.bitmap = Bitmap.createBitmap(bitmap)
        this.predictions = predictions
        this.inputImageWidth = inputImageWidth
        this.inputImageHeight = inputImageHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(tag, "onDraw")
        val bitmap = this.bitmap ?: return
        val resizedBitmap = resizeWithWidthRatio(bitmap)
        val leftPoint = width / 2.0f - resizedBitmap.width / 2.0f
        val topPoint = height / 2.0f - resizedBitmap.height / 2.0f
        canvas.drawBitmap(resizedBitmap, leftPoint, topPoint, paint)
        drawPredictions(canvas, resizedBitmap, predictions, leftPoint, topPoint)
    }

    /**
     * CanvasViewの横幅に合わせてリサイズ
     */
    private fun resizeWithWidthRatio(bitmap: Bitmap): Bitmap {
        val ratio = width.toFloat() / bitmap.width.toFloat()
        val newHeight = bitmap.height * ratio
        return Bitmap.createScaledBitmap(bitmap, width, newHeight.toInt(), true)
    }

    /**
     * 予測値からバウンディングボックスと認識率の描画
     */
    private fun drawPredictions(canvas: Canvas, resizedBitmap: Bitmap, predictions: ArrayList<Recognition>,
                                leftPoint: Float, topPoint: Float) {

        val widthRatio = resizedBitmap.width.toFloat() / this.inputImageWidth.toFloat()
        val heightRatio = resizedBitmap.height.toFloat() / this.inputImageHeight.toFloat()

        for (i in 0 until predictions.count()) {
            // バウンディングボックスの描画
            paint.color = selectColor(i)
            paint.strokeWidth = 5.0f
            paint.style = Paint.Style.STROKE
            val left = leftPoint + (predictions[i].location.left * widthRatio)
            val top = topPoint + (predictions[i].location.top * heightRatio)
            val right = leftPoint + (predictions[i].location.right * widthRatio)
            val bottom = topPoint + (predictions[i].location.bottom * heightRatio)
            canvas.drawRect(left, top, right, bottom, paint)

            // 認識率の描画
            paint.strokeWidth = 1.0f
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.textSize = 20.0f
            canvas.drawText(
                "${predictions[i].label}:${String.format("%.0f", predictions[i].confidence)}%",
                left,
                top + 20.0f,
                paint)
        }
    }

    /**
     * 色の選択
     */
    private fun selectColor(index: Int) : Int {
        val colors = arrayOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA)
        return colors[index % colors.size]
    }
}