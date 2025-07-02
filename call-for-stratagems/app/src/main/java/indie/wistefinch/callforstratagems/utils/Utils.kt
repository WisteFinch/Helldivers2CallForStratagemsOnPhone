package indie.wistefinch.callforstratagems.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
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


        data class UrlParts(val dir: String = "", val fileName: String = "")

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
         * Get List Form Preferences
         */
        @JvmStatic
        fun getPreferenceList(p: SharedPreferences, key: String): List<String> {
            val count = p.getInt("${key}_count", 0)
            val list: MutableList<String> = emptyList<String>().toMutableList()
            for (i in 1..count) {
                list.add(p.getString("${key}_${i}", "")!!)
            }
            return list
        }

        /**
         * Set List to Preferences
         */
        @JvmStatic
        fun setPreferenceList(p: SharedPreferences, key: String, list: List<String>) {
            p.edit {
                putInt("${key}_count", list.count())
                for (i in 1..list.count()) {
                    putString("${key}_${i}", list[i - 1])
                }
            }
        }
    }
}