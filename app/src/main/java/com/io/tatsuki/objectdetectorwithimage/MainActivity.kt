package com.io.tatsuki.objectdetectorwithimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

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

    private val maxBoxes = 10
    private val minScore = 20.0f

    private var boxPriors = Array(4, { arrayOfNulls<Float>(numResults) })
    private var labels = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.dog)
        val canvasView = findViewById<CanvasView>(R.id.canvas_view)

        loadBoxPriors()
        loadLabels()

        configureLocalModelSource()
        val interpreter = createInterpreter()!!
        val inputOutputOptions = createInputOutputOptions()
        val resizeBitmap = Bitmap.createScaledBitmap(bitmap, inputImageSize, inputImageSize, true)
        val batchNum = 0
        val input = Array(1) { Array(inputImageSize) { Array(inputImageSize) { FloatArray(3) } } }
        for (x in 0 until inputImageSize) {
            for (y in 0 until inputImageSize) {
                val pixel = resizeBitmap.getPixel(x, y)
                input[batchNum][y][x][0] = (Color.red(pixel) - 127) / 255.0f
                input[batchNum][y][x][1] = (Color.green(pixel) - 127) / 255.0f
                input[batchNum][y][x][2] = (Color.blue(pixel) - 127) / 255.0f
            }
        }
        val inputs = FirebaseModelInputs.Builder()
            .add(input)
            .build()
        interpreter.run(inputs, inputOutputOptions)
            .addOnSuccessListener { result ->
                val predictions = result.getOutput<Array<Array<Array<FloatArray>>>>(0)
                val outputClasses = result.getOutput<Array<Array<FloatArray>>>(1)
                Log.d(tag, "predictions shape = (" + predictions.count().toString() + "," + predictions[0].count().toString() + ","
                        + predictions[0][0].count().toString() + "," + predictions[0][0][0].count().toString() + ")")
                Log.d(tag, "output classes shape = (" + outputClasses.count().toString() + "," + outputClasses[0].count().toString()
                        + "," + outputClasses[0][0].count().toString() + ")")

                decodeCenterSizeBoxes(predictions)

                val recognitions = ArrayList<Recognition>()

                for (i in 0 until numResults) {
                    var topClassScore = -1000f
                    var topClassScoreIndex = -1

                    // 最初のラベルはスキップ
                    for (j in 1 until numClasses) {
                        val score = expit(outputClasses[0][i][j])

                        if (score > topClassScore) {
                            topClassScoreIndex = j
                            topClassScore = score
                        }
                    }

                    if (topClassScore > 0.01f) {
                        //Log.d(tag, "TopClassScoreIndex=${topClassScoreIndex}, ${labels.get(topClassScoreIndex)} : ${outputClasses[0][i][topClassScoreIndex]}")

                        val location = RectF(
                            predictions[0][i][0][1] * inputImageSize,
                            predictions[0][i][0][0] * inputImageSize,
                            predictions[0][i][0][3] * inputImageSize,
                            predictions[0][i][0][2] * inputImageSize)
                        //Log.d(tag, "Location=(${location.left}, ${location.top}, ${location.right}, ${location.bottom})")

                        val recognition = Recognition(
                            i.toString(),
                            labels.get(topClassScoreIndex),
                            outputClasses[0][i][topClassScoreIndex],
                            location)
                        recognitions.add(recognition)
                    }
                }

                val finalPredictions = ArrayList<Recognition>()
                val suppressedPredictions = NMS(recognitions, 0.5f, maxBoxes)
                for (i in 0 until suppressedPredictions.count()) {
                    val score = 100.0f / (1.0f + exp(-suppressedPredictions[i].confidence))
                    if (score < minScore) {
                        break
                    }
                    val scoreString = String.format("%.2f", score)
                    suppressedPredictions[i].confidence = score
                    Log.d(tag, "Recognition ${suppressedPredictions[i].label} : $scoreString%\n" +
                            "(${suppressedPredictions[i].location.left}, ${suppressedPredictions[i].location.top}) " +
                            "(${suppressedPredictions[i].location.right}, ${suppressedPredictions[i].location.bottom})")
                    finalPredictions.add(suppressedPredictions[i])
                }

                canvasView.showCanvas(bitmap, finalPredictions, inputImageSize, inputImageSize)
            }
            .addOnFailureListener { e -> Log.e(tag, e.message) }
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
                    throw RuntimeException("BoxPrior length mismatch : $priorIndex vs $numResults")
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

    /**
     * ラベルファイル読み込み
     */
    private fun loadLabels() {
        try {
            val inputStream = assets.open(labelFile)
            val bufferReader = BufferedReader(InputStreamReader(inputStream))
            var label: String?
            do {
                label = bufferReader.readLine()
                if (label == null) {
                    break
                }
                labels.add(label)
                //Log.d(tag, "Label : $label")
            }
            while (true)
            Log.d(tag, "Loaded Labels.")

            inputStream.close()
            bufferReader.close()
        } catch (e: IOException) {
            Log.e(tag, e.message)
        }
    }

    /**
     * 座標のフォーマット変換
     * (cy, cx, h, w) → (ymin, xmin, ymax, xmax)
     */
    private fun decodeCenterSizeBoxes(locations: Array<Array<Array<FloatArray>>>) {
        for (i in 0 until numResults) {
            val yCenter = locations[0][i][0][0] / yScale * (boxPriors[2][i] as Float) + (boxPriors[0][i] as Float)
            val xCenter = locations[0][i][0][1] / xScale * (boxPriors[3][i] as Float) + (boxPriors[1][i] as Float)
            val h = (exp(locations[0][i][0][2] / hScale) * (boxPriors[2][i] as Float))
            val w = (exp(locations[0][i][0][3] / wScale) * (boxPriors[3][i] as Float))

            val yMin = yCenter - h / 2.0f
            val xMin = xCenter - w / 2.0f
            val yMax = yCenter + h / 2.0f
            val xMax = xCenter + w / 2.0f

            locations[0][i][0][0] = yMin
            locations[0][i][0][1] = xMin
            locations[0][i][0][2] = yMax
            locations[0][i][0][3] = xMax
        }
    }

    /**
     * IoU計算
     */
    private fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val xA = max(boxA.left, boxB.left)
        val yA = max(boxA.top, boxB.top)
        val xB = min(boxA.right, boxB.right)
        val yB = min(boxA.bottom, boxB.bottom)

        val intersectionArea = (xB - xA + 1) * (yB - yA + 1)

        val boxAArea = (boxA.right - boxA.left + 1) * (boxA.bottom - boxA.top + 1)
        val boxBArea = (boxB.right - boxB.left + 1) * (boxB.bottom - boxB.top + 1)

        return intersectionArea / (boxAArea + boxBArea - intersectionArea)
    }

    /**
     * Non-Maximum-Suppression
     */
    private fun NMS(recognitions: ArrayList<Recognition>, iouThreshold: Float, maxBoxes: Int): ArrayList<Recognition> {
        recognitions.sortByDescending { it.confidence }
        val selectedRecognitions = ArrayList<Recognition>()
        for (a in 0 until recognitions.count()) {
            if (selectedRecognitions.count() > maxBoxes) {
                break
            }
            var shouldSelect = true
            for (b in 0 until selectedRecognitions.count()) {
                if (calculateIoU(recognitions[a].location, selectedRecognitions[b].location) > iouThreshold) {
                    shouldSelect = false
                    break
                }
            }
            if (shouldSelect) {
                selectedRecognitions.add(recognitions[a])
            }
        }
        return selectedRecognitions
    }
}
