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
package org.atalk.ohos.gui.call;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;

import org.atalk.impl.neomedia.MediaServiceImpl;
import org.atalk.impl.neomedia.NeomediaActivator;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.service.neomedia.VolumeControl;
import org.atalk.service.neomedia.event.VolumeChangeEvent;
import org.atalk.service.neomedia.event.VolumeChangeListener;

/**
 * AbilitySlice used to control call volume. Key events for volume up and down have to be captured by the parent
 * <code>Ability</code> and passed here, before they get to system audio service. The volume is increased using
 * <code>AudioManager</code> until it reaches maximum level, then we increase the Libjitsi volume gain.
 * The opposite happens when volume is being decreased.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class CallVolumeCtrlSlice extends BaseSlice implements VolumeChangeListener
{
    /**
     * Current volume gain "position" in range from 0 to 10.
     */
    private int position;

    /**
     * Output volume control.
     */
    private VolumeControl volumeControl;

    /**
     * The <code>AudioManager</code> used to control voice call stream volume.
     */
    private AudioManager audioManager;

    @Override
    public void onStart(Intent intent)
    {
        super.onStart(intent);
        audioManager = (AudioManager)  aTalkApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
        MediaServiceImpl mediaService = NeomediaActivator.getMediaServiceImpl();
        if (mediaService != null)
            volumeControl = mediaService.getOutputVolumeControl();
    }

    @Override
    public void onActive()
    {
        super.onActive();
        if (volumeControl == null)
            return;

        float currentVol = volumeControl.getVolume();
        // Default
        if (currentVol < 0) {
            position = 5;
        }
        else {
            position = calcPosition(currentVol);
        }
        volumeControl.addVolumeChangeListener(this);
    }

    @Override
    public void onInactive()
    {
        if (volumeControl != null) {
            volumeControl.removeVolumeChangeListener(this);
        }
        super.onInactive();
    }

    /**
     * Returns current volume index for <code>AudioManager.STREAM_VOICE_CALL</code>.
     *
     * @return current volume index for <code>AudioManager.STREAM_VOICE_CALL</code>.
     */
    private int getAudioStreamVolume()
    {
        return audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
    }

    /**
     * Method should be called by the parent <code>Ability</code> when volume up key is pressed.
     */
    public void onKeyVolUp()
    {
        int controlMode = AudioManager.ADJUST_RAISE;
        if (position < 5) {
            controlMode = AudioManager.ADJUST_SAME;
        }

        int current = getAudioStreamVolume();
        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, controlMode, AudioManager.FLAG_SHOW_UI);
        int newStreamVol = getAudioStreamVolume();

        if (current == newStreamVol) {
            setVolumeGain(position + 1);
        }
        else {
            setVolumeGain(5);
        }
    }

    /**
     * Method should be called by the parent <code>Ability</code> when volume down key is pressed.
     */
    public void onKeyVolDown()
    {
        int controlMode = AudioManager.ADJUST_LOWER;
        if (position > 5) {
            // We adjust the same just to show the gui
            controlMode = AudioManager.ADJUST_SAME;
        }

        int current = getAudioStreamVolume();
        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, controlMode, AudioManager.FLAG_SHOW_UI);
        int newStreamVol = getAudioStreamVolume();

        if (current == newStreamVol) {
            setVolumeGain(position - 1);
        }
        else {
            setVolumeGain(5);
        }
    }

    private int calcPosition(float volumeGain)
    {
        return (int) ((volumeGain / getVolumeCtrlRange()) * 10f);
    }

    private void setVolumeGain(int newPosition)
    {
        float newVolume = getVolumeCtrlRange() * (((float) newPosition) / 10f);
        this.position = calcPosition(volumeControl.setVolume(newVolume));
    }

    @Override
    public void volumeChange(VolumeChangeEvent volumeChangeEvent)
    {
        position = calcPosition(volumeChangeEvent.getLevel() / getVolumeCtrlRange());
        BaseAbility.runOnUiThread(() -> {
            Ability parent = getAbility();
            if (parent == null)
                return;
            aTalkApp.showToastMessage(ResourceTable.String_volume_gain_level, position * 10);
        });
    }

    /**
     * Returns abstract volume control range calculated for volume control min and max values.
     *
     * @return the volume control range calculated for current volume control min and max values.
     */
    private float getVolumeCtrlRange()
    {
        return volumeControl.getMaxValue() - volumeControl.getMinValue();
    }
}
