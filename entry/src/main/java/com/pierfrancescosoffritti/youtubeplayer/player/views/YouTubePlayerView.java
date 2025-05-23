package com.pierfrancescosoffritti.youtubeplayer.player.views;

import ohos.aafwk.ability.Lifecycle;
import ohos.aafwk.ability.LifecycleStateObserver;
import ohos.aafwk.content.Intent;
import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.app.Context;
import ohos.global.resource.solidxml.TypedAttribute;
import ohos.global.systemres.ResourceTable;

import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerCallback;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerFullScreenListener;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.youtubeplayer.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.youtubeplayer.player.utils.FullScreenHelper;
import com.pierfrancescosoffritti.youtubeplayer.player.utils.YouTubePlayerUtils;
import com.pierfrancescosoffritti.youtubeplayer.ui.PlayerUiController;

public class YouTubePlayerView extends SixteenByNineFrameLayout implements LifecycleStateObserver {
    private final LegacyYouTubePlayerView legacyTubePlayerView;
    private final FullScreenHelper fullScreenHelper;
    private final boolean enableAutomaticInitialization;

    public YouTubePlayerView(Context context) {
        this(context, null);
    }

    public YouTubePlayerView(Context context, AttrSet attrSet) {
        this(context, attrSet, null);
    }

    public YouTubePlayerView(Context context, AttrSet attrSet, String styleName) {
        super(context, attrSet, styleName);
        legacyTubePlayerView = new LegacyYouTubePlayerView(context);
        fullScreenHelper = new FullScreenHelper(this);

        addComponent(legacyTubePlayerView, new LayoutConfig(LayoutConfig.MATCH_PARENT, LayoutConfig.MATCH_PARENT));

        TypedAttribute typedAttr = context.getTheme().oobtainStyledAttributes(attrSet, ResourceTable.Styleable_YouTubePlayerView, 0, 0);

        enableAutomaticInitialization = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_enableAutomaticInitialization, true);
        boolean autoPlay = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_autoPlay, false);
        boolean handleNetworkEvents = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_handleNetworkEvents, true);
        String videoId = typedAttr.getStringValue(R.styleable.YouTubePlayerView_videoId);

        boolean useWebUi = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_useWebUi, false);
        boolean enableLiveVideoUi = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_enableLiveVideoUi, false);
        boolean showYouTubeButton = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_showYouTubeButton, true);
        boolean showFullScreenButton = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_showFullScreenButton, true);
        boolean showVideoCurrentTime = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_showVideoCurrentTime, true);
        boolean showVideoDuration = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_showVideoDuration, true);
        boolean showSeekBar = typedAttr.getBooleanValue(R.styleable.YouTubePlayerView_showSeekBar, true);

        // typedAttr.recycle();

        if (!enableAutomaticInitialization && useWebUi) {
            throw new IllegalStateException("YouTubePlayerView: 'enableAutomaticInitialization' is false and 'useWebUi' is set to true. " +
                    "This is not possible, if you want to manually initialize YouTubePlayerView and use the web ui, " +
                    "you should manually initialize the YouTubePlayerView using 'initializeWithWebUi'");
        }

        if (videoId == null && autoPlay)
            throw new IllegalStateException("YouTubePlayerView: videoId is not set but autoPlay is set to true. This combination is not possible.");

        if (!useWebUi) {
            legacyTubePlayerView.getPlayerUiController()
                    .enableLiveVideoUi(enableLiveVideoUi)
                    .showYouTubeButton(showYouTubeButton)
                    .showFullscreenButton(showFullScreenButton)
                    .showCurrentTime(showVideoCurrentTime)
                    .showDuration(showVideoDuration)
                    .showSeekBar(showSeekBar);
        }

        AbstractYouTubePlayerListener youTubePlayerListener = new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(YouTubePlayer youTubePlayer) {
                if (videoId != null)
                    YouTubePlayerUtils.loadOrCueVideo(youTubePlayer, legacyTubePlayerView.canPlay && autoPlay, videoId, 0f);

                youTubePlayer.removeListener(this);
            }
        };

        if (enableAutomaticInitialization) {
            if (useWebUi)
                legacyTubePlayerView.initializeWithWebUi(youTubePlayerListener, handleNetworkEvents);
            else
                legacyTubePlayerView.initialize(youTubePlayerListener, handleNetworkEvents);
        }


        legacyTubePlayerView.addFullScreenListener(new YouTubePlayerFullScreenListener() {
            @Override
            public void onYouTubePlayerEnterFullScreen() {
                fullScreenHelper.enterFullScreen();
            }

            @Override
            public void onYouTubePlayerExitFullScreen() {
                fullScreenHelper.exitFullScreen();
            }
        });
    }

    /**
     * Initialize the player. You must call this method before using the player.
     *
     * @param youTubePlayerListener listener for player events
     * @param handleNetworkEvents if set to true a broadcast receiver will be registered and network events will be handled automatically.
     * If set to false, you should handle network events with your own broadcast receiver.
     * @param playerOptions customizable options for the embedded video player, can be null.
     */
    public void initialize(YouTubePlayerListener youTubePlayerListener, boolean handleNetworkEvents, IFramePlayerOptions playerOptions) {
        if (enableAutomaticInitialization)
            throw new IllegalStateException("YouTubePlayerView: If you want to initialize this view manually, you need to set 'enableAutomaticInitialization' to false");
        else
            legacyTubePlayerView.initialize(youTubePlayerListener, handleNetworkEvents, playerOptions);
    }

    /**
     * Initialize the player.
     *
     * @param handleNetworkEvents if set to true a broadcast receiver will be registered and network events will be handled automatically.
     * If set to false, you should handle network events with your own broadcast receiver.
     *
     * @see YouTubePlayerView#initialize
     */
    public void initialize(YouTubePlayerListener youTubePlayerListener, boolean handleNetworkEvents) {
        if (enableAutomaticInitialization)
            throw new IllegalStateException("YouTubePlayerView: If you want to initialize this view manually, you need to set 'enableAutomaticInitialization' to false");
        else
            legacyTubePlayerView.initialize(youTubePlayerListener, handleNetworkEvents, null);
    }

    /**
     * Initialize the player. Network events are automatically handled by the player.
     *
     * @param youTubePlayerListener listener for player events
     *
     * @see YouTubePlayerView#initialize
     */
    public void initialize(YouTubePlayerListener youTubePlayerListener) {
        if (enableAutomaticInitialization)
            throw new IllegalStateException("YouTubePlayerView: If you want to initialize this view manually, you need to set 'enableAutomaticInitialization' to false");
        else
            legacyTubePlayerView.initialize(youTubePlayerListener, true);
    }

    /**
     * Initialize a player using the web-base Ui instead pf the native Ui.
     * The default PlayerUiController will be removed and [YouTubePlayerView.getPlayerUiController] will throw exception.
     *
     * @see YouTubePlayerView#initialize
     */
    public void initializeWithWebUi(YouTubePlayerListener youTubePlayerListener, boolean handleNetworkEvents) {
        if (enableAutomaticInitialization)
            throw new IllegalStateException("YouTubePlayerView: If you want to initialize this view manually, you need to set 'enableAutomaticInitialization' to false");
        else
            legacyTubePlayerView.initializeWithWebUi(youTubePlayerListener, handleNetworkEvents);
    }

    /**
     * @param youTubePlayerCallback A callback that will be called when the YouTubePlayer is ready.
     * If the player is ready when the function is called, the callback is called immediately.
     * This function is called only once.
     */
    public void getYouTubePlayerWhenReady(YouTubePlayerCallback youTubePlayerCallback) {
        legacyTubePlayerView.getYouTubePlayerWhenReady(youTubePlayerCallback);
    }

    /**
     * Use this method to replace the default Ui of the player with a custom Ui.
     * <p>
     * You will be responsible to manage the custom Ui from your application,
     * the default controller obtained through [YouTubePlayerView.getPlayerUiController] won't be available anymore.
     *
     * @param layoutId the ID of the layout defining the custom Ui.
     *
     * @return The inflated View
     */
    public Component inflateCustomPlayerUi(int layoutId) {
        return legacyTubePlayerView.inflateCustomPlayerUi(layoutId);
    }

    public PlayerUiController getPlayerUiController() {
        return legacyTubePlayerView.getPlayerUiController();
    }

    /**
     * Don't use this method if you want to publish your app on the PlayStore. Background playback is against YouTube terms of service.
     */
    public void enableBackgroundPlayback(boolean enable) {
        legacyTubePlayerView.enableBackgroundPlayback(enable);
    }

    // Call this method before destroying the host Fragment/Activity, or register this View as an observer of its host lifecycle
    public void onStateChanged(Lifecycle.Event event, Intent intent) {
        switch (event) {
            case ON_ACTIVE:
                legacyTubePlayerView.onResume();
                break;

            case ON_INACTIVE:
                legacyTubePlayerView.release();
                break;

            case ON_STOP:
                legacyTubePlayerView.onStop();
                break;
        }
    }

    public void addYouTubePlayerListener(YouTubePlayerListener youTubePlayerListener) {
        legacyTubePlayerView.getYoutubePlayer().addListener(youTubePlayerListener);
    }

    public void removeYouTubePlayerListener(YouTubePlayerListener youTubePlayerListener) {
        legacyTubePlayerView.getYoutubePlayer().removeListener(youTubePlayerListener);
    }

    public void enterFullScreen() {
        legacyTubePlayerView.enterFullScreen();
    }

    public void exitFullScreen() {
        legacyTubePlayerView.exitFullScreen();
    }

    public void toggleFullScreen() {
        legacyTubePlayerView.toggleFullScreen();
    }

    public boolean isFullScreen() {
        return fullScreenHelper.isFullScreen();
    }

    public boolean addFullScreenListener(YouTubePlayerFullScreenListener fullScreenListener) {
        return fullScreenHelper.addFullScreenListener(fullScreenListener);
    }

    public boolean removeFullScreenListener(YouTubePlayerFullScreenListener fullScreenListener) {
        return fullScreenHelper.removeFullScreenListener(fullScreenListener);
    }
}