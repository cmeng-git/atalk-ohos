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
package org.atalk.ohos.gui.settings;

import ohos.data.preferences.Preferences;

import org.apache.http.util.TextUtils;
import org.atalk.impl.neomedia.MediaServiceImpl;
import org.atalk.impl.neomedia.NeomediaActivator;
import org.atalk.impl.neomedia.device.AudioSystem;
import org.atalk.impl.neomedia.device.DeviceConfiguration;
import org.atalk.impl.neomedia.device.DeviceSystem;
import org.atalk.impl.neomedia.device.util.OhosCamera;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.settings.util.SummaryMapper;

/**
 * The preferences fragment implements for Expert settings.
 *
 * @author Eng Chong Meng
 */
public class ExpertSettingsSlice extends BasePreferenceSlice
        implements Preferences.PreferencesObserver {
    // Advance video/audio settings
    private static final String PC_KEY_ADVANCED = "pref.cat.settings.advanced";
    // private static final String PC_KEY_VIDEO = "pref.cat.settings.video";
    // private static final String PC_KEY_AUDIO = "pref.cat.settings.audio";

    // Audio settings
    private static final String P_KEY_AUDIO_ECHO_CANCEL = "pref.key.audio.echo_cancel";
    private static final String P_KEY_AUDIO_AGC = "pref.key.audio.agc";
    private static final String P_KEY_AUDIO_DENOISE = "pref.key.audio.denoise";

    // Hardware encoding/decoding (>=API16)
    private static final String P_KEY_VIDEO_HW_ENCODE = "neomedia.android.hw_encode";
    private static final String P_KEY_VIDEO_HW_DECODE = "neomedia.android.hw_decode";
    // Direct surface encoding(hw encoding required and API18)
    private static final String P_KEY_VIDEO_ENC_DIRECT_SURFACE = "neomedia.android.surface_encode";
    private static final String P_KEY_VIDEO_DEC_DIRECT_SURFACE = "neomedia.android.surface_decode";

    // Video advanced settings
    private static final String P_KEY_VIDEO_LIMIT_FPS = "pref.key.video.limit_fps";
    private static final String P_KEY_VIDEO_TARGET_FPS = "pref.key.video.frame_rate";
    private static final String P_KEY_VIDEO_MAX_BANDWIDTH = "pref.key.video.max_bandwidth";
    private static final String P_KEY_VIDEO_BITRATE = "pref.key.video.bitrate";

    /**
     * The device configuration
     */
    private DeviceConfiguration mDeviceConfig;
    private AudioSystem mAudioSystem;
    private PreferenceScreen mPreferenceScreen;
    private Preferences shPrefs;

    /**
     * Summary mapper used to display preferences values as summaries.
     */
    private final SummaryMapper summaryMapper = new SummaryMapper();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the expert_preferences from an XML resource
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.expert_preferences, rootKey);
        setPrefTitle(ResourceTable.String_settings_expert);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActive() {
        super.onActive();

        mPreferenceScreen = getPreferenceScreen();
        shPrefs = BaseAbility.getPreferenceStore();
        shPrefs.registerObserver(this);
        shPrefs.registerObserver(summaryMapper);

        if (!aTalk.disableMediaServiceOnFault) {
            MediaServiceImpl mediaServiceImpl = NeomediaActivator.getMediaServiceImpl();
            if (mediaServiceImpl != null) {
                mDeviceConfig = mediaServiceImpl.getDeviceConfiguration();
                mAudioSystem = mDeviceConfig.getAudioSystem();
            }
            else {
                // Do not proceed if mediaServiceImpl == null; else system crashes on NPE
                disableMediaOptions();
                return;
            }
            // Audio section
            initAudioPreferences();

            // Video section
            initVideoPreferences();
        }
        else {
            disableMediaOptions();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        shPrefs.unregisterObserver(this);
        shPrefs.unregisterObserver(summaryMapper);
        super.onStop();
    }

    /**
     * Initializes audio settings.
     */
    private void initAudioPreferences() {
        int audioSystemFeatures = mAudioSystem.getFeatures();

        // Echo cancellation
        CheckBoxPreference echoCancelPRef = findPreference(P_KEY_AUDIO_ECHO_CANCEL);
        boolean hasEchoFeature = (AudioSystem.FEATURE_ECHO_CANCELLATION & audioSystemFeatures) != 0;
        echoCancelPRef.setEnabled(hasEchoFeature);
        echoCancelPRef.setChecked(hasEchoFeature && mAudioSystem.isEchoCancel());

        // Automatic gain control
        CheckBoxPreference agcPRef = findPreference(P_KEY_AUDIO_AGC);
        boolean hasAgcFeature = (AudioSystem.FEATURE_AGC & audioSystemFeatures) != 0;
        agcPRef.setEnabled(hasAgcFeature);
        agcPRef.setChecked(hasAgcFeature && mAudioSystem.isAutomaticGainControl());

        // Denoise
        CheckBoxPreference denoisePref = findPreference(P_KEY_AUDIO_DENOISE);
        boolean hasDenoiseFeature = (AudioSystem.FEATURE_DENOISE & audioSystemFeatures) != 0;
        denoisePref.setEnabled(hasDenoiseFeature);
        denoisePref.setChecked(hasDenoiseFeature && mAudioSystem.isDenoise());
    }

    // Disable all media options when MediaServiceImpl is not initialized due to text-relocation in ffmpeg
    private void disableMediaOptions() {
        // android OS cannot support removal of nested PreferenceCategory, so just disable all advance settings
        PreferenceCategory myPrefCat = findPreference(PC_KEY_ADVANCED);
        if (myPrefCat != null) {
            mPreferenceScreen.removePreference(myPrefCat);

            // myPrefCat = (PreferenceCategory) findPreference(PC_KEY_VIDEO);
            // if (myPrefCat != null) {
            //     preferenceScreen.removePreference(myPrefCat);
            // }

            // myPrefCat = (PreferenceCategory) findPreference(PC_KEY_AUDIO);
            // if (myPrefCat != null) {
            //     preferenceScreen.removePreference(myPrefCat);
            // }
        }
    }

    /**
     * Initializes video preferences part.
     */
    private void initVideoPreferences() {
        updateHwCodecStatus();

        // Frame rate
        String defaultFpsStr = "20";
        CheckBoxPreference limitFpsPref = findPreference(P_KEY_VIDEO_LIMIT_FPS);
        int targetFps = mDeviceConfig.getFrameRate();
        limitFpsPref.setChecked(targetFps != -1);

        EditTextPreference targetFpsPref = findPreference(P_KEY_VIDEO_TARGET_FPS);
        targetFpsPref.setText(targetFps != DeviceConfiguration.DEFAULT_VIDEO_FRAMERATE
                ? Integer.toString(targetFps) : defaultFpsStr);

        // Max bandwidth
        int videoMaxBandwith = mDeviceConfig.getVideoRTPPacingThreshold();
        // Accord the current value with the maximum allowed value. Fixes existing
        // configurations that have been set to a number larger than the advised maximum value.
        videoMaxBandwith = (Math.min(videoMaxBandwith, 999));

        EditTextPreference maxBWPref = findPreference(P_KEY_VIDEO_MAX_BANDWIDTH);
        maxBWPref.setText(Integer.toString(videoMaxBandwith));

        // Video bitrate
        int bitrate = mDeviceConfig.getVideoBitrate();
        EditTextPreference bitratePref = findPreference(P_KEY_VIDEO_BITRATE);
        bitratePref.setText(Integer.toString(bitrate));

        // Summaries mapping
        summaryMapper.includePreference(targetFpsPref, defaultFpsStr);
        summaryMapper.includePreference(maxBWPref, Integer.toString(DeviceConfiguration.DEFAULT_VIDEO_RTP_PACING_THRESHOLD));
        summaryMapper.includePreference(bitratePref, Integer.toString(DeviceConfiguration.DEFAULT_VIDEO_BITRATE));
    }

    /**
     * Update the android codec preferences enabled status based on camera device selected option.
     * <p>
     * Note: Current aTalk implementation requires direct surface option to be enabled in order
     * for fmj to use the android codec if enabled. So couple both the surface and codec options
     *
     * @see ConfigWidgetUtil#handlePersistValue(final Object value)
     */
    private void updateHwCodecStatus() {
        OhosCamera selectedCamera = OhosCamera.getSelectedCameraDevInfo();

        // MediaCodecs only work with OhosCameraSystem(at least for now)
        boolean enableMediaCodecs = (selectedCamera != null)
                && DeviceSystem.LOCATOR_PROTOCOL_OHOSCAMERA.equals(selectedCamera.getCameraProtocol());

        findPreference(P_KEY_VIDEO_HW_ENCODE).setEnabled(enableMediaCodecs);
        findPreference(P_KEY_VIDEO_HW_DECODE).setEnabled(enableMediaCodecs);

        findPreference(P_KEY_VIDEO_ENC_DIRECT_SURFACE).setEnabled(false);
        findPreference(P_KEY_VIDEO_DEC_DIRECT_SURFACE).setEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    public void onChange(Preferences preg, String key) {
        // Echo cancellation
        switch (key) {
            case P_KEY_AUDIO_ECHO_CANCEL:
                mAudioSystem.setEchoCancel(preg.getBoolean(P_KEY_AUDIO_ECHO_CANCEL, true));
                break;
            // Auto gain control
            case P_KEY_AUDIO_AGC:
                mAudioSystem.setAutomaticGainControl(preg.getBoolean(P_KEY_AUDIO_AGC, true));
                break;
            // Noise reduction
            case P_KEY_AUDIO_DENOISE:
                mAudioSystem.setDenoise(preg.getBoolean(P_KEY_AUDIO_DENOISE, true));
                break;
            // Frame rate
            case P_KEY_VIDEO_LIMIT_FPS:
            case P_KEY_VIDEO_TARGET_FPS:
                boolean isLimitOn = preg.getBoolean(P_KEY_VIDEO_LIMIT_FPS, false);
                if (isLimitOn) {
                    EditTextPreference fpsPref = findPreference(P_KEY_VIDEO_TARGET_FPS);
                    String fpsStr = fpsPref.getText();
                    if (!TextUtils.isEmpty(fpsStr)) {
                        int fps = Integer.parseInt(fpsStr);
                        if (fps > 30) {
                            fps = 30;
                        }
                        else if (fps < 5) {
                            fps = 5;
                        }
                        mDeviceConfig.setFrameRate(fps);
                        fpsPref.setText(Integer.toString(fps));
                    }
                }
                else {
                    mDeviceConfig.setFrameRate(DeviceConfiguration.DEFAULT_VIDEO_FRAMERATE);
                }
                break;
            // Max bandwidth
            case P_KEY_VIDEO_MAX_BANDWIDTH:
                String resStr = preg.getString(P_KEY_VIDEO_MAX_BANDWIDTH, null);
                if (!TextUtils.isEmpty(resStr)) {
                    int maxBw = Integer.parseInt(resStr);
                    if (maxBw > 999) {
                        maxBw = 999;
                    }
                    else if (maxBw < 1) {
                        maxBw = 1;
                    }
                    mDeviceConfig.setVideoRTPPacingThreshold(maxBw);
                }
                else {
                    mDeviceConfig.setVideoRTPPacingThreshold(DeviceConfiguration.DEFAULT_VIDEO_RTP_PACING_THRESHOLD);
                }
                ((EditTextPreference) findPreference(P_KEY_VIDEO_MAX_BANDWIDTH))
                        .setText(Integer.toString(mDeviceConfig.getVideoRTPPacingThreshold()));
                break;
            // Video bit rate
            case P_KEY_VIDEO_BITRATE:
                String bitrateStr = preg.getString(P_KEY_VIDEO_BITRATE, "");
                int bitrate = 0;
                if (bitrateStr != null) {
                    bitrate = !TextUtils.isEmpty(bitrateStr)
                            ? Integer.parseInt(bitrateStr) : DeviceConfiguration.DEFAULT_VIDEO_BITRATE;
                }
                if (bitrate < 1) {
                    bitrate = 1;
                }
                mDeviceConfig.setVideoBitrate(bitrate);
                ((EditTextPreference) findPreference(P_KEY_VIDEO_BITRATE)).setText(Integer.toString(bitrate));
                break;
        }
    }
}