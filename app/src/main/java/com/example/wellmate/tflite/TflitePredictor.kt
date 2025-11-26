package com.example.wellmate.tflite

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TflitePredictor(
    context: Context,
    modelFilename: String = "health_model_fp16.tflite",
    preprocFile: String = "preproc_for_android.json"
) {

    private val interpreter: Interpreter
    private val featureOrder: List<String>
    private val means: FloatArray
    private val stds: FloatArray

    init {
        interpreter = Interpreter(loadModelFile(context, modelFilename))

        val json = loadJson(context, preprocFile)

        // ---- READ FEATURE ORDER (correct key) ----
        val inputOrderJson = json.getJSONArray("input_order")
        featureOrder = List(inputOrderJson.length()) { i ->
            inputOrderJson.getString(i)
        }

        // ---- READ NUMERIC MEANS/STDS (first 12 features) ----
        val numMeansJson = json.getJSONArray("numeric_means")
        val numStdsJson = json.getJSONArray("numeric_stds")

        val numericCount = numMeansJson.length() // 12

        // ---- Extend means/stds to match full feature order (16 features) ----
        val fullMeans = FloatArray(featureOrder.size)
        val fullStds = FloatArray(featureOrder.size)

        // Fill numeric part
        for (i in 0 until numericCount) {
            fullMeans[i] = numMeansJson.getDouble(i).toFloat()
            fullStds[i] = numStdsJson.getDouble(i).toFloat()
        }

        // Fill binary part: no scaling → mean=0, std=1
        for (i in numericCount until featureOrder.size) {
            fullMeans[i] = 0f
            fullStds[i] = 1f
        }

        means = fullMeans
        stds = fullStds
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        val fis = FileInputStream(fd.fileDescriptor)
        return fis.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    private fun loadJson(context: Context, filename: String): JSONObject {
        val stream = context.assets.open(filename)
        val text = BufferedReader(InputStreamReader(stream)).readText()
        return JSONObject(text)
    }

    fun predict(input: Map<String, Any>): Float {
        val rawVector = FloatArray(featureOrder.size)

        // Build raw features from input map
        for (i in featureOrder.indices) {
            val key = featureOrder[i]
            val value = when (val v = input[key]) {
                is Number -> v.toFloat()
                is String -> v.toFloatOrNull() ?: 0f
                else -> 0f
            }
            rawVector[i] = value
        }

        // Apply scaling
        val processed = FloatArray(rawVector.size) { i ->
            val std = stds[i]
            if (std == 0f) rawVector[i] - means[i] else (rawVector[i] - means[i]) / std
        }

        val inputBatch = arrayOf(processed)
        val output = Array(1) { FloatArray(1) }

        interpreter.run(inputBatch, output)

        return output[0][0].coerceIn(0f, 100f)
    }

    fun close() {
        interpreter.close()
    }
}
