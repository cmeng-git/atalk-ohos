package com.pierfrancescosoffritti.youtubeplayer.ui.utils;

import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.animation.AnimatorScatter;
import ohos.agp.components.Component;

import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants;
import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants.PlayerState;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerListener;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;

public class FadeViewHelper implements YouTubePlayerListener {

    public static final long DEFAULT_ANIMATION_DURATION = 300L;
    public static final long DEFAULT_FADE_OUT_DELAY = 3000L;

    private boolean isPlaying = false;
    private boolean canFade = false;
    private boolean isVisible = true;
    public boolean isDisabled = false;

    /**
     * Duration of the fade animation in milliseconds.
     */
    private final long animationDuration = DEFAULT_ANIMATION_DURATION;

    /**
     * Delay after which the view automatically fades out.
     */
    private final long fadeOutDelay = DEFAULT_FADE_OUT_DELAY;

    private final Component mTargetView;

    public FadeViewHelper(Component targetView) {
        mTargetView = targetView;
    }

    public void toggleVisibility() {
        fade(isVisible ? 0f : 1f);
    }

    private void fade(float finalAlpha) {
        if (!canFade || isDisabled)
            return;

        isVisible = finalAlpha != 0f;

        // if the controls are shown and the player is playing they should automatically fade after a while.
        // otherwise don't do anything automatically
        AnimatorScatter scatter = AnimatorScatter.getInstance(aTalkApp.getInstance());
        AnimatorProperty pAnimate = (AnimatorProperty) scatter.parse(ResourceTable.Animation_fade_out);
        pAnimate.setTarget(mTargetView);

        if (finalAlpha == 1f && isPlaying) {
            pAnimate.setDuration(fadeOutDelay).start();
        }
        else {
            pAnimate.release();
        }

        mTargetView.createAnimatorProperty()
                .alpha(finalAlpha)
                .setDuration(animationDuration)
                .setStateChangedListener(new Animator.StateChangedListener() {

                    @Override
                    public void onStart(Animator animator) {
                        if (finalAlpha == 1f)
                            mTargetView.setVisibility(Component.VISIBLE);
                    }

                    @Override
                    public void onStop(Animator animator) {

                    }

                    @Override
                    public void onCancel(Animator animator) {

                    }

                    @Override
                    public void onEnd(Animator animator) {
                        if (finalAlpha == 0f)
                            mTargetView.setVisibility(Component.HIDE);
                    }

                    @Override
                    public void onPause(Animator animator) {

                    }

                    @Override
                    public void onResume(Animator animator) {

                    }
                }).start();
    }

    private void updateState(PlayerState state) {
        if (PlayerState.ENDED == state)
            isPlaying = false;
        else if (PlayerState.PAUSED == state)
            isPlaying = false;
        else if (PlayerState.PLAYING == state)
            isPlaying = true;
        else if (PlayerState.UNSTARTED == state)
            isPlaying = false;
    }

    @Override
    public void onReady(YouTubePlayer youTubePlayer) {
    }

    @Override
    public void onStateChange(YouTubePlayer youTubePlayer, PlayerState state) {
        updateState(state);

        if (PlayerConstants.PlayerState.PLAYING == state
                || PlayerConstants.PlayerState.PAUSED == state
                || PlayerConstants.PlayerState.VIDEO_CUED == state) {
            canFade = true;

            AnimatorScatter scatter = AnimatorScatter.getInstance(aTalkApp.getInstance());
            AnimatorProperty pAnimate = (AnimatorProperty) scatter.parse(ResourceTable.Animation_fade_out);
            pAnimate.setTarget(mTargetView);

            if (PlayerConstants.PlayerState.PLAYING == state) {
                pAnimate.setDuration(fadeOutDelay).start();
            }
            else {
                pAnimate.release();
            }

            if (PlayerConstants.PlayerState.BUFFERING == state
                    || PlayerConstants.PlayerState.UNSTARTED == state) {
                canFade = false;
                fade(1f);
            }

            if (PlayerConstants.PlayerState.UNKNOWN == state
                    || PlayerConstants.PlayerState.ENDED == state) {
                fade(1f);
            }
        }
    }

    @Override
    public void onPlaybackQualityChange(YouTubePlayer youTubePlayer, PlayerConstants.PlaybackQuality
            playbackQuality) {
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
    public void onVideoId(YouTubePlayer youTubePlayer, String videoId) {
    }

    @Override
    public void onVideoUrl(YouTubePlayer youTubePlayer, String videoUrl) {
    }

    @Override
    public void onApiChange(YouTubePlayer youTubePlayer) {
    }
}