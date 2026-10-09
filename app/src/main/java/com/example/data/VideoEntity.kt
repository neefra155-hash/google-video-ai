package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val prompt: String,
    val imageUri: String? = null,
    val durationText: String,
    val durationSeconds: Long,
    val isInfinite: Boolean = false,
    val audioMode: String,
    val sceneMood: String,
    val cameraMotion: String,
    val visualFx: String,
    val narrationText: String,
    val createdAt: Long = System.currentTimeMillis()
)
