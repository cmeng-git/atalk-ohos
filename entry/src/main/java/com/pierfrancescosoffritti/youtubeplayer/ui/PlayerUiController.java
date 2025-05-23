package com.pierfrancescosoffritti.youtubeplayer.ui;

import ohos.agp.components.Component;
import ohos.media.image.PixelMap;

import com.pierfrancescosoffritti.youtubeplayer.ui.menu.YouTubePlayerMenu;

public interface PlayerUiController {
    PlayerUiController showUi(boolean show);
    PlayerUiController showPlayPauseButton(boolean show);

    PlayerUiController showVideoTitle(boolean show);
    PlayerUiController setVideoTitle(String videoTitle);

    PlayerUiController enableLiveVideoUi(boolean enable);

    /**
     * Set custom action to the left of the Play/Pause button
     */
    PlayerUiController setRewindAction(PixelMap icon, Component.ClickedListener clickListener);
    /**
     * Set custom action to the right of the Play/Pause button
     */
    PlayerUiController setForwardAction(PixelMap icon, Component.ClickedListener clickListenerclickListener);
    PlayerUiController setPreviousAction(PixelMap icon, Component.ClickedListener clickListener);
    PlayerUiController setNextAction(PixelMap icon, Component.ClickedListener clickListener);

    PlayerUiController showCustomAction1(boolean show);
    PlayerUiController showCustomAction2(boolean show);

    PlayerUiController setRateIncAction(PixelMap icon, Component.ClickedListener clickListener);
    PlayerUiController setRateDecAction(PixelMap icon, Component.ClickedListener clickListener);
    PlayerUiController setHideScreenAction(PixelMap icon, Component.ClickedListener clickListener);

    PlayerUiController showFullscreenButton(boolean show);
    PlayerUiController setFullScreenButtonClickListener(Component.ClickedListener customFullScreenButtonClickListener);

    PlayerUiController showMenuButton(boolean show);
    PlayerUiController setMenuButtonClickListener(Component.ClickedListener customMenuButtonClickListener);

    PlayerUiController showCurrentTime(boolean show);
    PlayerUiController showDuration(boolean show);

    PlayerUiController showSeekBar(boolean show);
    PlayerUiController showBufferingProgress(boolean show);

    PlayerUiController showYouTubeButton(boolean show);

    /**
     * Adds a View to the top of the player
     * @param view View to be added
     */
    PlayerUiController addView(Component view);

    /**
     * Removes a View added with [PlayerUiController.addView]
     * @param view View to be removed
     */
    PlayerUiController removeView(Component view);

    YouTubePlayerMenu getMenu();
    void setMenu(YouTubePlayerMenu youTubePlayerMenu);
}
