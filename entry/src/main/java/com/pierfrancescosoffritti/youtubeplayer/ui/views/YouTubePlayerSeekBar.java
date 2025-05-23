package com.pierfrancescosoffritti.youtubeplayer.ui.views;

import ohos.agp.colors.RgbColor;
import ohos.agp.components.AttrSet;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Slider;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.TextAlignment;
import ohos.app.Context;
import ohos.global.resource.solidxml.TypedAttribute;

import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants;
import com.pierfrancescosoffritti.youtubeplayer.player.PlayerConstants.PlayerState;
import com.pierfrancescosoffritti.youtubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.youtubeplayer.player.listeners.YouTubePlayerListener;
import com.pierfrancescosoffritti.youtubeplayer.ui.utils.TimeUtilities;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.ResourceTool;

public class YouTubePlayerSeekBar extends DirectionalLayout implements Slider.ValueChangedListener, YouTubePlayerListener {

    // I need this variable because onCurrentSecond gets called every 100 mils, so without the proper checks on this variable in onCurrentSeconds the seek bar glitches when touched.
    private int newSeekBarProgress = -1;

    private boolean seekBarTouchStarted = false;
    private boolean isPlaying = false;
    public boolean showBufferingProgress = true;
    public YouTubePlayerSeekBarListener youtubePlayerSeekBarListener = null;

    public Text videoCurrentTimeText = new Text(getContext());
    public Text videoDurationText = new Text(getContext());
    public Slider slider = new Slider(getContext());

    // DirectionalLayout(context, attrs)

    public YouTubePlayerSeekBar(Context context, AttrSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        TypedAttribute typedAttr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.YouTubePlayerSeekBar, 0, 0);

        float fontSize = typedAttr.getDimensionPixelSize(R.styleable.YouTubePlayerSeekBar_fontSize, resources.getDimensionPixelSize(ResourceTable.Float_ayp_12sp));
        int color = typedAttr.getColorValue(R.styleable.YouTubePlayerSeekBar_color, context.getColor(ResourceTable.Color_ayp_red));

        // typedAttr.recycle();

        int padding = ResourceTool.getInt(context, ResourceTable.Float_ayp_8dp, 8);
        videoCurrentTimeText.setText(ResourceTable.String_ayp_null_time);
        videoCurrentTimeText.setPadding(padding, padding, 0, padding);
        videoCurrentTimeText.setTextColor(Color.WHITE);
        videoCurrentTimeText.setTextAlignment(TextAlignment.VERTICAL_CENTER);

        videoDurationText.setText(ResourceTable.String_ayp_null_time);
        videoDurationText.setPadding(0, padding, padding, padding);
        videoDurationText.setTextColor(Color.WHITE);
        videoDurationText.setTextAlignment(TextAlignment.VERTICAL_CENTER);

        setFontSize(Math.round(fontSize));
        slider.setPadding(padding * 2, padding, padding * 2, padding);
        setColor(color);

        addComponent(videoCurrentTimeText, new LayoutConfig(LayoutConfig.MATCH_CONTENT, LayoutConfig.MATCH_CONTENT));
        addComponent(slider, new DirectionalLayout.LayoutConfig(LayoutConfig.MATCH_CONTENT, 1));
        addComponent(videoDurationText, new LayoutConfig(LayoutConfig.MATCH_CONTENT, LayoutConfig.MATCH_CONTENT));

        // gravity = Gravity.CENTER_VERTICAL;
        slider.setValueChangedListener(this);
    }

    /**
     * @param fontSize in pixels.
     */
    private void setFontSize(int fontSize) {
        videoCurrentTimeText.setTextSize(fontSize, Text.TextSizeType.PX);
        videoDurationText.setTextSize(fontSize, Text.TextSizeType.PX);
    }

    private void setColor(int color) {
        ShapeElement sElement = new ShapeElement(new RgbColor(color));
        slider.setThumbElement(sElement);
        slider.setProgressColor(new Color(color));
    }

    private void updateState(PlayerState state) {
        switch (state) {
            case PAUSED:
            case ENDED:
                isPlaying = false;
                break;

            case PLAYING:
                isPlaying = true;
                break;

            case UNSTARTED:
                resetUi();
                break;
            default:
                break;
        }
    }

    private void resetUi() {
        slider.setProgressValue(0);
        slider.setMaxValue(0);
        BaseAbility.runOnUiThread(() -> {
            videoDurationText.setText("");
        });
    }

    // Seekbar
    @Override
    public void onProgressUpdated(Slider slider, int progress, boolean fromUser) {
        videoCurrentTimeText.setText(TimeUtilities.formatTime(progress));
    }

    @Override
    public void onTouchStart(Slider slider) {
        seekBarTouchStarted = true;
    }

    @Override
    public void onTouchEnd(Slider slider) {
        if (isPlaying)
            newSeekBarProgress = slider.getProgress();

        youtubePlayerSeekBarListener.seekTo(slider.getProgress());
        seekBarTouchStarted = false;
    }

    // YouTubePlayerListener
    @Override
    public void onReady(YouTubePlayer youTubePlayer) {
    }

    @Override
    public void onStateChange(YouTubePlayer youTubePlayer, PlayerState state) {
        newSeekBarProgress = -1;
        updateState(state);
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
        // ignore if the user is currently moving the SeekBar
        if (seekBarTouchStarted)
            return;

        // ignore if the current time is older than what the user selected with the SeekBar
        if (newSeekBarProgress > 0 && TimeUtilities.formatTime(second).equals(TimeUtilities.formatTime(newSeekBarProgress)))
            return;

        newSeekBarProgress = -1;
        slider.setProgressValue(Math.round(second));

    }

    @Override
    public void onVideoDuration(YouTubePlayer youTubePlayer, float duration) {
        videoDurationText.setText(TimeUtilities.formatTime(duration));
        slider.setMaxValue(Math.round(duration));
    }

    @Override
    public void onVideoLoadedFraction(YouTubePlayer youTubePlayer, float loadedFraction) {
        if (showBufferingProgress)
            slider.setViceProgress(Math.round(loadedFraction * slider.getMax()));
        else
            slider.setViceProgress(0);
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

    public interface YouTubePlayerSeekBarListener {
        void seekTo(float time);
    }
}