/*
 * hymnchtv: COG hymns' lyrics viewer and player client
 * Copyright 2020 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.ohos.plugin.mediaplayer;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.StackLayout;
import ohos.media.common.Source;
import ohos.media.player.Player;
import ohos.utils.PacMap;
import ohos.utils.net.Uri;

import net.java.sip.communicator.util.UtilActivator;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.settings.SettingsAbility;
import org.atalk.persistance.FileBackend;
import org.atalk.service.configuration.ConfigurationService;

import java.util.ArrayList;
import java.util.List;

import static org.atalk.ohos.plugin.mediaplayer.YoutubePlayerSlice.rateMax;
import static org.atalk.ohos.plugin.mediaplayer.YoutubePlayerSlice.rateMin;

/**
 * The class handles the actual content source address decoding for the user selected hymn
 * see https://developer.android.com/codelabs/exoplayer-intro#0
 * <p>
 * This MediaExoPlayerFragment requires its parent AbilitySlice to handle onOrientationChanged()
 * It does not consider onSaveInstanceState(); it uses the speed in the user configuration setting.
 *
 * @author Eng Chong Meng
 */
public class MediaPlayerSlice extends BaseSlice {
    // Tag for the instance state bundle.
    public static final String ATTR_MEDIA_URL = "mediaUrl";
    public static final String ATTR_MEDIA_URLS = "mediaUrls";
    public static final String PREF_PLAYBACK_SPEED = "playBack_speed";

    private static final String sampleUrl = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4";

    // Default playback video url
    private String mediaUrl = sampleUrl;
    private ArrayList<String> mediaUrls = null;

    // Playback ratio of normal speed.
    private float mSpeed = 1.0f;

    private AbilitySlice mContext;
    private static final ConfigurationService configService = UtilActivator.getConfigurationService();

    private Player mPlayer = null;
    private StackLayout mPlayerView;
    private PlayerCallback playbackStateListener;

    /**
     * Create a new instance of MediaExoPlayerFragment, providing "bundle" as an argument.
     */
    public static MediaPlayerSlice getInstance(PacMap args) {
        MediaPlayerSlice playerSlice = new MediaPlayerSlice();
        // playerSlice.setsetArguments(args);
        return playerSlice;
    }

    @Override
    public void onStart(Intent intent) {
        mContext = (AbilitySlice) getContext();

        // mediaUrl = "https://youtu.be/vCKCkc8llaM";  for testing only
        mediaUrl = intent.getStringParam(ATTR_MEDIA_URL);
        mediaUrls = intent.getStringArrayListParam(ATTR_MEDIA_URLS);
        playbackStateListener = new PlayerCallback();

        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        Component mConvertView = inflater.parse(ResourceTable.Layout_media_player_ui, null, false);
        mPlayerView = mConvertView.findComponentById(ResourceTable.Id_playerView);

//        if (container != null)
//            container.setVisibility(Component.VISIBLE);
    }

    @Override
    public void onActive() {
        super.onActive();
        // Load the media and start playback each time .onActive() is called.
        initializePlayer();
    }

    @Override
    public void onInactive() {
        super.onInactive();
        releasePlayer();
    }

    public void initializePlayer() {
        if (mPlayer == null) {
            mPlayer = new Player(mContext);
            mPlayer.setPlayerCallback(playbackStateListener);
        }

        if ((mediaUrls == null) || mediaUrls.isEmpty()) {
            Source mediaItem = buildMediaSource(mediaUrl);
            if (mediaItem != null)
                playMedia(mediaItem);
        }
        else {
            playVideoUrls();
        }
    }

    /**
     * Media play-back takes a lot of resources, so everything should be stopped and released at this time.
     * Release all media-related resources. In a more complicated app this
     * might involve unregistering listeners or releasing audio focus.
     * Save the user defined playback speed
     */
    public void releasePlayer() {
        if (mPlayer != null) {
            mSpeed = mPlayer.getPlaybackSpeed();

            // Audio media player speed is (0.25 >= mSpeed <= 2.0)
            if (mSpeed >= rateMin && mSpeed <= rateMax) {
                configService.setProperty(PREF_PLAYBACK_SPEED, mSpeed);
            }
            mPlayer.release();
            mPlayer = null;
        }
    }

    /**
     * Prepare and play the specified mediaItem
     *
     * @param avSource for playback
     */
    private void playMedia(Source avSource) {
        if (avSource != null) {
            mSpeed = (float) configService.getDouble(PREF_PLAYBACK_SPEED, 1.0);

            setPlaybackSpeed(mSpeed);
            mPlayer.setSource(avSource);
            mPlayer.prepare();
        }
    }

