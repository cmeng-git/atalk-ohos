package com.pierfrancescosoffritti.youtubeplayer.player.views;

import java.util.HashSet;

import ohos.aafwk.ability.Lifecycle;
import ohos.aafwk.ability.LifecycleStateObserver;
import ohos.aafwk.content.Intent;
import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.app.Context;
import ohos.net.NetCapabilities;
import ohos.net.NetHandle;
import ohos.net.NetManager;
import ohos.net.NetSpecifier;
import ohos.net.NetStatusCallback;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;

import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerCallback;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerFullScreenListener;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.youtubeplayer.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.youtubeplayer.player.utils.FullScreenHelper;
import com.pierfrancescosoffritti.youtubeplayer.player.utils.PlaybackResumer;
import com.pierfrancescosoffritti.youtubeplayer.ui.DefaultPlayerUiController;
import com.pierfrancescosoffritti.youtubeplayer.ui.PlayerUiController;

public class LegacyYouTubePlayerView extends SixteenByNineFrameLayout implements LifecycleStateObserver {

    private final WebViewYouTubePlayer youTubePlayer = new WebViewYouTubePlayer(getContext());
    private DefaultPlayerUiController defaultPlayerUiController;
    private final PlaybackResumer playbackResumer = new PlaybackResumer();
    private final FullScreenHelper fullScreenHelper = new FullScreenHelper(this);

    private NetStatusCallback netStatusCallback = new NetStatusCallback();
    private boolean isYouTubePlayerReady = false;
    private final HashSet<YouTubePlayerCallback> youTubePlayerCallbacks = new HashSet<>();

    public boolean canPlay = true;
    private boolean isUsingCustomUi = false;
    private final Context mContext;

    protected LegacyYouTubePlayerView(Context context) {
        this(context, null);
    }

    protected LegacyYouTubePlayerView(Context context, AttrSet attrs) {
        this(context, attrs, null);
    }

    protected LegacyYouTubePlayerView(Context context, AttrSet attrs, String styleName) {
        super(context, attrs, styleName);
        mContext = context;
        init();
    }

