package com.ayush.vallpaper.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val durationMs: Long = 0L
)