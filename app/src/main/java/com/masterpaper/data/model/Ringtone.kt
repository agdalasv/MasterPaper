package com.masterpaper.data.model

data class Ringtone(
    val id: String,
    val name: String,
    val uri: String,
    val type: RingtoneType = RingtoneType.RINGTONE
)

enum class RingtoneType {
    RINGTONE,
    NOTIFICATION,
    ALARM
}