package com.danielgoodwyn.mycountdowns.model

import java.util.UUID

data class ColorSwatch(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var hexColor: String = "#0000FF",
    var profileId: String = "[DEFAULT]"
)