    /**
     * Prepare and playback a list of given video URLs if not empty
     */
    private void playVideoUrls() {
        if ((mediaUrls != null) && !mediaUrls.isEmpty()) {
            List<Source> mediaItems = new ArrayList<>();
            for (String tmpUrl : mediaUrls) {
                mediaItems.add(buildMediaSource(tmpUrl));
            }

            mSpeed = (float) configService.getDouble(PREF_PLAYBACK_SPEED, 1.0);
            setPlaybackSpeed(mSpeed);

            // mPlayer.setMediaItems(mediaItems);
            mPlayer.prepare();
        }
    }

    /**
     * Play the specified videoUrl using android Intent.ACTION_VIEW
     * Use setDataAndType(uri, mimeType) to ensure android has default defined.
     *
     * @param videoUrl videoUrl not playable by ExoPlayer
     */
    private void playVideoUrlExt(String videoUrl) {
        // remove the exoPlayer fragment
        // mContext.getSupportFragmentManager().beginTransaction().remove(this).commit();

        Uri uri = Uri.parse(videoUrl);
        String mimeType = FileBackend.getMimeType(mContext, uri);

        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withBundleName(mContext.getBundleName())
                .withAbilityName(SettingsAbility.class)
                .withAction(Intent.ACTION_PLAY)
                .withUri(uri)
                .withFlags(Intent.FLAG_ABILITY_NEW_MISSION)
                .build();
        intent.setOperation(operation);
        intent.setUriAndType(uri, mimeType);
        mContext.startAbility(intent, 0);
    }

    /**
     * set SimpleExoPlayer playback speed
     *
     * @param speed playback speed: default 1.0f
     */
    private void setPlaybackSpeed(float speed) {
        if (mPlayer != null) {
            mPlayer.setPlaybackSpeed(speed);
        }
    }

    /**
     * Build and return the mediaItem or
     * Proceed to play if it is a youtube link; return null;
     *
     * @param mediaUrl for building the mediaItem
     *
     * @return built mediaItem
     */
    private Source buildMediaSource(String mediaUrl) {
        if (TextUtils.isEmpty(mediaUrl))
            return null;

        Source mediaSource;
        Uri uri = Uri.parse(mediaUrl);
        String mimeType = FileBackend.getMimeType(mContext, uri);
        if (!TextUtils.isEmpty(mimeType) && (mimeType.contains("video") || mimeType.contains("audio"))) {
            mediaSource = new Source(mediaUrl);
        }
        else {
            mediaSource = new Source(mediaUrl);
        }
        return mediaSource;
    }

    /**
     * ExoPlayer playback state listener
     */
    private class PlayerCallback implements Player.IPlayerCallback {
        @Override
        public void onPrepared() {
            if (mPlayer.getVideoWidth() == 0 || mPlayer.getVideoHeight() == 0) {
                float vHeight = 0.62f * aTalkApp.mDisplaySize.height;
                mPlayerView.setLayoutConfig(new StackLayout.LayoutConfig(aTalkApp.mDisplaySize.width, (int) vHeight));
            }
            mPlayer.play();
        }

        @Override
        public void onMessage(int type, int extra) {
        }

        @Override
        public void onError(int errorType, int errorCode) {
            if (Player.PLAYER_ERROR_UNSUPPORTED == errorCode) {
                // Attempt to use other ohos player if media plaer failed to play
                playVideoUrlExt(mediaUrl);
            }
            else {
                aTalkApp.showToastMessage(ResourceTable.String_playback_error);
            }
        }

        @Override
        public void onResolutionChanged(int width, int height) {

        }

        @Override
        public void onPlayBackComplete() {
            aTalkApp.showToastMessage(ResourceTable.String_playback_completed);
        }

        @Override
        public void onRewindToComplete() {

        }

        @Override
        public void onBufferingChange(int percent) {

        }

        @Override
        public void onNewTimedMetaData(Player.MediaTimedMetaData mediaTimedMetaData) {

        }

        @Override
        public void onMediaTimeIncontinuity(Player.MediaTimeInfo mediaTimeInfo) {

        }
    }

    public void setPlayerVisible(boolean show) {
        mPlayerView.setVisibility(show ? Component.VISIBLE : Component.HIDE);
    }

    public boolean isPlayerVisible() {
        return mPlayerView.getVisibility() == Component.VISIBLE;
    }
}

