package org.atalk.util;

import ohos.aafwk.ability.AbilitySlice;
import ohos.agp.components.Component;

/**
 * Class responsible for changing the view from full screen to non-full screen and vice versa.
 *
 * @author Eng Chong Meng
 */
public class FullScreenHelper {
    private final AbilitySlice mContext;
    private final Component[] mViews;
    private final Component mDecorView;

    /**
     * @param context ohos Context
     * @param views to hide/show
     */
    public FullScreenHelper(AbilitySlice context, Component... views) {
        mContext = context;
        mViews = views;
        mDecorView = context.getWindow().getDecorView();
    }

    /**
     * call this method to enter full screen
     */
    public void enterFullScreen() {
        hideSystemUi();

        for (Component view : mViews) {
            view.setVisibility(Component.HIDE);
            view.invalidate();
        }
    }

    /**
     * call this method to exit full screen
     */
    public void exitFullScreen() {
        showSystemUi();

        for (Component view : mViews) {
            view.setVisibility(Component.VISIBLE);
            view.invalidate();
        }
    }

    private void hideSystemUi() {
        mDecorView.setSystemUiVisibility(Component.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | Component.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | Component.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | Component.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | Component.SYSTEM_UI_FLAG_FULLSCREEN
                | Component.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void showSystemUi() {
        mDecorView.setSystemUiVisibility(Component.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
