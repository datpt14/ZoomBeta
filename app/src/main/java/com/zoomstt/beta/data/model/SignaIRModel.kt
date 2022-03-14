package com.zoomstt.beta.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SignaIRTranslate(
    val text: String = "",
    val language: String = "",
    val targetLanguage: String = ""
) : Serializable

data class SignaIRMessage(
    val userName: String = "",
    val oriLang: String = "",
    val msg: String = "",
) : Serializable

data class SignaIRTranslateModel(
    @SerializedName("text")
    val text: String = "",
    @SerializedName("language")
    val language: String = "",
    @SerializedName("targetLanguage")
    val targetLanguage: String = ""
) : Serializable

data class SignaIRTranslateResponse(
    @SerializedName("originalText")
    val originalText: String = "",
    @SerializedName("translatedText")
    val translatedText: String = "",
    @SerializedName("detectedSourceLanguage")
    val detectedSourceLanguage: Any? = null,
    @SerializedName("specifiedSourceLanguage")
    val specifiedSourceLanguage: String = "",
    @SerializedName("targetLanguage")
    val targetLanguage: String = "",
    @SerializedName("model")
    val model: Any? = null,
    @SerializedName("modelName")
    val modelName: Any? = null
) : Serializable