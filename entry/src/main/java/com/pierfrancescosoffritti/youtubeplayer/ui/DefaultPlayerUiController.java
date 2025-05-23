package com.pierfrancescosoffritti.youtubeplayer.ui;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.app.Context;
import ohos.media.image.PixelMap;
import ohos.utils.net.Uri;

import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerFullScreenListener;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.youtubeplayer.player.views.LegacyYouTubePlayerView;
import com.pierfrancescosoffritti.youtubeplayer.ui.menu.YouTubePlayerMenu;
import com.pierfrancescosoffritti.youtubeplayer.ui.menu.defaultMenu.DefaultYouTubePlayerMenu;
import com.pierfrancescosoffritti.youtubeplayer.ui.utils.FadeViewHelper;
import com.pierfrancescosoffritti.youtubeplayer.ui.views.YouTubePlayerSeekBar;
import com.pierfrancescosoffritti.youtubeplayer.ui.views.YouTubePlayerSeekBar.YouTubePlayerSeekBarListener;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.settings.SettingsAbility;

public class DefaultPlayerUiController implements PlayerUiController, YouTubePlayerListener, YouTubePlayerFullScreenListener, YouTubePlayerSeekBarListener {
    private final LegacyYouTubePlayerView youTubePlayerView;
    private final YouTubePlayer youTubePlayer;
    private YouTubePlayerMenu youTubePlayerMenu;

    /**
     * View used for for intercepting clicks and for drawing a black background.
     * Could have used controlsContainer, but in this way I'm able to hide all the control at once by hiding controlsContainer
     */
    private Component panel;

    private Component controlsContainer;
    private DirectionalLayout extraViewsContainer;

    private Text videoTitle;
    private Text liveVideoIndicator;

    private ProgressBar progressBar;
    private Image menuButton;
    private Image playPauseButton;
    private Image youTubeButton;
    private Image rateIncButton;
    private Image rateDecButton;
    private Image hideScreenButton;
    private Image fullScreenButton;

    private Image customActionLeft;
    private Image customActionRight;
    private Image previousAction;
    private Image nextAction;

    private YouTubePlayerSeekBar youtubePlayerSeekBar;
    private Component.ClickedListener onFullScreenButtonListener;
    private Component.ClickedListener onMenuButtonClickListener;
    private FadeViewHelper fadeControlsContainer;

    private boolean isPlaying = false;
    private boolean isPlayPauseButtonEnabled = true;
    private boolean isCustomActionLeftEnabled = false;
    private boolean isCustomActionRightEnabled = false;

    public DefaultPlayerUiController(LegacyYouTubePlayerView youTubePlayerView, YouTubePlayer youTubePlayer) {
        this.youTubePlayerView = youTubePlayerView;
        this.youTubePlayer = youTubePlayer;
        Context context = aTalkApp.getInstance();

        LayoutScatter layoutScatter = LayoutScatter.getInstance(context);

        Component defaultPlayerUI = layoutScatter.parse(ResourceTable.Layout_ayp_default_player_ui, youTubePlayerView, true);
        initViews(defaultPlayerUI);
        youTubePlayerMenu = new DefaultYouTubePlayerMenu(youTubePlayerView.getContext());
    }

