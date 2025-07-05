package com.k2fsa.sherpa.ncnn

import android.content.Context
import android.content.res.AssetManager
import androidx.annotation.Keep
import indie.wistefinch.callforstratagems.Constants

@Keep
data class FeatureExtractorConfig(
    var sampleRate: Float,
    var featureDim: Int,
)

@Keep
data class ModelConfig(
    var encoderParam: String,
    var encoderBin: String,
    var decoderParam: String,
    var decoderBin: String,
    var joinerParam: String,
    var joinerBin: String,
    var tokens: String,
    var numThreads: Int = 1,
    var useGPU: Boolean = true, // If there is a GPU and useGPU true, we will use GPU
)

@Keep
data class DecoderConfig(
    var method: String = "modified_beam_search", // valid values: greedy_search, modified_beam_search
    var numActivePaths: Int = 4, // used only by modified_beam_search
)

@Keep
data class RecognizerConfig(
    var featConfig: FeatureExtractorConfig,
    var modelConfig: ModelConfig,
    var decoderConfig: DecoderConfig,
    var enableEndpoint: Boolean = true,
    var rule1MinTrailingSilence: Float = 2.4f,
    var rule2MinTrailingSilence: Float = 1.0f,
    var rule3MinUtteranceLength: Float = 30.0f,
    var hotwordsFile: String = "",
    var hotwordsScore: Float = 1.5f,
)

@Keep
class SherpaNcnn(
    var config: RecognizerConfig,
    assetManager: AssetManager? = null,
) {
    private val ptr: Long

    init {
        ptr = if (assetManager != null) {
            newFromAsset(assetManager, config)
        } else {
            newFromFile(config)
        }
    }

    protected fun finalize() {
        delete(ptr)
    }

    fun acceptSamples(samples: FloatArray) =
        acceptWaveform(ptr, samples = samples, sampleRate = config.featConfig.sampleRate)

    fun isReady() = isReady(ptr)

    fun decode() = decode(ptr)

    fun inputFinished() = inputFinished(ptr)
    fun isEndpoint(): Boolean = isEndpoint(ptr)
    fun reset(recreate: Boolean = false) = reset(ptr, recreate = recreate)

    val text: String
        get() = getText(ptr)

    private external fun newFromAsset(
        assetManager: AssetManager,
        config: RecognizerConfig,
    ): Long

    private external fun newFromFile(
        config: RecognizerConfig,
    ): Long

    private external fun delete(ptr: Long)
    private external fun acceptWaveform(ptr: Long, samples: FloatArray, sampleRate: Float)
    private external fun inputFinished(ptr: Long)
    private external fun isReady(ptr: Long): Boolean
    private external fun decode(ptr: Long)
    private external fun isEndpoint(ptr: Long): Boolean
    private external fun reset(ptr: Long, recreate: Boolean)
    private external fun getText(ptr: Long): String

    companion object {
        init {
            System.loadLibrary("sherpa-ncnn-jni")
        }
    }
}

@Keep
fun getFeatureExtractorConfig(
    sampleRate: Float,
    featureDim: Int
): FeatureExtractorConfig {
    return FeatureExtractorConfig(
        sampleRate = sampleRate,
        featureDim = featureDim,
    )
}

@Keep
fun getDecoderConfig(method: String, numActivePaths: Int): DecoderConfig {
    return DecoderConfig(method = method, numActivePaths = numActivePaths)
}

@Keep
fun getModelConfig(model: String, useGPU: Boolean, context: Context? = null): ModelConfig {
    return if (context == null) {
        val modelDir = "models/$model"
        ModelConfig(
            encoderParam = "$modelDir/encoder_jit_trace-pnnx.ncnn.param",
            encoderBin = "$modelDir/encoder_jit_trace-pnnx.ncnn.bin",
            decoderParam = "$modelDir/decoder_jit_trace-pnnx.ncnn.param",
            decoderBin = "$modelDir/decoder_jit_trace-pnnx.ncnn.bin",
            joinerParam = "$modelDir/joiner_jit_trace-pnnx.ncnn.param",
            joinerBin = "$modelDir/joiner_jit_trace-pnnx.ncnn.bin",
            tokens = "$modelDir/tokens.txt",
            numThreads = 1,
            useGPU = useGPU,
        )
    } else {
        val modelDir = context.filesDir.path + Constants.PATH_ASR_MODELS + model
        return ModelConfig(
            encoderParam = "$modelDir/encoder_jit_trace-pnnx.ncnn.param",
            encoderBin = "$modelDir/encoder_jit_trace-pnnx.ncnn.bin",
            decoderParam = "$modelDir/decoder_jit_trace-pnnx.ncnn.param",
            decoderBin = "$modelDir/decoder_jit_trace-pnnx.ncnn.bin",
            joinerParam = "$modelDir/joiner_jit_trace-pnnx.ncnn.param",
            joinerBin = "$modelDir/joiner_jit_trace-pnnx.ncnn.bin",
            tokens = "$modelDir/tokens.txt",
            numThreads = 1,
            useGPU = useGPU,
        )
    }
}
