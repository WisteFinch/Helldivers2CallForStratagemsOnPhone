package indie.wistefinch.callforstratagems.asr

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.k2fsa.sherpa.ncnn.RecognizerConfig
import com.k2fsa.sherpa.ncnn.SherpaNcnn
import com.k2fsa.sherpa.ncnn.getDecoderConfig
import com.k2fsa.sherpa.ncnn.getFeatureExtractorConfig
import com.k2fsa.sherpa.ncnn.getModelConfig
import indie.wistefinch.callforstratagems.Constants.PATH_ASR_MODELS
import java.io.File
import kotlin.concurrent.thread

object AsrService {
    enum class ASRErrType {
        ASR_MODEL_INIT_FAILED,
        ASR_MODEL_FILE_CHECK_FAILED,
        ASR_MIC_PERMISSION_DENIED,
    }

    private var model: SherpaNcnn? = null
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    // Microphone Config
    private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
    private const val SAMPLE_RATE_IN_HZ = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    // ASR Status
    @Volatile
    var isRecording: Boolean = false

    // Listener callback
    /**
     * Callback after generating processing results.
     */
    var onProcess: (txt: String) -> Unit = {}

    /**
     * Callback when an endpoint is detected
     */
    var onEndPoint: (txt: String) -> Unit = {}

    /**
     * Callback after recording started.
     */
    var onStarted: () -> Unit = {}

    /**
     * Callback after recording stopped.
     */
    var onStopped: () -> Unit = {}

    /**
     * Callback after exception occurred.
     */
    var onError: (e: ASRErrType) -> Unit = {}

    /**
     * Callback when model is ready.
     */
    var onReady: () -> Unit = {}


    /**
     * Check whether ths ASR model file is complete.
     */
    @JvmStatic
    fun checkAsrModelFiles(context: Context, name: String): Boolean {
        val modelsPath = context.filesDir.path + PATH_ASR_MODELS + "$name/"
        val files = listOf(
            "encoder_jit_trace-pnnx.ncnn.param",
            "encoder_jit_trace-pnnx.ncnn.bin",
            "decoder_jit_trace-pnnx.ncnn.param",
            "decoder_jit_trace-pnnx.ncnn.bin",
            "joiner_jit_trace-pnnx.ncnn.param",
            "joiner_jit_trace-pnnx.ncnn.bin",
            "tokens.txt"
        )
        for (i in files) {
            if (!File(modelsPath + i).exists()) {
                return false
            }
        }
        return true
    }

    /**
     * Init ASR model.
     */
    fun initModel(
        name: String,
        useGPU: Boolean = true,
        context: Context,
        activity: Activity,
        onProcess: (txt: String) -> Unit = {},
        onEndPoint: (txt: String) -> Unit = {},
        onStarted: () -> Unit = {},
        onStopped: () -> Unit = {},
        onError: (e: ASRErrType) -> Unit = {},
        onReady: () -> Unit = {}
    ): Boolean {
        this.onProcess = onProcess
        this.onEndPoint = onEndPoint
        this.onStarted = onStarted
        this.onStopped = onStopped
        this.onError = onError
        this.onReady = onReady

        // Check model files
        if (!checkAsrModelFiles(context, name)) {
            activity.runOnUiThread {
                onError(ASRErrType.ASR_MODEL_FILE_CHECK_FAILED)
            }
            return false
        }
        // Prepare model config
        val featConfig = getFeatureExtractorConfig(
            sampleRate = SAMPLE_RATE_IN_HZ.toFloat(),
            featureDim = 80
        )
        val modelConfig = getModelConfig(model = name, useGPU = useGPU, context = context)
        val decoderConfig = getDecoderConfig(method = "greedy_search", numActivePaths = 4)
        val config = RecognizerConfig(
            featConfig = featConfig,
            modelConfig = modelConfig,
            decoderConfig = decoderConfig,
            enableEndpoint = true,
            rule1MinTrailingSilence = 2.0f,
            rule2MinTrailingSilence = 0.8f,
            rule3MinUtteranceLength = 20.0f,
        )

        // Init model with config
        try {
            model = SherpaNcnn(
                config = config
            )
            Log.i("[ASR Service]", "Model loaded")
        } catch (e: Exception) {
            Log.e("[ASR Service]", "Failed to initialize model: $e")
            activity.runOnUiThread {
                onError(ASRErrType.ASR_MODEL_INIT_FAILED)
            }
            return false
        }
        activity.runOnUiThread {
            onReady()
        }
        return true
    }

    /**
     * Destroy ASR model.
     */
    fun destroyModel() {
        stopRecord()
        model = null
        onProcess = {}
        onEndPoint = {}
        onStarted = {}
        onStopped = {}
        onError = {}
        onReady = {}
        Log.i(
            "[ASR Service]",
            "Model destroyed"
        )
    }

    /**
     * Init microphone.
     */
    private fun initMicrophone(context: Context, activity: Activity): Boolean {
        // Require microphone permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                200
            )
            return false
        }

        val numBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        Log.i(
            "[ASR Service]",
            "buffer size in milliseconds: ${numBytes * 1000.0f / SAMPLE_RATE_IN_HZ}"
        )

        audioRecord = AudioRecord(
            AUDIO_SOURCE,
            SAMPLE_RATE_IN_HZ,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            numBytes * 2
        )
        return true
    }

    /**
     * Start microphone record.
     */
    fun startRecord(context: Context, activity: Activity): Boolean {
        val ret = initMicrophone(context, activity)
        if (!ret) {
            Log.e("[ASR Service]", "Failed to initialize microphone")
            activity.runOnUiThread {
                onError(ASRErrType.ASR_MIC_PERMISSION_DENIED)
            }
            return false
        }
        Log.i("[ASR Service]", "State: ${audioRecord?.state}")
        audioRecord!!.startRecording()

        isRecording = true
        recordingThread = thread(true) {
            model?.reset(true)
            processSamples(activity)
        }
        Log.i("[ASR Service]", "Record started")

        activity.runOnUiThread {
            onStarted()
        }
        return true
    }

    /**
     * Stop microphone record.
     */
    fun stopRecord() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.i("[ASR Service]", "Record stopped")
    }

    /**
     * Process recorded samples
     */
    private fun processSamples(activity: Activity) {
        val interval = 0.1f
        val bufferSize = (interval * SAMPLE_RATE_IN_HZ).toInt() // in samples
        val buffer = ShortArray(bufferSize)

        while (isRecording) {
            try {
                val ret = audioRecord?.read(buffer, 0, buffer.size)
                if (ret != null && ret > 0) {
                    val samples = FloatArray(ret) { buffer[it] / 32768.0f }
                    model?.acceptSamples(samples)
                    while (model?.isReady()!!) {
                        model?.decode()
                    }
                    val isEndpoint = model?.isEndpoint()!!
                    val text = model?.text!!
                    activity.runOnUiThread {
                        onProcess(text)
                    }

                    if (isEndpoint) {
                        model?.reset()
                        if (text.isNotBlank()) {
                            Log.i("[ASR Service]", "Result: $text")
                        }
                        activity.runOnUiThread {
                            onEndPoint(text)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("[ASR Service]", "Runtime exception: $e")
            }
        }
        activity.runOnUiThread {
            onStopped()
        }
    }
}