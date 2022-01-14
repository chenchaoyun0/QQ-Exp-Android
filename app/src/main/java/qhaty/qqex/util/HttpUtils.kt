package qhaty.qqex.util

import com.alibaba.fastjson.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ResponseInfo(var message: String)


val contenType: MediaType = "application/json".toMediaType()
val urlAPI: String = "http://www.shopbop.ink/pig/uploadMsg"
val client = OkHttpClient.Builder()
    .connectTimeout(3600, TimeUnit.SECONDS)
    .callTimeout(3600, TimeUnit.SECONDS)
    .pingInterval(5, TimeUnit.SECONDS)
    .readTimeout(3600, TimeUnit.SECONDS)
    .writeTimeout(3600, TimeUnit.SECONDS)
    .build();


suspend fun callApi(chatListObject: ChatListObject) {
    withContext(Dispatchers.Default) {
        var toJSON = JSON.toJSONString(chatListObject)
        val requestBody: RequestBody = toJSON.toRequestBody(contenType)
        var request = Request.Builder()
            .url(urlAPI)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                println("result: $result")
            }

            override fun onFailure(call: Call, e: IOException) {
                println("Failed request api :( " + e.message)
            }
        })
    }

}