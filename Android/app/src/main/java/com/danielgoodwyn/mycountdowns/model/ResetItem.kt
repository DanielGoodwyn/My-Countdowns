package com.danielgoodwyn.mycountdowns.model

import java.util.UUID

data class ResetItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var resetTime: Long = 0L,
    var hexColor: String = "#0000FF",
    var imageDataBase64: String? = null,
    var orderIndex: Int = 0,
    var profileId: String = "[DEFAULT]"
)