    private void initViews(Component controlsView) {
        panel = controlsView.findComponentById(ResourceTable.Id_panel);

        controlsContainer = controlsView.findComponentById(ResourceTable.Id_controls_container);
        extraViewsContainer = controlsView.findComponentById(ResourceTable.Id_extra_views_container);

        videoTitle = controlsView.findComponentById(ResourceTable.Id_video_title);
        liveVideoIndicator = controlsView.findComponentById(ResourceTable.Id_live_video_indicator);

        progressBar = controlsView.findComponentById(ResourceTable.Id_progress);
        menuButton = controlsView.findComponentById(ResourceTable.Id_menu_button);
        playPauseButton = controlsView.findComponentById(ResourceTable.Id_play_pause_button);
        youTubeButton = controlsView.findComponentById(ResourceTable.Id_youtube_button);
        rateIncButton = controlsView.findComponentById(ResourceTable.Id_rate_inc_button);
        rateDecButton = controlsView.findComponentById(ResourceTable.Id_rate_dec_button);
        hideScreenButton = controlsView.findComponentById(ResourceTable.Id_hide_screen_button);
        fullScreenButton = controlsView.findComponentById(ResourceTable.Id_fullscreen_button);

        customActionLeft = controlsView.findComponentById(ResourceTable.Id_action_rewind_button);
        customActionRight = controlsView.findComponentById(ResourceTable.Id_action_forward_button);
        previousAction = controlsView.findComponentById(ResourceTable.Id_action_previous_button);
        nextAction = controlsView.findComponentById(ResourceTable.Id_action_next_button);

        youtubePlayerSeekBar = controlsView.findComponentById(ResourceTable.Id_youtube_player_seekbar);
        fadeControlsContainer = new FadeViewHelper(controlsContainer);

        onFullScreenButtonListener = component -> youTubePlayerView.toggleFullScreen();
        onFullScreenButtonListener = component -> youTubePlayerMenu.show(menuButton);

        initClickListeners();
    }

    private void initClickListeners() {
        youTubePlayer.addListener(youtubePlayerSeekBar);
        youTubePlayer.addListener(fadeControlsContainer);
        youtubePlayerSeekBar.youtubePlayerSeekBarListener = this;

        panel.setClickedListener(component -> {
            fadeControlsContainer.toggleVisibility();
        });

        playPauseButton.setClickedListener(component -> {
            onPlayButtonPressed();
        });

        fullScreenButton.setClickedListener(component -> {
            onFullScreenButtonListener.onClick(fullScreenButton);
        });

        menuButton.setClickedListener(component -> {
            onMenuButtonClickListener.onClick(menuButton);
        });
    }