    private void init() {
        addComponent(youTubePlayer, new ComponentContainer.LayoutConfig(ComponentContainer.LayoutConfig.MATCH_PARENT, ComponentContainer.LayoutConfig.MATCH_PARENT));
        defaultPlayerUiController = new DefaultPlayerUiController(this, youTubePlayer);

        fullScreenHelper.addFullScreenListener(defaultPlayerUiController);

        youTubePlayer.addListener(defaultPlayerUiController);
        youTubePlayer.addListener(playbackResumer);

        // stop playing if the user loads a video but then leaves the app before the video starts playing.
        youTubePlayer.addListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onStateChange(YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.PLAYING && !isEligibleForPlayback())
                    youTubePlayer.pause();
            }
        });

        youTubePlayer.addListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(YouTubePlayer youTubePlayer) {
                isYouTubePlayerReady = true;

                for (YouTubePlayerCallback ytcb : youTubePlayerCallbacks) {
                    ytcb.onYouTubePlayer(youTubePlayer);
                }
                youTubePlayerCallbacks.clear();
                youTubePlayer.removeListener(this);
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
        if (isYouTubePlayerReady)
            throw new IllegalStateException("This YouTubePlayerView has already been initialized.");

        if (handleNetworkEvents) {
            registerNetworkCallback();
        }

//        initialize = {
//                youTubePlayer.initialize({it.addListener(youTubePlayerListener)}, playerOptions)
//        };

        youTubePlayer.initialize(youTubePlayerListener, playerOptions);
        youTubePlayer.addListener(youTubePlayerListener);

        if (!handleNetworkEvents)
            initialize();
    }

    private void initialize() {
    }

    private void registerNetworkCallback() {
        NetManager netManager = NetManager.getInstance(mContext);

        NetSpecifier netSpecifier = new NetSpecifier.Builder()
                .addCapability(NetCapabilities.BEARER_WIFI)
                .addCapability(NetCapabilities.BEARER_ETHERNET)
                .addCapability(NetCapabilities.BEARER_CELLULAR)
                .build();

        netStatusCallback = new NetStatusCallback() {
            @Override
            public void onAvailable(NetHandle network) {
                BaseAbility.runOnUiThread(() -> {
                    if (!isYouTubePlayerReady)
                        initialize();
                    else
                        playbackResumer.resume(youTubePlayer);
                });
            }

            @Override
            public void onUnavailable() {
            }
        };
        netManager.addNetStatusCallback(netSpecifier, netStatusCallback);
    }

    /**
     * Initialize the player.
     *
     * @param handleNetworkEvents if set to true a broadcast receiver will be registered and network events will be handled automatically.
     * If set to false, you should handle network events with your own broadcast receiver.
     *
     * @see LegacyYouTubePlayerView#initialize
     */
    public void initialize(YouTubePlayerListener youTubePlayerListener, boolean handleNetworkEvents) {
        initialize(youTubePlayerListener, handleNetworkEvents, null);
    }

    /**
     * Initialize the player. Network events are automatically handled by the player.
     *
     * @param youTubePlayerListener listener for player events
     *
     * @see LegacyYouTubePlayerView#initialize
     */
    private void initialize(YouTubePlayerListener youTubePlayerListener) {
        initialize(youTubePlayerListener, true);
    }

    /**
     * Initialize a player using the web-base Ui instead pf the native Ui.
     * The default PlayerUiController will be removed and [LegacyYouTubePlayerView.getPlayerUiController] will throw exception.
     *
     * @see LegacyYouTubePlayerView#initialize
     */
    public void initializeWithWebUi(YouTubePlayerListener youTubePlayerListener, boolean handleNetworkEvents) {
        IFramePlayerOptions iFramePlayerOptions = new IFramePlayerOptions.Builder().controls(1).build();
        inflateCustomPlayerUi(ResourceTable.Layout_ayp_empty_layout);
        initialize(youTubePlayerListener, handleNetworkEvents, iFramePlayerOptions);
    }

    /**
     * @param youTubePlayerCallback A callback that will be called when the YouTubePlayer is ready.
     * If the player is ready when the function is called, the callback is called immediately.
     * This function is called only once.
     */
    public void getYouTubePlayerWhenReady(YouTubePlayerCallback youTubePlayerCallback) {
        if (isYouTubePlayerReady)
            youTubePlayerCallback.onYouTubePlayer(youTubePlayer);
        else
            youTubePlayerCallbacks.add(youTubePlayerCallback);
    }

    /**
     * Use this method to replace the default Ui of the player with a custom Ui.
     * <p>
     * You will be responsible to manage the custom Ui from your application,
     * the default controller obtained through [LegacyYouTubePlayerView.getPlayerUiController] won't be available anymore.
     *
     * @param layoutId the ID of the layout defining the custom Ui.
     *
     * @return The inflated View
     */
    public Component inflateCustomPlayerUi(int layoutId) {
        removeComponents(1, getChildCount() - 1);

        if (!isUsingCustomUi) {
            youTubePlayer.removeListener(defaultPlayerUiController);
            fullScreenHelper.removeFullScreenListener(defaultPlayerUiController);
        }

        isUsingCustomUi = true;
        return LayoutScatter.getInstance(mContext).parse(layoutId, this, true);
    }

    @Override
    public void onStateChanged(Lifecycle.Event event, Intent intent) {
        switch (event) {
            case ON_ACTIVE:
                onResume();
                break;

            case ON_INACTIVE:
                release();
                break;

            case ON_STOP:
                onStop();
                break;
        }
    }

    /**
     * Call this method before destroying the host Fragment/Activity, or register this View as an observer of its host lifecycle
     */
    public void release() {
        removeComponent(youTubePlayer);
        // youTubePlayer.removeAllComponents();
        youTubePlayer.onStop();
        try {
            NetManager manager = NetManager.getInstance (mContext);
            manager.removeNetStatusCallback(netStatusCallback);
        } catch (Exception ignore) {
        }
    }

    public void onResume() {
        playbackResumer.onLifecycleResume();
        canPlay = true;
    }

    // W/cr_AwContents: Application attempted to call on a destroyed WebView
    public void onStop() {
        // W/cr_AwContents: Application attempted to call on a destroyed WebView
        if (youTubePlayer.isComponentDisplayed())
            youTubePlayer.pause();
        playbackResumer.onLifecycleStop();
        canPlay = false;
    }

    public WebViewYouTubePlayer getYoutubePlayer() {
        return youTubePlayer;
    }

    /**
     * Checks whether the player is in an eligible state for playback in
     * respect of the {@link WebViewYouTubePlayer#isBackgroundPlaybackEnabled}
     * property.
     */
    private boolean isEligibleForPlayback() {
        return canPlay || youTubePlayer.isBackgroundPlaybackEnabled;
    }

    /**
     * Don't use this method if you want to publish your app on the PlayStore. Background playback is against YouTube terms of service.
     */
    public void enableBackgroundPlayback(boolean enable) {
        youTubePlayer.isBackgroundPlaybackEnabled = enable;
    }

    public PlayerUiController getPlayerUiController() {
        if (isUsingCustomUi)
            throw new RuntimeException("You have inflated a custom player Ui. You must manage it with your own controller.");

        return defaultPlayerUiController;
    }

    public void enterFullScreen() {
        fullScreenHelper.enterFullScreen();
    }

    public void exitFullScreen() {
        fullScreenHelper.exitFullScreen();
    }

    public void toggleFullScreen() {
        fullScreenHelper.toggleFullScreen();
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
