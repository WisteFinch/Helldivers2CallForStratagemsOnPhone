package indie.wistefinch.callforstratagems

import java.util.Random


/**
 * Provide basic tools。
 */
class Util {
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
    }
}