    @Override
    public PlayerUiController showVideoTitle(boolean show) {
        videoTitle.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    @Override
    public PlayerUiController setVideoTitle(String title) {
        videoTitle.setText(title);
        return this;
    }

    @Override
    public PlayerUiController showUi(boolean show) {
        fadeControlsContainer.isDisabled = !show;
        controlsContainer.setVisibility(show ? Component.VISIBLE : Component.INVISIBLE);
        return this;
    }

    @Override
    public PlayerUiController showPlayPauseButton(boolean show) {
        playPauseButton.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        isPlayPauseButtonEnabled = show;
        return this;
    }

    @Override
    public PlayerUiController enableLiveVideoUi(boolean enable) {
        youtubePlayerSeekBar.setVisibility(enable ? Component.INVISIBLE : Component.VISIBLE);
        liveVideoIndicator.setVisibility(enable ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    @Override
    public PlayerUiController setRewindAction(PixelMap icon, Component.ClickedListener clickListener) {
        customActionLeft.setClickedListener(clickListener);
        showCustomAction1(true);
        return this;
    }

    @Override
    public PlayerUiController setForwardAction(PixelMap icon, Component.ClickedListener clickListener) {
        customActionRight.setClickedListener(clickListener);
        showCustomAction2(true);
        return this;
    }

    @Override
    public PlayerUiController setPreviousAction(PixelMap icon, Component.ClickedListener clickListener) {
        previousAction.setClickedListener(clickListener);
        previousAction.setVisibility(Component.VISIBLE);
        return this;
    }

    @Override
    public PlayerUiController setNextAction(PixelMap icon, Component.ClickedListener clickListener) {
        nextAction.setClickedListener(clickListener);
        nextAction.setVisibility(Component.VISIBLE);
        return this;
    }

    @Override
    public PlayerUiController setRateIncAction(PixelMap icon, Component.ClickedListener clickListener) {
        rateIncButton.setVisibility(Component.VISIBLE);
        rateIncButton.setClickedListener(clickListener);
        return this;
    }

    @Override
    public PlayerUiController setRateDecAction(PixelMap icon, Component.ClickedListener clickListener) {
        rateDecButton.setVisibility(Component.VISIBLE);
        rateDecButton.setClickedListener(clickListener);
        return this;
    }

    @Override
    public PlayerUiController setHideScreenAction(PixelMap icon, Component.ClickedListener clickListener) {
        hideScreenButton.setPixelMap(icon);
        hideScreenButton.setVisibility(Component.VISIBLE);
        hideScreenButton.setClickedListener(clickListener);
        return this;
    }

    public PlayerUiController showCustomAction1(boolean show) {
        isCustomActionLeftEnabled = show;
        customActionLeft.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    public PlayerUiController showCustomAction2(boolean show) {
        isCustomActionRightEnabled = show;
        customActionRight.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    @Override
    public PlayerUiController showMenuButton(boolean show) {
        menuButton.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    @Override
    public PlayerUiController setMenuButtonClickListener(Component.ClickedListener customMenuButtonClickListener) {
        this.onMenuButtonClickListener = customMenuButtonClickListener;
        return this;
    }

    @Override
    public PlayerUiController showCurrentTime(boolean show) {
        youtubePlayerSeekBar.videoCurrentTimeText.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    @Override
    public PlayerUiController showDuration(boolean show) {
        youtubePlayerSeekBar.videoDurationText.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    @Override
    public PlayerUiController showSeekBar(boolean show) {
        youtubePlayerSeekBar.slider.setVisibility(show ? Component.VISIBLE : Component.INVISIBLE);
        return this;
    }

    @Override
    public PlayerUiController showBufferingProgress(boolean show) {
        youtubePlayerSeekBar.showBufferingProgress = show;
        return this;
    }

    @Override
    public PlayerUiController showYouTubeButton(boolean show) {
        youTubeButton.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    @Override
    public PlayerUiController addView(Component view) {
        extraViewsContainer.addComponent(view, 0);
        return this;
    }

    @Override
    public PlayerUiController removeView(Component view) {
        extraViewsContainer.removeComponent(view);
        return this;
    }

    @Override
    public YouTubePlayerMenu getMenu() {
        return youTubePlayerMenu;
    }

    @Override
    public void setMenu(YouTubePlayerMenu youTubePlayerMenu) {
        this.youTubePlayerMenu = youTubePlayerMenu;
    }

    @Override
    public PlayerUiController showFullscreenButton(boolean show) {
        fullScreenButton.setVisibility(show ? Component.VISIBLE : Component.HIDE);
        return this;
    }

    @Override
    public PlayerUiController setFullScreenButtonClickListener(Component.ClickedListener customFullScreenButtonClickListener) {
        this.onFullScreenButtonListener = customFullScreenButtonClickListener;
        return this;
    }

    private void onMenuButtonPressed() {
        if (onMenuButtonClickListener == null)
            youTubePlayerMenu.show(menuButton);
        else
            onMenuButtonClickListener.onClick(menuButton);
    }

    private void onFullScreenButtonPressed() {
        if (onFullScreenButtonListener == null)
            youTubePlayerView.toggleFullScreen();
        else
            onFullScreenButtonListener.onClick(fullScreenButton);
    }

    private void onPlayButtonPressed() {
        if (isPlaying)
            youTubePlayer.pause();
        else
            youTubePlayer.play();
    }

    @Override
    public void onYouTubePlayerEnterFullScreen() {
        fullScreenButton.setPixelMap(ResourceTable.Graphic_ayp_ic_fullscreen_exit_24dp);
    }

    @Override
    public void onYouTubePlayerExitFullScreen() {
        fullScreenButton.setPixelMap(ResourceTable.Graphic_ayp_ic_fullscreen_24dp);
    }

    private void updateState(PlayerConstants.PlayerState state) {
        switch (state) {
            case PAUSED:
            case ENDED:
                isPlaying = false;
                break;

            case PLAYING:
                isPlaying = true;
                break;

            default:
                break;
        }

        updatePlayPauseButtonIcon(!isPlaying);
    }

    private void updatePlayPauseButtonIcon(boolean playing) {
        playPauseButton.setPixelMap(playing ? ResourceTable.Graphic_ayp_ic_pause_36dp :
                ResourceTable.Graphic_ayp_ic_play_36dp);
    }

    @Override
    public void seekTo(float time) {
        youTubePlayer.seekTo(time);
    }

    // @Override
    public void advanceTo(float time) {
        youTubePlayer.advanceTo(time);
    }

    // @Override
    public void setPlaybackRate(float rate) {
        youTubePlayer.setPlaybackRate(rate);
    }

    // @Override
    public void getVideoUrl() {
        youTubePlayer.getVideoUrl();
    }

    // YouTubePlayer callbacks
    @Override
    public void onReady(YouTubePlayer youTubePlayer) {
    }

    @Override
    public void onStateChange(YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
        updateState(state);
        if (state == PlayerConstants.PlayerState.PLAYING
                || state == PlayerConstants.PlayerState.PAUSED
                || state == PlayerConstants.PlayerState.VIDEO_CUED) {
            panel.setBackground(new ShapeElement(panel.getContext(), ResourceTable.Color_transparent_black));
            progressBar.setVisibility(Component.HIDE);

            if (isPlayPauseButtonEnabled) playPauseButton.setVisibility(Component.VISIBLE);

            if (isCustomActionLeftEnabled) customActionLeft.setVisibility(Component.VISIBLE);
            if (isCustomActionRightEnabled) customActionRight.setVisibility(Component.VISIBLE);

            updatePlayPauseButtonIcon(state == PlayerConstants.PlayerState.PLAYING);
        }
        else {
            updatePlayPauseButtonIcon(false);

            if (state == PlayerConstants.PlayerState.BUFFERING) {
                progressBar.setVisibility(Component.VISIBLE);
                panel.setBackground(new ShapeElement(panel.getContext(), ResourceTable.Color_transparent_black));
                if (isPlayPauseButtonEnabled) playPauseButton.setVisibility(Component.INVISIBLE);

                customActionLeft.setVisibility(Component.HIDE);
                customActionRight.setVisibility(Component.HIDE);
            }

            if (state == PlayerConstants.PlayerState.UNSTARTED) {
                progressBar.setVisibility(Component.HIDE);
                if (isPlayPauseButtonEnabled) playPauseButton.setVisibility(Component.VISIBLE);
            }
        }
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
    public void onCurrentSecond(YouTubePlayer youTubePlayer, float second) {
    }

    @Override
    public void onVideoDuration(YouTubePlayer youTubePlayer, float duration) {
    }

    @Override
    public void onVideoLoadedFraction(YouTubePlayer youTubePlayer, float loadedFraction) {
    }

    @Override
    public void onVideoId(YouTubePlayer youTubePlayer, final String videoId) {
        youTubeButton.setClickedListener(component -> {
            Uri aytLink = Uri.parse("http://www.youtube.com/watch?v=" + videoId + "#t=" + youtubePlayerSeekBar.slider.getProgress());
            Context ctx = component.getContext();

            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withBundleName(ctx.getBundleName())
                    .withAbilityName(SettingsAbility.class)
                    .withAction(Intent.ACTION_PLAY)
                    .withUri(aytLink)
                    .build();
            intent.setOperation(operation);
            ctx.startAbility(intent, 0);
        });
    }

    @Override
    public void onVideoUrl(YouTubePlayer youTubePlayer, String videoUrl) {
    }

    @Override
    public void onApiChange(YouTubePlayer youTubePlayer) {
    }
}
