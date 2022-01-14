package qhaty.qqex.util

import com.alibaba.fastjson.JSON
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

data class ResponseInfo(var message: String)


val contenType: MediaType = "application/json".toMediaType()
val urlAPI: String = "http://www.shopbop.ink/pig/uploadMsg"
val client = OkHttpClient()

fun callApi(chatListObject: ChatListObject) {
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