/*
 * aTalk, ohos VoIP and Instant Messaging client
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
package org.atalk.ohos.plugin.textspeech;

import java.io.File;
import java.io.IOException;

import ohos.aafwk.content.Intent;
import ohos.agp.components.AbsButton;
import ohos.agp.components.Button;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.ai.tts.TtsClient;
import ohos.ai.tts.TtsListener;
import ohos.ai.tts.TtsParams;
import ohos.ai.tts.constants.TtsEvent;
import ohos.media.audio.AudioManager;
import ohos.utils.PacMap;

import net.java.sip.communicator.util.ConfigurationUtils;
import net.sf.fmj.media.Log;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.persistance.FileBackend;

import timber.log.Timber;

public class TTSAbility extends BaseAbility implements Component.ClickedListener, Checkbox.CheckedStateChangedListener {
    private String ttsDelay;
    private TextField mTtsText;
    private TextField mTtsDelay;
    private Button btnPlay;
    private Button btnSave;

    private TtsClient mTTSClient;
    private static TTSAbility mContext;

    private static State mState = State.UNKNOWN;

    public enum State {
        LOADING,
        DOWNLOAD_FAILED,
        ERROR,
        SUCCESS,
        UNKNOWN
    }

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        mContext = this;

        setUIContent(ResourceTable.Layout_tts_main);
        setMainTitle(ResourceTable.String_tts_settings);

        mTtsText = findComponentById(ResourceTable.Id_tts_text);
        mTtsText.addTextObserver(mTextObserver);

        Checkbox cbTts = findComponentById(ResourceTable.Id_tts_enable);
        cbTts.setChecked(ConfigurationUtils.isTtsEnable());
        cbTts.setCheckedStateChangedListener(this);

        TextField mTtsLocale = findComponentById(ResourceTable.Id_tts_locale);
        mTtsDelay = findComponentById(ResourceTable.Id_tts_delay);
        ttsDelay = String.valueOf(ConfigurationUtils.getTtsDelay());
        mTtsDelay.setText(ttsDelay);
        mTtsDelay.addTextObserver(mTextObserver);

        btnPlay = findComponentById(ResourceTable.Id_tts_play);
        btnPlay.setClickedListener(this);
        btnPlay.setEnabled(false);

        btnSave = findComponentById(ResourceTable.Id_tts_save);
        btnSave.setClickedListener(this);
        btnSave.setEnabled(false);

        Button btnOK = findComponentById(ResourceTable.Id_tts_ok);
        btnOK.setClickedListener(this);

        // Perform the dynamic permission request
        aTalk.hasWriteStoragePermission(this, true);
        setState(State.LOADING);

        mTTSClient = TtsClient.getInstance();
        mTTSClient.create(getContext(), ttsListener);
        mTTSClient.setAudioType(AudioManager.AudioVolumeType.STREAM_TTS);

        TtsParams ttsParams = new TtsParams();
        ttsParams.setDeviceId(getOriginalDeviceId());
        mTTSClient.release();
        mTTSClient.init(ttsParams);
    }

    @Override
    protected void onBackground() {
        super.onBackground();
        String tmp = ComponentUtil.toString(mTtsDelay);
        if ((tmp != null) && !ttsDelay.equals(tmp)) {
            ConfigurationUtils.setTtsDelay(Integer.parseInt(tmp));
        }
    }

    @Override
    protected void onStop() {
        if (mTTSClient != null) {
            mTTSClient.stopSpeak();
            mTTSClient.destroy();
        }
        super.onStop();
    }

    private static final TtsListener ttsListener = new TtsListener() {
        @Override
        public void onEvent(int eventType, PacMap pacMap) {
            // Log.info("onEvent:" + eventType);
            if (eventType == TtsEvent.CREATE_TTS_CLIENT_SUCCESS) {
                Log.info("TTS Client create success");
                mContext.setState(State.SUCCESS);
            }
            /*
             * Device without TTS engine will cause aTalk to crash; Check to see if we have TTS voice data
             * Launcher the voice data verifier.
             */
            else if (eventType == TtsEvent.CREATE_TTS_CLIENT_FAILED) {
                Log.info("TTS Client create failed");
                mContext.setState(State.ERROR);
            }
        }

        @Override
        public void onStart(String utteranceId) {
            // Log.info(utteranceId + " audio synthesis begins");
        }

        @Override
        public void onProgress(String utteranceId, byte[] audioData, int progress) {
            // Log.info(utteranceId + " audio synthesis progress:" + progress);
        }

        @Override
        public void onFinish(String utteranceId) {
            // Log.info(utteranceId + " audio synthesis completed");
        }

        @Override
        public void onSpeechStart(String utteranceId) {
            // Log.info(utteranceId + " begins to speech");
        }

        @Override
        public void onSpeechProgressChanged(String utteranceId, int progress) {
            // Log.info(utteranceId + " speech progress:" + progress);
        }

        @Override
        public void onSpeechFinish(String utteranceId) {
            // Log.info(utteranceId + " speech completed");
        }

        @Override
        public void onError(String utteranceId, String errorMessage) {
            // Log.info(utteranceId + " errorMessage: " + errorMessage);
        }
    };

    @Override
    public void onClick(Component v) {
        String ttsText = ComponentUtil.toString(mTtsText);
        switch (v.getId()) {
            case ResourceTable.Id_tts_play:
                if (ttsText != null) {
                    String mUtteranceID = "toTts";
                    if (ttsText.length() > 512) {
                        mTTSClient.speakLongText(ttsText, mUtteranceID);
                    } else {
                        mTTSClient.speakText(ttsText, mUtteranceID);
                    }
                }
                break;

            case ResourceTable.Id_tts_save:
                if (ttsText != null)
                    saveToAudioFile(ttsText);
                break;

            case ResourceTable.Id_tts_ok:
                terminateAbility();
                break;
        }
    }

    @Override
    public void onCheckedChanged(AbsButton absButton, boolean isChecked) {
        if (absButton.getId() == ResourceTable.Id_tts_enable) {
            ConfigurationUtils.setTtsEnable(isChecked);
        }
    }

    /**
     * Sets the UI state.
     *
     * @param state The current state.
     */
    private void setState(State state) {
        mState = state;
        if (mState == State.LOADING) {
            findComponentById(ResourceTable.Id_loading).setVisibility(Component.VISIBLE);
            findComponentById(ResourceTable.Id_success).setVisibility(Component.HIDE);
        } else {
            findComponentById(ResourceTable.Id_loading).setVisibility(Component.HIDE);
            findComponentById(ResourceTable.Id_success).setVisibility(Component.VISIBLE);
        }
    }

    Text.TextObserver mTextObserver = new Text.TextObserver() {
        @Override
        public void onTextUpdated(String text, int start, int before, int count) {
            boolean enable = (State.SUCCESS == mState) && (text.length() > 0);
            btnPlay.setEnabled(enable);
            btnSave.setEnabled(enable);

            float alpha = enable ? 1.0f : 0.5f;
            btnPlay.setAlpha(alpha);
            btnSave.setAlpha(alpha);
        }
    };

    private void saveToAudioFile(String text) {
        // Create tts audio file
        File ttsFile = createTtsSpeechFile();
        if (ttsFile == null) {
            return;
        }

        String audioFilename = ttsFile.getAbsolutePath();
//        mTTSClient.synthesizeToFile(text, null, new File(audioFilename), mUtteranceID);
//        mTTSClient.setOnUtteranceProgressListener(new UtteranceProgressListener() {
//            @Override
//            public void onStart(String utteranceId) {
//            }
//
//            @Override
//            public void onDone(String utteranceId) {
//                if (utteranceId.equals(mUtteranceID)) {
//                    aTalkApp.showToastMessage("Saved to " + audioFilename);
//                }
//            }
//
//            @Override
//            public void onError(String utteranceId) {
//            }
//        });
    }

    /**
     * Create the audio file if it does not exist
     *
     * @return Voice file for saving audio
     */
    private static File createTtsSpeechFile() {
        File ttsFile = null;
        File mediaDir = FileBackend.getaTalkStore(FileBackend.MEDIA_VOICE_SEND, true);
        if (!mediaDir.exists() && !mediaDir.mkdirs()) {
            Timber.d("Fail to create Media voice directory!");
            return null;
        }

        try {
            ttsFile = File.createTempFile("tts_", ".wav", mediaDir);
        } catch (IOException e) {
            Timber.d("Fail to create Media voice file!");
        }
        return ttsFile;
    }
}