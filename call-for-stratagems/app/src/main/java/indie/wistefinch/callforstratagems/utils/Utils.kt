package indie.wistefinch.callforstratagems.utils

import android.content.Context
import indie.wistefinch.callforstratagems.Constants.PATH_ASR_MODELS
import java.io.File
import java.net.URL
import java.util.Random


/**
 * Provide basic tools。
 */
class Utils {
    companion object {
        /**
         * Get random string.
         */
        @JvmStatic
        fun getRandomString(len: Int): String {
            val base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            val random = Random()
            val sb = StringBuffer()
            for (i in 0 until len) {
                val number: Int = random.nextInt(base.length)
                sb.append(base[number])
            }
            return sb.toString()
        }


        data class UrlParts(val dir: String, val fileName: String)

        /**
         * For the given URL, retrieve the directory URL.
         */
        @JvmStatic
        fun parseUrl(urlStr: String, defaultFileName: String = ""): UrlParts {
            val url = URL(urlStr)
            val path = url.path

            val isFile = path.contains(".") && !path.endsWith("/")
            var fileName = if (isFile) path.substringAfterLast("/") else ""
            if (fileName.isEmpty() && defaultFileName.isNotEmpty()) {
                fileName = defaultFileName
            }

            val dirPath = when {
                path.endsWith("/") -> path
                isFile -> path.substringBeforeLast("/") + "/"
                else -> "$path/"
            }

            val portPart = if (url.port != -1 && url.port != url.defaultPort) ":${url.port}" else ""
            val directoryUrl = "${url.protocol}://${url.host}$portPart$dirPath"

            return UrlParts(directoryUrl, fileName)
        }

        /**
         * Convert dip to px
         */
        @JvmStatic
        fun dpToPx(context: Context, dps: Int): Int {
            return Math.round(context.resources.displayMetrics.density * dps)
        }

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
    }
}