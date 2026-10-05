package com.danielgoodwyn.mycountdowns.model

data class UserProfile(
    val uid: String,
    val email: String,
    val photoUrl: String?,
    val appName: String,
    var customPhotoBase64: String? = null,
    var ringColor: String? = null
)
