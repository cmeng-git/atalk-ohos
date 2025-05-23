package com.pierfrancescosoffritti.youtubeplayer.player.utils;

import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.AbstractYouTubePlayerListener;

/**
 * Utility class responsible for tracking the state of YouTubePlayer.
 * This is a YouTubePlayerListener, therefore to work it has to be added as listener to a YouTubePlayer.
 */
public class YouTubePlayerTracker extends AbstractYouTubePlayerListener {
    /**
     * @return the player state. A value from [PlayerConstants.PlayerState]
     */
    private PlayerConstants.PlayerState mState = PlayerConstants.PlayerState.UNKNOWN;
    private float currentSecond = 0.0f;
    private float videoDuration = 0.0f;;
    private String videoId = null;
    private String videoUrl = null;

    @Override
    public void onStateChange(YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
        mState = state;
    }

    @Override
    public void onCurrentSecond(YouTubePlayer youTubePlayer, float second) {
        currentSecond = second;
    }

    @Override
    public void onVideoDuration(YouTubePlayer youTubePlayer, float duration) {
        videoDuration = duration;
    }

    @Override
    public void onVideoId(YouTubePlayer youTubePlayer, String videoId) {
        this.videoId = videoId;
    }

    @Override
    public void onVideoUrl(YouTubePlayer youTubePlayer, String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
