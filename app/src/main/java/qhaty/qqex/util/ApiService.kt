package qhaty.qqex.util

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    companion object {
        //此类接口的基地址
        val baseUrl = "http://www.shopbop.ink/pig/"
    }

    //请求类型 + 路由
    @POST("uploadMsg")
    fun uploadMsg(@Body body: ChatListObject): Call<ResponseInfo> //由于json采用手动解，所以没有用泛型
}