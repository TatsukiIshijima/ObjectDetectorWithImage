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

    private fun drawPredictions(canvas: Canvas, predictions: ArrayList<Recognition>,
                                leftPoint: Float, topPoint: Float) {
        paint.strokeWidth = 5.0f
        paint.style = Paint.Style.STROKE
        for (i in 0 until predictions.count()) {
            paint.color = selectColor(i)
            val left = leftPoint + predictions[i].location.left
            val top = topPoint + predictions[i].location.top
            val right = leftPoint + predictions[i].location.right
            val bottom = topPoint + predictions[i].location.bottom
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }

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