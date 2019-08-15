package com.io.tatsuki.objectdetectorwithimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*

class MainActivity : AppCompatActivity() {

    private val tag = MainActivity::class.java.simpleName
    private val localModelName = "ssdlite_mobilenet_v2"
    private val localModelFile = "ssdlite_mobilenet_v2_coco.tflite"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val imageView = findViewById<ImageView>(R.id.image_view)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.dog)
        imageView.setImageBitmap(bitmap)

        configureLocalModelSource()
        val interpreter = createInterpreter()!!
        val inputOutputOptions = createInputOutputOptions()
        val resizeBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
        val batchNum = 0
        val input = Array(1) { Array(300) { Array(300) { FloatArray(3) } } }
        for (x in 0..299) {
            for (y in 0..299) {
                val pixel = resizeBitmap.getPixel(x, y)
                input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 255.0f
                input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 255.0f
                input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 255.0f
            }
        }
        val inputs = FirebaseModelInputs.Builder()
            .add(input)
            .build()
        interpreter.run(inputs, inputOutputOptions)
            .addOnSuccessListener { result ->
                Log.d(tag, "Run Success! " + result.getOutput(0))
                val predictions = result.getOutput<Array<Array<Array<FloatArray>>>>(0)
                val outputClasses = result.getOutput<Array<Array<FloatArray>>>(1)
                Log.d(tag, "predictions shape = (" + predictions.count().toString() + "," + predictions[0].count().toString() + ","
                        + predictions[0][0].count().toString() + "," + predictions[0][0][0].count().toString() + ")")
                Log.d(tag, "output classes shape = (" + outputClasses.count().toString() + "," + outputClasses[0].count().toString()
                        + "," + outputClasses[0][0].count().toString() + ")")
            }
            .addOnFailureListener( object : OnFailureListener {
                override fun onFailure(e: Exception) {
                    Log.e(tag, e.message)
                }
            })
    }

    /**
     * ローカルモデルを構成
     */
    private fun configureLocalModelSource() {
        val localSource = FirebaseLocalModel.Builder(localModelName)
            .setAssetFilePath(localModelFile)
            .build()
        FirebaseModelManager.getInstance().registerLocalModel(localSource)
    }

    @Throws(FirebaseMLException::class)
    private fun createInterpreter(): FirebaseModelInterpreter? {
        val options = FirebaseModelOptions.Builder()
            .setLocalModelName(localModelName)
            .build()
        return FirebaseModelInterpreter.getInstance(options)
    }

    @Throws(FirebaseMLException::class)
    private fun createInputOutputOptions(): FirebaseModelInputOutputOptions {
        val inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
            .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 300, 300, 3))
            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 1917, 1, 4))
            .setOutputFormat(1, FirebaseModelDataType.FLOAT32, intArrayOf(1, 1917, 91))
            .build()
        return inputOutputOptions
    }
}
