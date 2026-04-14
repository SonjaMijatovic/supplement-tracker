package com.sonja.tracker.domain.model

data class Item(
    val id: Long,
    val name: String,
    val reminderWeekdayTime: String?,
    val reminderWeekendTime: String?,
    val imagePath: String?,
    val iconId: String?
)
