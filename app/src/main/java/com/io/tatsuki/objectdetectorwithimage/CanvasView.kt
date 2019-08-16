package com.io.tatsuki.objectdetectorwithimage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class CanvasView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val tag = CanvasView::class.java.simpleName

    private var paint: Paint = Paint()
    private var bitmap: Bitmap? = null

    fun showCanvas(bitmap: Bitmap) {
        Log.d(tag, "showCanvas")
        this.bitmap = Bitmap.createBitmap(bitmap)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(tag, "onDraw")
        val bitmap = this.bitmap ?: return
        /*
        paint.color = Color.argb(255, 0, 0, 255)
        paint.strokeWidth = 20.0f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(0.0f, 0.0f, width * 1.0f, height* 1.0f, paint)
        */
        val leftPoint = width / 2.0f - bitmap.width / 2.0f
        val topPoint = height / 2.0f - bitmap.height / 2.0f
        canvas.drawBitmap(bitmap, leftPoint, topPoint, paint)
    }
}