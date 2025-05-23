package com.pierfrancescosoffritti.youtubeplayer.player.views;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.agp.components.webengine.WebAgent;
import ohos.agp.components.webengine.WebConfig;
import ohos.agp.components.webengine.WebView;
import ohos.app.Context;
import ohos.media.image.PixelMap;

import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayerBridge;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.youtubeplayer.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.youtubeplayer.player.utils.Utils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.webview.MyWebViewClient;

import timber.log.Timber;

/**
 * WebView implementing the actual YouTube Player
 */
public class WebViewYouTubePlayer extends WebView implements YouTubePlayer, YouTubePlayerBridge.YouTubePlayerBridgeCallbacks {

    private YouTubePlayerListener youTubePlayerInitListener;

    private final Set<YouTubePlayerListener> youTubePlayerListeners = new HashSet<>();
    protected boolean isBackgroundPlaybackEnabled = false;

    public WebViewYouTubePlayer(Context context) {
        this(context, null);
    }

    public WebViewYouTubePlayer(Context context, AttrSet attrs) {
        super(context, attrs);
    }

    protected void initialize(YouTubePlayerListener initListener, IFramePlayerOptions playerOptions) {
        youTubePlayerInitListener = initListener;
        initWebView(playerOptions == null ? IFramePlayerOptions.getDefault() : playerOptions);
    }

    @Override
    public void onYouTubeIFrameAPIReady() {
        // youTubePlayerInitListener(this);
    }

    @Override
    public YouTubePlayer getInstance() {
        return this;
    }

    @Override
    public void loadVideo(final String videoId, final float startSeconds) {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:loadVideo('" + videoId + "', " + startSeconds + ")");
        });
    }

    @Override
    public void cueVideo(final String videoId, final float startSeconds) {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:cueVideo('" + videoId + "', " + startSeconds + ")");
        });
    }

    @Override
    public void loadPlaylist(final String playlist, final int startIndex) {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:loadPlaylist('" + playlist + "', " + startIndex + ")");
        });
    }

    @Override
    public void loadPlaylist_videoIds(final String videoIds) {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:loadPlaylist_videoIds('" + videoIds + ")");
        });
    }

    @Override
    public void play() {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:playVideo()");
        });
    }

    @Override
    public void pause() {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:pauseVideo()");
        });
    }

    @Override
    public void nextVideo() {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:nextVideo()");
        });
    }

    @Override
    public void previousVideo() {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:previousVideo()");
        });
    }

    @Override
    public void mute() {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:mute()");
        });
    }

    @Override
    public void unMute() {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:unMute()");
        });
    }

    @Override
    public void setVolume(final int volumePercent) {
        if (volumePercent < 0 || volumePercent > 100)
            throw new IllegalArgumentException("Volume must be between 0 and 100");

        BaseAbility.runOnUiThread(() -> {
            load("javascript:setVolume(" + volumePercent + ")");
        });
    }

    /**
     * secondsThe parameter identifies the time the player should advance.
     * Unless the player has downloaded the part of the video that the user is looking for,
     * the player will advance to the closest key frame before that time.
     * If the allowSeekAhead parameter specifies a time outside of the currently buffered video data,
     * the seconds parameter determines whether the player sends a new request to the server.
     */
    @Override
    public void seekTo(final float time) {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:seekTo(" + time + ")");
        });
    }

    @Override
    public void advanceTo(final float time) {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:advanceTo(" + time + ")");
        });
    }

    @Override
    public void setPlaybackRate(final float rate) {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:setPlaybackRate(" + rate + ")");
        });
    }

    @Override
    public void getVideoUrl() {
        BaseAbility.runOnUiThread(() -> {
            load("javascript:getVideoUrl()");
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        youTubePlayerListeners.clear();
        BaseAbility.runOnUiThread(() -> {
            BaseAbility.uiHandler.removeAllEvent();;
        });
    }

    @Override
    public Collection<YouTubePlayerListener> getListeners() {
        return Collections.unmodifiableCollection(new HashSet<>(youTubePlayerListeners));
    }

    @Override
    public boolean addListener(YouTubePlayerListener listener) {
        if (listener == null) {
            Timber.e("YouTubePlayer", "null YouTubePlayerListener not allowed.");
            return false;
        }

        return youTubePlayerListeners.add(listener);
    }

    @Override
    public boolean removeListener(YouTubePlayerListener listener) {
        return youTubePlayerListeners.remove(listener);
    }

    private void initWebView(IFramePlayerOptions playerOptions) {
        WebConfig settings = this.getWebConfig();
        settings.setJavaScriptPermit(true);
        settings.setMediaAutoReplay(false);
        settings.setWebCachePriority(WebConfig.PRIORITY_NETWORK_ONLY);

        this.addJavascriptInterface(new YouTubePlayerBridge(this), "YouTubePlayerBridge");

        final String htmlPage = Utils
                .readHTMLFromUTF8File(resources.openRawResource(ResourceTable.Rawfile_ayp_youtube_player))
                .replace("<<injectedPlayerVars>>", playerOptions.toString());

        this.load(playerOptions.getOrigin(), htmlPage, "text/html", "utf-8", null);

        // if the video's thumbnail is not in memory, show a black screen
//        this.setWebAgent(new WebAgent() {
//            @Override
//            public PixelMap getDefaultVideoPoster() {
//                PixelMap result = super.getDefaultVideoPoster();
//
//                if (result == null)
//                    return PixelMap.createBitmap(1, 1, PixelMap.Config.RGB_565);
//                else
//                    return result;
//            }
//        });
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (isBackgroundPlaybackEnabled && (visibility == Component.HIDE || visibility == Component.INVISIBLE))
            return;

        super.onWindowVisibilityChanged(visibility);
    }
}
