package com.ayush.vallpaper.domain.repository

import com.ayush.vallpaper.domain.model.Track

interface TrackRepository {

    fun getCurrentTrack(): Track

    fun getHistory(): List<Track>
}