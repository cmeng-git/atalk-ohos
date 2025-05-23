package com.pierfrancescosoffritti.youtubeplayer.player;

import java.util.Collection;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerListener;

/**
 * Bridge used for Javascript-Java communication.
 */
public class YouTubePlayerBridge {

    // these constant values correspond to the values in the Javascript player
    private static final String STATE_UNSTARTED = "UNSTARTED";
    private static final String STATE_ENDED = "ENDED";
    private static final String STATE_PLAYING = "PLAYING";
    private static final String STATE_PAUSED = "PAUSED";
    private static final String STATE_BUFFERING = "BUFFERING";
    private static final String STATE_CUED = "CUED";

    private static final String QUALITY_SMALL = "small";
    private static final String QUALITY_MEDIUM = "medium";
    private static final String QUALITY_LARGE = "large";
    private static final String QUALITY_HD720 = "hd720";
    private static final String QUALITY_HD1080 = "hd1080";
    private static final String QUALITY_HIGH_RES = "highres";
    private static final String QUALITY_DEFAULT = "default";

    private static final String ERROR_INVALID_PARAMETER_IN_REQUEST = "2";
    private static final String ERROR_HTML_5_PLAYER = "5";
    private static final String ERROR_VIDEO_NOT_FOUND = "100";
    private static final String ERROR_VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER1 = "101";
    private static final String ERROR_VIDEO_NOT_PLAYABLE_CONTENT_RESTRICTION = "150";

    private final YouTubePlayerBridgeCallbacks youTubePlayerOwner;

    public interface YouTubePlayerBridgeCallbacks {
        YouTubePlayer getInstance();

        Collection<YouTubePlayerListener> getListeners();

        void onYouTubeIFrameAPIReady();
    }

    public YouTubePlayerBridge(YouTubePlayerBridgeCallbacks youTubePlayer) {
        youTubePlayerOwner = youTubePlayer;
    }

    @JavascriptInterface
    public void sendYouTubeIFrameAPIReady() {
        BaseAbility.runOnUiThread(youTubePlayerOwner::onYouTubeIFrameAPIReady);
    }

