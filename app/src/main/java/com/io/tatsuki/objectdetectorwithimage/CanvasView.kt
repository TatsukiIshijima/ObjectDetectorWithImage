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

    fun showCanvas(bitmap: Bitmap, predictions: ArrayList<Recognition>) {
        Log.d(tag, "showCanvas")
        this.bitmap = Bitmap.createBitmap(bitmap)
        this.predictions = predictions
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(tag, "onDraw")
        val bitmap = this.bitmap ?: return

        val leftPoint = width / 2.0f - bitmap.width / 2.0f
        val topPoint = height / 2.0f - bitmap.height / 2.0f
        canvas.drawBitmap(bitmap, leftPoint, topPoint, paint)

        drawPredictions(canvas, predictions, leftPoint, topPoint)
    }

    /**
     * 予測値からバウンディングボックスと認識率の描画
     */
    private fun drawPredictions(canvas: Canvas, predictions: ArrayList<Recognition>,
                                leftPoint: Float, topPoint: Float) {
        for (i in 0 until predictions.count()) {
            // バウンディングボックスの描画
            paint.color = selectColor(i)
            paint.strokeWidth = 5.0f
            paint.style = Paint.Style.STROKE
            val left = leftPoint + predictions[i].location.left
            val top = topPoint + predictions[i].location.top
            val right = leftPoint + predictions[i].location.right
            val bottom = topPoint + predictions[i].location.bottom
            canvas.drawRect(left, top, right, bottom, paint)

            // 認識率の描画
            paint.strokeWidth = 1.0f
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.textSize = 17.5f
            canvas.drawText(
                "${predictions[i].label}:${String.format("%.0f", predictions[i].confidence)}%",
                left,
                top + 17.5f,
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