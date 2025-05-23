package com.pierfrancescosoffritti.youtubeplayer.player;

import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerListener;

/**
 * Use this interface to control the playback of YouTube videos and to listen to their events.
 */
public interface YouTubePlayer {
    /**
     * Loads and automatically plays the video.
     * @param videoId id of the video
     * @param startSeconds the time from which the video should start playing
     */
    void loadVideo(final String videoId, final float startSeconds);

    /**
     * Loads the specified video's thumbnail and prepares the player to play the video. Does not automatically play the video.
     * @param videoId id of the video
     * @param startSeconds the time from which the video should start playing
     */
    void cueVideo(final String videoId, final float startSeconds);

    /**
     * Loads the video playlist.
     * @param playlist youtube playlist id
     * @param startIndex the first video start playing
     */
    void loadPlaylist(String playlist, int startIndex);

    void loadPlaylist_videoIds(String videoIds);

    void play();
    void pause();

    void nextVideo();
    void previousVideo();

    void mute();
    void unMute();

    /**
     * @param volumePercent Integer between 0 and 100
     */
    void setVolume(final int volumePercent);

    /**
     *
     * @param time The absolute time in seconds to seek to
     */
    void seekTo(final float time);

    /**
     *
     * @param time a signed advance time in seconds to seek to in backward or forward
     */
    void advanceTo(final float time);

    void setPlaybackRate(final float rate);

    void getVideoUrl();

    boolean addListener(YouTubePlayerListener listener);
    boolean removeListener(YouTubePlayerListener listener);
}
