package com.io.tatsuki.objectdetectorwithimage

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.FirebaseModelDataType
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions
import com.google.firebase.ml.custom.FirebaseModelInterpreter
import com.google.firebase.ml.custom.FirebaseModelOptions
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.math.exp

class MainActivity : AppCompatActivity() {

    private val tag = MainActivity::class.java.simpleName

    private val localModelName = "ssdlite_mobilenet_v2"
    private val localModelFile = "ssdlite_mobilenet_v2_coco.tflite"
    private val boxPriorsFile = "box_priors.txt"
    private val labelFile = "coco_labels_list.txt"

    private val inputImageSize = 300
    private val numResults = 1917
    private val numClasses = 91

    private val yScale = 10.0f
    private val xScale = 10.0f
    private val hScale = 5.0f
    private val wScale = 5.0f

    private var boxPriors = Array(4, { arrayOfNulls<Float>(numResults) })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val imageView = findViewById<ImageView>(R.id.image_view)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.dog)
        imageView.setImageBitmap(bitmap)

        loadBoxPriors()
        /*
        configureLocalModelSource()
        val interpreter = createInterpreter()!!
        val inputOutputOptions = createInputOutputOptions()
        val resizeBitmap = Bitmap.createScaledBitmap(bitmap, inputImageSize, inputImageSize, true)
        val batchNum = 0
        val input = Array(1) { Array(inputImageSize) { Array(inputImageSize) { FloatArray(3) } } }
        for (x in 0 until inputImageSize) {
            for (y in 0 until inputImageSize) {
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
            */
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

    /**
     * モデルの読み込み＆構築
     */
    @Throws(FirebaseMLException::class)
    private fun createInterpreter(): FirebaseModelInterpreter? {
        val options = FirebaseModelOptions.Builder()
            .setLocalModelName(localModelName)
            .build()
        return FirebaseModelInterpreter.getInstance(options)
    }

    /**
     * 入力と出力の定義
     */
    @Throws(FirebaseMLException::class)
    private fun createInputOutputOptions(): FirebaseModelInputOutputOptions {
        return FirebaseModelInputOutputOptions.Builder()
            .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, inputImageSize, inputImageSize, 3))
            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, numResults, 1, 4))
            .setOutputFormat(1, FirebaseModelDataType.FLOAT32, intArrayOf(1, numResults, numClasses))
            .build()
    }

    private fun expit(x: Float): Float {
        return (1.0f / (1.0f + exp(-x)))
    }

    /**
     * デフォルトボックス座標のテキストファイル読み込み
     */
    private fun loadBoxPriors() {
        try {
            val inputStream = assets.open(boxPriorsFile)
            val bufferReader = BufferedReader(InputStreamReader(inputStream))
            for (lineNum in 0 until 4) {
                val line = bufferReader.readLine()
                val stringTokenizer = StringTokenizer(line, ", ")
                var priorIndex = 0
                while (stringTokenizer.hasMoreTokens()) {
                    val token = stringTokenizer.nextToken()
                    try {
                        val number = token.toFloat()
                        boxPriors[lineNum][priorIndex] = number
                        //Log.d(tag, "LoadBoxPriors: Index=" + priorIndex + ", Number=" + number)
                        priorIndex++
                    } catch (e: NumberFormatException) {
                        Log.e(tag, e.message)
                    }
                }
                if (priorIndex != numResults) {
                    inputStream.close()
                    bufferReader.close()
                    throw RuntimeException("BoxPrior length mismatch : " + priorIndex + " vs " + numResults)
                }
            }

            Log.d(tag, "Loaded box priors.")

            inputStream.close()
            bufferReader.close()
        }
        catch (e: IOException) {
            Log.e(tag, e.message)
        }
    }

    private fun loadLabels() {

    }
}
