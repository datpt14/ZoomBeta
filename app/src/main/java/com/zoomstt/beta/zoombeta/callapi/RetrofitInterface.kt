package com.zoomstt.beta.zoombeta.callapi

import com.zoomstt.beta.data.model.SignaIRTranslateModel
import com.zoomstt.beta.data.model.SignaIRTranslateResponse
import com.zoomstt.beta.zoombeta.callapi.RetrofitInstance.Companion.CONTENT_TYPE_JSON
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RetrofitInterface {
    @Headers(CONTENT_TYPE_JSON)
    @POST("/api/Translate/translate")
    fun translate(@Body translateObject: SignaIRTranslateModel): Call<SignaIRTranslateResponse>?
}