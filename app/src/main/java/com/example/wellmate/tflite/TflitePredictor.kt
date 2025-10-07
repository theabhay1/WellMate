package com.example.wellmate.tflite

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

class TflitePredictor(context: Context, modelFilename: String = "health_model_fp16.tflite") {

    private val interpreter: Interpreter
    val inputSize: Int
    val inputShape: IntArray

    init {
        // load model file from assets
        val afd: AssetFileDescriptor = context.assets.openFd(modelFilename)
        val inputStream = FileInputStream(afd.fileDescriptor)
        val channel: FileChannel = inputStream.channel
        val mapped: MappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)

        // create interpreter
        interpreter = Interpreter(mapped)

        // inspect input tensor shape & dtype
        val t = interpreter.getInputTensor(0)
        inputShape = t.shape() // usually [1, featureCount] or [featureCount]
        inputSize = if (inputShape.size >= 2) inputShape[1] else inputShape[0]
        Log.d("TFLITE", "Loaded model '$modelFilename' inputShape=${inputShape.contentToString()} dtype=${t.dataType()}")
    }

    /**
     * Predict risk value from features.
     * features: FloatArray sized exactly inputSize in the same order as feature_spec.json
     * returns: float risk in [0..100]
     */
    fun predictRisk(features: FloatArray): Float {
        require(features.size == inputSize) { "Expected $inputSize features, got ${features.size}" }
        // model expects shape [1, N] -> provide as arrayOf(features)
        val inputArray = arrayOf(features)
        val output = Array(1) { FloatArray(1) }
        interpreter.run(inputArray, output)
        val raw = output[0][0]
        val clamped = raw.coerceIn(0f, 100f)
        Log.d("TFLITE", "predictRisk raw=$raw clamped=$clamped")
        return clamped
    }

    fun close() {
        try {
            interpreter.close()
        } catch (e: Exception) {
            Log.w("TFLITE", "Error closing interpreter: ${e.message}")
        }
    }
}
