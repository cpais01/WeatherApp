package wit.android.test.challengeWIT.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.HttpURLConnection
import java.net.URL

object Utils {
    private const val LIMIT = 4096
    fun verifyNetworkState(context: Context):Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connMgr.allNetworks.forEach { network ->
            connMgr.getNetworkCapabilities(network).apply {
                if (this?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                    this?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true)
                    return true
            }
        }
        return false
    }

    fun getData(strUrl: String?): String? {
        val resp = StringBuilder()
        var conn: HttpURLConnection? = null
        try {
            val url = URL(strUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()
            val code: Int = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                var count = 0
                conn.inputStream.bufferedReader().forEachLine{
                    count++
                    if (count >= LIMIT)
                        return@forEachLine
                    resp.append(it + "\n")
                }
            } else {
                resp.append("Error: $code")
                return null
            }
        } catch (_: Exception) {
            return null
        } finally {
            conn?.inputStream?.close()
            conn?.disconnect()
        }
        return resp.toString()
    }
}