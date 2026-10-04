package com.ayush.vallpaper.data

import com.ayush.vallpaper.domain.model.Track
import com.ayush.vallpaper.domain.repository.TrackRepository

class MockTrackRepository : TrackRepository {

    override fun getCurrentTrack(): Track {
        return Track(
            id = "1",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            artworkUrl =
                "https://images.unsplash.com/photo-1519608487953-e999c86e7455"
        )
    }

    override fun getHistory(): List<Track> {
        return listOf(
            getCurrentTrack(),

            Track(
                id = "2",
                title = "After Dark",
                artist = "Mr.Kitty",
                album = "Time",
                artworkUrl =
                    "https://images.unsplash.com/photo-1519681393784-d120267933ba"
            ),

            Track(
                id = "3",
                title = "Space Song",
                artist = "Beach House",
                album = "Depression Cherry",
                artworkUrl =
                    "https://images.unsplash.com/photo-1462331940025-496dfbfc7564"
            ),

            Track(
                id = "4",
                title = "Intro",
                artist = "The xx",
                album = "xx",
                artworkUrl =
                    "https://images.unsplash.com/photo-1534791547706-0c292a91208d"
            )
        )
    }
}