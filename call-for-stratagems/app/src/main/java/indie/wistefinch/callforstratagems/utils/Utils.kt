package indie.wistefinch.callforstratagems.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.Random
import javax.net.ssl.HttpsURLConnection


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

        /**
         * Download file.
         */
        suspend fun download(url: String, path: String, deleteIfExist: Boolean = true) {
            withContext(Dispatchers.IO) {
                val connection: HttpsURLConnection ?
                try {
                    val file = File(path)
                    if (!file.getParentFile()?.exists()!!) {
                        file.getParentFile()?.mkdirs()
                    }
                    if (file.exists()) {
                        if (deleteIfExist) {
                            file.delete()
                        }
                        else {
                            return@withContext
                        }
                    }
                    connection = (URL(url).openConnection() as HttpsURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10 * 1000
                        readTimeout = 10 * 1000
                    }
                    val bytes = connection.inputStream.readBytes()
                    connection.inputStream.close()
                    file.writeBytes(bytes)
                }
                catch (e: Exception) {
                    throw e
                }
            }
        }

        /**
         * Download file and convert to string.
         */
        suspend fun downloadToStr(url: String): String {
            var str = ""
            withContext(Dispatchers.IO) {
                val connection: HttpsURLConnection ?
                try {
                    connection = (URL(url).openConnection() as HttpsURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10 * 1000
                        readTimeout = 10 * 1000
                    }
                    val bytes = connection.inputStream.readBytes()
                    connection.inputStream.close()
                    str = bytes.decodeToString()
                }
                catch (e: Exception) {
                    throw e
                }
            }
            return str
        }

        fun dpToPx(context: Context, dps: Int): Int {
            return Math.round(context.resources.displayMetrics.density * dps)
        }
    }
}