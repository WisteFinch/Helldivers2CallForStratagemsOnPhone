package indie.wistefinch.callforstratagems.utils

import kotlinx.coroutines.*
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DownloadService(
    private val url: String,
    private val onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    private val onComplete: () -> Unit = {},
    private val onError: (Exception) -> Unit = { _ -> }
) {
    private val client = OkHttpClient()
    private var job: Job? = null

    fun downloadToFile(scope: CoroutineScope, path: String) {
        job = scope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("Err: Code ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Err: Body empty")
                    val totalLength = body.contentLength()

                    val file = File(path)
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
                                    onProgress(downloaded, totalLength)
                                    lastUpdateTime = currentTime
                                }
                            }
                        }
                    }
                    if (isActive) onProgress(downloaded, totalLength)
                    if (isActive) onComplete()
                }
            } catch (e: Exception) {
                if (isActive) onError(e)
            }
        }
    }

    suspend fun downloadAsStr(): String {
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