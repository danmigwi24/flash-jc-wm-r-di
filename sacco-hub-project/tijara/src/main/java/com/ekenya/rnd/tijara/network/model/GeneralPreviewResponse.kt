package com.ekenya.rnd.tijara.network.model


import com.google.gson.annotations.SerializedName

data class GeneralPreviewResponse(
    @SerializedName("data")
    val `data`: AirtimeData,
    @SerializedName("message")
    val message: String, // Preview
    @SerializedName("status")
    val status: Int // 1
)
    data class AirtimeData(
        @SerializedName("charges")
        val charges: Int, // 0
        @SerializedName("exerciseDuty")
        val exerciseDuty: Int, // 0
        @SerializedName("formId")
        val formId: Int // 207
    )
