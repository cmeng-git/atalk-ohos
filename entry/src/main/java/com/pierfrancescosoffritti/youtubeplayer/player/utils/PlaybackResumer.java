package com.pierfrancescosoffritti.youtubeplayer.player.utils;

import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.AbstractYouTubePlayerListener;

/**
 * Class responsible for resuming the playback state in case of network problems.
 * eg: player is playing -> network goes out -> player stops -> network comes back -> player resumes playback automatically.
 */
public class PlaybackResumer extends AbstractYouTubePlayerListener {

    private boolean canLoad = false;
    private boolean isPlaying = false;
    private PlayerConstants.PlayerError error = null;

    private String currentVideoId;
    private String currentVideoUrl;
    private float currentSecond;

    public void resume(YouTubePlayer youTubePlayer) {
        String videoId = currentVideoId;

        if (isPlaying && error == PlayerConstants.PlayerError.HTML_5_PLAYER) {
            YouTubePlayerUtils.loadOrCueVideo(youTubePlayer, canLoad, videoId, currentSecond);
        }
        else if (!isPlaying && error == PlayerConstants.PlayerError.HTML_5_PLAYER) {
            youTubePlayer.cueVideo(videoId, currentSecond);
        }
        error = null;
    }

    @Override
    public void onStateChange(YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
        switch (state) {
            case PLAYING:
                isPlaying = true;
                return;

            case PAUSED:
            case ENDED:
                isPlaying = false;
                return;
        }
    }

    @Override
    public void onError(YouTubePlayer youTubePlayer, PlayerConstants.PlayerError error) {
        if (error == PlayerConstants.PlayerError.HTML_5_PLAYER)
            this.error = error;
    }

    @Override
    public void onCurrentSecond(YouTubePlayer youTubePlayer, float second) {
        this.currentSecond = second;
    }

    @Override
    public void onVideoId(YouTubePlayer youTubePlayer, String videoId) {
        this.currentVideoId = videoId;
    }

    @Override
    public void onVideoUrl(YouTubePlayer youTubePlayer, String videoUrl) {
        this.currentVideoUrl = videoUrl;
    }

    public void onLifecycleResume() {
        canLoad = true;
    }

    public void onLifecycleStop() {
        canLoad = false;
    }
}
