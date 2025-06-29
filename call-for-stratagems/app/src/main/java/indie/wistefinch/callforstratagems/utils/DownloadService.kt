package indie.wistefinch.callforstratagems.utils

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DownloadService(
    var onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    var onComplete: () -> Unit = {},
    var onError: (Exception) -> Unit = { _ -> }
) {
    private val client = OkHttpClient()
    private var job: Job? = null

    fun downloadToFile(url: String, path: String, scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.getParentFile()?.exists()!!) {
                    file.getParentFile()?.mkdirs()
                }
                if (file.exists()) {
                    withContext(Dispatchers.Main) {
                        if (isActive) onComplete()
                    }
                } else {
                    val request = Request.Builder().addHeader("Accept-Encoding", "*").url(url).build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("Err: Code ${response.code}")
                        }

                        val body = response.body ?: throw Exception("Err: Body empty")
                        val totalLength = body.contentLength()

                        val outputStream = FileOutputStream(file)
                        val inputStream: InputStream = body.byteStream()

                        val buffer = ByteArray(8 * 1024)
                        var bytesRead = 0
                        var downloaded = 0L
                        var lastUpdateTime = System.currentTimeMillis()

                        inputStream.use { input ->
                            outputStream.use { output ->
                                while (isActive && input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    downloaded += bytesRead

                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastUpdateTime >= 500 || downloaded == totalLength) {
                                        withContext(Dispatchers.Main) {
                                            onProgress(downloaded, totalLength)
                                        }
                                        lastUpdateTime = currentTime
                                    }
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            if (isActive) onProgress(downloaded, totalLength)
                            if (isActive) onComplete()
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isActive) onError(e)
                }
            }
        }
    }

    suspend fun downloadAsStr(url: String): String {
        var body = ""
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Err: Code ${response.code}")
                }
                body = response.body?.string() ?: throw Exception("Err: Body empty")
            }
        }
        return body
    }

    fun cancel() {
        job?.cancel()
    }
}