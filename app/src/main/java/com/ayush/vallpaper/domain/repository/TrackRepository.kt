package com.ayush.vallpaper.domain.repository

import com.ayush.vallpaper.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface TrackRepository {

    val currentTrack: StateFlow<Track?>

    val history: StateFlow<List<Track>>
}
