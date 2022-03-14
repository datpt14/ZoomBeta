package com.zoomstt.beta.zoombeta.callapi

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

abstract class RetrofitInstance {

    companion object {
        private var retrofit: Retrofit? = null
        private val BASE_URL = "https://kedu-relay.jeyunvn.com"

        const val TYPE_JSON_UTF8 = "application/json; charset=UTF-8"
        const val CONTENT_TYPE_JSON = "Content-Type: $TYPE_JSON_UTF8"

        val gson = GsonBuilder().serializeNulls().create()

        val retrofitInstance: Retrofit?
            get() {
                if (retrofit == null) {
                    retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
                return retrofit
            }
    }

    protected fun serializeNulls(data: Any): String = gson.toJson(data)
}