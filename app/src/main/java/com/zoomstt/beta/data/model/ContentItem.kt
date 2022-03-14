package com.zoomstt.beta.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ContentSpeechItem(
    val userName: String = "",
    val content: String = ""
) : Serializable

data class ResponseMessage(
    val userName: String = "",
    @SerializedName("language code")
    val oriLang: String = "",
    @SerializedName("message")
    val msg: String = ""
) : Serializable