    @JavascriptInterface
    public void sendReady() {
        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onReady(youTubePlayerOwner.getInstance());
        });
    }

    @JavascriptInterface
    public void sendStateChange(final String state) {
        final PlayerConstants.PlayerState playerState = parsePlayerState(state);

        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onStateChange(youTubePlayerOwner.getInstance(), playerState);
        });
    }

    @JavascriptInterface
    public void sendPlaybackQualityChange(final String quality) {
        final PlayerConstants.PlaybackQuality playbackQuality = parsePlaybackQuality(quality);

        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onPlaybackQualityChange(youTubePlayerOwner.getInstance(), playbackQuality);
        });
    }

    @JavascriptInterface
    public void sendPlaybackRateChange(final String rate) {
        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onPlaybackRateChange(youTubePlayerOwner.getInstance(), rate);
        });
    }

    @JavascriptInterface
    public void sendError(final String error) {
        final PlayerConstants.PlayerError playerError = parsePlayerError(error);

        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onError(youTubePlayerOwner.getInstance(), playerError);
        });
    }

    @JavascriptInterface
    public void sendApiChange() {
        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onApiChange(youTubePlayerOwner.getInstance());
        });
    }

    @JavascriptInterface
    public void sendVideoCurrentTime(final String seconds) {
        final float currentTimeSeconds;
        try {
            currentTimeSeconds = Float.parseFloat(seconds);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onCurrentSecond(youTubePlayerOwner.getInstance(), currentTimeSeconds);
        });
    }

    @JavascriptInterface
    public void sendVideoDuration(final String seconds) {
        final float videoDuration;
        try {
            String finalSeconds = TextUtils.isEmpty(seconds) ? "0" : seconds;
            videoDuration = Float.parseFloat(finalSeconds);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onVideoDuration(youTubePlayerOwner.getInstance(), videoDuration);
        });
    }

    @JavascriptInterface
    public void sendVideoLoadedFraction(final String fraction) {
        final float loadedFraction;
        try {
            loadedFraction = Float.parseFloat(fraction);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onVideoLoadedFraction(youTubePlayerOwner.getInstance(), loadedFraction);
        });
    }

    @JavascriptInterface
    public void sendVideoId(final String videoId) {
        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onVideoId(youTubePlayerOwner.getInstance(), videoId);
        });
    }

    @JavascriptInterface
    public void sendVideoUrl(final String videoUrl) {
        BaseAbility.runOnUiThread(() -> {
            for (YouTubePlayerListener listener : youTubePlayerOwner.getListeners())
                listener.onVideoUrl(youTubePlayerOwner.getInstance(), videoUrl);
        });
    }

    @NonNull
    private PlayerConstants.PlayerState parsePlayerState(String state) {
        PlayerConstants.PlayerState playerState;

        if (state.equalsIgnoreCase(STATE_UNSTARTED))
            playerState = PlayerConstants.PlayerState.UNSTARTED;
        else if (state.equalsIgnoreCase(STATE_ENDED))
            playerState = PlayerConstants.PlayerState.ENDED;
        else if (state.equalsIgnoreCase(STATE_PLAYING))
            playerState = PlayerConstants.PlayerState.PLAYING;
        else if (state.equalsIgnoreCase(STATE_PAUSED))
            playerState = PlayerConstants.PlayerState.PAUSED;
        else if (state.equalsIgnoreCase(STATE_BUFFERING))
            playerState = PlayerConstants.PlayerState.BUFFERING;
        else if (state.equalsIgnoreCase(STATE_CUED))
            playerState = PlayerConstants.PlayerState.VIDEO_CUED;
        else
            playerState = PlayerConstants.PlayerState.UNKNOWN;

        return playerState;
    }


    @NonNull
    private PlayerConstants.PlaybackQuality parsePlaybackQuality(String quality) {
        PlayerConstants.PlaybackQuality playbackQuality;

        if (quality.equalsIgnoreCase(QUALITY_SMALL))
            playbackQuality = PlayerConstants.PlaybackQuality.SMALL;
        else if (quality.equalsIgnoreCase(QUALITY_MEDIUM))
            playbackQuality = PlayerConstants.PlaybackQuality.MEDIUM;
        else if (quality.equalsIgnoreCase(QUALITY_LARGE))
            playbackQuality = PlayerConstants.PlaybackQuality.LARGE;
        else if (quality.equalsIgnoreCase(QUALITY_HD720))
            playbackQuality = PlayerConstants.PlaybackQuality.HD720;
        else if (quality.equalsIgnoreCase(QUALITY_HD1080))
            playbackQuality = PlayerConstants.PlaybackQuality.HD1080;
        else if (quality.equalsIgnoreCase(QUALITY_HIGH_RES))
            playbackQuality = PlayerConstants.PlaybackQuality.HIGH_RES;
        else if (quality.equalsIgnoreCase(QUALITY_DEFAULT))
            playbackQuality = PlayerConstants.PlaybackQuality.DEFAULT;
        else
            playbackQuality = PlayerConstants.PlaybackQuality.UNKNOWN;

        return playbackQuality;
    }

    @NonNull
    private PlayerConstants.PlayerError parsePlayerError(String error) {
        PlayerConstants.PlayerError playerError;

        if (error.equalsIgnoreCase(ERROR_INVALID_PARAMETER_IN_REQUEST))
            playerError = PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST;
        else if (error.equalsIgnoreCase(ERROR_HTML_5_PLAYER))
            playerError = PlayerConstants.PlayerError.HTML_5_PLAYER;
        else if (error.equalsIgnoreCase(ERROR_VIDEO_NOT_FOUND))
            playerError = PlayerConstants.PlayerError.VIDEO_NOT_FOUND;
        else if (error.equalsIgnoreCase(ERROR_VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER1))
            playerError = PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER;
        else if (error.equalsIgnoreCase(ERROR_VIDEO_NOT_PLAYABLE_CONTENT_RESTRICTION))
            playerError = PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER;
        else
            playerError = PlayerConstants.PlayerError.UNKNOWN;

        return playerError;
    }
}
