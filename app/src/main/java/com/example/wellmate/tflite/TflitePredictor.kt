package com.example.wellmate.tflite

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
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

        // Try to read input_order (fallbacks supported)
        featureOrder = when {
            json.has("input_order") -> {
                val arr = json.getJSONArray("input_order")
                List(arr.length()) { i -> arr.getString(i) }
            }
            json.has("feature_order") -> {
                val arr = json.getJSONArray("feature_order")
                List(arr.length()) { i -> arr.getString(i) }
            }
            else -> throw IllegalArgumentException("preproc json missing 'input_order' or 'feature_order'")
        }

        // numeric means/stds arrays correspond to numeric_features length (usually first N)
        val numericMeans = if (json.has("numeric_means")) json.getJSONArray("numeric_means") else null
        val numericStds = if (json.has("numeric_stds")) json.getJSONArray("numeric_stds") else null

        val numericCount = numericMeans?.length() ?: 0

        // prepare full means/stds to match featureOrder length
        val fm = FloatArray(featureOrder.size)
        val fs = FloatArray(featureOrder.size)

        if (numericMeans != null && numericStds != null) {
            // fill first numericCount
            for (i in 0 until numericCount) {
                fm[i] = numericMeans.getDouble(i).toFloat()
                fs[i] = numericStds.getDouble(i).toFloat()
            }
            // remaining (binary features) -> mean 0 std 1
            for (i in numericCount until featureOrder.size) {
                fm[i] = 0f
                fs[i] = 1f
            }
        } else {
            // fallback: zero mean, unit std
            for (i in fm.indices) {
                fm[i] = 0f
                fs[i] = 1f
            }
        }

        means = fm
        stds = fs

        Log.d("TFLITE_DEBUG", "FEATURE ORDER = $featureOrder")
        Log.d("TFLITE_DEBUG", "MEANS = ${means.joinToString()}")
        Log.d("TFLITE_DEBUG", "STDS  = ${stds.joinToString()}")
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

    /**
     * input: map of featureName -> numeric value (Float/Double/Int)
     * returns: predicted float in [0,100]
     */
    fun predict(input: Map<String, Any>): Float {
        // build raw vector according to featureOrder
        val raw = FloatArray(featureOrder.size)
        for (i in featureOrder.indices) {
            val key = featureOrder[i]
            val v = input[key]
            raw[i] = when (v) {
                is Number -> v.toFloat()
                is String -> v.toFloatOrNull() ?: 0f
                else -> 0f
            }
        }

        // Log raw
        Log.d("TFLITE_DEBUG", "RAW VECTOR = ${raw.joinToString()}")

        // standardize using means/stds
        val processed = FloatArray(raw.size) { i ->
            val std = stds[i]
            if (std == 0f) raw[i] - means[i] else (raw[i] - means[i]) / std
        }

        Log.d("TFLITE_DEBUG", "PROCESSED VECTOR = ${processed.joinToString()}")

        // TFLite expects a batch dimension: [1, features]
        val inputBatch = arrayOf(processed)
        val output = Array(1) { FloatArray(1) }

        interpreter.run(inputBatch, output)

        val out = output[0][0]
        Log.d("TFLITE_DEBUG", "RAW MODEL OUTPUT = $out")
        val clamped = out.coerceIn(0f, 100f)
        Log.d("TFLITE_DEBUG", "CLAMPED OUTPUT = $clamped")
        return clamped
    }

    fun close() {
        try { interpreter.close() } catch (_: Exception) {}
    }
}
