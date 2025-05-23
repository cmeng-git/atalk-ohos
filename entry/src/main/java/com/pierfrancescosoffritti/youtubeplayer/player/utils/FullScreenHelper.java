package com.pierfrancescosoffritti.youtubeplayer.player.utils;

import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerFullScreenListener;

import java.util.HashSet;
import java.util.Set;

import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;

public class FullScreenHelper {
    private final Component targetView;
    private boolean isFullScreen;
    private final Set<YouTubePlayerFullScreenListener> fullScreenListeners;

    public FullScreenHelper(Component view) {
        targetView = view;
        isFullScreen = false;
        fullScreenListeners = new HashSet<>();
    }

    public void enterFullScreen() {
        if(isFullScreen)
            return;

        isFullScreen = true;

        ComponentContainer.LayoutConfig viewParams = targetView.getLayoutConfig();
        viewParams.height = ComponentContainer.LayoutConfig.MATCH_PARENT;
        viewParams.width = ComponentContainer.LayoutConfig.MATCH_PARENT;
        targetView.setLayoutConfig(viewParams);

        for(YouTubePlayerFullScreenListener fullScreenListener : fullScreenListeners)
            fullScreenListener.onYouTubePlayerEnterFullScreen();
    }

    public void exitFullScreen() {
        if(!isFullScreen)
            return;

        isFullScreen = false;

        ComponentContainer.LayoutConfig viewParams = targetView.getLayoutConfig();
        viewParams.height = ComponentContainer.LayoutConfig.MATCH_CONTENT;
        viewParams.width = ComponentContainer.LayoutConfig.MATCH_PARENT;
        targetView.setLayoutConfig(viewParams);

        for(YouTubePlayerFullScreenListener fullScreenListener : fullScreenListeners)
            fullScreenListener.onYouTubePlayerExitFullScreen();
    }

    public void toggleFullScreen() {
        if(isFullScreen)
            exitFullScreen();
        else
            enterFullScreen();
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public boolean addFullScreenListener(YouTubePlayerFullScreenListener fullScreenListener) {
        return fullScreenListeners.add(fullScreenListener);
    }

    public boolean removeFullScreenListener(YouTubePlayerFullScreenListener fullScreenListener) {
        return fullScreenListeners.remove(fullScreenListener);
    }
}
