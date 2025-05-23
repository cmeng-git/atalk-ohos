package com.pierfrancescosoffritti.youtubeplayer.player.utils;

import ohos.aafwk.ability.Lifecycle;

import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;

public class YouTubePlayerUtils {

    /**
     * Calls [YouTubePlayer.cueVideo] or [YouTubePlayer.loadVideo] depending on which one is more appropriate.
     * If it can't decide, calls [YouTubePlayer.cueVideo] by default.
     * <p>
     * In most cases you want to avoid calling [YouTubePlayer.loadVideo] if the Activity/Fragment is not in the foreground.
     * This function automates these checks for you.
     *
     * @param lifecycle the lifecycle of the Activity or Fragment containing the YouTubePlayerView.
     * @param videoId id of the video.
     * @param startSeconds the time from which the video should start playing.
     */
    public static void loadOrCueVideo(YouTubePlayer youTubePlayer, Lifecycle lifecycle, String videoId, float startSeconds) {
        loadOrCueVideo(youTubePlayer, lifecycle.getLifecycleState() == Lifecycle.Event.ON_ACTIVE, videoId, startSeconds);
    }

    public static void loadOrCueVideo(YouTubePlayer youTubePlayer, boolean canLoad, String videoId, float startSeconds) {
        if (canLoad)
            youTubePlayer.loadVideo(videoId, startSeconds);
        else
            youTubePlayer.cueVideo(videoId, startSeconds);
    }
}