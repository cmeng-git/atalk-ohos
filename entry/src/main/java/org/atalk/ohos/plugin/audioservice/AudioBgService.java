/*
 * aTalk, android VoIP and Instant Messaging client
 * Copyright 2024 Eng Chong Meng
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
package org.atalk.ohos.plugin.audioservice;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ohos.aafwk.ability.IntentAbility;
import ohos.aafwk.content.Intent;
import ohos.event.commonevent.CommonEventData;
import ohos.event.commonevent.CommonEventManager;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.media.common.Source;
import ohos.media.common.StorageProperty;
import ohos.media.player.Player;
import ohos.media.recorder.Recorder;
import ohos.miscservices.timeutility.Time;
import ohos.rpc.RemoteException;
import ohos.utils.net.Uri;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.aTalkApp;
import org.atalk.persistance.FileBackend;

import timber.log.Timber;

public class AudioBgService extends IntentAbility {
    // Media player actions
    public static final String ACTION_PLAYER_INIT = "player_init";
    public static final String ACTION_PLAYER_START = "player_start";
    public static final String ACTION_PLAYER_PAUSE = "player_pause";
    public static final String ACTION_PLAYER_STOP = "player_stop";
    public static final String ACTION_PLAYER_SEEK = "player_seek";

    // Playback without any UI update
    public static final String ACTION_PLAYBACK_PLAY = "playback_play";
    public static final String ACTION_PLAYBACK_SPEED = "playback_speed";

    // Media player broadcast status parameters
    public static final String PLAYBACK_STATE = "playback_state";
    public static final String PLAYBACK_STATUS = "playback_status";
    public static final String PLAYBACK_DURATION = "playback_duration";
    public static final String PLAYBACK_POSITION = "playback_position";
    public static final String PLAYBACK_URI = "playback_uri";

    private final Map<Uri, Player> uriPlayers = new ConcurrentHashMap<>();

    // Handler for media player playback status broadcast
    private EventHandler mHandlerPlayback;

    private Player mPlayer = null;
    private Uri fileUri;

    private float playbackSpeed = 1.0f;

    public enum PlaybackState {
        init,
        play,
        pause,
        stop
    }

    // ==== Audio recording ====
    public static final String ACTION_RECORDING = "recording";
    public static final String ACTION_CANCEL = "cancel";
    public static final String ACTION_SEND = "send";

    public static final String ACTION_AUDIO_RECORD = "audio_record";
    public static final String ACTION_SMI = "sound_meter_info";

    public static final String URI = "uri";
    public static final String SPL_LEVEL = "spl_level";
    public static final String RECORD_TIMER = "record_timer";

    private File audioFile = null;

    private Recorder mRecorder = null;

    private long startTime = 0L;

    // Handler for Sound Level Meter and Record Timer
    private EventHandler mHandlerRecord;

    // The Google ASR input requirements state that audio input sensitivity should be set such
    // that 90 dB SPL_LEVEL at 1000 Hz yields RMS of 2500 for 16-bit samples,
    // i.e. 20 * log_10 (2500 / mGain) = 90.
    private final double mGain = 2500.0 / Math.pow(10.0, 90.0 / 20.0);

    // For displaying error in calibration.
    public static double mOffsetDB = 0.0f;  //10 Offset for bar, i.e. 0 lit LEDs at 10 dB.
    public static double mDBRange = 70.0f;  //SPL display range.

    private static double mEMA = 1.0; // a temporally filtered version of RMS
    //private double mAlpha =  0.9 Coefficient of IIR smoothing filter for RMS.
    static final private double EMA_FILTER = 0.4;

    public AudioBgService(String name) {
        super(name);
    }

    @Override
    protected void onProcessIntent(Intent intent) {
    }

    @Override
    protected void onCommand(Intent intent, boolean restart, int startId) {
        super.onCommand(intent, restart, startId);

        switch (intent.getAction()) {
            case ACTION_PLAYER_INIT:
                fileUri = intent.getUri();
                playerInit(fileUri);
                break;

            case ACTION_PLAYER_START:
                fileUri = intent.getUri();
                playerStart(fileUri);
                break;

            case ACTION_PLAYER_PAUSE:
                fileUri = intent.getUri();
                playerPause(fileUri);
                break;

            case ACTION_PLAYER_STOP:
                fileUri = intent.getUri();
                playerRelease(fileUri);
                break;

            case ACTION_PLAYER_SEEK:
                fileUri = intent.getUri();
                int seekPosition = intent.getIntParam(PLAYBACK_POSITION, 0);
                playerSeek(fileUri, seekPosition);
                break;

            case ACTION_PLAYBACK_PLAY:
                fileUri = intent.getUri();
                playerPlay(fileUri);
                break;

            case ACTION_PLAYBACK_SPEED:
                String speed = intent.getType();
                if (!TextUtils.isEmpty(speed)) {
                    playbackSpeed = Float.parseFloat(speed);
                    setPlaybackSpeed();
                }
                break;

            case ACTION_RECORDING:
                mHandlerRecord = new EventHandler(EventRunner.create());
                recordAudio();
                break;

            case ACTION_SEND:
                stopTimer();
                stopRecording();
                if (audioFile != null) {
                    // sendBroadcast(FileAccess.getUriForFile(this, audioFile));
                    String filePath = audioFile.getAbsolutePath();
                    sendBroadcast(filePath);
                }
                break;

            case ACTION_CANCEL:
                stopTimer();
                stopRecording();
                if (audioFile != null) {
                    File soundFile = new File(audioFile.getAbsolutePath());
                    soundFile.delete();
                    audioFile = null;
                }
                terminateAbility();
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopTimer();
        stopRecording();

        if (mHandlerPlayback != null) {
            mHandlerPlayback.removeTask(playbackStatus);
            mHandlerPlayback = null;
        }

        for (Uri uri : uriPlayers.keySet()) {
            fileUri = uri;
            playerRelease(uri);
        }
    }

    /* =============================================================
     * Media player handlers
     * ============================================================= */

    /**
     * Create a new media player instance for the specified uri
     *
     * @param uri Media file uri
     *
     * @return true is creation is successful
     */
    public boolean playerCreate(Uri uri) {
        if (uri == null)
            return false;

        if (mHandlerPlayback == null)
            mHandlerPlayback = new EventHandler(EventRunner.create());

        mPlayer = new Player(aTalkApp.getInstance());
        uriPlayers.put(uri, mPlayer);
        // mPlayer.setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build());

        mPlayer.setPlayerCallback(mCallback);
        if (uri.toString().startsWith("https")) {
            mPlayer.setSource(new Source(uri.toString()));
        }
        else {
            mPlayer.setSource(new Source(uri.getDecodedPath()));
        }
        return mPlayer.prepare();
    }

    /**
     * Return the status of current active player if present; keep the state as it
     * else get the media file info and release player to conserve resource
     *
     * @param uri the media file uri
     */
    public void playerInit(Uri uri) {
        if (uri == null)
            return;

        if (mHandlerPlayback == null)
            mHandlerPlayback = new EventHandler(EventRunner.create());

        // Check player status on return to chatSession before start new
        mPlayer = uriPlayers.get(uri);
        if (mPlayer != null) {
            if (mPlayer.isNowPlaying()) {
                playbackState(PlaybackState.play, uri);
                // Cancel and re-sync with only one loop running
                mHandlerPlayback.removeTask(playbackStatus);
                mHandlerPlayback.postTask(playbackStatus, 500);
            }
            else {
                int position = mPlayer.getCurrentTime();
                int duration = mPlayer.getDuration();
                if ((position > 0) && (position <= duration)) {
                    playbackState(PlaybackState.pause, uri);
                }
                else {
                    playerReInit(uri);
                }
            }
        }
        else {
            // Create new to get media info and then release to conserve resource
            if (playerCreate(uri)) {
                playerRelease(uri);
            }
        }
    }

    /**
     * Re-init an existing player and broadcast its state
     *
     * @param uri the media file uri
     */
    private void playerReInit(Uri uri) {
        mPlayer = uriPlayers.get(uri);
        if (mPlayer != null) {
            if (mPlayer.isNowPlaying())
                mPlayer.pause();
            playbackState(PlaybackState.init, uri);
        }
    }

    /**
     * Pause the current player and return the action result
     *
     * @param uri the media file uri
     */
    public void playerPause(Uri uri) {
        if (uri == null)
            return;

        mPlayer = uriPlayers.get(fileUri);
        if (mPlayer == null) {
            playbackState(PlaybackState.stop, uri);
        }
        else if (mPlayer.isNowPlaying()) {
            mPlayer.pause();
            playbackState(PlaybackState.pause, uri);
        }
    }

    /**
     * Start playing back on existing player or create new if none
     * Broadcast the player status at regular interval
     *
     * @param uri the media file uri
     */
    public void playerStart(Uri uri) {
        if (uri == null)
            return;

        mPlayer = uriPlayers.get(uri);
        if (mPlayer == null) {
            if (!playerCreate(uri))
                return;
        }
        else if (mPlayer.isNowPlaying()) {
            return;
        }

        try {
            mPlayer.setPlaybackSpeed(playbackSpeed);
            mPlayer.play();
            playbackState(PlaybackState.play, uri);
        } catch (Exception e) {
            Timber.e("Playback failed: %s", e.getMessage());
            playerRelease(uri);
        }
        mHandlerPlayback.removeTask(playbackStatus);
        mHandlerPlayback.postTask(playbackStatus, 500);
    }

    /**
     * Start playing back on existing player or create new if none
     * Broadcast the player satus at regular interval
     *
     * @param uri the media file uri
     */
    public void playerSeek(Uri uri, int seekPosition) {
        if (uri == null)
            return;

        mPlayer = uriPlayers.get(uri);
        if ((mPlayer == null) && !playerCreate(uri))
            return;

        try {
            mPlayer.rewindTo(seekPosition * 1000L);
            if (!mPlayer.isNowPlaying())
                playbackState(PlaybackState.pause, uri);
        } catch (Exception e) {
            Timber.e("Playback failed");
            playerRelease(uri);
        }
    }

    /**
     * Setting of playback speed is only support in Android.M
     */
    private void setPlaybackSpeed() {
        for (Map.Entry<Uri, Player> entry : uriPlayers.entrySet()) {
            Player player = entry.getValue();
            Uri uri = entry.getKey();
            if (player == null)
                continue;

            try {
                player.setPlaybackSpeed(playbackSpeed);

                // Update player state: play will start upon speed change if it was in pause state
                playbackState(PlaybackState.play, uri);
            } catch (IllegalStateException e) {
                Timber.e("Playback setSpeed failed: %s", e.getMessage());
            }
        }
    }

    /**
     * Release the player resource and remove it from uriPlayers
     *
     * @param uri the media file uri
     */
    private void playerRelease(Uri uri) {
        if (uri == null)
            return;

        mPlayer = uriPlayers.get(uri);
        if (mPlayer != null) {
            playbackState(PlaybackState.stop, uri);
            uriPlayers.remove(uri);

            if (mPlayer.isNowPlaying()) {
                mPlayer.stop();
            }
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private final Player.IPlayerCallback mCallback = new Player.IPlayerCallback() {
        @Override
        public void onPrepared() {
        }

        @Override
        public void onMessage(int type, int extra) {
        }

        @Override
        public void onError(int errorType, int errorCode) {
            terminateAbility();
        }

        @Override
        public void onResolutionChanged(int width, int height) {
        }

        // callback from the specific media player when playback of a media source has completed.
        @Override
        public void onPlayBackComplete() {
            fileUri = getUriByPlayer(mPlayer);
            if (fileUri == null) {
                mPlayer.release();
            }
            else {
                playerRelease(fileUri);
            }
            terminateAbility();
        }

        @Override
        public void onRewindToComplete() {
        }

        @Override
        public void onBufferingChange(int i) {
        }

        @Override
        public void onNewTimedMetaData(Player.MediaTimedMetaData mediaTimedMetaData) {
        }

        @Override
        public void onMediaTimeIncontinuity(Player.MediaTimeInfo mediaTimeInfo) {
        }
    };


    /**
     * Return the uri of the given mp
     *
     * @param mp the media player
     *
     * @return Uri of the player
     */
    private Uri getUriByPlayer(Player mp) {
        for (Map.Entry<Uri, Player> entry : uriPlayers.entrySet()) {
            if (entry.getValue().equals(mp)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Broadcast the relevant info of the media player (uri)
     * a. player state
     * b. player uri file
     * c. playback position
     * d. uri playback duration
     *
     * @param pState player state
     * @param uri media file uri
     */
    private void playbackState(PlaybackState pState, Uri uri) {
        Player xPlayer = uriPlayers.get(uri);
        if (xPlayer != null) {
            Intent intent = new Intent();
            intent.setParam(PLAYBACK_URI, uri);
            intent.setParam(PLAYBACK_STATE, pState);
            intent.setParam(PLAYBACK_POSITION, xPlayer.getCurrentTime());
            intent.setParam(PLAYBACK_DURATION, xPlayer.getDuration());

            CommonEventData eventData = new CommonEventData(intent, 0, PLAYBACK_STATE);
            try {
                CommonEventManager.publishCommonEvent(eventData);
            } catch (RemoteException e) {
                Timber.w("%s", e.getMessage());
            }
            // Timber.d("Audio playback state: %s (%s): %s", pState, xPlayer.getDuration(), uri.getPath());
        }
    }

    /**
     * Broadcast the relevant info of the media playback status (uri); loop@500ms until no active player
     * a. player uri file
     * b. playback position
     * c. uri playback duration
     */
    private final Runnable playbackStatus = new Runnable() {
        public void run() {
            boolean hasActivePlayer = false;

            for (Map.Entry<Uri, Player> entry : uriPlayers.entrySet()) {
                Player playerX = entry.getValue();
                if ((playerX == null) || !playerX.isNowPlaying())
                    continue;

                hasActivePlayer = true;
                // Timber.d("Audio playback state: %s:  %s", playerX.getCurrentPosition(), entry.getKey());

                Intent intent = new Intent();
                intent.setParam(PLAYBACK_URI, entry.getKey());
                intent.setParam(PLAYBACK_POSITION, playerX.getCurrentTime());
                intent.setParam(PLAYBACK_DURATION, playerX.getDuration());
                CommonEventData eventData = new CommonEventData(intent, 0, PLAYBACK_STATE);
                try {
                    CommonEventManager.publishCommonEvent(eventData);
                } catch (RemoteException e) {
                    Timber.w("%s", e.getMessage());
                }
            }

            if (hasActivePlayer)
                mHandlerPlayback.postTask(this, 500);
        }
    };

    /**
     * Playback media audio without any UI update
     * hence mHandlerPlayback not required
     *
     * @param uri the audio file
     */
    public void playerPlay(Uri uri) {
        if (playerCreate(uri)) {
            mPlayer.play();
            uriPlayers.remove(uri);
        }
        mHandlerPlayback = null;
    }

    /* =============================================================
     * Voice recording handlers
     * ============================================================= */
    public void recordAudio() {
        audioFile = createMediaVoiceFile();
        if (audioFile == null) {
            return;
        }

        Source mic = new Source();
        mic.setRecorderAudioSource(Recorder.AudioSource.MIC);

        StorageProperty sFile = new StorageProperty.Builder()
                .setRecorderPath(audioFile.getPath())
                .build();

        mRecorder = new Recorder();
        mRecorder.setSource(mic);
        mRecorder.setOutputFormat(Recorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFormat(Recorder.AudioEncoder.AMR_NB);
        mRecorder.setStorageProperty(sFile);

        if (mRecorder.prepare()) {
            mRecorder.start();
        }

        startTime = Time.getCurrentTime();
        mHandlerRecord.postTask(updateSPL, 0);
    }

    private void stopRecording() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            } catch (RuntimeException ex) {
                /*
                 * Note that a RuntimeException is intentionally thrown to the application, if no
                 * valid audio/video data has been received when stop() is called. This happens
                 * if stop() is called immediately after start().
                 */
                ex.printStackTrace();
            }
        }
    }

    private void stopTimer() {
        if (mHandlerRecord != null) {
            mHandlerRecord.removeTask(updateSPL);
            mHandlerRecord = null;
        }
    }

    private void sendBroadcast(String filePath) {
        Intent intent = new Intent();
        // intent.setDataAndType(uri, "video/3gp");
        intent.setParam(URI, filePath);
        CommonEventData eventData = new CommonEventData(intent, 0, ACTION_AUDIO_RECORD);
        try {
            CommonEventManager.publishCommonEvent(eventData);
        } catch (RemoteException e) {
            Timber.w("%s", e.getMessage());
        }
    }

    private final Runnable updateSPL = new Runnable() {
        public void run() {
            long finalTime = Time.getCurrentTime() - startTime;
            int seconds = (int) (finalTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            String mDuration = String.format(Locale.US, "%02d:%02d", minutes, seconds);

            double mRmsSmoothed = getAmplitudeEMA();
            final double rmsdB = 20.0 * Math.log10(mGain * mRmsSmoothed);

            // The bar has an input range of [0.0 ; 1.0] and 14 segments.
            // Each LED corresponds to 70/14 dB.
            double mSPL = (mOffsetDB + rmsdB) / mDBRange;
            // mBarLevel.setLevel(mSPL);

            Intent intent = new Intent();
            intent.setParam(SPL_LEVEL, mSPL);
            intent.setParam(RECORD_TIMER, mDuration);
            CommonEventData eventData = new CommonEventData(intent, 0, ACTION_SMI);
            try {
                CommonEventManager.publishCommonEvent(eventData);
            } catch (RemoteException e) {
                Timber.w("%s", e.getMessage());
            }
            mHandlerRecord.postTask(this, 100);
        }
    };

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        // Compute a smoothed version for less flickering of the display.
        mEMA = EMA_FILTER * mEMA + (1.0 - EMA_FILTER) * amp;
        return mEMA;
    }

    public double getAmplitude() {
        if (mRecorder != null)
            return (mRecorder.obtainMaxAmplitude());
        else
            return 0;
    }

    /**
     * Create the audio file if it does not exist
     *
     * @return Voice file for saving audio
     */
    private static File createMediaVoiceFile() {
        File voiceFile = null;
        File mediaDir = FileBackend.getaTalkStore(FileBackend.MEDIA_VOICE_SEND, true);

        if (!mediaDir.exists() && !mediaDir.mkdirs()) {
            Timber.w("Fail to create Media voice directory!");
            return null;
        }

        try {
            voiceFile = File.createTempFile("voice-", ".3gp", mediaDir);
        } catch (IOException e) {
            Timber.w("Fail to create Media voice file!");
        }
        return voiceFile;
    }
}
