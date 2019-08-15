package com.io.tatsuki.objectdetectorwithimage

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val imageView = findViewById<ImageView>(R.id.image_view)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.dog)
        imageView.setImageBitmap(bitmap)
    }
}
