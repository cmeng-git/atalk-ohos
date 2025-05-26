package org.atalk.util;

import ohos.aafwk.ability.AbilitySlice;
import ohos.agp.components.Component;

/**
 * Class responsible for changing the view from full screen to non-full screen and vice versa.
 *
 * @author Pierfrancesco Soffritti
 * @author Eng Chong Meng
 */
public class FullScreenHelper {
    private final AbilitySlice context;
    private final Component[] views;

    /**
     * @param context
     * @param views to hide/show
     */
    public FullScreenHelper(AbilitySlice context, Component... views) {
        this.context = context;
        this.views = views;
    }

    /**
     * call this method to enter full screen
     */
    public void enterFullScreen() {
        Component decorView = context.getWindow().getDecorView();

        hideSystemUi(decorView);

        for (Component view : views) {
            view.setVisibility(Component.HIDE);
            view.invalidate();
        }
    }

    /**
     * call this method to exit full screen
     */
    public void exitFullScreen() {
        Component decorView = context.getWindow().getDecorView();
        showSystemUi(decorView);

        for (Component view : views) {
            view.setVisibility(Component.VISIBLE);
            view.invalidate();
        }
    }

    private void hideSystemUi(Component mDecorView) {
        mDecorView.setSystemUiVisibility(Component.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | Component.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | Component.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | Component.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | Component.SYSTEM_UI_FLAG_FULLSCREEN
                | Component.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void showSystemUi(Component mDecorView) {
        mDecorView.setSystemUiVisibility(Component.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
