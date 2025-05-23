package com.pierfrancescosoffritti.youtubeplayer.player.listeners;

import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;

/**
 * Extend this class if you want to implement only some of the methods of {@link YouTubePlayerListener}
 */
public abstract class AbstractYouTubePlayerListener implements YouTubePlayerListener {
    @Override
    public void onReady(YouTubePlayer youTubePlayer) {
    }

    @Override
    public void onStateChange(YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
    }

    @Override
    public void onPlaybackQualityChange(YouTubePlayer youTubePlayer, PlayerConstants.PlaybackQuality playbackQuality) {
    }

    @Override
    public void onPlaybackRateChange(YouTubePlayer youTubePlayer, String playbackRate) {
    }

    @Override
    public void onError(YouTubePlayer youTubePlayer, PlayerConstants.PlayerError error) {
    }

    @Override
    public void onApiChange(YouTubePlayer youTubePlayer) {
    }

    @Override
    public void onCurrentSecond(YouTubePlayer youTubePlayer, float second) {
    }

    @Override
    public void onVideoDuration(YouTubePlayer youTubePlayer, float duration) {
    }

    @Override
    public void onVideoLoadedFraction(YouTubePlayer youTubePlayer, float loadedFraction) {
    }

    @Override
    public void onVideoId(YouTubePlayer youTubePlayer, String videoId) {
    }

    @Override
    public void onVideoUrl(YouTubePlayer youTubePlayer, String videoUrl) {
    }
}
